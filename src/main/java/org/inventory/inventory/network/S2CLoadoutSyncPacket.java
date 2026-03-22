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

    public final long serverVersion;
    public final CompoundTag loadoutNbt;

    public S2CLoadoutSyncPacket(long serverVersion, CompoundTag loadoutNbt) {
        this.serverVersion = serverVersion;
        this.loadoutNbt = loadoutNbt == null ? new CompoundTag() : loadoutNbt.copy();
    }

    // ---- Codec ----

    public void encode(FriendlyByteBuf buf) {
        buf.writeLong(serverVersion);
        buf.writeNbt(loadoutNbt);
    }

    public static S2CLoadoutSyncPacket decode(FriendlyByteBuf buf) {
        long version = buf.readLong();
        CompoundTag nbt = buf.readNbt();
        return new S2CLoadoutSyncPacket(version, nbt);
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
        Player player = mc.player;
        if (player == null) return;

        LOGGER.debug("[S2C_Sync] received serverVersion={}", serverVersion);

        player.getCapability(LoadoutCapability.PLAYER_LOADOUT).ifPresent(loadout ->
                loadout.deserializeNBT(loadoutNbt.copy()));

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

