package org.inventory.inventory.server;

import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Holds the context for a single loadout operation opened by
 * InventoryTransactionService.beginLoadoutOp().
 *
 * Lifetime: beginLoadoutOp() → [perform changes] → endLoadoutOp()
 * The per-player lock is held for the entire lifetime of this context.
 */
public final class OpContext {

    public final UUID opId;
    public final ServerPlayer player;
    public final long expectedLoadoutVersion;

    private boolean completed = false;

    OpContext(UUID opId, ServerPlayer player, long expectedLoadoutVersion) {
        this.opId = opId;
        this.player = player;
        this.expectedLoadoutVersion = expectedLoadoutVersion;
    }

    public boolean isCompleted() { return completed; }

    /** Called by InventoryTransactionService.endLoadoutOp() exactly once. */
    void markCompleted() { this.completed = true; }
}

