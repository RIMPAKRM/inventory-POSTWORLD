package org.inventory.inventory.server;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import org.inventory.inventory.capability.LoadoutCapability;
import org.inventory.inventory.network.ModNetwork;
import org.inventory.inventory.network.S2CLoadoutSyncPacket;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Debounced S2C_LoadoutSync dispatcher.
 *
 * Instead of sending a sync packet on every inventory mutation, callers
 * call {@link #scheduleSyncFor(ServerPlayer)}. The scheduler batches
 * pending syncs and flushes them on the server tick, but only after
 * {@code Config.syncDebounceMs} have elapsed since the first pending change.
 *
 * Critical events (overflow commit, craft completion) bypass debounce via
 * {@link #sendImmediately(ServerPlayer)}.
 *
 * Call {@link #tick(MinecraftServer)} from a {@code ServerTickEvent} handler.
 */
public final class LoadoutSyncScheduler {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Map: player UUID → timestamp (ms) when the first un-flushed change occurred. */
    private static final ConcurrentMap<UUID, Long> PENDING = new ConcurrentHashMap<>();

    private LoadoutSyncScheduler() {}

    /**
     * Schedule a debounced sync for the given player.
     * Subsequent calls before the debounce window expires are no-ops (first-write wins).
     */
    public static void scheduleSyncFor(ServerPlayer player) {
        scheduleSync(player.getUUID());
    }

    /**
     * Schedule a debounced sync by player UUID.
     * Subsequent calls before the debounce window expires are no-ops (first-write wins).
     *
     * <p>This overload is used internally and exposed for unit testing.
     */
    public static void scheduleSync(UUID playerId) {
        PENDING.putIfAbsent(playerId, System.currentTimeMillis());
    }

    /**
     * Send S2C_LoadoutSync immediately, bypassing debounce.
     * Removes any pending debounced entry for this player.
     */
    public static void sendImmediately(ServerPlayer player) {
        PENDING.remove(player.getUUID());
        sendSync(player);
    }

    /**
     * Called on every server tick. Flushes syncs whose debounce window has expired.
     */
    public static void tick(MinecraftServer server) {
        if (PENDING.isEmpty()) return;

        long now = System.currentTimeMillis();
        long debounceMs = org.inventory.inventory.Config.syncDebounceMs;

        PENDING.entrySet().removeIf(entry -> {
            if (now - entry.getValue() >= debounceMs) {
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                if (player != null) {
                    sendSync(player);
                }
                return true;
            }
            return false;
        });
    }

    /** Remove a player's pending sync (e.g., on logout). */
    public static void cancelSync(UUID playerId) {
        PENDING.remove(playerId);
    }

    /** True if there is a pending debounced sync for this player. */
    public static boolean hasPending(UUID playerId) {
        return PENDING.containsKey(playerId);
    }

    /** Number of players currently awaiting a debounced sync. Useful for metrics / tests. */
    public static int pendingCount() {
        return PENDING.size();
    }

    /** Clear all pending sync entries. Useful for tests. */
    public static void clearAllPending() {
        PENDING.clear();
    }

    // ---- Internal ----

    private static void sendSync(ServerPlayer player) {
        player.getCapability(LoadoutCapability.PLAYER_LOADOUT).ifPresent(loadout -> {
            long version = loadout.getLoadoutVersion();
            CompoundTag snapshot = loadout.serializeNBT();
            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new S2CLoadoutSyncPacket(version, snapshot));
            MetricsService.recordSyncSent(player.getUUID());
            LOGGER.debug("[SyncScheduler] sent S2C_LoadoutSync player={} version={}",
                    player.getName().getString(), version);
        });
    }
}



