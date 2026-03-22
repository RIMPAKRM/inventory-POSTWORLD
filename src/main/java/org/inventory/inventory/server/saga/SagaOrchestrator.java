package org.inventory.inventory.server.saga;

import com.mojang.logging.LogUtils;
import org.inventory.inventory.server.MetricsService;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Executes a Saga: an ordered sequence of {@link SagaStep}s with
 * compensation (rollback) support.
 *
 * <pre>
 * Execution contract:
 *   1. Steps are executed in the provided order.
 *   2. If a step returns false or throws, all previously executed steps
 *      are compensated in reverse order.
 *   3. All transitions are logged with the opId for tracing.
 *   4. Returns true on full success; false if any step failed.
 * </pre>
 *
 * Thread safety: Saga instances are not shared; each call is independent.
 */
public final class SagaOrchestrator {

    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String KEY_FAILED_STEP = "failedStep";

    private SagaOrchestrator() {}

    /**
     * Execute all steps of the saga.
     *
     * @param sagaName human-readable name for log messages
     * @param ctx      shared saga context (created by the caller)
     * @param steps    ordered list of steps to execute
     * @return {@code true} if all steps succeeded; {@code false} if any step failed
     *         (compensations will have already run)
     */
    public static boolean execute(String sagaName, SagaContext ctx, List<SagaStep> steps) {
        List<SagaStep> executed = new ArrayList<>();

        for (SagaStep step : steps) {
            LOGGER.debug("[Saga] {}/{} START opId={}", sagaName, step.name(), ctx.opId);
            boolean ok;
            try {
                ok = step.execute(ctx);
            } catch (Exception e) {
                LOGGER.error("[Saga] {}/{} EXCEPTION opId={}", sagaName, step.name(), ctx.opId, e);
                ok = false;
            }

            if (!ok) {
                ctx.set(KEY_FAILED_STEP, step.name());
                LOGGER.warn("[Saga] {}/{} FAILED — compensating {} steps opId={}",
                        sagaName, step.name(), executed.size(), ctx.opId);
                compensateAll(sagaName, ctx, executed);
                MetricsService.incrementRollbacks();
                return false;
            }

            LOGGER.debug("[Saga] {}/{} OK opId={}", sagaName, step.name(), ctx.opId);
            executed.add(step);
        }

        LOGGER.debug("[Saga] {} COMPLETE opId={}", sagaName, ctx.opId);
        return true;
    }

    private static void compensateAll(String sagaName, SagaContext ctx, List<SagaStep> executed) {
        for (int i = executed.size() - 1; i >= 0; i--) {
            SagaStep step = executed.get(i);
            LOGGER.debug("[Saga] {}/{} COMPENSATE opId={}", sagaName, step.name(), ctx.opId);
            try {
                step.compensate(ctx);
            } catch (Exception e) {
                LOGGER.error("[Saga] {}/{} COMPENSATE EXCEPTION opId={}", sagaName, step.name(), ctx.opId, e);
            }
        }
    }
}

