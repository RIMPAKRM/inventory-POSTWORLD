package org.inventory.inventory.network;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import org.inventory.inventory.capability.LoadoutCapability;
import org.inventory.inventory.client.ClientLoadoutState;
import org.inventory.inventory.client.PendingActionTracker;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Server → Client: authoritative sync of player loadout state.
 *
 * Contains the server-side loadout version and authoritative serialized loadout so the client can:
 *  1. Accept and apply the new version for future C2S packets.
 *  2. Clear all pending (unconfirmed) actions up to this version.
 *  3. Refresh open inventory/craft screens with the latest active rows/items.
 */
public final class S2CLoadoutSyncPacket {

    private static final Logger LOGGER = LogUtils.getLogger();

    public final UUID targetPlayerId;
    public final long serverVersion;
    public final CompoundTag loadoutNbt;

    public S2CLoadoutSyncPacket(UUID targetPlayerId, long serverVersion, CompoundTag loadoutNbt) {
        this.targetPlayerId = targetPlayerId;
        this.serverVersion = serverVersion;
        this.loadoutNbt = loadoutNbt == null ? new CompoundTag() : loadoutNbt.copy();
    }

    // ---- Codec ----

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(targetPlayerId);
        buf.writeLong(serverVersion);
        buf.writeNbt(loadoutNbt);
    }

    public static S2CLoadoutSyncPacket decode(FriendlyByteBuf buf) {
        UUID targetPlayerId = buf.readUUID();
        long version = buf.readLong();
        CompoundTag nbt = buf.readNbt();
        return new S2CLoadoutSyncPacket(targetPlayerId, version, nbt);
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Player targetPlayer = mc.level.getPlayerByUUID(targetPlayerId);
        if (targetPlayer == null && mc.player != null && mc.player.getUUID().equals(targetPlayerId)) {
            targetPlayer = mc.player;
        }
        if (targetPlayer == null) return;

        LOGGER.debug("[S2C_Sync] received target={} serverVersion={}", targetPlayerId, serverVersion);

        targetPlayer.getCapability(LoadoutCapability.PLAYER_LOADOUT).ifPresent(loadout ->
                loadout.deserializeNBT(loadoutNbt.copy()));

        if (mc.player != null && mc.player.getUUID().equals(targetPlayerId)) {
            ClientLoadoutState.onServerSync(serverVersion);

            // Clear pending actions that have been confirmed
            PendingActionTracker.onServerAck(serverVersion);

            // Refresh open screens
            if (mc.screen instanceof org.inventory.inventory.client.screen.InventoryScreen invScreen) {
                invScreen.onLoadoutSync(serverVersion);
            } else if (mc.screen instanceof org.inventory.inventory.client.screen.CraftScreen craftScreen) {
                craftScreen.onLoadoutSync(serverVersion);
            }
        }
    }
}

