package org.inventory.inventory.event;

import com.mojang.logging.LogUtils;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.inventory.inventory.Inventory;
import org.inventory.inventory.client.PendingActionTracker;
import org.inventory.inventory.network.C2SOpenInventoryPacket;
import org.inventory.inventory.network.ModNetwork;
import org.slf4j.Logger;

/**
 * Client-only event handler for Phase B UI integration.
 *
 * Responsibilities:
 *  1. Intercept the vanilla {@link InventoryScreen} opening for
 *     survival/adventure players and redirect to the custom inventory
 *     (by sending {@link C2SOpenInventoryPacket} to the server).
 *  2. Clear pending action state when any screen is closed.
 *
 * Creative mode: vanilla inventory screen is NOT intercepted, so creative
 * players always see the standard UI.
 */
@Mod.EventBusSubscriber(modid = Inventory.MODID, value = Dist.CLIENT)
public final class ClientEventHandler {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long OPEN_REQUEST_COOLDOWN_MS = 200L;
    private static long lastOpenRequestAtMs = 0L;

    private ClientEventHandler() {}

    /**
     * Intercept the vanilla inventory screen and replace it with our custom
     * inventory by asking the server to open the custom menu.
     *
     * Cancels the vanilla screen; once the server processes {@link C2SOpenInventoryPacket}
     * it calls {@code player.openMenu(CustomInventoryMenu)}, which triggers the
     * standard container-open flow and shows our registered {@code InventoryScreen}.
     */
    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!(event.getScreen() instanceof InventoryScreen)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Creative players always use vanilla UI — do NOT intercept
        if (mc.player.isCreative()) {
            LOGGER.debug("[ClientEvents] creative mode — using vanilla inventory");
            return;
        }

        long now = Util.getMillis();
        if (now - lastOpenRequestAtMs < OPEN_REQUEST_COOLDOWN_MS) {
            event.setCanceled(true);
            return;
        }
        lastOpenRequestAtMs = now;

        LOGGER.debug("[ClientEvents] intercepting vanilla inventory — requesting custom UI");
        event.setCanceled(true);
        ModNetwork.CHANNEL.sendToServer(new C2SOpenInventoryPacket());
    }

    /** Clear pending state when a screen is closed (prevents stale indicators). */
    @SubscribeEvent
    public static void onScreenClose(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof org.inventory.inventory.client.screen.InventoryScreen
                || event.getScreen() instanceof org.inventory.inventory.client.screen.CraftScreen) {
            PendingActionTracker.clear();
        }
    }
}

