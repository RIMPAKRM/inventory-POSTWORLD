package org.inventory.inventory.server;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.inventory.inventory.capability.IPlayerLoadout;
import org.inventory.inventory.capability.LoadoutCapability;
import org.inventory.inventory.server.saga.OverflowSaga;
import org.inventory.inventory.server.saga.SagaContext;
import org.inventory.inventory.server.saga.SagaOrchestrator;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Facade over {@link OverflowSaga} that provides the public API used by the rest of
 * the mod. All actual redistribution logic lives in {@link OverflowSaga}.
 *
 * Transaction steps (via Saga):
 *   persistJournal → moveToAvailable → dropRemainder → clearJournal
 *
 * On disconnect before clearJournal, {@link #recoverPendingOverflow} re-runs the saga.
 */
public final class OverflowService {

    private static final Logger LOGGER = LogUtils.getLogger();

    private OverflowService() {}

    /**
     * Execute an overflow transaction for displaced items.
     */
    public static void applyOverflow(ServerPlayer player, List<ItemStack> displaced, UUID opId) {
        if (displaced.isEmpty()) return;

        LOGGER.debug("[Overflow] applyOverflow opId={} count={}", opId, displaced.size());

        SagaContext ctx = new SagaContext(opId, player);
        ctx.set(OverflowSaga.KEY_DISPLACED, displaced);

        boolean ok = SagaOrchestrator.execute("overflow", ctx, OverflowSaga.steps());
        if (!ok) {
            LOGGER.error("[Overflow] saga failed opId={} — items may be lost!", opId);
        }

        LOGGER.debug("[Overflow] applyOverflow opId={} done", opId);
    }

    /**
     * Called on player login to resume an overflow interrupted by a disconnect.
     */
    public static void recoverPendingOverflow(ServerPlayer player) {
        Optional<IPlayerLoadout> loadoutOpt = player.getCapability(LoadoutCapability.PLAYER_LOADOUT).resolve();
        if (loadoutOpt.isEmpty()) return;

        IPlayerLoadout loadout = loadoutOpt.get();
        if (!loadout.hasPendingOverflow()) return;

        CompoundTag pending = loadout.getPendingOverflow();
        UUID opId = UUID.fromString(pending.getString("opId"));
        List<ItemStack> items = OverflowSaga.readItemsFromTag(pending);

        LOGGER.warn("[Overflow] recoverPendingOverflow player={} opId={} items={}",
                player.getName().getString(), opId, items.size());

        applyOverflow(player, items, opId);
    }
}
