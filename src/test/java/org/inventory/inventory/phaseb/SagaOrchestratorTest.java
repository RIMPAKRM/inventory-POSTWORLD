package org.inventory.inventory.phaseb;

import org.inventory.inventory.server.MetricsService;
import org.inventory.inventory.server.saga.SagaContext;
import org.inventory.inventory.server.saga.SagaOrchestrator;
import org.inventory.inventory.server.saga.SagaStep;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SagaOrchestrator}.
 *
 * Phase B DoD: multi-step operations (craft/overflow) must compensate previous steps
 * in reverse order on failure, without leaking items or leaving inventory corrupted.
 *
 * No Minecraft runtime is required — steps are pure Java lambdas that record
 * execution order into a shared log list.
 */
class SagaOrchestratorTest {

    @BeforeEach
    void resetMetrics() {
        MetricsService.resetAll();
    }

    @AfterEach
    void cleanup() {
        MetricsService.resetAll();
    }

    // ------------------------------------------------------------------ helpers

    /** Create a SagaContext without a real player (steps in this test do not use ctx.player). */
    private static SagaContext ctx() {
        return new SagaContext(UUID.randomUUID(), null);
    }

    /** A step that logs exec/comp events and returns the given executeResult. */
    private static SagaStep step(String name, boolean executeResult, List<String> log) {
        return new SagaStep() {
            @Override public String name() { return name; }
            @Override public boolean execute(SagaContext c)  { log.add("exec:" + name); return executeResult; }
            @Override public void compensate(SagaContext c)  { log.add("comp:" + name); }
        };
    }

    /** A step that throws RuntimeException on execute (treated as failure). */
    private static SagaStep throwingStep(String name, List<String> log) {
        return new SagaStep() {
            @Override public String name() { return name; }
            @Override public boolean execute(SagaContext c) {
                log.add("exec:" + name);
                throw new RuntimeException("simulated exception in step " + name);
            }
            @Override public void compensate(SagaContext c) { log.add("comp:" + name); }
        };
    }

    /** A step that succeeds but throws during compensation. */
    private static SagaStep compensateThrowingStep(String name, List<String> log) {
        return new SagaStep() {
            @Override public String name() { return name; }
            @Override public boolean execute(SagaContext c)  { log.add("exec:" + name); return true; }
            @Override public void compensate(SagaContext c) {
                log.add("comp:" + name);
                throw new RuntimeException("simulated compensation failure in " + name);
            }
        };
    }

    // ------------------------------------------------------------------ tests

    @Test
    void emptyStepList_returnsTrue() {
        assertTrue(SagaOrchestrator.execute("test", ctx(), List.of()),
                "Empty saga must return true (no failure)");
    }

    @Test
    void singleSuccessfulStep_returnsTrue_stepExecuted() {
        List<String> log = new ArrayList<>();
        boolean result = SagaOrchestrator.execute("test", ctx(), List.of(step("A", true, log)));
        assertTrue(result);
        assertEquals(List.of("exec:A"), log);
    }

    @Test
    void singleFailingStep_returnsFalse_stepIsNotCompensatedItself() {
        List<String> log = new ArrayList<>();
        boolean result = SagaOrchestrator.execute("test", ctx(), List.of(step("A", false, log)));
        assertFalse(result, "Saga must return false when a step fails");
        // The failed step itself must NOT be compensated — only previously successful steps are
        assertEquals(List.of("exec:A"), log,
                "Failed step must not trigger its own compensation");
    }

    @Test
    void multipleSuccessfulSteps_allExecutedInOrder_returnsTrue() {
        List<String> log = new ArrayList<>();
        List<SagaStep> steps = List.of(
                step("A", true, log),
                step("B", true, log),
                step("C", true, log)
        );
        boolean result = SagaOrchestrator.execute("test", ctx(), steps);
        assertTrue(result);
        assertEquals(List.of("exec:A", "exec:B", "exec:C"), log,
                "Steps must execute in declaration order");
    }

    @Test
    void secondStepFails_firstIsCompensated_thirdNeverRuns() {
        List<String> log = new ArrayList<>();
        List<SagaStep> steps = List.of(
                step("A", true,  log),
                step("B", false, log),  // fails
                step("C", true,  log)   // must never execute
        );
        boolean result = SagaOrchestrator.execute("test", ctx(), steps);
        assertFalse(result);
        assertEquals(List.of("exec:A", "exec:B", "comp:A"), log,
                "Only A must be compensated; C must never execute");
    }

    @Test
    void thirdStepFails_previousTwoCompensatedInReverseOrder() {
        List<String> log = new ArrayList<>();
        List<SagaStep> steps = List.of(
                step("A", true,  log),
                step("B", true,  log),
                step("C", false, log)   // fails
        );
        boolean result = SagaOrchestrator.execute("test", ctx(), steps);
        assertFalse(result);
        // Compensations must run in reverse: B then A
        assertEquals(List.of("exec:A", "exec:B", "exec:C", "comp:B", "comp:A"), log,
                "Compensations must run in reverse execution order");
    }

    @Test
    void stepThrows_treatedAsFail_previousStepsAreCompensated() {
        List<String> log = new ArrayList<>();
        List<SagaStep> steps = List.of(
                step("A", true, log),
                throwingStep("B", log),  // throws → failure
                step("C", true, log)     // must never execute
        );
        assertDoesNotThrow(
                () -> SagaOrchestrator.execute("test", ctx(), steps),
                "Orchestrator must not propagate step exceptions"
        );
        assertEquals(List.of("exec:A", "exec:B", "comp:A"), log,
                "Exception in B must compensate A; C must never run");
    }

    @Test
    void compensateThrows_doesNotAbortRemainingCompensations() {
        List<String> log = new ArrayList<>();
        List<SagaStep> steps = List.of(
                step("A",                   true,  log),  // A compensate is clean
                compensateThrowingStep("B",        log),  // B compensate throws
                step("C",                   false, log)   // C fails → trigger rollback
        );
        assertDoesNotThrow(
                () -> SagaOrchestrator.execute("test", ctx(), steps),
                "Compensation exception must not abort remaining compensations"
        );
        // Both B and A compensations must have been attempted
        assertTrue(log.contains("comp:B"), "B compensation must be attempted");
        assertTrue(log.contains("comp:A"), "A compensation must run even after B throws");
    }

    @Test
    void failure_incrementsRollbackMetric() {
        long before = MetricsService.getRollbacks();
        List<String> log = new ArrayList<>();
        SagaOrchestrator.execute("test", ctx(), List.of(
                step("A", true,  log),
                step("B", false, log)
        ));
        assertEquals(before + 1, MetricsService.getRollbacks(),
                "Each saga failure must increment the rollback metric");
    }

    @Test
    void success_doesNotIncrementRollbackMetric() {
        long before = MetricsService.getRollbacks();
        List<String> log = new ArrayList<>();
        SagaOrchestrator.execute("test", ctx(), List.of(step("A", true, log)));
        assertEquals(before, MetricsService.getRollbacks(),
                "Successful saga must not increment the rollback metric");
    }

    @Test
    void multipleFailuresAccumulate_rollbackMetricIncreasesOnce() {
        // Two separate sagas, both fail → counter incremented twice
        long before = MetricsService.getRollbacks();
        List<String> log = new ArrayList<>();
        SagaOrchestrator.execute("saga1", ctx(), List.of(step("X", false, log)));
        SagaOrchestrator.execute("saga2", ctx(), List.of(step("Y", false, log)));
        assertEquals(before + 2, MetricsService.getRollbacks());
    }

    @Test
    void contextDataSharedBetweenSteps() {
        // Steps communicate via ctx; verify a value written by step A is readable by step B
        String KEY = "ping";
        SagaStep writer = new SagaStep() {
            @Override public String name() { return "writer"; }
            @Override public boolean execute(SagaContext c) { c.set(KEY, "pong"); return true; }
            @Override public void compensate(SagaContext c) {}
        };
        SagaStep reader = new SagaStep() {
            @Override public String name() { return "reader"; }
            @Override public boolean execute(SagaContext c) {
                return "pong".equals(c.get(KEY));
            }
            @Override public void compensate(SagaContext c) {}
        };

        boolean result = SagaOrchestrator.execute("context-test", ctx(), List.of(writer, reader));
        assertTrue(result, "Reader step must see value written by writer step");
    }
}

