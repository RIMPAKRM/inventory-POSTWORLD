package org.inventory.inventory.network;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Server-side dedup cache for C2S requestIds.
 *
 * Each entry is keyed as "playerId:requestId" and expires after {@link #TTL_MS}.
 * GC runs automatically every {@link #GC_MODULO} mark() calls.
 *
 * Thread-safe: uses ConcurrentHashMap and AtomicInteger.
 */
public final class RequestDedup {

    /** How long a requestId is remembered as "recently seen" (ms). */
    public static final long TTL_MS = 30_000L;

    /** Run GC every this many mark calls. */
    private static final int GC_MODULO = 100;

    private static final Map<String, Long> SEEN = new ConcurrentHashMap<>();
    private static final AtomicInteger MARK_COUNT = new AtomicInteger(0);

    private RequestDedup() {}

    /**
     * Returns {@code true} if this requestId from this player is a duplicate
     * (was seen within the last {@link #TTL_MS} ms).
     *
     * If not a duplicate, registers the entry and returns {@code false}.
     */
    public static boolean isDuplicate(UUID playerId, UUID requestId) {
        String key = playerId + ":" + requestId;
        long now = System.currentTimeMillis();

        Long existing = SEEN.get(key);
        if (existing != null && now - existing < TTL_MS) {
            return true;
        }

        SEEN.put(key, now);

        int count = MARK_COUNT.incrementAndGet();
        if (count % GC_MODULO == 0) {
            runGC(now);
        }

        return false;
    }

    /** Expire all entries older than {@link #TTL_MS}. */
    public static void runGC(long now) {
        SEEN.entrySet().removeIf(e -> now - e.getValue() >= TTL_MS);
    }

    /** Remove all cached entries for a specific player (call on logout). */
    public static void clearPlayer(UUID playerId) {
        String prefix = playerId + ":";
        SEEN.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
    }

    /** Clear all entries. Useful for tests. */
    public static void clearAll() {
        SEEN.clear();
        MARK_COUNT.set(0);
    }

    /** Current number of tracked entries (for metrics / tests). */
    public static int size() {
        return SEEN.size();
    }
}

