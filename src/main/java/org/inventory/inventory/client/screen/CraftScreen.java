package org.inventory.inventory.client.screen;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.inventory.inventory.capability.IPlayerLoadout;
import org.inventory.inventory.capability.LoadoutCapability;
import org.inventory.inventory.client.ClientLoadoutState;
import org.inventory.inventory.client.PendingActionTracker;
import org.inventory.inventory.domain.CraftCard;
import org.inventory.inventory.domain.CraftCardRegistry;
import org.inventory.inventory.domain.CraftCategory;
import org.inventory.inventory.domain.ProtectionProfileRegistry;
import org.inventory.inventory.domain.StorageProfileRegistry;
import org.inventory.inventory.network.C2SRequestCraftPacket;
import org.inventory.inventory.network.ModNetwork;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;

/**
 * Craft Screen — lists available recipes grouped by category.
 *
 * Layout:
 *  - Left panel  : scrollable category list (one button per category)
 *  - Centre      : recipe cards for the selected category
 *    Each card shows: ingredient icons + amounts, result icon + name
 *  - Right panel : selected recipe details + "Craft" button
 *
 * Pending / rollback UX:
 *  - While a craft request is in-flight, the "Craft" button is disabled
 *    and shows "Crafting…" text.
 *  - If the server rejects the request, a flash animation is shown and
 *    the button is re-enabled.
 *
 * Phase C: categories and cards will be loaded from JSON data files.
 */
@OnlyIn(Dist.CLIENT)
public class CraftScreen extends Screen {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int CATEGORY_PANEL_WIDTH = 86;
    private static final int CARD_LIST_LEFT = 98;
    private static final int CARD_ROW_HEIGHT = 24;
    private static final int CARD_LIST_TOP = 30;
    private static final int CATEGORY_HEIGHT = 18;
    private static final int PANEL_TOP = 24;
    private static final int PANEL_BOTTOM_MARGIN = 24;
    private static final Component CRAFT_BUTTON_LABEL = Component.translatable("gui.craft.craft_button");
    private static final Component CRAFTING_BUTTON_LABEL = Component.translatable("gui.craft.crafting");
    private static final Component PENDING_LABEL = Component.translatable("gui.craft.pending");
    private static final Component INGREDIENTS_LABEL = Component.translatable("gui.craft.ingredients");
    private static final Component STATS_LABEL = Component.translatable("gui.craft.stats");
    private static final Component DETAILS_HINT_LABEL = Component.translatable("gui.craft.details_hint");

    private final Screen parent;

    private CraftCategory selectedCategory = null;
    private CraftCard selectedCard = null;
    private final List<CraftCard> visibleCards = new ArrayList<>();
    private int cardScroll = 0;

    private final List<Button> categoryButtons = new ArrayList<>();
    private Button craftButton;

    private UUID pendingRequestId = null;
    private int rejectFlashTicks  = 0;

    public CraftScreen(Screen parent) {
        super(Component.translatable("gui.craft.title"));
        this.parent = parent;
    }

    // ---- Lifecycle ----

    @Override
    protected void init() {
        categoryButtons.clear();

        int catX = 10;
        int catY = CARD_LIST_TOP;

        for (CraftCategory category : CraftCardRegistry.allCategoriesSorted()) {
            final CraftCategory cat = category;
            Button btn = Button.builder(
                    Component.literal(cat.getDisplayName()),
                    b -> selectCategory(cat)
            ).bounds(catX, catY, CATEGORY_PANEL_WIDTH - 6, CATEGORY_HEIGHT).build();
            categoryButtons.add(btn);
            addRenderableWidget(btn);
            catY += CATEGORY_HEIGHT + 2;
        }

        // "Craft" button (bottom right)
        craftButton = addRenderableWidget(Button.builder(
                CRAFT_BUTTON_LABEL,
                b -> executeSelectedCraft()
        ).bounds(this.width - 90, this.height - 30, 80, 20).build());
        craftButton.active = false;

        // Back button
        addRenderableWidget(Button.builder(
                Component.translatable("gui.back"),
                b -> onClose()
        ).bounds(10, this.height - 30, 60, 20).build());

        // Select first category if any
        if (!CraftCardRegistry.allCategoriesSorted().isEmpty()) {
            selectCategory(CraftCardRegistry.allCategoriesSorted().get(0));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (rejectFlashTicks > 0) rejectFlashTicks--;

        // Re-enable craft button once pending request clears
        if (pendingRequestId != null && !PendingActionTracker.isPending(pendingRequestId)) {
            pendingRequestId = null;
            updateCraftButtonState();
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    // ---- Rendering ----

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        int categoryPanelLeft = 5;
        int categoryPanelRight = CATEGORY_PANEL_WIDTH + 5;
        int listLeft = CARD_LIST_LEFT;
        int detailLeft = getDetailPanelLeft();
        int panelBottom = this.height - PANEL_BOTTOM_MARGIN;

        graphics.fill(categoryPanelLeft, PANEL_TOP, categoryPanelRight, panelBottom, 0xAA000000);
        graphics.fill(listLeft, PANEL_TOP, detailLeft - 4, panelBottom, 0x88000000);
        graphics.fill(detailLeft, PANEL_TOP, this.width - 5, panelBottom, 0x99000000);

        renderCardList(graphics, mouseX, mouseY, listLeft, detailLeft - 4, panelBottom);

        // Rejection flash
        if (rejectFlashTicks > 0) {
            int alpha = (int) ((rejectFlashTicks / 10f) * 0x66);
            graphics.fill(0, 0, this.width, this.height, (alpha << 24) | 0xFF4444);
        }

        // Pending indicator
        if (pendingRequestId != null && PendingActionTracker.isPending(pendingRequestId)) {
            graphics.drawString(this.font,
                    PENDING_LABEL,
                    this.width - 90, this.height - 44, 0xFFFF80, false);
        }

        // Title
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);

        renderSelectedCardDetails(graphics, mouseX, mouseY, detailLeft);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderCardList(GuiGraphics graphics, int mouseX, int mouseY, int listLeft, int listRight, int panelBottom) {
        int rowY = CARD_LIST_TOP;
        int visibleRows = getVisibleCardRows(panelBottom);
        int fromIndex = cardScroll;
        int toIndex = Math.min(visibleCards.size(), fromIndex + visibleRows);

        for (int i = fromIndex; i < toIndex; i++) {
            CraftCard card = visibleCards.get(i);
            int y = rowY + (i - fromIndex) * CARD_ROW_HEIGHT;
            int rowBottom = y + CARD_ROW_HEIGHT - 2;

            boolean selected = card == selectedCard;
            boolean hovered = mouseX >= listLeft + 2 && mouseX <= listRight - 6 && mouseY >= y && mouseY <= rowBottom;
            int bg = selected ? 0xAA356090 : (hovered ? 0x88404040 : 0x66303030);
            graphics.fill(listLeft + 2, y, listRight - 6, rowBottom, bg);

            ItemStack result = card.getResult();
            graphics.renderItem(result, listLeft + 6, y + 3);

            int color = canAfford(card) ? 0xE0E0E0 : 0xC06060;
            Component line = Component.literal(result.getHoverName().getString() + " x" + result.getCount());
            graphics.drawString(this.font, line, listLeft + 26, y + 8, color, false);
        }

        renderCardScrollbar(graphics, listRight, panelBottom, visibleRows);
    }

    private void renderCardScrollbar(GuiGraphics graphics, int listRight, int panelBottom, int visibleRows) {
        if (visibleCards.size() <= visibleRows || visibleRows <= 0) {
            return;
        }

        int trackTop = CARD_LIST_TOP;
        int trackBottom = panelBottom - 2;
        int trackHeight = Math.max(1, trackBottom - trackTop);
        int maxScroll = getMaxCardScroll(visibleRows);
        int thumbHeight = Math.max(14, trackHeight * visibleRows / Math.max(1, visibleCards.size()));
        int available = Math.max(1, trackHeight - thumbHeight);
        int thumbTop = trackTop + (available * cardScroll / Math.max(1, maxScroll));

        graphics.fill(listRight - 5, trackTop, listRight - 2, trackBottom, 0x55333333);
        graphics.fill(listRight - 5, thumbTop, listRight - 2, thumbTop + thumbHeight, 0xCCB0B0B0);
    }

    private void renderSelectedCardDetails(GuiGraphics graphics, int mouseX, int mouseY, int detailLeft) {
        int detailX = detailLeft + 8;
        int detailY = CARD_LIST_TOP;

        if (selectedCard == null) {
            graphics.drawString(this.font, DETAILS_HINT_LABEL, detailX, detailY, 0xBFBFBF, false);
            return;
        }

        ItemStack result = selectedCard.getResult();
        graphics.renderItem(result, detailX, detailY);
        graphics.drawString(this.font, result.getHoverName(), detailX + 22, detailY + 5, 0xFFFFFF, false);
        detailY += 26;

        graphics.drawString(this.font, STATS_LABEL, detailX, detailY, 0xD8D8D8, false);
        detailY += 12;
        for (Component stat : buildResultStats(result)) {
            graphics.drawString(this.font, stat, detailX + 4, detailY, 0xC8C8C8, false);
            detailY += 10;
        }

        detailY += 6;
        graphics.drawString(this.font, INGREDIENTS_LABEL, detailX, detailY, 0xD8D8D8, false);
        detailY += 12;

        Map<Item, Integer> counts = countAvailableItems();
        for (var ingredient : selectedCard.getIngredients()) {
            ItemStack ingredientStack = ingredient.item().getDefaultInstance();
            int have = counts.getOrDefault(ingredient.item(), 0);
            boolean enough = have >= ingredient.count();

            graphics.renderItem(ingredientStack, detailX + 2, detailY - 3);
            Component text = Component.literal(ingredientStack.getHoverName().getString() + " " + have + "/" + ingredient.count());
            graphics.drawString(this.font, text, detailX + 22, detailY + 2, enough ? 0xDADADA : 0xFF7878, false);
            detailY += 20;
        }
    }

    // ---- Category / card selection ----

    private void selectCategory(CraftCategory category) {
        selectedCategory = category;
        selectedCard = null;

        visibleCards.clear();
        visibleCards.addAll(CraftCardRegistry.cardsInCategory(category.getId()));
        cardScroll = 0;
        if (!visibleCards.isEmpty()) {
            selectedCard = visibleCards.get(0);
        }

        updateCraftButtonState();
        LOGGER.debug("[CraftScreen] selected category={}", category.getId());
    }

    private void selectCard(CraftCard card) {
        selectedCard = card;
        updateCraftButtonState();
    }

    private Component buildCardLabel(CraftCard card) {
        ItemStack result = card.getResult();
        return Component.literal(result.getHoverName().getString() + " x" + result.getCount());
    }

    private void updateCraftButtonState() {
        if (craftButton == null) return;
        craftButton.active = selectedCard != null && pendingRequestId == null && canAfford(selectedCard);
    }

    private boolean canAfford(CraftCard card) {
        if (card == null) return false;
        Map<Item, Integer> itemCounts = countAvailableItems();

        for (var ingredient : card.getIngredients()) {
            int have = itemCounts.getOrDefault(ingredient.item(), 0);
            if (have < ingredient.count()) return false;
        }
        return true;
    }

    private Map<Item, Integer> countAvailableItems() {
        Minecraft mc = Minecraft.getInstance();
        Map<Item, Integer> itemCounts = new HashMap<>();
        if (mc.player == null) {
            return itemCounts;
        }

        IPlayerLoadout loadout = mc.player.getCapability(LoadoutCapability.PLAYER_LOADOUT).orElse(null);

        for (int i = 0; i <= 8; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            itemCounts.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }

        if (loadout != null) {
            for (int i = 0; i < loadout.getDynamicSlotCount(); i++) {
                if (!loadout.isDynamicSlotActive(i)) continue;
                ItemStack stack = loadout.getDynamicSlot(i);
                if (stack.isEmpty()) continue;
                itemCounts.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        return itemCounts;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int panelBottom = this.height - PANEL_BOTTOM_MARGIN;
        int detailLeft = getDetailPanelLeft();
        if (mouseX >= CARD_LIST_LEFT && mouseX <= detailLeft - 4 && mouseY >= CARD_LIST_TOP && mouseY <= panelBottom) {
            int visibleRows = getVisibleCardRows(panelBottom);
            if (visibleRows <= 0 || visibleCards.isEmpty()) {
                return true;
            }
            int maxScroll = getMaxCardScroll(visibleRows);
            cardScroll = Math.max(0, Math.min(maxScroll, cardScroll + (delta < 0 ? 1 : -1)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int panelBottom = this.height - PANEL_BOTTOM_MARGIN;
            int detailLeft = getDetailPanelLeft();
            if (mouseX >= CARD_LIST_LEFT + 2 && mouseX <= detailLeft - 10 && mouseY >= CARD_LIST_TOP && mouseY <= panelBottom) {
                int row = (int) ((mouseY - CARD_LIST_TOP) / CARD_ROW_HEIGHT);
                int index = cardScroll + row;
                int visibleRows = getVisibleCardRows(panelBottom);
                if (row >= 0 && row < visibleRows && index >= 0 && index < visibleCards.size()) {
                    selectCard(visibleCards.get(index));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int getDetailPanelLeft() {
        return Math.max(220, this.width - 156);
    }

    private int getVisibleCardRows(int panelBottom) {
        return Math.max(1, (panelBottom - CARD_LIST_TOP) / CARD_ROW_HEIGHT);
    }

    private int getMaxCardScroll(int visibleRows) {
        return Math.max(0, visibleCards.size() - visibleRows);
    }

    private List<Component> buildResultStats(ItemStack result) {
        List<Component> lines = new ArrayList<>();

        StorageProfileRegistry.lookup(result)
                .filter(p -> p.getSlotCount() > 0)
                .ifPresent(profile -> lines.add(Component.translatable(
                        "gui.craft.stat.storage", profile.getSlotCount())));

        ProtectionProfileRegistry.resolve(result)
                .ifPresent(profile -> lines.add(Component.translatable(
                        "gui.craft.stat.armor", formatStatValue(profile.getArmorValue()))));

        FoodProperties food = result.getFoodProperties(Minecraft.getInstance().player);
        if (food != null && food.getNutrition() > 0) {
            lines.add(Component.translatable("gui.craft.stat.food", food.getNutrition()));
        }

        double attackDamage = result.getAttributeModifiers(EquipmentSlot.MAINHAND)
                .get(Attributes.ATTACK_DAMAGE)
                .stream()
                .mapToDouble(mod -> mod.getAmount())
                .sum();
        if (attackDamage > 0d) {
            lines.add(Component.translatable("gui.craft.stat.damage", formatStatValue(attackDamage)));
        }

        return lines;
    }

    private String formatStatValue(double value) {
        if (Math.abs(value - Math.rint(value)) < 1.0E-6d) {
            return Integer.toString((int) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    // ---- Craft execution ----

    private void executeSelectedCraft() {
        if (selectedCard == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        long capabilityVersion = mc.player.getCapability(LoadoutCapability.PLAYER_LOADOUT)
                .map(l -> l.getLoadoutVersion()).orElse(0L);
        long clientViewVersion = ClientLoadoutState.versionForRequest(capabilityVersion);

        UUID requestId = UUID.randomUUID();
        pendingRequestId = requestId;
        PendingActionTracker.registerPending(requestId, clientViewVersion);

        ModNetwork.CHANNEL.sendToServer(
                new C2SRequestCraftPacket(requestId, selectedCard.getId(), clientViewVersion));

        craftButton.active = false;
        craftButton.setMessage(CRAFTING_BUTTON_LABEL);
        LOGGER.debug("[CraftScreen] sent craft request id={} recipe={}", requestId, selectedCard.getId());
    }

    // ---- Sync callbacks (called from packet handlers) ----

    /** Called when S2CLoadoutSyncPacket is received. */
    public void onLoadoutSync(long serverVersion) {
        // Re-evaluate affordability after state changes
        updateCraftButtonState();
        if (craftButton != null) {
            craftButton.setMessage(CRAFT_BUTTON_LABEL);
        }
    }

    /** Called when S2CActionRejectedPacket is received for a pending craft. */
    public void onCraftRejected() {
        rejectFlashTicks = 10;
        pendingRequestId = null;
        if (craftButton != null) {
            craftButton.setMessage(CRAFT_BUTTON_LABEL);
        }
        updateCraftButtonState();
    }
}

