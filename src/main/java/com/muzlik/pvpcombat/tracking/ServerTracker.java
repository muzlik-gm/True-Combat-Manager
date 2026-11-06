package com.muzlik.pvpcombat.tracking;

import com.muzlik.pvpcombat.config.ServerTrackingConfig;
import com.muzlik.pvpcombat.core.PvPCombatPlugin;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;

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
    private final String serverAddress;

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_BASE_DELAY_MS = 1000;

    private BukkitTask heartbeatTask;
    private long startTime;

    public ServerTracker(PvPCombatPlugin plugin, ServerTrackingConfig trackingConfig) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.startTime = System.currentTimeMillis();

        // Use configured server address (domain name) if set, otherwise auto-detect IP
        this.serverAddress = trackingConfig.getServerAddress();
        if (this.serverAddress != null && !this.serverAddress.isEmpty()) {
            this.serverId = this.serverAddress + ":" + Bukkit.getPort();
            logger.info("Server ID generated (configured address): " + this.serverId);
        } else {
            this.serverId = resolveAddress() + ":" + Bukkit.getPort();
            logger.info("Server ID generated (auto-detected IP): " + this.serverId);
        }

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
     * Bukkit.getIp() returns an empty string (not null) when server-ip is unset
     * (bound to all interfaces), so we fall back to the machine's actual local
     * IP rather than sending 0.0.0.0, which is not a connectable address.
     * Cloud/Pterodactyl hosts often bind to 0.0.0.0, so we filter it out and
     * only accept real unicast addresses.
     */
    private String resolveAddress() {
        String ip = Bukkit.getIp();
        if (isUsableIp(ip)) return ip;

        // Try InetAddress.getLocalHost() as a fallback
        try {
            java.net.InetAddress localhost = java.net.InetAddress.getLocalHost();
            String host = localhost.getHostAddress();
            if (isUsableIp(host)) return host;
        } catch (Exception ignored) {
        }

        // Enumerate network interfaces for a usable non-loopback IPv4 address
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
     */
    private ServerInfo buildServerInfo() {
        String host = (serverAddress != null && !serverAddress.isEmpty()) ? serverAddress : resolveAddress();
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
