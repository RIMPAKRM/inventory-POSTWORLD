package org.inventory.inventory.menu;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.Container;
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
import org.inventory.inventory.client.screen.StorageBrowserLayout;
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

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.core.Direction;

/**
 * Container browser menu that combines the player's custom loadout slots,
 * hotbar, and an external storage container in a vertical browser layout.
 */
public class StorageBrowserMenu extends AbstractContainerMenu {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static MenuType<StorageBrowserMenu> TYPE;

    private static final Map<EquipmentSlotType, Integer> DYNAMIC_PROVIDER_COLUMNS = buildDynamicProviderColumns();
    private static final Map<EquipmentSlotType, int[]> BROWSER_DYNAMIC_PROVIDER_ANCHORS = buildBrowserDynamicProviderAnchors();

    private final Player player;
    private final IPlayerLoadout loadout;
    private final Container storageContainer;
    private final int storageSlotCount;
    private final int storageColumns;
    private final int customSlotCount;
    private final int hotbarStartIndex;
    private final int dynamicSlotCountSnapshot;
    private final OpenCloseEffect openCloseEffect;

    public StorageBrowserMenu(int windowId, Inventory playerInventory, Container storageContainer) {
        this(windowId, playerInventory, storageContainer, OpenCloseEffect.forContainer(storageContainer));
    }

    public StorageBrowserMenu(int windowId, Inventory playerInventory, Container storageContainer, OpenCloseEffect openCloseEffect) {
        super(TYPE, windowId);
        this.player = playerInventory.player;
        this.loadout = player.getCapability(LoadoutCapability.PLAYER_LOADOUT)
                .orElseThrow(() -> new IllegalStateException("Player missing PLAYER_LOADOUT capability"));
        this.storageContainer = storageContainer;
        this.storageSlotCount = storageContainer.getContainerSize();
        this.storageColumns = StorageBrowserLayout.chooseStorageColumns(storageSlotCount);
        this.customSlotCount = EquipmentSlotType.COUNT + PlayerLoadout.MAX_DYNAMIC_SLOTS;
        this.hotbarStartIndex = customSlotCount + storageSlotCount;
        this.dynamicSlotCountSnapshot = PlayerLoadout.MAX_DYNAMIC_SLOTS;
        this.openCloseEffect = openCloseEffect == null ? OpenCloseEffect.none() : openCloseEffect;

        this.openCloseEffect.open(this.player);

        if (this.player instanceof ServerPlayer serverPlayer) {
            normalizeLoadoutStateOnMenuOpen(serverPlayer);
        }

        addLoadoutSlots();
        addStorageSlots();
        addHotbarSlots(playerInventory);
    }

    public StorageBrowserMenu(int windowId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(windowId, playerInventory, new SimpleContainer(Math.max(0, buf.readVarInt())));
    }

    private void addLoadoutSlots() {
        for (EquipmentSlotType type : EquipmentSlotType.values()) {
            int x = StorageBrowserLayout.LOADOUT_LEFT_X + (type.ordinal() % 2) * 18;
            int y = StorageBrowserLayout.LOADOUT_TOP_Y + (type.ordinal() / 2) * 18;
            addSlot(new CustomInventoryMenu.EquipmentItemSlot(loadout, type, x, y));
        }

        for (int i = 0; i < dynamicSlotCountSnapshot; i++) {
            EquipmentSlotType provider = PlayerLoadout.providerForDynamicIndex(i);
            int offset = PlayerLoadout.providerOffset(i);

            int providerCols = getDynamicColumns(provider);
            int col = Math.max(0, offset % providerCols);
            int row = Math.max(0, offset / providerCols);
            int x = getDynamicProviderX(provider) + col * 18;
            int y = getDynamicProviderY(provider) + row * 18;
            addSlot(new CustomInventoryMenu.DynamicStorageSlot(loadout, i, x, y));
        }
    }

    private void addStorageSlots() {
        for (int i = 0; i < storageSlotCount; i++) {
            int col = i % storageColumns;
            int row = i / storageColumns;
            int x = StorageBrowserLayout.STORAGE_LEFT_X + col * 18;
            int y = StorageBrowserLayout.STORAGE_TOP_Y + row * 18;
            addSlot(new Slot(storageContainer, i, x, y));
        }
    }

    private void addHotbarSlots(Inventory inv) {
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, StorageBrowserLayout.HOTBAR_LEFT_X + col * 18, StorageBrowserLayout.HOTBAR_MIN_Y));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return storageContainer.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        openCloseEffect.close(player);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            super.clicked(slotId, button, clickType, player);
            return;
        }

        if (slotId < 0 || slotId >= slots.size()) {
            super.clicked(slotId, button, clickType, player);
            return;
        }

        Optional<OpContext> ctxOpt = InventoryTransactionService.beginLoadoutOp(serverPlayer, loadout.getLoadoutVersion());
        if (ctxOpt.isEmpty()) {
            LOGGER.debug("[StorageBrowserMenu] click rejected due to missing ctx/version mismatch player={}", player.getName().getString());
            return;
        }

        OpContext ctx = ctxOpt.get();
        boolean success = false;
        boolean equipmentChanged = false;
        try {
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

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return doQuickMoveStack(index, player);
        }

        Optional<OpContext> ctxOpt = InventoryTransactionService.beginLoadoutOp(serverPlayer, loadout.getLoadoutVersion());
        if (ctxOpt.isEmpty()) {
            LOGGER.debug("[StorageBrowserMenu] quickMove rejected due to missing ctx/version mismatch player={}", player.getName().getString());
            return ItemStack.EMPTY;
        }

        OpContext ctx = ctxOpt.get();
        boolean success = false;
        boolean equipmentChanged = false;
        ItemStack moved = ItemStack.EMPTY;
        try {
            ItemStack[] equipBefore = captureEquipmentSnapshot();
            moved = doQuickMoveStack(index, serverPlayer);
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
        }
        if (success) {
            LoadoutSyncScheduler.sendImmediately(serverPlayer);
        }
        return moved;
    }

    private ItemStack doQuickMoveStack(int index, Player clickingPlayer) {
        Slot slot = slots.get(index);

        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stackInSlot = slot.getItem();
        ItemStack original = stackInSlot.copy();

        int storageStart = customSlotCount;
        int storageEnd = storageStart + storageSlotCount;

        if (index < customSlotCount) {
            transferStackToRange(stackInSlot, storageStart, storageEnd, false);
            transferStackToRange(stackInSlot, storageEnd, slots.size(), false);
        } else if (index < storageEnd) {
            transferStackToRange(stackInSlot, 0, customSlotCount, false);
            transferStackToRange(stackInSlot, storageEnd, slots.size(), false);
        } else {
            transferStackToRange(stackInSlot, 0, customSlotCount, false);
            transferStackToRange(stackInSlot, storageStart, storageEnd, false);
        }

        if (ItemStack.matches(stackInSlot, original) && stackInSlot.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.set(stackInSlot.isEmpty() ? ItemStack.EMPTY : stackInSlot);
        slot.setChanged();
        slot.onTake(clickingPlayer, stackInSlot);
        return original;
    }

    private boolean transferStackToRange(ItemStack movingStack, int startIndex, int endIndex, boolean reverse) {
        if (movingStack.isEmpty()) {
            return false;
        }

        boolean changed = false;

        if (reverse) {
            for (int i = endIndex - 1; i >= startIndex && !movingStack.isEmpty(); i--) {
                changed |= mergeIntoSlot(movingStack, slots.get(i));
            }
            for (int i = endIndex - 1; i >= startIndex && !movingStack.isEmpty(); i--) {
                changed |= placeIntoEmptySlot(movingStack, slots.get(i));
            }
        } else {
            for (int i = startIndex; i < endIndex && !movingStack.isEmpty(); i++) {
                changed |= mergeIntoSlot(movingStack, slots.get(i));
            }
            for (int i = startIndex; i < endIndex && !movingStack.isEmpty(); i++) {
                changed |= placeIntoEmptySlot(movingStack, slots.get(i));
            }
        }

        return changed;
    }

    private boolean mergeIntoSlot(ItemStack movingStack, Slot targetSlot) {
        if (movingStack.isEmpty()) {
            return false;
        }
        if (!targetSlot.hasItem() || !targetSlot.mayPlace(movingStack)) {
            return false;
        }

        ItemStack targetStack = targetSlot.getItem();
        if (targetStack.isEmpty() || !ItemStack.isSameItemSameTags(targetStack, movingStack)) {
            return false;
        }

        int slotLimit = Math.min(targetSlot.getMaxStackSize(targetStack), movingStack.getMaxStackSize());
        int space = slotLimit - targetStack.getCount();
        if (space <= 0) {
            return false;
        }

        int moved = Math.min(space, movingStack.getCount());
        if (moved <= 0) {
            return false;
        }

        targetStack.grow(moved);
        targetSlot.set(targetStack);
        targetSlot.setChanged();
        movingStack.shrink(moved);
        return true;
    }

    private boolean placeIntoEmptySlot(ItemStack movingStack, Slot targetSlot) {
        if (movingStack.isEmpty()) {
            return false;
        }
        if (targetSlot.hasItem() || !targetSlot.mayPlace(movingStack)) {
            return false;
        }

        int slotLimit = Math.min(targetSlot.getMaxStackSize(movingStack), movingStack.getMaxStackSize());
        if (slotLimit <= 0) {
            return false;
        }

        int moved = Math.min(slotLimit, movingStack.getCount());
        ItemStack placed = movingStack.copy();
        placed.setCount(moved);
        targetSlot.set(placed);
        targetSlot.setChanged();
        movingStack.shrink(moved);
        return moved > 0;
    }

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

        LOGGER.debug("[StorageBrowserMenu] equipment changed for player={}, recalculating storage", player.getName().getString());

        List<ItemStack> displaced = loadout.recalculateStorageSlots(StorageProfileRegistry::lookup);

        if (!displaced.isEmpty()) {
            OverflowService.applyOverflow(player, displaced, opId);
        }
        return true;
    }

    private void normalizeLoadoutStateOnMenuOpen(ServerPlayer serverPlayer) {
        List<ItemStack> displaced = loadout.recalculateStorageSlots(StorageProfileRegistry::lookup);
        if (!displaced.isEmpty()) {
            OverflowService.applyOverflow(serverPlayer, displaced, UUID.randomUUID());
        }
        org.inventory.inventory.server.ArmorAttributeService.applyLoadoutArmor(serverPlayer, loadout);
        LoadoutSyncScheduler.sendImmediately(serverPlayer);
    }

    public int getStorageSlotCount() {
        return storageSlotCount;
    }

    public int getStorageColumns() {
        return storageColumns;
    }

    public int getHotbarStartIndex() {
        return hotbarStartIndex;
    }

    public ItemStack getEquippedItemForProvider(EquipmentSlotType provider) {
        return loadout.getEquipment(provider);
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

    public static int getDynamicProviderX(EquipmentSlotType provider) {
        int[] anchor = BROWSER_DYNAMIC_PROVIDER_ANCHORS.get(provider);
        return anchor != null ? anchor[0] : 82;
    }

    public static int getDynamicProviderY(EquipmentSlotType provider) {
        int[] anchor = BROWSER_DYNAMIC_PROVIDER_ANCHORS.get(provider);
        return anchor != null ? anchor[1] : 18;
    }

    public static int getDynamicColumns(EquipmentSlotType provider) {
        return Math.max(1, DYNAMIC_PROVIDER_COLUMNS.getOrDefault(provider, 4));
    }

    private static Map<EquipmentSlotType, Integer> buildDynamicProviderColumns() {
        Map<EquipmentSlotType, Integer> result = new EnumMap<>(EquipmentSlotType.class);
        result.put(EquipmentSlotType.CHEST, 2);
        result.put(EquipmentSlotType.VEST, 1);
        result.put(EquipmentSlotType.BACKPACK, 4);
        result.put(EquipmentSlotType.LEGS, 1);
        return result;
    }

    private static Map<EquipmentSlotType, int[]> buildBrowserDynamicProviderAnchors() {
        Map<EquipmentSlotType, int[]> result = new EnumMap<>(EquipmentSlotType.class);
        result.put(EquipmentSlotType.CHEST, new int[] {82, 26});
        result.put(EquipmentSlotType.BACKPACK, new int[] {118, 26});
        result.put(EquipmentSlotType.VEST, new int[] {82, 62});
        result.put(EquipmentSlotType.LEGS, new int[] {100, 62});
        return result;
    }

    public static final class StorageSlot extends Slot {
        public StorageSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }
    }

    public interface OpenCloseEffect {
        void open(Player player);

        void close(Player player);

        static OpenCloseEffect none() {
            return new OpenCloseEffect() {
                @Override
                public void open(Player player) {
                }

                @Override
                public void close(Player player) {
                }
            };
        }

        static OpenCloseEffect forContainer(Container container) {
            return new OpenCloseEffect() {
                @Override
                public void open(Player player) {
                    container.startOpen(player);
                }

                @Override
                public void close(Player player) {
                    container.stopOpen(player);
                }
            };
        }

        static OpenCloseEffect forChest(Level level, BlockPos pos, BlockState state) {
            return new OpenCloseEffect() {
                @Override
                public void open(Player player) {
                    apply(level, pos, state, true);
                }

                @Override
                public void close(Player player) {
                    apply(level, pos, state, false);
                }
            };
        }

        private static void apply(Level level, BlockPos pos, BlockState state, boolean open) {
            int signal = open ? 1 : 0;
            playChestSound(level, pos, state, open ? SoundEvents.CHEST_OPEN : SoundEvents.CHEST_CLOSE);
            level.blockEvent(pos, state.getBlock(), ChestBlock.EVENT_SET_OPEN_COUNT, signal);

            if (state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                Direction connectedDirection = ChestBlock.getConnectedDirection(state);
                BlockPos partnerPos = pos.relative(connectedDirection);
                BlockState partnerState = level.getBlockState(partnerPos);
                if (partnerState.getBlock() == state.getBlock()) {
                    level.blockEvent(partnerPos, partnerState.getBlock(), ChestBlock.EVENT_SET_OPEN_COUNT, signal);
                }
            }
        }

        private static void playChestSound(Level level, BlockPos pos, BlockState state, net.minecraft.sounds.SoundEvent soundEvent) {
            double x = pos.getX() + 0.5D;
            double y = pos.getY() + 0.5D;
            double z = pos.getZ() + 0.5D;

            if (state.getValue(ChestBlock.TYPE) == ChestType.RIGHT) {
                Direction connectedDirection = ChestBlock.getConnectedDirection(state);
                x += connectedDirection.getStepX() * 0.5D;
                z += connectedDirection.getStepZ() * 0.5D;
            }

            level.playSound(null, x, y, z, soundEvent, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
        }
    }
}