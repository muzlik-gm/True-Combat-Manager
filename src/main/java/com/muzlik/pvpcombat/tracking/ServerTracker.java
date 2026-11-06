package com.muzlik.pvpcombat.tracking;

import com.muzlik.pvpcombat.core.PvPCombatPlugin;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sends periodic heartbeat requests to the True Combat Manager tracking website.
 * Reports server IP, online status, player count, version, and other stats.
 *
 * <p>The API endpoint is hardcoded to prevent tampering. Authentication is done
 * via an {@code X-API-Key} header — only the plugin (with the correct key) can
 * submit data; the tracking website rejects all unauthenticated requests.</p>
 */
public class ServerTracker {

    /** Hardcoded tracking website API endpoint — cannot be overridden via config. */
    private static final String API_ENDPOINT = "https://truecombat-tracker.vercel.app/api/servers/heartbeat";

    /** Hardcoded API key for heartbeat authentication — must match the tracking website's TRACKER_API_KEY. */
    private static final String API_KEY = "tcm_live_re5r7658yiugkhvfir86669780yougyi";

    private final PvPCombatPlugin plugin;
    private final Logger logger;
    private final HttpClient httpClient;
    private final String serverId;

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_BASE_DELAY_MS = 1000;

    private BukkitTask heartbeatTask;
    private long startTime;

    public ServerTracker(PvPCombatPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.startTime = System.currentTimeMillis();

        // Fully automated: resolve server address via Pterodactyl panel API (ip_alias domain),
        // falling back to IP auto-detection if panel is not configured.
        this.serverId = resolveAddress() + ":" + Bukkit.getPort();
        logger.info("Server ID generated: " + this.serverId);

        // Create HTTP client with a longer connect timeout; Vercel serverless
        // cold starts can take several seconds on the first request.
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

        logger.info("ServerTracker initialized with hardcoded API endpoint: " + API_ENDPOINT);
    }

    /**
     * Starts the periodic heartbeat task.
     */
    public void start() {
        long intervalSeconds = 60;
        long intervalTicks = intervalSeconds * 20L;

        heartbeatTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::sendHeartbeat, intervalTicks, intervalTicks);
        logger.info("Server tracking heartbeat started (interval: " + intervalSeconds + "s, serverId: " + serverId + ")");

        // Send an immediate heartbeat on startup
        sendHeartbeat();
    }

    /**
     * Stops the heartbeat task.
     */
    public void stop() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel();
            heartbeatTask = null;
            logger.info("Server tracking heartbeat stopped.");
        }
    }

    /**
     * Sends a single heartbeat request asynchronously to the hardcoded endpoint.
     * Retries with exponential backoff on transient failures (timeouts, 5xx,
     * network errors) so a single slow Vercel cold start does not break
     * tracking permanently.
     */
    private void sendHeartbeat() {
        ServerInfo info = buildServerInfo();
        String json = info.toJson();
        sendHeartbeat(0, json);
    }

    private void sendHeartbeat(int attempt, String json) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_ENDPOINT))
            .header("Content-Type", "application/json")
            .header("X-API-Key", API_KEY)
            .header("User-Agent", "TrueCombatManager/" + plugin.getDescription().getVersion())
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .timeout(Duration.ofSeconds(20))
            .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept(response -> {
                int status = response.statusCode();
                if (status >= 200 && status < 300) {
                    logger.fine("Heartbeat sent successfully (HTTP " + status + ")");
                } else if (status == 401) {
                    logger.warning("Heartbeat rejected by server (HTTP 401): Invalid API key. "
                        + "Check TRACKER_API_KEY on the tracking website.");
                } else {
                    logger.warning("Heartbeat rejected by server (HTTP " + status + "): " + response.body());
                }
            }).exceptionally(ex -> {
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                if (attempt < MAX_RETRIES) {
                    long delayMs = RETRY_BASE_DELAY_MS * (1L << attempt);
                    logger.warning("Heartbeat attempt " + (attempt + 1) + " failed, retrying in "
                        + delayMs + "ms: " + cause.getMessage());
                    Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                        sendHeartbeat(attempt + 1, json);
                    }, delayMs / 50L);
                    return null;
                }
                logger.warning("Failed to send heartbeat to " + API_ENDPOINT
                    + " after " + (MAX_RETRIES + 1) + " attempts: " + cause.getMessage());
                return null;
            });
    }

    /**
     * Resolves the server address for the server ID and heartbeat host field.
     * Fully automated — no manual configuration required.
     * <ol>
     *   <li>Pterodactyl panel API — queries {@code /api/client/servers/{uuid}?include=allocations}
     *       and reads {@code ip_alias} (domain name like play.pixelmc.com) from the primary allocation</li>
     *   <li>Pterodactyl {@code SERVER_IP} environment variable</li>
     *   <li>{@link Bukkit#getIp()} — if set in server.properties</li>
     *   <li>Local network interfaces — filters out 0.0.0.0, loopback, link-local</li>
     *   <li>External IP detection services — queries api.ipify.org, icanhazip.com, checkip.amazonaws.com</li>
     * </ol>
     * Only falls back to 0.0.0.0 if everything else fails.
     */
    private String resolveAddress() {
        // 1. Pterodactyl panel API — read ip_alias (domain name) from primary allocation
        String panelDomain = resolveViaPterodactylApi();
        if (panelDomain != null && !panelDomain.isEmpty()) {
            return panelDomain;
        }

        // 2. Pterodactyl SERVER_IP environment variable
        String pterodactylIp = System.getenv("SERVER_IP");
        if (isUsableIp(pterodactylIp)) return pterodactylIp;

        // 3. Bukkit.getIp() — set in server.properties
        String ip = Bukkit.getIp();
        if (isUsableIp(ip)) return ip;

        // 4. Local network interfaces — filters out 0.0.0.0, loopback, link-local
        try {
            java.util.Enumeration<java.net.NetworkInterface> ifaces =
                    java.net.NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                java.net.NetworkInterface iface = ifaces.nextElement();
                if (iface == null || !iface.isUp() || iface.isLoopback() || iface.isVirtual()) continue;
                java.util.Enumeration<java.net.InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    java.net.InetAddress addr = addrs.nextElement();
                    if (addr == null || addr.isLoopbackAddress() || !(addr instanceof java.net.Inet4Address)) continue;
                    String addrStr = addr.getHostAddress();
                    if (isUsableIp(addrStr)) return addrStr;
                }
            }
        } catch (Exception ignored) {
        }

        // 5. External IP detection services — for cloud hosts that bind to 0.0.0.0
        String detectedIp = null;
        String[] services = {
            "https://api.ipify.org",
            "https://icanhazip.com",
            "https://checkip.amazonaws.com"
        };
        for (String service : services) {
            try {
                java.net.URI uri = java.net.URI.create(service);
                java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(java.time.Duration.ofSeconds(5))
                    .build();
                java.net.http.HttpResponse<String> res = httpClient.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() == 200) {
                    String detected = res.body().trim();
                    if (isUsableIp(detected)) {
                        detectedIp = detected;
                        break;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (detectedIp != null) {
            // Try reverse DNS lookup to get a domain name (e.g. play.pixelmc.com)
            try {
                java.net.InetAddress addr = java.net.InetAddress.getByName(detectedIp);
                String hostName = addr.getHostName();
                if (hostName != null && !hostName.equals(detectedIp) && hostName.contains(".")) {
                    return hostName;
                }
            } catch (Exception ignored) {
            }
            return detectedIp;
        }

        return "0.0.0.0";
    }

    /**
     * Queries the Pterodactyl panel API for the server's allocations and returns
     * the {@code ip_alias} (domain name) of the primary/default allocation.
     * Requires {@code PTERODACTYL_PANEL_URL}, {@code PTERODACTYL_API_KEY}, and
     * {@code PTERODACTYL_SERVER_UUID} environment variables to be set.
     */
    private String resolveViaPterodactylApi() {
        String panelUrl = System.getenv("PTERODACTYL_PANEL_URL");
        String apiKey = System.getenv("PTERODACTYL_API_KEY");
        String serverUuid = System.getenv("PTERODACTYL_SERVER_UUID");

        if (panelUrl == null || apiKey == null || serverUuid == null) {
            return null;
        }

        panelUrl = panelUrl.trim().replaceAll("/+$", "");
        String apiEndpoint = panelUrl + "/api/client/servers/" + serverUuid + "?include=allocations";

        try {
            java.net.URI uri = java.net.URI.create(apiEndpoint);
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "Application/vnd.pterodactyl.v1+json")
                .timeout(java.time.Duration.ofSeconds(10))
                .build();

            java.net.http.HttpResponse<String> res = httpClient.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                logger.warning("Pterodactyl API returned HTTP " + res.statusCode() + " when fetching server allocations");
                return null;
            }

            String body = res.body();
            if (body == null || body.isEmpty()) {
                return null;
            }

            // Find the allocations array in the JSON response
            int allocIdx = body.indexOf("\"allocations\"");
            if (allocIdx == -1) {
                return null;
            }

            // Find the data array within allocations
            int dataIdx = body.indexOf("\"data\"", allocIdx);
            if (dataIdx == -1) {
                return null;
            }

            // Extract the allocations data array
            int arrayStart = body.indexOf("[", dataIdx);
            int arrayEnd = body.indexOf("]", arrayStart);
            if (arrayStart == -1 || arrayEnd == -1) {
                return null;
            }

            String allocationsJson = body.substring(arrayStart, arrayEnd + 1);

            // Split into individual allocation objects
            String[] allocations = splitJsonArray(allocationsJson);
            String primaryIpAlias = null;
            String fallbackIpAlias = null;

            for (String alloc : allocations) {
                String ipAlias = extractJsonField(alloc, "ip_alias");
                String isDefault = extractJsonField(alloc, "is_default");

                if (ipAlias != null && !ipAlias.isEmpty() && !"null".equalsIgnoreCase(ipAlias)) {
                    if ("true".equalsIgnoreCase(isDefault)) {
                        return ipAlias; // primary allocation — use immediately
                    }
                    if (fallbackIpAlias == null) {
                        fallbackIpAlias = ipAlias;
                    }
                }
            }

            if (fallbackIpAlias != null) {
                return fallbackIpAlias;
            }

        } catch (Exception e) {
            logger.warning("Pterodactyl API lookup failed: " + e.getMessage());
        }

        return null;
    }

    /**
     * Splits a JSON array string into individual object strings.
     */
    private String[] splitJsonArray(String jsonArray) {
        java.util.ArrayList<String> items = new java.util.ArrayList<>();
        int depth = 0;
        int start = -1;
        for (int i = 0; i < jsonArray.length(); i++) {
            char c = jsonArray.charAt(i);
            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start != -1) {
                    items.add(jsonArray.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return items.toArray(new String[0]);
    }

    /**
     * Extracts a JSON field value (string) from a JSON object string.
     */
    private String extractJsonField(String json, String field) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private static boolean isUsableIp(String ip) {
        if (ip == null || ip.isEmpty()) return false;
        if (ip.equals("0.0.0.0")) return false;
        if (ip.startsWith("127.")) return false;
        if (ip.startsWith("169.254.")) return false; // link-local
        return true;
    }

    /**
     * Builds the current ServerInfo from the running server.
     */
    private ServerInfo buildServerInfo() {
        String host = resolveAddress();
        int port = Bukkit.getPort();
        String version = Bukkit.getVersion();
        String bukkitVersion = Bukkit.getBukkitVersion();
        String minecraftVersion = extractMinecraftVersion(version);
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        int maxPlayers = Bukkit.getMaxPlayers();
        long uptime = System.currentTimeMillis() - startTime;
        String pluginVersion = plugin.getDescription().getVersion();
        String serverName = "Minecraft Server (" + host + ":" + port + ")";

        return new ServerInfo(serverId, serverName, host, port,
            version, bukkitVersion, minecraftVersion,
            onlinePlayers, maxPlayers, true,
            uptime, pluginVersion);
    }

    /**
     * Extracts the Minecraft version from a Bukkit version string.
     * Handles formats like "1.20.4", "MC: 1.20.4", "git-Paper-...", etc.
     */
    private String extractMinecraftVersion(String version) {
        if (version == null || version.isEmpty()) {
            return "unknown";
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+\\.\\d+(?:\\.\\d+)?)").matcher(version);
        if (m.find()) {
            return m.group(1);
        }
        return version;
    }

    /**
     * Returns the server ID being used for tracking.
     */
    public String getServerId() {
        return serverId;
    }

    /**
     * Returns whether tracking is currently enabled.
     */
    public boolean isTracking() {
        return heartbeatTask != null;
    }

    /**
     * Returns the hardcoded API endpoint URL.
     */
    public static String getApiEndpoint() {
        return API_ENDPOINT;
    }
}
