package com.muzlik.pvpcombat.tracking;

import com.muzlik.pvpcombat.core.PvPCombatPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Logger;

/**
 * Sends periodic heartbeat requests to the True Combat Manager tracking website.
 * Reports server address, online status, player count, version, and other stats.
 *
 * <p>The API endpoint is hardcoded to prevent tampering. Authentication is done
 * via an {@code X-API-Key} header — only the plugin (with the correct key) can
 * submit data; the tracking website rejects all unauthenticated requests.</p>
 *
 * <p>Server address resolution is fully automated — no manual configuration required.
 * Resolution order:
 * <ol>
 *   <li>Player handshake domain (via ProtocolLib) — the domain players typed, e.g. play.pixelmc.com</li>
 *   <li>External IP detection services — queries api.ipify.org, icanhazip.com, checkip.amazonaws.com</li>
 *   <li>Reverse DNS lookup — attempts to resolve a domain name from the detected IP</li>
 *   <li>Pterodactyl {@code SERVER_IP} environment variable (auto-set by Wings)</li>
 *   <li>{@link Bukkit#getIp()} — if set in server.properties</li>
 *   <li>Local network interfaces — filters out 0.0.0.0, loopback, link-local</li>
 * </ol>
 * Only falls back to 0.0.0.0 if everything fails.</p>
 *
 * <p>The first heartbeat is sent when the first player joins (not on startup).
 * The server address is re-resolved every 24 hours to pick up domain changes.</p>
 */
public class ServerTracker implements Listener {

    /** Hardcoded tracking website API endpoint — cannot be overridden via config. */
    private static final String API_ENDPOINT = "https://truecombat-tracker.vercel.app/api/servers/heartbeat";

    /** Hardcoded API key for heartbeat authentication — must match the tracking website's TRACKER_API_KEY. */
    private static final String API_KEY = "tcm_live_re5r7658yiugkhvfir86669780yougyi";

    private final PvPCombatPlugin plugin;
    private final Logger logger;
    private final HttpClient httpClient;
    private final HandshakeDomainTracker handshakeTracker;

    private final String serverId;
    private final long heartbeatIntervalTicks;   // 5 minutes = 6000 ticks
    private final long domainRecheckIntervalTicks; // 24 hours = 1728000 ticks

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_BASE_DELAY_MS = 1000;

    private BukkitTask heartbeatTask;
    private BukkitTask domainRecheckTask;
    private long startTime;
    private boolean firstPlayerJoined = false;

    public ServerTracker(PvPCombatPlugin plugin, HandshakeDomainTracker handshakeTracker) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.handshakeTracker = handshakeTracker;
        this.startTime = System.currentTimeMillis();

        // Fully automated address resolution — handshake domain first, then IP fallbacks
        String resolvedAddress = resolveAddress();
        this.serverId = resolvedAddress + ":" + Bukkit.getPort();
        logger.info("Server ID generated: " + this.serverId);

        // 5 minutes = 300 seconds = 6000 ticks
        this.heartbeatIntervalTicks = 300 * 20L;
        // 24 hours = 86400 seconds = 1728000 ticks
        this.domainRecheckIntervalTicks = 86400 * 20L;

        // Create HTTP client with a longer connect timeout; Vercel serverless
        // cold starts can take several seconds on the first request.
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

        logger.info("ServerTracker initialized with hardcoded API endpoint: " + API_ENDPOINT);
    }

    /**
     * Registers the PlayerJoinEvent listener so the first heartbeat is sent
     * when the first player joins (not on startup).
     */
    public void registerListeners() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Starts the periodic heartbeat task (5 minute interval) and the daily
     * domain recheck task (24 hour interval).
     */
    public void start() {
        // Heartbeat every 5 minutes, starting after the first player join
        // (the first immediate heartbeat is triggered by onPlayerJoin)
        heartbeatTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::sendHeartbeat,
            heartbeatIntervalTicks, heartbeatIntervalTicks);

        // Re-resolve the server address every 24 hours to pick up domain changes
        domainRecheckTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::recheckDomain,
            domainRecheckIntervalTicks, domainRecheckIntervalTicks);

        logger.info("Server tracking started (heartbeat every 5 min, domain recheck every 24h, serverId: " + serverId + ")");
    }

    /**
     * Stops all scheduled tasks.
     */
    public void stop() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel();
            heartbeatTask = null;
        }
        if (domainRecheckTask != null) {
            domainRecheckTask.cancel();
            domainRecheckTask = null;
        }
        logger.info("Server tracking heartbeat stopped.");
    }

    /**
     * Called when a player joins. Sends the first heartbeat immediately on
     * the first player join, then subsequent heartbeats follow the 5-minute
     * schedule from the start() task.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!firstPlayerJoined) {
            firstPlayerJoined = true;
            logger.info("First player joined (" + event.getPlayer().getName() + "), sending initial heartbeat");
            sendHeartbeat();
        }
    }

    /**
     * Re-resolves the server address every 24 hours to pick up domain changes
     * (e.g. a player connecting via a different subdomain).
     */
    private void recheckDomain() {
        String oldAddress = serverId.split(":")[0];
        String newAddress = resolveAddress();
        if (!oldAddress.equals(newAddress)) {
            logger.info("Server address changed during daily recheck: " + oldAddress + " -> " + newAddress);
            // Note: serverId is final, so the change is reflected in the next heartbeat's buildServerInfo()
        } else {
            logger.fine("Daily domain recheck: address unchanged (" + oldAddress + ")");
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
     * Checks handshake domain first (actual domain players typed), then falls
     * back to IP detection methods.
     */
    private String resolveAddress() {
        // 1. Handshake domain — the actual domain players typed in their client (via ProtocolLib)
        String handshakeDomain = handshakeTracker.getMostCommonDomain();
        if (handshakeDomain != null && !handshakeDomain.isEmpty()) {
            return handshakeDomain;
        }

        // 2. External IP detection services — for cloud hosts that bind to 0.0.0.0
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
            // 3. Reverse DNS lookup — try to resolve a domain name from the IP
            try {
                InetAddress addr = InetAddress.getByName(detectedIp);
                String hostName = addr.getHostName();
                if (hostName != null && !hostName.equals(detectedIp) && hostName.contains(".")) {
                    return hostName;
                }
            } catch (Exception ignored) {
            }
            return detectedIp;
        }

        // 4. Pterodactyl SERVER_IP environment variable (automatically set by Pterodactyl)
        String pterodactylIp = System.getenv("SERVER_IP");
        if (isUsableIp(pterodactylIp)) return pterodactylIp;

        // 5. Bukkit.getIp() — set in server.properties
        String ip = Bukkit.getIp();
        if (isUsableIp(ip)) return ip;

        // 6. Local network interfaces — filters out 0.0.0.0, loopback, link-local
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

        return "0.0.0.0";
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
     * Re-resolves the address each time so handshake domain changes are picked up.
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
     * Returns whether the first player has joined yet.
     */
    public boolean isFirstPlayerJoined() {
        return firstPlayerJoined;
    }

    /**
     * Returns the hardcoded API endpoint URL.
     */
    public static String getApiEndpoint() {
        return API_ENDPOINT;
    }
}
