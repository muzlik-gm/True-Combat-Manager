package com.muzlik.pvpcombat.tracking;

import com.muzlik.pvpcombat.core.PvPCombatPlugin;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Automatically detects the domain name players use to connect (e.g. play.hypixel.net)
 * by listening for ProtocolLib handshake packets. Requires ProtocolLib to be installed;
 * gracefully degrades if it is not present.
 * <p>
 * Uses pure reflection — no compile-time dependency on ProtocolLib. Works out of the box
 * on any server running ProtocolLib with zero configuration.
 */
public class HandshakeDomainTracker {

    private final PvPCombatPlugin plugin;
    private final Logger logger;
    private final ConcurrentHashMap<UUID, String> playerDomains;
    private boolean protocolLibAvailable;
    private Object protocolManager;
    private Method addPacketListenerMethod;
    private Class<?> packetListenerClass;
    private Class<?> packetTypeClass;
    private Class<?> packetEventClass;
    private Class<?> packetContainerClass;

    public HandshakeDomainTracker(PvPCombatPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.playerDomains = new ConcurrentHashMap<>();
        this.protocolManager = null;
        this.addPacketListenerMethod = null;
        this.packetListenerClass = null;
        this.packetTypeClass = null;
        this.packetEventClass = null;
        this.packetContainerClass = null;
        this.protocolLibAvailable = initializeProtocolLib();

        if (protocolLibAvailable) {
            logger.info("ProtocolLib detected — handshake domain tracking enabled");
        } else {
            logger.fine("ProtocolLib not found — handshake domain tracking disabled");
        }
    }

    /**
     * Returns the domain a player used to connect, or null if unknown.
     */
    public String getDomain(UUID playerUuid) {
        return playerDomains.get(playerUuid);
    }

    /**
     * Returns the most commonly seen domain across all players, or null.
     */
    public String getMostCommonDomain() {
        if (playerDomains.isEmpty()) return null;

        ConcurrentHashMap<String, Integer> counts = new ConcurrentHashMap<>();
        playerDomains.values().forEach(domain ->
            counts.merge(domain, 1, Integer::sum));

        return counts.entrySet().stream()
            .max(java.util.Map.Entry.comparingByValue())
            .map(java.util.Map.Entry::getKey)
            .orElse(null);
    }

    /**
     * Initializes ProtocolLib using reflection — no compile-time dependency.
     * Creates a dynamic proxy implementing PacketListener to capture handshake packets.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean initializeProtocolLib() {
        try {
            // Check if ProtocolLib plugin is loaded
            if (plugin.getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
                return false;
            }

            // Get ProtocolLibrary class and ProtocolManager
            Class<?> protocolLibraryClass = Class.forName("com.comphenix.protocol.ProtocolLibrary");
            Method getProtocolManagerMethod = protocolLibraryClass.getMethod("getProtocolManager");
            this.protocolManager = getProtocolManagerMethod.invoke(null);

            // Cache classes
            this.packetListenerClass = Class.forName("com.comphenix.protocol.events.PacketListener");
            this.packetTypeClass = Class.forName("com.comphenix.protocol.PacketType");
            this.packetEventClass = Class.forName("com.comphenix.protocol.events.PacketEvent");
            this.packetContainerClass = Class.forName("com.comphenix.protocol.events.PacketContainer");

            // Cache addPacketListener method
            Class<?> protocolManagerClass = this.protocolManager.getClass();
            this.addPacketListenerMethod = protocolManagerClass.getMethod(
                "addPacketListener", this.packetListenerClass);

            // Create dynamic proxy implementing PacketListener
            Object listenerProxy = Proxy.newProxyInstance(
                this.packetListenerClass.getClassLoader(),
                new Class<?>[]{this.packetListenerClass},
                new HandshakeListenerHandler()
            );

            // Register the listener
            this.addPacketListenerMethod.invoke(this.protocolManager, listenerProxy);

            return true;
        } catch (Exception e) {
            logger.fine("Failed to initialize ProtocolLib handshake listener: " + e.getMessage());
            return false;
        }
    }

    /**
     * InvocationHandler for the PacketListener dynamic proxy.
     * Intercepts incoming handshake packets and extracts the server address.
     */
    private class HandshakeListenerHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (args == null || args.length == 0) return null;

            String methodName = method.getName();
            Object event = args[0];

            // We only care about onPacketReceiving
            if (!"onPacketReceiving".equals(methodName)) return null;

            try {
                // Get the packet from the event
                Method getPacketMethod = packetEventClass.getMethod("getPacket");
                Object packet = getPacketMethod.invoke(event);

                if (packet == null) return null;

                // Check if this is a Handshake.In.SET_PROTOCOL packet
                // PacketType is an inner class: PacketType.Handshake.Client.SET_PROTOCOL
                // We check by getting the packet type from the packet container
                Method getTypeMethod = packetContainerClass.getMethod("getType");
                Object packetType = getTypeMethod.invoke(packet);

                if (packetType == null) return null;

                String typeName = packetType.toString();
                // PacketType.toString() returns something like "HANDSHAKE_IN_SET_PROTOCOL(0x00)"
                if (typeName == null || !typeName.contains("HANDSHAKE_IN_SET_PROTOCOL")) {
                    return null;
                }

                // Read the server address string (index 0 of strings)
                Method getStringsMethod = packetContainerClass.getMethod("getStrings");
                Object stringsWrapper = getStringsMethod.invoke(packet);

                if (stringsWrapper == null) return null;

                Method readMethod = stringsWrapper.getClass().getMethod("read", int.class);
                Object hostObj = readMethod.invoke(stringsWrapper, 0);

                if (hostObj == null) return null;

                String host = hostObj.toString();
                if (host == null || host.isEmpty()) return null;

                // Strip port if present (e.g. "play.hypixel.net:25565" -> "play.hypixel.net")
                String domain = host.split(":")[0];

                // Skip IP addresses
                if (domain.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) return null;
                // Skip localhost
                if (domain.equalsIgnoreCase("localhost")) return null;

                // Get the player from the event
                Method getPlayerMethod = packetEventClass.getMethod("getPlayer");
                Object playerObj = getPlayerMethod.invoke(event);

                if (playerObj instanceof Player) {
                    Player player = (Player) playerObj;
                    playerDomains.put(player.getUniqueId(), domain);
                }
            } catch (Exception ignored) {
            }

            return null;
        }
    }
}
