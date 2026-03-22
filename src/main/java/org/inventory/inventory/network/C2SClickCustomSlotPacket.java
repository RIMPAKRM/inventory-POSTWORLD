package org.inventory.inventory.network;

import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.inventory.inventory.server.InventoryTransactionService;
import org.inventory.inventory.server.MetricsService;
import org.inventory.inventory.server.OpContext;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Client → Server: request to interact with a custom inventory slot.
 *
 * Fields:
 *   requestId         – unique per-click ID for dedup (UUID)
 *   canonicalSlotIndex – target slot (see CanonicalSlotMapping)
 *   clientViewVersion  – client's current loadoutVersion for optimistic concurrency
 */
public final class C2SClickCustomSlotPacket {

    private static final Logger LOGGER = LogUtils.getLogger();

    public final UUID requestId;
    public final int canonicalSlotIndex;
    public final long clientViewVersion;

    public C2SClickCustomSlotPacket(UUID requestId, int canonicalSlotIndex, long clientViewVersion) {
        this.requestId = requestId;
        this.canonicalSlotIndex = canonicalSlotIndex;
        this.clientViewVersion = clientViewVersion;
    }

    // ---- Codec ----

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(requestId);
        buf.writeVarInt(canonicalSlotIndex);
        buf.writeLong(clientViewVersion);
    }

    public static C2SClickCustomSlotPacket decode(FriendlyByteBuf buf) {
        UUID requestId = buf.readUUID();
        int slotIndex   = buf.readVarInt();
        long version    = buf.readLong();
        return new C2SClickCustomSlotPacket(requestId, slotIndex, version);
    }

    // ---- Handler (server thread via enqueueWork) ----

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            // 1. Dedup check
            if (RequestDedup.isDuplicate(player.getUUID(), requestId)) {
                MetricsService.incrementDedupRejections();
                LOGGER.debug("[C2S_Click] dedup rejected requestId={} player={}", requestId, player.getName().getString());
                return;
            }

            // 2. Optimistic version check
            Optional<OpContext> opOpt = InventoryTransactionService.beginLoadoutOp(player, clientViewVersion);
            if (opOpt.isEmpty()) {
                MetricsService.incrementVersionRejections();
                LOGGER.debug("[C2S_Click] version mismatch requestId={} player={}", requestId, player.getName().getString());
                ModNetwork.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new S2CActionRejectedPacket(requestId, S2CActionRejectedPacket.Reason.VERSION_MISMATCH));
                return;
            }

            // 3. Slot validation
            OpContext op = opOpt.get();
            boolean success = false;
            try {
                if (!InventoryTransactionService.validateAndApplyClick(player, canonicalSlotIndex, op.opId)) {
                    MetricsService.incrementValidationRejections();
                    LOGGER.debug("[C2S_Click] validation failed slot={} requestId={}", canonicalSlotIndex, requestId);
                    ModNetwork.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new S2CActionRejectedPacket(requestId, S2CActionRejectedPacket.Reason.VALIDATION_FAILED));
                    return;
                }
                success = true;
            } finally {
                InventoryTransactionService.endLoadoutOp(op, success);
            }
        });
        ctx.setPacketHandled(true);
    }
}

