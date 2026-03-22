package org.inventory.inventory.server;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import org.inventory.inventory.capability.IPlayerLoadout;
import org.inventory.inventory.capability.LoadoutCapability;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;

/**
 * Server-side API for all inventory/loadout operations.
 *
 * All public methods are intended to be called on the server thread only.
 *
 * Transaction pattern:
 * <pre>
 *   Optional<OpContext> ctxOpt = beginLoadoutOp(player, player.getCapability(...).map(...).orElse(0L));
 *   if (ctxOpt.isEmpty()) { // version mismatch → reject and ask client to retry
 *       return;
 *   }
 *   OpContext ctx = ctxOpt.get();
 *   boolean success = false;
 *   try {
 *       // perform mutations ...
 *       success = true;
 *   } finally {
 *       endLoadoutOp(ctx, success);
 *   }
 * </pre>
 */
public final class InventoryTransactionService {

    private static final Logger LOGGER = LogUtils.getLogger();

    private InventoryTransactionService() {}

    /**
     * Begin a loadout operation.
     *
     * Acquires the per-player lock and validates that the player's current
     * loadoutVersion matches expectedLoadoutVersion (optimistic concurrency).
     *
     * Returns empty on version mismatch or missing capability — the caller
     * must reject the packet and request a client re-sync (Phase B).
     */
    public static Optional<OpContext> beginLoadoutOp(ServerPlayer player, long expectedLoadoutVersion) {
        UUID pid = player.getUUID();
        PlayerLockService.lock(pid);

        Optional<IPlayerLoadout> loadoutOpt = player.getCapability(LoadoutCapability.PLAYER_LOADOUT).resolve();
        if (loadoutOpt.isEmpty()) {
            PlayerLockService.unlock(pid);
            LOGGER.warn("[InventoryTx] beginLoadoutOp: no capability for player={}", player.getName().getString());
            return Optional.empty();
        }

        long actual = loadoutOpt.get().getLoadoutVersion();
        if (actual != expectedLoadoutVersion) {
            PlayerLockService.unlock(pid);
            LOGGER.debug("[InventoryTx] beginLoadoutOp: version mismatch player={} expected={} actual={}",
                    player.getName().getString(), expectedLoadoutVersion, actual);
            return Optional.empty();
        }

        OpContext ctx = new OpContext(UUID.randomUUID(), player, expectedLoadoutVersion);
        LOGGER.debug("[InventoryTx] beginLoadoutOp: opId={} player={}", ctx.opId, player.getName().getString());
        return Optional.of(ctx);
    }

    /**
     * End a loadout operation and release the per-player lock.
     *
     * If success=true: increments loadoutVersion and schedules S2C_LoadoutSync (Phase B: TODO).
     * If success=false: changes are considered rolled back by the caller; version unchanged.
     */
    public static void endLoadoutOp(OpContext ctx, boolean success) {
        if (ctx.isCompleted()) {
            LOGGER.warn("[InventoryTx] endLoadoutOp called twice for opId={}", ctx.opId);
            return;
        }
        ctx.markCompleted();

        if (success) {
            ctx.player.getCapability(LoadoutCapability.PLAYER_LOADOUT).ifPresent(loadout -> {
                long v = loadout.incrementAndGetLoadoutVersion();
                LOGGER.debug("[InventoryTx] commit opId={} newVersion={}", ctx.opId, v);
                LoadoutSyncScheduler.scheduleSyncFor(ctx.player);
            });
        } else {
            LOGGER.debug("[InventoryTx] rollback opId={}", ctx.opId);
        }

        PlayerLockService.unlock(ctx.player.getUUID());
    }

    /**
     * Validate that a custom slot click is permitted.
     *
     * Called by CustomInventoryMenu before delegating to the base class.
     * Idempotency (requestId dedup) is a Phase B concern — this method only
     * enforces structural invariants.
     *
     * @param player     the clicking player
     * @param slotIndex  canonical slot index
     * @param opId       tracing opId
     * @return true if the click may proceed
     */
    public static boolean validateAndApplyClick(ServerPlayer player, int slotIndex, UUID opId) {
        if (CanonicalSlotMapping.isVanillaSlot(slotIndex)) {
            // Vanilla slots must not be routed through this validator
            LOGGER.warn("[InventoryTx] validateAndApplyClick: tried to validate vanilla slot {} opId={}", slotIndex, opId);
            return false;
        }

        if (!CanonicalSlotMapping.isEquipmentSlot(slotIndex) && !CanonicalSlotMapping.isDynamicSlot(slotIndex)) {
            LOGGER.warn("[InventoryTx] validateAndApplyClick: unknown slot index={} opId={}", slotIndex, opId);
            return false;
        }

        if (CanonicalSlotMapping.isDynamicSlot(slotIndex)) {
            int local = CanonicalSlotMapping.indexToDynamicSlot(slotIndex);
            boolean active = player.getCapability(LoadoutCapability.PLAYER_LOADOUT)
                    .map(l -> l.isDynamicSlotActive(local))
                    .orElse(false);
            if (!active) {
                LOGGER.warn("[InventoryTx] validateAndApplyClick: inactive dynamic slot={} opId={}", slotIndex, opId);
                return false;
            }
        }

        LOGGER.debug("[InventoryTx] validateAndApplyClick: ok slot={} opId={}", slotIndex, opId);
        return true;
    }
}
