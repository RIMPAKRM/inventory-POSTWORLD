package org.inventory.inventory.phaseb;

import org.inventory.inventory.network.RequestDedup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RequestDedup}: idempotency dedup cache.
 *
 * Phase B DoD: repeated C2S requestIds must never cause repeated grants/consumes.
 */
class IdempotencyTest {

    @AfterEach
    void cleanup() {
        RequestDedup.clearAll();
    }

    @Test
    void firstRequestIsNotDuplicate() {
        UUID player = UUID.randomUUID();
        UUID request = UUID.randomUUID();
        assertFalse(RequestDedup.isDuplicate(player, request),
                "First occurrence must not be flagged as duplicate");
    }

    @Test
    void secondRequestWithSameIdIsDuplicate() {
        UUID player = UUID.randomUUID();
        UUID request = UUID.randomUUID();

        RequestDedup.isDuplicate(player, request); // first call registers it
        assertTrue(RequestDedup.isDuplicate(player, request),
                "Second occurrence of the same requestId must be a duplicate");
    }

    @Test
    void sameRequestIdDifferentPlayersAreNotDuplicates() {
        UUID playerA = UUID.randomUUID();
        UUID playerB = UUID.randomUUID();
        UUID request = UUID.randomUUID();

        RequestDedup.isDuplicate(playerA, request);
        assertFalse(RequestDedup.isDuplicate(playerB, request),
                "Same requestId from different players is NOT a duplicate");
    }

    @Test
    void differentRequestIdsAreBothAccepted() {
        UUID player  = UUID.randomUUID();
        UUID req1    = UUID.randomUUID();
        UUID req2    = UUID.randomUUID();

        assertFalse(RequestDedup.isDuplicate(player, req1));
        assertFalse(RequestDedup.isDuplicate(player, req2));
    }

    @Test
    void clearPlayerRemovesOnlyThatPlayersEntries() {
        UUID playerA = UUID.randomUUID();
        UUID playerB = UUID.randomUUID();
        UUID reqA    = UUID.randomUUID();
        UUID reqB    = UUID.randomUUID();

        RequestDedup.isDuplicate(playerA, reqA);
        RequestDedup.isDuplicate(playerB, reqB);

        RequestDedup.clearPlayer(playerA);

        // playerA's entry is gone → not a duplicate any more
        assertFalse(RequestDedup.isDuplicate(playerA, reqA),
                "After clearPlayer(A), playerA's requests should be re-accepted");
        // playerB's entry is still there → still a duplicate
        assertTrue(RequestDedup.isDuplicate(playerB, reqB),
                "playerB's entries must not be affected by clearPlayer(A)");
    }

    @Test
    void gcEvictsExpiredEntries() throws InterruptedException {
        // Force a very short TTL by manipulating timestamps
        UUID player  = UUID.randomUUID();
        UUID request = UUID.randomUUID();

        // First mark — sets timestamp
        RequestDedup.isDuplicate(player, request);
        assertEquals(1, RequestDedup.size());

        // Manually run GC with a now so far in the future that the entry is expired
        long futureNow = System.currentTimeMillis() + RequestDedup.TTL_MS + 1;
        RequestDedup.runGC(futureNow);

        assertEquals(0, RequestDedup.size(), "GC must evict expired entries");
    }

    @Test
    void massiveRapidRequestsNoDuplicateGrants() {
        UUID player = UUID.randomUUID();
        int unique  = 0;
        int dupes   = 0;
        UUID[] ids  = new UUID[200];

        for (int i = 0; i < ids.length; i++) {
            ids[i] = UUID.randomUUID();
        }

        // Send each ID twice
        for (UUID id : ids) {
            if (!RequestDedup.isDuplicate(player, id)) unique++;
        }
        for (UUID id : ids) {
            if (RequestDedup.isDuplicate(player, id)) dupes++;
        }

        assertEquals(ids.length, unique, "Every unique ID must be accepted exactly once");
        assertEquals(ids.length, dupes,  "Every ID must be a duplicate on second send");
    }
}

