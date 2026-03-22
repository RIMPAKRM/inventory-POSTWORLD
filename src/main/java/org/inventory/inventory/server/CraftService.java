package org.inventory.inventory.server;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.inventory.inventory.domain.CraftCard;
import org.inventory.inventory.domain.CraftCardRegistry;
import org.inventory.inventory.server.saga.CraftSaga;
import org.inventory.inventory.server.saga.SagaContext;
import org.inventory.inventory.server.saga.SagaOrchestrator;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;

/**
 * Server-side API for processing craft requests.
 *
 * Flow:
 *   1. Validate requestId (dedup done by caller / packet handler).
 *   2. Check loadoutVersion via {@link InventoryTransactionService#beginLoadoutOp}.
 *   3. Look up the recipe in {@link CraftCardRegistry}.
 *   4. Execute {@link CraftSaga} (validate → consume → grant) with compensation support.
 *   5. On success: commit transaction, schedule {@link LoadoutSyncScheduler#sendImmediately}.
 */
public final class CraftService {

    private static final Logger LOGGER = LogUtils.getLogger();

    private CraftService() {}

    /**
     * Possible outcomes of a craft request.
     */
    public enum CraftResult {
        SUCCESS,
        VERSION_MISMATCH,
        UNKNOWN_RECIPE,
        MISSING_INGREDIENTS,
        SAGA_FAILED
    }

    /**
     * Execute a craft request.
     *
     * This method acquires and releases the per-player lock via
     * {@link InventoryTransactionService}.
     *
     * @param player            the crafting player
     * @param craftId           recipe to craft
     * @param requestId         unique ID for tracing
     * @param clientViewVersion client's current loadoutVersion
     * @return the outcome of the craft operation
     */
    public static CraftResult requestCraft(ServerPlayer player,
                                           ResourceLocation craftId,
                                           UUID requestId,
                                           long clientViewVersion) {
        MetricsService.incrementCraftRequests();
        LOGGER.debug("[CraftService] requestCraft craftId={} requestId={} player={}",
                craftId, requestId, player.getName().getString());

        // 1. Optimistic version check + lock acquisition
        Optional<OpContext> opOpt = InventoryTransactionService.beginLoadoutOp(player, clientViewVersion);
        if (opOpt.isEmpty()) {
            MetricsService.incrementVersionRejections();
            LOGGER.debug("[CraftService] version mismatch requestId={}", requestId);
            return CraftResult.VERSION_MISMATCH;
        }

        OpContext op = opOpt.get();
        boolean success = false;
        try {
            // 2. Lookup recipe
            Optional<CraftCard> cardOpt = CraftCardRegistry.findCard(craftId);
            if (cardOpt.isEmpty()) {
                LOGGER.debug("[CraftService] unknown recipe {} requestId={}", craftId, requestId);
                return CraftResult.UNKNOWN_RECIPE;
            }

            // 3. Execute saga
            SagaContext ctx = new SagaContext(op.opId, player);
            ctx.set(CraftSaga.KEY_CRAFT_CARD, cardOpt.get());

            boolean sagaOk = SagaOrchestrator.execute("craft", ctx, CraftSaga.steps());
            if (!sagaOk) {
                String failedStep = ctx.get(SagaOrchestrator.KEY_FAILED_STEP);
                if ("checkIngredients".equals(failedStep) || "consumeIngredients".equals(failedStep)) {
                    return CraftResult.MISSING_INGREDIENTS;
                }
                return CraftResult.SAGA_FAILED;
            }

            success = true;
            MetricsService.incrementCraftSuccesses();
            LOGGER.debug("[CraftService] SUCCESS craftId={} requestId={}", craftId, requestId);
            return CraftResult.SUCCESS;

        } finally {
            InventoryTransactionService.endLoadoutOp(op, success);
            // Immediate sync on craft completion (critical event — bypass debounce)
            if (success) {
                LoadoutSyncScheduler.sendImmediately(player);
            }
        }
    }
}

