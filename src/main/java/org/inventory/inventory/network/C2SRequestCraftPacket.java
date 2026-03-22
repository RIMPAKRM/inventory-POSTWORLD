package org.inventory.inventory.network;

import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.inventory.inventory.server.CraftService;
import org.inventory.inventory.server.LoadoutSyncScheduler;
import org.inventory.inventory.server.MetricsService;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Client → Server: request to craft a specific recipe.
 *
 * Fields:
 *   requestId         – unique per-request ID for dedup
 *   craftId           – ResourceLocation of the CraftCard to execute
 *   clientViewVersion – client's current loadoutVersion for optimistic concurrency
 */
public final class C2SRequestCraftPacket {

    private static final Logger LOGGER = LogUtils.getLogger();

    public final UUID requestId;
    public final ResourceLocation craftId;
    public final long clientViewVersion;

    public C2SRequestCraftPacket(UUID requestId, ResourceLocation craftId, long clientViewVersion) {
        this.requestId = requestId;
        this.craftId   = craftId;
        this.clientViewVersion = clientViewVersion;
    }

    // ---- Codec ----

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(requestId);
        buf.writeResourceLocation(craftId);
        buf.writeLong(clientViewVersion);
    }

    public static C2SRequestCraftPacket decode(FriendlyByteBuf buf) {
        UUID requestId           = buf.readUUID();
        ResourceLocation craftId = buf.readResourceLocation();
        long version             = buf.readLong();
        return new C2SRequestCraftPacket(requestId, craftId, version);
    }

    // ---- Handler (server thread) ----

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            // Dedup check
            if (RequestDedup.isDuplicate(player.getUUID(), requestId)) {
                MetricsService.incrementDedupRejections();
                LOGGER.debug("[C2S_Craft] dedup rejected requestId={} player={}", requestId, player.getName().getString());
                return;
            }

            // Dispatch to CraftService
            CraftService.CraftResult result = CraftService.requestCraft(player, craftId, requestId, clientViewVersion);
            LOGGER.debug("[C2S_Craft] result={} craftId={} requestId={} player={}",
                    result, craftId, requestId, player.getName().getString());

            if (result != CraftService.CraftResult.SUCCESS) {
                MetricsService.incrementValidationRejections();
                S2CActionRejectedPacket.Reason reason = switch (result) {
                    case UNKNOWN_RECIPE      -> S2CActionRejectedPacket.Reason.UNKNOWN_RECIPE;
                    case MISSING_INGREDIENTS -> S2CActionRejectedPacket.Reason.MISSING_INGREDIENTS;
                    case VERSION_MISMATCH    -> S2CActionRejectedPacket.Reason.VERSION_MISMATCH;
                    default                  -> S2CActionRejectedPacket.Reason.VALIDATION_FAILED;
                };

                if (result == CraftService.CraftResult.VERSION_MISMATCH) {
                    LoadoutSyncScheduler.sendImmediately(player);
                }

                ModNetwork.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new S2CActionRejectedPacket(requestId, reason));
            }
        });
        ctx.setPacketHandled(true);
    }
}

