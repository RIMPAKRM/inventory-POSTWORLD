package org.inventory.inventory.server;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Per-player reentrant lock registry.
 *
 * All inventory operations for a given player must acquire this lock so that
 * parallel click/craft packets cannot execute concurrently and cause duplication
 * or desync. The lock is reentrant to support nested calls on the same thread.
 *
 * Lock lifecycle:
 *   PlayerLockService.lock(uuid)   — called in InventoryTransactionService.beginLoadoutOp()
 *   PlayerLockService.unlock(uuid) — called in InventoryTransactionService.endLoadoutOp()
 *   PlayerLockService.removeLock() — called on player logout to free memory
 */
public final class PlayerLockService {

    private static final ConcurrentHashMap<UUID, ReentrantLock> LOCKS = new ConcurrentHashMap<>();

    private PlayerLockService() {}

    /** Acquire the lock for the given player. Blocks until available. */
    public static void lock(UUID playerId) {
        LOCKS.computeIfAbsent(playerId, id -> new ReentrantLock()).lock();
    }

    /** Release the lock. Safe to call even if the current thread does not hold it. */
    public static void unlock(UUID playerId) {
        ReentrantLock lock = LOCKS.get(playerId);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /** Remove the lock entry when the player logs out (avoids memory leak). */
    public static void removeLock(UUID playerId) {
        LOCKS.remove(playerId);
    }
}

