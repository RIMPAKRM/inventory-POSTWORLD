package org.inventory.inventory.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Generic browser-style container screen used for chests and storage-like menus.
 *
 * The underlying menu stays untouched; only slot geometry and presentation are
 * adapted on the client so arbitrary storage sizes keep a consistent layout.
 */
@OnlyIn(Dist.CLIENT)
public class ContainerBrowserScreen extends AbstractContainerScreen<AbstractContainerMenu> {

    private static final int SLOT_BORDER = 1;
    private static final int PANEL_PADDING = 6;
    private static final int HEADER_HEIGHT = 18;
    private static final int PANEL_FILL = 0xFF1A1D21;
    private static final int PANEL_FILL_2 = 0xFF232830;
    private static final int HEADER_FILL = 0xFF34424D;
    private static final int HEADER_ACCENT = 0xFFB39B62;
    private static final int SLOT_BG = 0xFF2A2F36;
    private static final int SLOT_BG_HILITE = 0xFF37404A;

    private final int storageSlotCount;
    private final ContainerBrowserLayout.Layout layout;

    public ContainerBrowserScreen(AbstractContainerScreen<?> source, Inventory playerInventory) {
        this(source.getMenu(), playerInventory, source.getTitle());
        this.imageWidth = source.getXSize();
        this.imageHeight = source.getYSize();
    }

    public ContainerBrowserScreen(AbstractContainerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.storageSlotCount = Math.max(0, menu.slots.size() - ContainerBrowserLayout.PLAYER_INVENTORY_SLOTS);
        this.layout = ContainerBrowserLayout.create(storageSlotCount);
        this.imageWidth = Math.max(layout.screenWidth(), 176);
        this.imageHeight = Math.max(layout.screenHeight(), 166);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = Math.max(0, imageHeight - 94);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        int left = leftPos;
        int top = topPos;
        int right = left + imageWidth;
        int bottom = top + imageHeight;

        graphics.fill(left, top, right, bottom, PANEL_FILL);
        graphics.fill(left + 4, top + 4, right - 4, bottom - 4, PANEL_FILL_2);

        int headerBottom = top + HEADER_HEIGHT;
        graphics.fill(left + 4, top + 4, right - 4, headerBottom, HEADER_FILL);
        graphics.fill(left + 4, headerBottom - 1, right - 4, headerBottom, HEADER_ACCENT);

        int storagePanelBottom = top + Math.max(HEADER_HEIGHT + 24, imageHeight - 100);
        graphics.fill(left + 4, headerBottom, right - 4, storagePanelBottom, PANEL_FILL);

        int playerPanelTop = Math.max(storagePanelBottom - 6, top + imageHeight - 100);
        graphics.fill(left + 4, playerPanelTop, right - 4, bottom - 4, PANEL_FILL);

        renderSlotBackdrops(graphics);
    }

    private void renderSlotBackdrops(GuiGraphics graphics) {
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) {
                continue;
            }
            int x = leftPos + slot.x - SLOT_BORDER;
            int y = topPos + slot.y - SLOT_BORDER;
            graphics.fill(x, y, x + ContainerBrowserLayout.SLOT_SIZE + SLOT_BORDER * 2, y + ContainerBrowserLayout.SLOT_SIZE + SLOT_BORDER * 2, SLOT_BG);
            graphics.fill(x + 1, y + 1, x + ContainerBrowserLayout.SLOT_SIZE + SLOT_BORDER, y + ContainerBrowserLayout.SLOT_SIZE + SLOT_BORDER, SLOT_BG_HILITE);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xF2E7D0, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xC9D4D7, false);
    }

    public int getStorageSlotCount() {
        return storageSlotCount;
    }
}