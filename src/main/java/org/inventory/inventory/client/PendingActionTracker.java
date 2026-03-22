package org.inventory.inventory.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.inventory.inventory.network.S2CActionRejectedPacket;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Client-side registry of unconfirmed (pending) actions.
 *
 * When the client sends a C2S packet, it registers the requestId here with
 * the current clientViewVersion. When the server replies:
 *  - {@link #onServerAck(long)}         : clear all entries with version ≤ serverVersion
 *  - {@link #onActionRejected(UUID, S2CActionRejectedPacket.Reason)} : remove the entry and show rollback UX
 *
 * This enables the pending / rollback overlay indicators in
 * {@code InventoryScreen} and {@code CraftScreen}.
 *
 * Only exists on the client side ({@code @OnlyIn(Dist.CLIENT)}).
 */
@OnlyIn(Dist.CLIENT)
public final class PendingActionTracker {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** requestId → clientViewVersion at time of send */
    private static final Map<UUID, Long> PENDING = new LinkedHashMap<>();

    /** requestId → rejection reason (set on rollback, cleared after UI dismissal) */
    private static final Map<UUID, S2CActionRejectedPacket.Reason> REJECTED = new LinkedHashMap<>();

    private PendingActionTracker() {}

    // ---- Registration ----

    /**
     * Call before sending any C2S packet that modifies inventory state.
     */
    public static void registerPending(UUID requestId, long clientViewVersion) {
        PENDING.put(requestId, clientViewVersion);
        LOGGER.debug("[PendingTracker] registered requestId={} version={}", requestId, clientViewVersion);
    }

    // ---- Acknowledgements ----

    /**
     * Server confirmed state up to {@code serverVersion}.
     * Clears all pending entries whose clientViewVersion ≤ serverVersion.
     */
    public static void onServerAck(long serverVersion) {
        int removed = 0;
        var it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (entry.getValue() <= serverVersion) {
                it.remove();
                REJECTED.remove(entry.getKey()); // clear any stale rejection for same id
                removed++;
            }
        }
        if (removed > 0) {
            LOGGER.debug("[PendingTracker] acked {} entries at serverVersion={}", removed, serverVersion);
        }
    }

    /**
     * Server rejected a specific action.
     * Shows a transient rollback message to the player.
     */
    public static void onActionRejected(UUID requestId, S2CActionRejectedPacket.Reason reason) {
        PENDING.remove(requestId);
        REJECTED.put(requestId, reason);
        LOGGER.debug("[PendingTracker] rejected requestId={} reason={}", requestId, reason);
        showRejectionMessage(reason);
    }

    // ---- Queries ----

    /** True if there are any unconfirmed pending requests. */
    public static boolean hasPending() {
        return !PENDING.isEmpty();
    }

    /** True if a specific requestId is pending. */
    public static boolean isPending(UUID requestId) {
        return PENDING.containsKey(requestId);
    }

    /** True if a specific requestId was rejected by the server. */
    public static boolean isRejected(UUID requestId) {
        return REJECTED.containsKey(requestId);
    }

    /** Clear all state (e.g., on disconnect or screen close). */
    public static void clear() {
        PENDING.clear();
        REJECTED.clear();
    }

    // ---- UI feedback ----

    private static void showRejectionMessage(S2CActionRejectedPacket.Reason reason) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        String key = switch (reason) {
            case VERSION_MISMATCH    -> "inventory.action_rejected.version";
            case VALIDATION_FAILED   -> "inventory.action_rejected.validation";
            case UNKNOWN_RECIPE      -> "inventory.action_rejected.unknown_recipe";
            case MISSING_INGREDIENTS -> "inventory.action_rejected.missing_ingredients";
        };
        mc.player.displayClientMessage(Component.translatable(key), true);
    }
}

