package org.inventory.inventory.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.inventory.inventory.domain.EquipmentSlotType;
import org.inventory.inventory.menu.CustomInventoryMenu;
import org.inventory.inventory.menu.StorageBrowserMenu;

/**
 * Browser screen for container openings. Shows the custom loadout on the left
 * and the opened storage as a vertical panel on the right, with the hotbar at
 * the bottom and no vanilla 3x9 inventory grid.
 */
@OnlyIn(Dist.CLIENT)
public class StorageBrowserScreen extends AbstractContainerScreen<StorageBrowserMenu> {

    private static final int PANEL_FILL = 0xFF1A1D21;
    private static final int PANEL_FILL_2 = 0xFF232830;
    private static final int HEADER_FILL = 0xFF34424D;
    private static final int HEADER_ACCENT = 0xFFB39B62;
    private static final int SLOT_BG = 0xFF2A2F36;
    private static final int SLOT_BG_HILITE = 0xFF37404A;

    private final int storageSlotCount;
    private final int storageColumns;

    public StorageBrowserScreen(StorageBrowserMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.storageSlotCount = menu.getStorageSlotCount();
        this.storageColumns = menu.getStorageColumns();
        this.imageWidth = Math.max(272, 210 + storageColumns * 18 + 8);
        this.imageHeight = Math.max(246, menu.getSlot(menu.getHotbarStartIndex()).y + 26);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = menu.getSlot(menu.getHotbarStartIndex()).y - 12;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        int left = leftPos;
        int top = topPos;
        int right = left + imageWidth;
        int bottom = top + imageHeight;

        graphics.fill(left, top, right, bottom, PANEL_FILL);
        graphics.fill(left + 0, top + 0, right - 0, bottom - 0, PANEL_FILL_2);

        int headerBottom = top + 18;
        graphics.fill(left + 4, top + 4, right - 4, headerBottom, HEADER_FILL);
        graphics.fill(left + 4, headerBottom - 1, right - 4, headerBottom, HEADER_ACCENT);

        int storagePanelLeft = left + 208;
        int storagePanelRight = right - 4;
        int storagePanelBottom = top + menu.getSlot(menu.getHotbarStartIndex()).y - 6;
        graphics.fill(storagePanelLeft, headerBottom, storagePanelRight, storagePanelBottom, PANEL_FILL);

        int loadoutPanelBottom = storagePanelBottom;
        graphics.fill(left + 4, headerBottom, storagePanelLeft - 6, loadoutPanelBottom, PANEL_FILL);

        int hotbarTop = top + menu.getSlot(menu.getHotbarStartIndex()).y - 6;
        graphics.fill(left + 4, hotbarTop, right - 4, bottom - 4, PANEL_FILL);

        renderSlotBackdrops(graphics);
    }

    private void renderSlotBackdrops(GuiGraphics graphics) {
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) {
                continue;
            }
            int x = leftPos + slot.x - 1;
            int y = topPos + slot.y - 1;
            graphics.fill(x, y, x + 19, y + 19, SLOT_BG);
            graphics.fill(x + 1, y + 1, x + 18, y + 18, SLOT_BG_HILITE);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderStorageTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xF2E7D0, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xC9D4D7, false);
    }

    private void renderStorageTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (hoveredSlot == null || hoveredSlot.hasItem()) {
            return;
        }

        if (hoveredSlot instanceof CustomInventoryMenu.EquipmentItemSlot equipmentSlot) {
            EquipmentSlotType type = equipmentSlot.getEquipmentSlotType();
            graphics.renderTooltip(font,
                    Component.translatable("gui.inventory.slot_hint.equipment",
                            Component.translatable("slot." + type.name().toLowerCase(java.util.Locale.ROOT))),
                    mouseX, mouseY);
            return;
        }

        if (hoveredSlot instanceof CustomInventoryMenu.DynamicStorageSlot dynamicSlot) {
            EquipmentSlotType provider = dynamicSlot.getProviderType();
            if (provider != null) {
                graphics.renderTooltip(font,
                        Component.translatable("gui.inventory.slot_hint.pocket",
                                Component.translatable("slot." + provider.name().toLowerCase(java.util.Locale.ROOT))),
                        mouseX, mouseY);
            }
        }
    }
}