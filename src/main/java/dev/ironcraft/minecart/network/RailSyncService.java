package dev.ironcraft.minecart.network;

import dev.ironcraft.minecart.rail.CustomRailType;
import dev.ironcraft.minecart.storage.CustomRailRegistry;
import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import io.papermc.paper.event.packet.PlayerChunkUnloadEvent;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

/** Synchronizes custom-rail positions only to clients that complete the Fabric handshake. */
public final class RailSyncService implements Listener, PluginMessageListener {
    private static final int MAX_HANDSHAKE_ATTEMPTS = 3;
    private static final long HANDSHAKE_RETRY_TICKS = 40L;

    private final Plugin plugin;
    private final CustomRailRegistry registry;
    private final Set<UUID> companionClients = new HashSet<>();
    private final Map<UUID, HandshakeAttempt> handshakes = new HashMap<>();

    public RailSyncService(Plugin plugin, CustomRailRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    public void enable() {
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, RailSyncProtocol.HELLO_CHANNEL, this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, RailSyncProtocol.HANDSHAKE_CHANNEL);
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, RailSyncProtocol.SYNC_CHANNEL);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getListeningPluginChannels().contains(RailSyncProtocol.HANDSHAKE_CHANNEL)) {
                beginHandshake(player);
            }
        }
    }

    public void disable() {
        companionClients.clear();
        handshakes.clear();
        Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, RailSyncProtocol.HELLO_CHANNEL, this);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, RailSyncProtocol.HANDSHAKE_CHANNEL);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, RailSyncProtocol.SYNC_CHANNEL);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!RailSyncProtocol.HELLO_CHANNEL.equals(channel)
                || message.length != 5
                || Byte.toUnsignedInt(message[0]) != RailSyncProtocol.VERSION) {
            return;
        }
        int nonce = ByteBuffer.wrap(message, 1, Integer.BYTES).getInt();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            HandshakeAttempt handshake = handshakes.get(player.getUniqueId());
            if (handshake == null
                    || handshake.nonce != nonce
                    || handshake.attempts > MAX_HANDSHAKE_ATTEMPTS
                    || !player.getListeningPluginChannels().contains(RailSyncProtocol.SYNC_CHANNEL)) {
                return;
            }
            companionClients.add(player.getUniqueId());
            synchronizeVisibleChunks(player);
        });
    }

    @EventHandler
    public void onRegisterChannel(PlayerRegisterChannelEvent event) {
        if (!RailSyncProtocol.HANDSHAKE_CHANNEL.equals(event.getChannel())) {
            return;
        }
        // The channel list is updated around this event, so defer the first
        // capability check by one tick before sending anything to the client.
        Bukkit.getScheduler().runTask(plugin, () -> beginHandshake(event.getPlayer()));
    }

    public void sendUpdate(Block block, CustomRailType type) {
        byte[] packet = RailSyncProtocol.update(
                block.getWorld().getKey().toString(),
                block.getX(),
                block.getY(),
                block.getZ(),
                type
        );
        for (Player player : block.getWorld().getPlayersSeeingChunk(block.getChunk())) {
            if (isCompanionClient(player)) {
                send(player, packet);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChunkLoad(PlayerChunkLoadEvent event) {
        if (isCompanionClient(event.getPlayer())) {
            sendSnapshot(event.getPlayer(), event.getChunk());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChunkUnload(PlayerChunkUnloadEvent event) {
        if (!isCompanionClient(event.getPlayer())) {
            return;
        }
        Chunk chunk = event.getChunk();
        send(event.getPlayer(), RailSyncProtocol.forgetChunk(
                chunk.getWorld().getKey().toString(),
                chunk.getX(),
                chunk.getZ()
        ));
    }

    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        if (!isCompanionClient(event.getPlayer())) {
            return;
        }
        send(event.getPlayer(), RailSyncProtocol.clearAll());
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                synchronizeVisibleChunks(event.getPlayer());
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        companionClients.remove(event.getPlayer().getUniqueId());
        handshakes.remove(event.getPlayer().getUniqueId());
    }

    private void beginHandshake(Player player) {
        if (!player.isOnline() || companionClients.contains(player.getUniqueId())) {
            return;
        }
        handshakes.computeIfAbsent(
                player.getUniqueId(),
                ignored -> new HandshakeAttempt(ThreadLocalRandom.current().nextInt())
        );
        attemptHandshake(player);
    }

    private void attemptHandshake(Player player) {
        if (!player.isOnline() || companionClients.contains(player.getUniqueId())) {
            return;
        }
        HandshakeAttempt handshake = handshakes.get(player.getUniqueId());
        if (handshake == null
                || handshake.attempts >= MAX_HANDSHAKE_ATTEMPTS
                || !player.getListeningPluginChannels().contains(RailSyncProtocol.HANDSHAKE_CHANNEL)) {
            return;
        }

        handshake.attempts++;
        player.sendPluginMessage(
                plugin,
                RailSyncProtocol.HANDSHAKE_CHANNEL,
                RailSyncProtocol.handshakeChallenge(handshake.nonce)
        );
        if (handshake.attempts < MAX_HANDSHAKE_ATTEMPTS) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> attemptHandshake(player), HANDSHAKE_RETRY_TICKS);
        }
    }

    private void synchronizeVisibleChunks(Player player) {
        send(player, RailSyncProtocol.clearAll());
        for (Chunk chunk : player.getSentChunks()) {
            if (chunk.getWorld().equals(player.getWorld())) {
                sendSnapshot(player, chunk, false);
            }
        }
    }

    private void sendSnapshot(Player player, Chunk chunk) {
        sendSnapshot(player, chunk, true);
    }

    private void sendSnapshot(Player player, Chunk chunk, boolean includeEmpty) {
        registry.validate(chunk);
        java.util.List<CustomRailRegistry.StoredRail> rails = registry.railsIn(chunk);
        if (rails.isEmpty() && !includeEmpty) {
            return;
        }
        for (byte[] packet : RailSyncProtocol.chunkSnapshot(
                chunk.getWorld().getKey().toString(),
                chunk.getX(),
                chunk.getZ(),
                rails
        )) {
            send(player, packet);
        }
    }

    private boolean isCompanionClient(Player player) {
        return companionClients.contains(player.getUniqueId());
    }

    private void send(Player player, byte[] packet) {
        player.sendPluginMessage(plugin, RailSyncProtocol.SYNC_CHANNEL, packet);
    }

    private static final class HandshakeAttempt {
        private final int nonce;
        private int attempts;

        private HandshakeAttempt(int nonce) {
            this.nonce = nonce;
        }
    }
}
