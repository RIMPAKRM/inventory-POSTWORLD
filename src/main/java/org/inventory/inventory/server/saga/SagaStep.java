package org.inventory.inventory.server.saga;

/**
 * A single step in a Saga.
 *
 * Steps are executed in order by {@link SagaOrchestrator}.
 * If any step's {@link #execute} returns {@code false} (or throws),
 * the orchestrator runs {@link #compensate} on all previously successful
 * steps in reverse order.
 */
public interface SagaStep {

    /** Human-readable name used in log messages. */
    String name();

    /**
     * Execute this step.
     *
     * @param ctx shared saga context (use for reading/writing shared state)
     * @return {@code true} if this step succeeded; {@code false} to trigger rollback
     */
    boolean execute(SagaContext ctx);

    /**
     * Undo the effects of this step.
     *
     * Called only if a *later* step failed after this one succeeded.
     * Implementations must be idempotent (safe to call more than once).
     */
    void compensate(SagaContext ctx);
}

