package org.inventory.inventory.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import org.inventory.inventory.client.PendingActionTracker;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Server → Client: a C2S action was rejected.
 *
 * Reasons:
 *  - VERSION_MISMATCH      : client's loadoutVersion did not match server's
 *  - VALIDATION_FAILED     : slot/state validation error
 *  - UNKNOWN_RECIPE        : craftId not found in registry
 *  - MISSING_INGREDIENTS   : not enough items to craft
 *
 * The client should clear the pending action and show a rollback indicator.
 */
public final class S2CActionRejectedPacket {

    public enum Reason {
        VERSION_MISMATCH,
        VALIDATION_FAILED,
        UNKNOWN_RECIPE,
        MISSING_INGREDIENTS
    }

    public final UUID requestId;
    public final Reason reason;

    public S2CActionRejectedPacket(UUID requestId, Reason reason) {
        this.requestId = requestId;
        this.reason    = reason;
    }

    // ---- Codec ----

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(requestId);
        buf.writeEnum(reason);
    }

    public static S2CActionRejectedPacket decode(FriendlyByteBuf buf) {
        UUID requestId = buf.readUUID();
        Reason reason  = buf.readEnum(Reason.class);
        return new S2CActionRejectedPacket(requestId, reason);
    }

    // ---- Handler ----

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) {
                handleOnClient();
            }
        });
        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleOnClient() {
        PendingActionTracker.onActionRejected(requestId, reason);
    }
}

