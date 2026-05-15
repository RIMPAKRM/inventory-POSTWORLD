package org.inventory.inventory.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.inventory.inventory.capability.PlayerLoadout;
import org.inventory.inventory.client.PendingActionTracker;
import org.inventory.inventory.domain.EquipmentSlotType;
import org.inventory.inventory.menu.CustomInventoryMenu;
import org.inventory.inventory.network.C2SOpenInventoryPacket;
import org.slf4j.Logger;

import java.util.Locale;

/**
 * Custom Inventory Screen (survival / adventure only).
 *
 * Layout:
 *  - Left panel  : equipment slots (HEAD → FEET) in a column
 *  - Centre      : player model preview (EntityRenderDispatcher)
 *  - Right panel : dynamic storage grid
 *  - Bottom      : vanilla hotbar row + main inventory grid
 *  - Top-right   : "Craft" button → opens CraftScreen
 *
 * When pending actions exist, an overlay indicator is shown.
 * When the server rejects an action, a brief flash animation is shown.
 *
 * Creative mode: this screen is never opened for creative players.
 * The client-side event handler ({@link org.inventory.inventory.event.ClientEventHandler})
 * intercepts the inventory key and sends {@link C2SOpenInventoryPacket} only for
 * survival/adventure players.
 */
@OnlyIn(Dist.CLIENT)
public class InventoryScreen extends AbstractContainerScreen<CustomInventoryMenu> {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Width / height of the GUI background (matches texture). */
    private static final int GUI_WIDTH  = 240;
    private static final int GUI_HEIGHT = 230;

        private static final int PANEL_FILL = 0xFF1A1D21;
        private static final int PANEL_FILL_2 = 0xFF232830;
        private static final int HEADER_FILL = 0xFF34424D;
        private static final int HEADER_ACCENT = 0xFFB39B62;
    private static final int STAT_LABEL_COLOUR = 0xC9D4D7;
    private static final int STAT_VALUE_COLOUR = 0xFCB86C;
    private static final int SLOT_BG = 0xFF2A2F36;
    private static final int SLOT_BG_HILITE = 0xFF37404A;
    private static final float DEFENSE_REFERENCE_DAMAGE = 10.0f;

    private static final int PENDING_OVERLAY_COLOUR = 0x44FFFF00;
    /** Rejection-flash colour (semi-transparent red). */
    private static final int REJECT_FLASH_COLOUR    = 0x44FF4444;

    private int rejectFlashTicks = 0;
    private int cachedDynamicPanelBottom = CustomInventoryMenu.DYNAMIC_Y + 74;
    private int cachedDynamicPanelRight = CustomInventoryMenu.DYNAMIC_X + 110;
    private int layoutCacheKey = Integer.MIN_VALUE;

    public InventoryScreen(CustomInventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth  = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    // ---- Lifecycle ----

    @Override
    protected void init() {
        super.init();

        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = CustomInventoryMenu.HOTBAR_Y - 12;

        // "Craft" button — opens CraftScreen
        int btnX = leftPos + imageWidth - 60;
        int btnY = topPos + 1;
        addRenderableWidget(Button.builder(
                Component.translatable("gui.inventory.craft"),
                btn -> openCraftScreen()
        ).bounds(btnX, btnY, 52, 16).build());

        refreshLayoutCache(true);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (rejectFlashTicks > 0) rejectFlashTicks--;
        refreshLayoutCache(false);
    }

    // ---- Rendering ----

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        int right = leftPos + imageWidth;
        int bottom = topPos + imageHeight;

        graphics.fill(leftPos, topPos, right, bottom, PANEL_FILL);
        graphics.fill(leftPos + 4, topPos + 4, right - 4, bottom - 4, PANEL_FILL_2);

        int headerBottom = topPos + 18;
        graphics.fill(leftPos + 4, topPos + 4, right - 4, headerBottom, HEADER_FILL);
        graphics.fill(leftPos + 4, headerBottom - 1, right - 4, headerBottom, HEADER_ACCENT);

        // Equipment block.
        int equipLeft = leftPos + CustomInventoryMenu.EQUIPMENT_LEFT_X - 4;
        int equipTop = topPos + CustomInventoryMenu.EQUIPMENT_TOP_Y - 4;
        int equipRight = equipLeft + 44;
        int equipBottom = equipTop + 80;
        graphics.fill(equipLeft, equipTop, equipRight, equipBottom, PANEL_FILL);

        // Character preview block.
        int previewLeft = leftPos + 56;
        int previewTop = topPos + 18;
        int previewRight = leftPos + 122;
        int previewBottom = topPos + 100;
        graphics.fill(previewLeft, previewTop, previewRight, previewBottom, PANEL_FILL_2);

        // Center player render, same interaction style as vanilla.
        if (this.minecraft != null && this.minecraft.player != null) {
            net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics,
                    leftPos + 92,
                    topPos + 92,
                    30,
                    (float) (leftPos + 92) - mouseX,
                    (float) (topPos + 62) - mouseY,
                    this.minecraft.player
            );
        }

        // Stats display under player model
        renderCharacterStats(graphics);

        // Dynamic storage block.
        int dynLeft = leftPos + CustomInventoryMenu.DYNAMIC_X - 4;
        int dynTop = topPos + CustomInventoryMenu.DYNAMIC_Y - 4;
        int dynRight = leftPos + cachedDynamicPanelRight + 4;
        int dynBottom = topPos + cachedDynamicPanelBottom;
        graphics.fill(dynLeft, dynTop, dynRight, dynBottom, PANEL_FILL);

        // Hotbar block only (no 3x9 vanilla main grid in this design).
        int hotbarLeft = leftPos + CustomInventoryMenu.VANILLA_INV_X - 4;
        int hotbarTop = topPos + CustomInventoryMenu.HOTBAR_Y - 4;
        int hotbarRight = hotbarLeft + 170;
        int hotbarBottom = hotbarTop + 26;
        graphics.fill(hotbarLeft, hotbarTop, hotbarRight, hotbarBottom, PANEL_FILL_2);

        renderSlotBackdrops(graphics);

        // Pending indicator overlay
        if (PendingActionTracker.hasPending()) {
            graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PENDING_OVERLAY_COLOUR);
        }

        // Rejection flash
        if (rejectFlashTicks > 0) {
            int alpha = (int) ((rejectFlashTicks / 10f) * 0x44);
            int rgb = REJECT_FLASH_COLOUR & 0x00FFFFFF;
            graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight,
                    (alpha << 24) | rgb);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Title
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xF2E7D0, false);
        // Inventory label
        graphics.drawString(this.font, this.playerInventoryTitle,
            this.inventoryLabelX, this.inventoryLabelY, 0xC9D4D7, false);

    }

    private Component buildDynamicRowLabel(EquipmentSlotType provider) {
        var equipped = menu.getEquippedItemForProvider(provider);
        if (!equipped.isEmpty()) {
            return equipped.getHoverName();
        }
        return Component.translatable("slot." + provider.name().toLowerCase());
    }

    private void renderSlotBackdrops(GuiGraphics graphics) {
        int limit = this.menu.slots.size();
        for (int i = 0; i < limit; i++) {
            Slot slot = this.menu.slots.get(i);
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
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
        renderSlotHintTooltip(graphics, mouseX, mouseY);
    }

    // ---- Sync callbacks (called from S2CLoadoutSyncPacket) ----

    /**
     * Called when a {@code S2CLoadoutSyncPacket} is received.
     * Refreshes slot display and clears confirmed pending entries.
     */
    public void onLoadoutSync(long serverVersion) {
        LOGGER.debug("[InventoryScreen] sync received version={}", serverVersion);
        refreshLayoutCache(true);
        // PendingActionTracker already cleared entries in the packet handler
    }

    /** Trigger the rejection flash animation. */
    public void onActionRejected() {
        rejectFlashTicks = 10; // 0.5 s at 20 TPS
    }

    // ---- Actions ----

    private void openCraftScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.setScreen(new CraftScreen(this));
    }

    private void refreshLayoutCache(boolean force) {
        int newKey = computeLayoutCacheKey();
        if (!force && newKey == layoutCacheKey) {
            return;
        }
        layoutCacheKey = newKey;

        int maxBottom = CustomInventoryMenu.DYNAMIC_Y + 30;
        int maxRight = CustomInventoryMenu.DYNAMIC_X + 40;
        for (EquipmentSlotType provider : PlayerLoadout.STORAGE_PROVIDERS) {
            int activeSlots = menu.getActiveSlotsForProvider(provider);
            if (activeSlots <= 0) {
                continue;
            }

            int cols = CustomInventoryMenu.getDynamicColumns(provider);
            int rows = (activeSlots + cols - 1) / cols;
            int right = CustomInventoryMenu.getDynamicProviderX(provider) + cols * 18 + 4;
            int bottom = CustomInventoryMenu.getDynamicProviderY(provider) + rows * 18 + 4;
            maxRight = Math.max(maxRight, right);
            maxBottom = Math.max(maxBottom, bottom);
        }

        cachedDynamicPanelRight = Math.min(imageWidth - 8, maxRight + 6);
        cachedDynamicPanelBottom = Math.min(CustomInventoryMenu.HOTBAR_Y - 8, maxBottom + 8);

    }

    private int computeLayoutCacheKey() {
        int hash = 1;
        for (EquipmentSlotType provider : PlayerLoadout.STORAGE_PROVIDERS) {
            hash = 31 * hash + menu.getActiveSlotsForProvider(provider);
            ItemStack equipped = menu.getEquippedItemForProvider(provider);
            hash = 31 * hash + (equipped.isEmpty() ? 0 : equipped.getItem().hashCode());
            hash = 31 * hash + (equipped.hasTag() && equipped.getTag() != null ? equipped.getTag().hashCode() : 0);
            hash = 31 * hash + equipped.getCount();
        }
        return hash;
    }

    private void renderSlotHintTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        Slot hovered = this.hoveredSlot;
        if (hovered == null) {
            return;
        }
        if (hovered.hasItem()) {
            return;
        }

        if (hovered instanceof CustomInventoryMenu.EquipmentItemSlot equipmentSlot) {
            EquipmentSlotType type = equipmentSlot.getEquipmentSlotType();
            Component typeName = Component.translatable("slot." + type.name().toLowerCase(Locale.ROOT));
            graphics.renderTooltip(this.font,
                    Component.translatable("gui.inventory.slot_hint.equipment", typeName),
                    mouseX, mouseY);
            return;
        }

        if (hovered instanceof CustomInventoryMenu.DynamicStorageSlot dynamicSlot) {
            EquipmentSlotType provider = dynamicSlot.getProviderType();
            if (provider == null) {
                return;
            }
            Component providerName = buildDynamicRowLabel(provider);
            graphics.renderTooltip(this.font,
                    Component.translatable("gui.inventory.slot_hint.pocket", providerName),
                    mouseX, mouseY);
        }
    }

    private void renderCharacterStats(GuiGraphics graphics) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }

        net.minecraft.world.entity.player.Player player = this.minecraft.player;
        int startX = leftPos + 48;
        int startY = topPos + 102;
        int lineHeight = 10;

        // Defense %
        float armor = (float) player.getArmorValue();
        float toughness = (float) player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float damageAfterArmor = CombatRules.getDamageAfterAbsorb(DEFENSE_REFERENCE_DAMAGE, armor, toughness);
        int enchantProtection = EnchantmentHelper.getDamageProtection(player.getArmorSlots(), player.damageSources().generic());
        float damageAfterEnchant = damageAfterArmor * (1.0f - Math.min(20.0f, enchantProtection) / 25.0f);
        int defensePercent = Math.round((1.0f - (damageAfterEnchant / DEFENSE_REFERENCE_DAMAGE)) * 100f);

        graphics.drawString(this.font, "Защита: ", startX, startY, STAT_LABEL_COLOUR, false);
        graphics.drawString(this.font, defensePercent + "%", startX + 50, startY, STAT_VALUE_COLOUR, false);


        // Health %
        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        int healthPercent = Math.round((health / maxHealth) * 100f);
        graphics.drawString(this.font, "Здоровье: ", startX, startY + lineHeight, STAT_LABEL_COLOUR, false);
        graphics.drawString(this.font, healthPercent + "%", startX + 50, startY + lineHeight, STAT_VALUE_COLOUR, false);

        // Hunger %
        int foodLevel = player.getFoodData().getFoodLevel();
        int maxFood = 20;
        int hungerPercent = Math.round((foodLevel / (float) maxFood) * 100f);
        graphics.drawString(this.font, "Голод: ", startX, startY + lineHeight * 2, STAT_LABEL_COLOUR, false);
        graphics.drawString(this.font, hungerPercent + "%", startX + 50, startY + lineHeight * 2, STAT_VALUE_COLOUR, false);

        // Radiation (placeholder for future)
        graphics.drawString(this.font, "Радиация: ", startX, startY + lineHeight * 3, STAT_LABEL_COLOUR, false);
        graphics.drawString(this.font, "0%", startX + 50, startY + lineHeight * 3, STAT_VALUE_COLOUR, false);
    }
}

