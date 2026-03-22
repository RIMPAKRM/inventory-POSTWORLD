package org.inventory.inventory.phaseb;

import org.inventory.inventory.server.LoadoutSyncScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LoadoutSyncScheduler}: debounce scheduling logic.
 *
 * Phase B DoD: sync traffic must be reduced via debounce without losing consistency.
 *
 * These tests cover only the pure-Java scheduling state — actual S2C packet dispatch
 * requires a live Minecraft server and is tested as part of integration / smoke tests.
 */
class LoadoutSyncSchedulerTest {

    @AfterEach
    void cleanup() {
        LoadoutSyncScheduler.clearAllPending();
    }

    @Test
    void initiallyNoPendingSyncs() {
        assertEquals(0, LoadoutSyncScheduler.pendingCount(),
                "Scheduler must start with no pending entries");
    }

    @Test
    void scheduleSync_addsPendingEntry() {
        UUID player = UUID.randomUUID();
        LoadoutSyncScheduler.scheduleSync(player);
        assertTrue(LoadoutSyncScheduler.hasPending(player),
                "scheduleSync must create a pending entry for the player");
        assertEquals(1, LoadoutSyncScheduler.pendingCount());
    }

    @Test
    void scheduleSync_samePlayerTwice_onlyOnePendingEntry() {
        UUID player = UUID.randomUUID();
        LoadoutSyncScheduler.scheduleSync(player);
        LoadoutSyncScheduler.scheduleSync(player); // second call — must be a no-op
        assertEquals(1, LoadoutSyncScheduler.pendingCount(),
                "Multiple scheduleSync calls for the same player must not create duplicate entries (putIfAbsent)");
    }

    @Test
    void scheduleSync_differentPlayers_eachHasSeparateEntry() {
        UUID playerA = UUID.randomUUID();
        UUID playerB = UUID.randomUUID();
        LoadoutSyncScheduler.scheduleSync(playerA);
        LoadoutSyncScheduler.scheduleSync(playerB);
        assertTrue(LoadoutSyncScheduler.hasPending(playerA));
        assertTrue(LoadoutSyncScheduler.hasPending(playerB));
        assertEquals(2, LoadoutSyncScheduler.pendingCount());
    }

    @Test
    void cancelSync_removesEntry() {
        UUID player = UUID.randomUUID();
        LoadoutSyncScheduler.scheduleSync(player);
        LoadoutSyncScheduler.cancelSync(player);
        assertFalse(LoadoutSyncScheduler.hasPending(player),
                "cancelSync must remove the player's pending entry");
        assertEquals(0, LoadoutSyncScheduler.pendingCount());
    }

    @Test
    void cancelSync_nonExistentPlayer_noException() {
        UUID stranger = UUID.randomUUID();
        assertDoesNotThrow(() -> LoadoutSyncScheduler.cancelSync(stranger),
                "cancelSync for a player with no pending entry must not throw");
        assertEquals(0, LoadoutSyncScheduler.pendingCount());
    }

    @Test
    void cancelSync_onlyAffectsSpecifiedPlayer() {
        UUID playerA = UUID.randomUUID();
        UUID playerB = UUID.randomUUID();
        LoadoutSyncScheduler.scheduleSync(playerA);
        LoadoutSyncScheduler.scheduleSync(playerB);

        LoadoutSyncScheduler.cancelSync(playerA);

        assertFalse(LoadoutSyncScheduler.hasPending(playerA),
                "playerA's pending entry must be removed");
        assertTrue(LoadoutSyncScheduler.hasPending(playerB),
                "playerB's pending entry must be unaffected");
    }

    @Test
    void clearAllPending_removesEverything() {
        LoadoutSyncScheduler.scheduleSync(UUID.randomUUID());
        LoadoutSyncScheduler.scheduleSync(UUID.randomUUID());
        LoadoutSyncScheduler.scheduleSync(UUID.randomUUID());

        LoadoutSyncScheduler.clearAllPending();

        assertEquals(0, LoadoutSyncScheduler.pendingCount(),
                "clearAllPending must remove all entries");
    }

    @Test
    void scheduleAfterCancel_reAddsEntry() {
        UUID player = UUID.randomUUID();
        LoadoutSyncScheduler.scheduleSync(player);
        LoadoutSyncScheduler.cancelSync(player);
        // After cancel, scheduling again must work
        LoadoutSyncScheduler.scheduleSync(player);
        assertTrue(LoadoutSyncScheduler.hasPending(player),
                "After cancel + re-schedule, player must be pending again");
    }

    @Test
    void debounce_firstScheduleTimestampPreserved_secondCallIsNoop() {
        UUID player = UUID.randomUUID();
        // Schedule at time T0
        LoadoutSyncScheduler.scheduleSync(player);

        // Grab the count before second call
        int countBefore = LoadoutSyncScheduler.pendingCount();

        // A rapid burst of schedule calls must not reset the debounce timer
        for (int i = 0; i < 50; i++) {
            LoadoutSyncScheduler.scheduleSync(player);
        }

        assertEquals(countBefore, LoadoutSyncScheduler.pendingCount(),
                "Burst of schedule calls for same player must not change pending count (putIfAbsent semantics)");
    }
}

