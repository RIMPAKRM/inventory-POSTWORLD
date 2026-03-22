package org.inventory.inventory.menu;

import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.inventory.inventory.capability.IPlayerLoadout;
import org.inventory.inventory.capability.LoadoutCapability;
import org.inventory.inventory.capability.PlayerLoadout;
import org.inventory.inventory.domain.EquipmentSlotType;
import org.inventory.inventory.domain.StorageProfileRegistry;
import org.inventory.inventory.server.CanonicalSlotMapping;
import org.inventory.inventory.server.InventoryTransactionService;
import org.inventory.inventory.server.LoadoutSyncScheduler;
import org.inventory.inventory.server.OpContext;
import org.inventory.inventory.server.OverflowService;
import org.slf4j.Logger;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Custom inventory container menu for survival/adventure mode.
 *
 * Slot layout inside the menu's `slots` list:
 *   [0  …  7]          Equipment slots  (one per EquipmentSlotType, in ordinal order)
 *   [8  …  8+dyn-1]    Dynamic storage slots (count from PlayerLoadout)
 *   [8+dyn … 8+dyn+8]  Vanilla hotbar only (indices 0-8)
 *
 * Creative mode: always use vanilla inventory screen (enforced by Inventory.java listener).
 */
public class CustomInventoryMenu extends AbstractContainerMenu {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Set by Inventory.java during registration via DeferredRegister. */
    public static MenuType<CustomInventoryMenu> TYPE;

    // GUI layout anchors (must match InventoryScreen visual layout).
    public static final int EQUIPMENT_LEFT_X = 8;
    public static final int EQUIPMENT_TOP_Y = 18;
    public static final int EQUIPMENT_COLS = 2;
    public static final int DYNAMIC_X = 126;
    public static final int DYNAMIC_Y = 18;
    public static final int DYNAMIC_COLS = 4;
    public static final int VANILLA_INV_X = 8;
    public static final int HOTBAR_Y = 202;

    private static final Map<EquipmentSlotType, Integer> DYNAMIC_PROVIDER_COLUMNS = buildDynamicProviderColumns();
    private static final Map<EquipmentSlotType, int[]> DYNAMIC_PROVIDER_ANCHORS = buildDynamicProviderAnchors();

    private final Player player;
    private final IPlayerLoadout loadout;

    /** Fixed dynamic slot count in the menu; visibility is controlled by Slot#isActive. */
    private final int dynamicSlotCountSnapshot;

    // ---- Constructors ----

    /** Server-side constructor. */
    public CustomInventoryMenu(int windowId, Inventory playerInventory) {
        super(TYPE, windowId);
        this.player = playerInventory.player;
        this.loadout = player.getCapability(LoadoutCapability.PLAYER_LOADOUT)
                .orElseThrow(() -> new IllegalStateException("Player missing PLAYER_LOADOUT capability"));

        if (this.player instanceof ServerPlayer serverPlayer) {
            normalizeLoadoutStateOnMenuOpen(serverPlayer);
        }

        this.dynamicSlotCountSnapshot = PlayerLoadout.MAX_DYNAMIC_SLOTS;

        addEquipmentSlots();
        addDynamicStorageSlots();
        addVanillaInventorySlots(playerInventory);
    }

    /** Client-side constructor (called from MenuType factory with network buffer). */
    public CustomInventoryMenu(int windowId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(windowId, playerInventory);
    }

    // ---- Slot registration ----

    private void addEquipmentSlots() {
        for (EquipmentSlotType type : EquipmentSlotType.values()) {
            int x = EQUIPMENT_LEFT_X + (type.ordinal() % EQUIPMENT_COLS) * 18;
            int y = EQUIPMENT_TOP_Y + (type.ordinal() / EQUIPMENT_COLS) * 18;
            addSlot(new EquipmentItemSlot(loadout, type, x, y));
        }
    }

    private void addDynamicStorageSlots() {
        for (int i = 0; i < dynamicSlotCountSnapshot; i++) {
            EquipmentSlotType provider = PlayerLoadout.providerForDynamicIndex(i);
            int offset = PlayerLoadout.providerOffset(i);

            int providerCols = getDynamicColumns(provider);
            int col = Math.max(0, offset % providerCols);
            int row = Math.max(0, offset / providerCols);
            int x = getDynamicProviderX(provider) + col * 18;
            int y = getDynamicProviderY(provider) + row * 18;
            addSlot(new DynamicStorageSlot(loadout, i, x, y));
        }
    }

    public static int getDynamicProviderX(EquipmentSlotType provider) {
        int[] anchor = DYNAMIC_PROVIDER_ANCHORS.get(provider);
        return anchor != null ? anchor[0] : DYNAMIC_X;
    }

    public static int getDynamicProviderY(EquipmentSlotType provider) {
        int[] anchor = DYNAMIC_PROVIDER_ANCHORS.get(provider);
        return anchor != null ? anchor[1] : DYNAMIC_Y;
    }

    public static int getDynamicColumns(EquipmentSlotType provider) {
        return Math.max(1, DYNAMIC_PROVIDER_COLUMNS.getOrDefault(provider, DYNAMIC_COLS));
    }

    private static Map<EquipmentSlotType, Integer> buildDynamicProviderColumns() {
        Map<EquipmentSlotType, Integer> result = new EnumMap<>(EquipmentSlotType.class);
        result.put(EquipmentSlotType.CHEST, 2);
        result.put(EquipmentSlotType.VEST, 1);
        result.put(EquipmentSlotType.BACKPACK, 4);
        result.put(EquipmentSlotType.LEGS, 1);
        return result;
    }

    private static Map<EquipmentSlotType, int[]> buildDynamicProviderAnchors() {
        Map<EquipmentSlotType, int[]> result = new EnumMap<>(EquipmentSlotType.class);
        result.put(EquipmentSlotType.CHEST, new int[] {DYNAMIC_X, DYNAMIC_Y});
        result.put(EquipmentSlotType.BACKPACK, new int[] {DYNAMIC_X + 36, DYNAMIC_Y});
        result.put(EquipmentSlotType.VEST, new int[] {DYNAMIC_X, DYNAMIC_Y + 62});
        result.put(EquipmentSlotType.LEGS, new int[] {DYNAMIC_X + 18, DYNAMIC_Y + 62});
        return result;
    }

    private void normalizeLoadoutStateOnMenuOpen(ServerPlayer serverPlayer) {
        // Protect against stale slotActive layout after profile/slot geometry changes.
        List<ItemStack> displaced = loadout.recalculateStorageSlots(StorageProfileRegistry::lookup);
        if (!displaced.isEmpty()) {
            OverflowService.applyOverflow(serverPlayer, displaced, UUID.randomUUID());
        }
        org.inventory.inventory.server.ArmorAttributeService.applyLoadoutArmor(serverPlayer, loadout);
        LoadoutSyncScheduler.sendImmediately(serverPlayer);
    }

    public int getActiveSlotsForProvider(EquipmentSlotType provider) {
        int active = 0;
        int providerIndex = PlayerLoadout.providerIndex(provider);
        if (providerIndex < 0) return 0;

        int base = providerIndex * PlayerLoadout.SLOTS_PER_PROVIDER;
        for (int offset = 0; offset < PlayerLoadout.SLOTS_PER_PROVIDER; offset++) {
            if (loadout.isDynamicSlotActive(base + offset)) {
                active++;
            }
        }
        return active;
    }

    public ItemStack getEquippedItemForProvider(EquipmentSlotType provider) {
        return loadout.getEquipment(provider);
    }

    private void addVanillaInventorySlots(Inventory inv) {
        // Hotbar only (indices 0–8). Main inventory grid is disabled in this mod.
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, VANILLA_INV_X + col * 18, HOTBAR_Y));
        }
    }

    // ---- Container behaviour ----

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /**
     * Intercepts all clicks to:
     *  1. Block interaction with inactive dynamic slots.
     *  2. Detect equipment changes after a click and trigger storage recalculation + overflow.
     */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            super.clicked(slotId, button, clickType, player);
            return;
        }

        if (!isCustomMenuSlot(slotId)) {
            super.clicked(slotId, button, clickType, player);
            return;
        }

        Optional<OpContext> ctxOpt = InventoryTransactionService.beginLoadoutOp(serverPlayer, loadout.getLoadoutVersion());
        if (ctxOpt.isEmpty()) {
            LOGGER.debug("[Menu] click rejected due to missing ctx/version mismatch player={}", player.getName().getString());
            return;
        }

        OpContext ctx = ctxOpt.get();
        boolean success = false;
        boolean equipmentChanged = false;
        try {
            Integer canonicalSlotIndex = toCanonicalCustomSlotIndex(slotId);
            if (canonicalSlotIndex == null || !InventoryTransactionService.validateAndApplyClick(serverPlayer, canonicalSlotIndex, ctx.opId)) {
                return;
            }

            ItemStack[] equipBefore = captureEquipmentSnapshot();
            super.clicked(slotId, button, clickType, player);
            equipmentChanged = handlePostClickEquipmentChange(serverPlayer, equipBefore, ctx.opId);
            success = true;
        } finally {
            InventoryTransactionService.endLoadoutOp(ctx, success);
        }

        if (success && equipmentChanged) {
            org.inventory.inventory.server.ArmorAttributeService.applyLoadoutArmor(serverPlayer, loadout);
            LoadoutSyncScheduler.sendImmediately(serverPlayer);
        }
    }

    /**
     * Shift-click: move item between custom slots and vanilla inventory.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return doQuickMoveStack(index);
        }

        Optional<OpContext> ctxOpt = InventoryTransactionService.beginLoadoutOp(serverPlayer, loadout.getLoadoutVersion());
        if (ctxOpt.isEmpty()) {
            LOGGER.debug("[Menu] quickMove rejected due to missing ctx/version mismatch player={}", player.getName().getString());
            return ItemStack.EMPTY;
        }

        OpContext ctx = ctxOpt.get();
        boolean success = false;
        boolean equipmentChanged = false;
        ItemStack moved = ItemStack.EMPTY;
        try {
            ItemStack[] equipBefore = captureEquipmentSnapshot();
            moved = doQuickMoveStack(index);
            if (moved.isEmpty()) {
                return ItemStack.EMPTY;
            }

            equipmentChanged = handlePostClickEquipmentChange(serverPlayer, equipBefore, ctx.opId);
            success = true;
        } finally {
            InventoryTransactionService.endLoadoutOp(ctx, success);
        }

        if (success && equipmentChanged) {
            org.inventory.inventory.server.ArmorAttributeService.applyLoadoutArmor(serverPlayer, loadout);
            LoadoutSyncScheduler.sendImmediately(serverPlayer);
        }
        return moved;
    }

    private ItemStack doQuickMoveStack(int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);

        if (!slot.hasItem()) return result;

        ItemStack stackInSlot = slot.getItem();
        result = stackInSlot.copy();

        int customEnd = EquipmentSlotType.COUNT + dynamicSlotCountSnapshot;
        int vanillaEnd = slots.size();

        if (index < customEnd) {
            // Custom -> hotbar
            if (!moveItemStackTo(stackInSlot, customEnd, vanillaEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Hotbar -> try equipment first, then dynamic storage
            if (!moveItemStackTo(stackInSlot, 0, customEnd, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stackInSlot.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return result;
    }

    private boolean isCustomMenuSlot(int slotId) {
        return slotId >= 0 && slotId < getCustomSlotCount();
    }

    private int getCustomSlotCount() {
        return EquipmentSlotType.COUNT + dynamicSlotCountSnapshot;
    }

    private Integer toCanonicalCustomSlotIndex(int menuSlotIndex) {
        if (menuSlotIndex < 0) return null;
        if (menuSlotIndex < EquipmentSlotType.COUNT) {
            return CanonicalSlotMapping.equipmentSlotToIndex(EquipmentSlotType.values()[menuSlotIndex]);
        }

        int localDynamicIndex = menuSlotIndex - EquipmentSlotType.COUNT;
        if (localDynamicIndex >= 0 && localDynamicIndex < dynamicSlotCountSnapshot) {
            return CanonicalSlotMapping.dynamicSlotToIndex(localDynamicIndex);
        }

        return null;
    }

    // ---- Equipment change detection & overflow ----

    private ItemStack[] captureEquipmentSnapshot() {
        ItemStack[] snapshot = new ItemStack[EquipmentSlotType.COUNT];
        for (EquipmentSlotType type : EquipmentSlotType.values()) {
            snapshot[type.ordinal()] = loadout.getEquipment(type);
        }
        return snapshot;
    }

    private boolean handlePostClickEquipmentChange(ServerPlayer player, ItemStack[] equipBefore, UUID opId) {
        boolean changed = false;
        for (EquipmentSlotType type : EquipmentSlotType.values()) {
            if (!ItemStack.matches(loadout.getEquipment(type), equipBefore[type.ordinal()])) {
                changed = true;
                break;
            }
        }
        if (!changed) return false;

        LOGGER.debug("[Menu] equipment changed for player={}, recalculating storage", player.getName().getString());

        List<ItemStack> displaced = loadout.recalculateStorageSlots(StorageProfileRegistry::lookup);

        if (!displaced.isEmpty()) {
            OverflowService.applyOverflow(player, displaced, opId);
        }
        return true;
    }

    // ======================================================================
    // Inner slot classes
    // ======================================================================

    /**
     * A slot that stores its item in PlayerLoadout's equipment array.
     * One slot per EquipmentSlotType. Holds exactly 1 item.
     *
     * Phase C: mayPlace() will enforce EquipmentSlotType matching via item tags.
     */
    public static class EquipmentItemSlot extends Slot {

        private static final SimpleContainer DUMMY = new SimpleContainer(EquipmentSlotType.COUNT);
        private static final Map<EquipmentSlotType, TagKey<Item>> SLOT_TAGS = buildSlotTags();

        private final IPlayerLoadout loadout;
        private final EquipmentSlotType slotType;

        public EquipmentItemSlot(IPlayerLoadout loadout, EquipmentSlotType slotType, int x, int y) {
            super(DUMMY, slotType.ordinal(), x, y);
            this.loadout = loadout;
            this.slotType = slotType;
        }

        @Override
        public ItemStack getItem() {
            return loadout.getEquipment(slotType);
        }

        @Override
        public void set(ItemStack stack) {
            loadout.setEquipment(slotType, stack);
            setChanged();
        }

        @Override
        public ItemStack remove(int amount) {
            ItemStack current = getItem();
            if (current.isEmpty() || amount <= 0) {
                return ItemStack.EMPTY;
            }

            ItemStack taken = current.split(Math.min(amount, current.getCount()));
            set(current.isEmpty() ? ItemStack.EMPTY : current);
            return taken;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (stack.isEmpty()) return false;

            TagKey<Item> expectedTag = SLOT_TAGS.get(slotType);
            if (expectedTag != null && stack.is(expectedTag)) {
                return true;
            }

            Equipable equipable = Equipable.get(stack);
            EquipmentSlot vanillaSlot = equipable != null ? equipable.getEquipmentSlot() : null;
            if (vanillaSlot != null && matchesVanillaSlot(vanillaSlot, slotType)) {
                return true;
            }

            // Data-driven fallback: storage profile can explicitly bind item to slot type.
            return StorageProfileRegistry.lookup(stack)
                    .map(profile -> profile.getSlotType() == slotType)
                    .orElse(false);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return 1;
        }

        public EquipmentSlotType getEquipmentSlotType() { return slotType; }

        /** Canonical slot index for network protocol. */
        public int getCanonicalIndex() {
            return CanonicalSlotMapping.equipmentSlotToIndex(slotType);
        }

        private static Map<EquipmentSlotType, TagKey<Item>> buildSlotTags() {
            Map<EquipmentSlotType, TagKey<Item>> tags = new java.util.EnumMap<>(EquipmentSlotType.class);
            for (EquipmentSlotType type : EquipmentSlotType.values()) {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath("inventory", "equip/" + type.name().toLowerCase(java.util.Locale.ROOT));
                tags.put(type, TagKey.create(Registries.ITEM, id));
            }
            return tags;
        }

        private static boolean matchesVanillaSlot(EquipmentSlot vanillaSlot, EquipmentSlotType customSlot) {
            return switch (customSlot) {
                case HEAD -> vanillaSlot == EquipmentSlot.HEAD;
                case CHEST -> vanillaSlot == EquipmentSlot.CHEST;
                case LEGS -> vanillaSlot == EquipmentSlot.LEGS;
                case FEET -> vanillaSlot == EquipmentSlot.FEET;
                default -> false;
            };
        }
    }

    /**
     * A slot backed by PlayerLoadout's dynamic storage array.
     * Inactive slots reject all interactions (isActive() returns false).
     */
    public static class DynamicStorageSlot extends Slot {

        private static final SimpleContainer DUMMY = new SimpleContainer(
                org.inventory.inventory.capability.PlayerLoadout.MAX_DYNAMIC_SLOTS);

        private final IPlayerLoadout loadout;
        private final int localIndex;

        public DynamicStorageSlot(IPlayerLoadout loadout, int localIndex, int x, int y) {
            super(DUMMY, localIndex, x, y);
            this.loadout = loadout;
            this.localIndex = localIndex;
        }

        @Override
        public boolean isActive() {
            return loadout.isDynamicSlotActive(localIndex);
        }

        @Override
        public ItemStack getItem() {
            return loadout.getDynamicSlot(localIndex);
        }

        @Override
        public void set(ItemStack stack) {
            if (!isActive()) return;
            loadout.setDynamicSlot(localIndex, stack);
            setChanged();
        }

        @Override
        public ItemStack remove(int amount) {
            if (!isActive() || amount <= 0) {
                return ItemStack.EMPTY;
            }

            ItemStack current = getItem();
            if (current.isEmpty()) {
                return ItemStack.EMPTY;
            }

            ItemStack taken = current.split(Math.min(amount, current.getCount()));
            set(current.isEmpty() ? ItemStack.EMPTY : current);
            return taken;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isActive();
        }

        public int getLocalIndex() { return localIndex; }

        public EquipmentSlotType getProviderType() {
            return loadout.getDynamicSlotProvider(localIndex);
        }

        /** Canonical slot index for network protocol. */
        public int getCanonicalIndex() {
            return CanonicalSlotMapping.dynamicSlotToIndex(localIndex);
        }
    }
}


