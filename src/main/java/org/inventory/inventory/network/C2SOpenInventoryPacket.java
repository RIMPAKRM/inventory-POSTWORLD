package org.inventory.inventory.network;

import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkEvent;
import org.inventory.inventory.menu.CustomInventoryMenu;
import org.inventory.inventory.server.LoadoutSyncScheduler;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * Client → Server: request to open the custom inventory screen.
 *
 * Sent by the client when the player presses 'E' (or equivalent) in
 * survival/adventure mode. The server opens the {@link CustomInventoryMenu}
 * for the player, which causes Minecraft to send {@code ClientboundOpenScreenPacket}
 * back. The client then shows {@code InventoryScreen} via {@code MenuScreens}.
 *
 * Creative mode players are NOT expected to send this packet; they use the
 * vanilla inventory UI (enforced in {@code ClientEventHandler}).
 */
public final class C2SOpenInventoryPacket {

    private static final Logger LOGGER = LogUtils.getLogger();

    public C2SOpenInventoryPacket() {}

    // ---- Codec (no payload) ----

    public void encode(FriendlyByteBuf buf) {}

    public static C2SOpenInventoryPacket decode(FriendlyByteBuf buf) {
        return new C2SOpenInventoryPacket();
    }

    // ---- Handler ----

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (player.isCreative()) {
                LOGGER.debug("[C2S_OpenInv] ignoring request from creative player={}", player.getName().getString());
                return;
            }

            LOGGER.debug("[C2S_OpenInv] opening custom inventory for player={}", player.getName().getString());
            player.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("container.inventory.custom");
                }

                @Override
                public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                    return new CustomInventoryMenu(id, inv);
                }
            });

            LoadoutSyncScheduler.sendImmediately(player);
        });
        ctx.setPacketHandled(true);
    }
}

