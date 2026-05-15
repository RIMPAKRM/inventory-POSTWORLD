package org.inventory.inventory.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Exposes the player's hotbar plus custom loadout as a Forge item handler.
 * TACZ queries ForgeCapabilities.ITEM_HANDLER when checking for reload ammo,
 * so this bridge lets ammo stored in the custom inventory participate in reloads.
 */
public final class PlayerAmmoItemHandlerProvider implements ICapabilitySerializable<CompoundTag> {

    private final PlayerAmmoItemHandler handler;
    private final LazyOptional<IItemHandler> lazyOptional;

    public PlayerAmmoItemHandlerProvider(net.minecraft.world.entity.player.Player player) {
        this.handler = new PlayerAmmoItemHandler(player);
        this.lazyOptional = LazyOptional.of(() -> handler);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
        return net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER.orEmpty(cap, lazyOptional);
    }

    @Override
    public CompoundTag serializeNBT() {
        return new CompoundTag();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
    }

    public void invalidate() {
        lazyOptional.invalidate();
    }

    private static final class PlayerAmmoItemHandler implements IItemHandler {

        private static final int HOTBAR_SLOTS = 9;

        private final net.minecraft.world.entity.player.Player player;

        private PlayerAmmoItemHandler(net.minecraft.world.entity.player.Player player) {
            this.player = player;
        }

        @Override
        public int getSlots() {
            return HOTBAR_SLOTS + getLoadoutSlotCount();
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            if (slot < 0) {
                return ItemStack.EMPTY;
            }

            if (slot < HOTBAR_SLOTS) {
                return player.getInventory().getItem(slot).copy();
            }

            IPlayerLoadout loadout = getLoadout();
            if (loadout == null) {
                return ItemStack.EMPTY;
            }

            int loadoutIndex = slot - HOTBAR_SLOTS;
            if (loadoutIndex < 0 || loadoutIndex >= loadout.getDynamicSlotCount()) {
                return ItemStack.EMPTY;
            }
            if (!loadout.isDynamicSlotActive(loadoutIndex)) {
                return ItemStack.EMPTY;
            }
            return loadout.getDynamicSlot(loadoutIndex).copy();
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || slot < 0 || slot >= getSlots()) {
                return stack;
            }

            ItemStack existing = getStackInSlot(slot);
            int limit = Math.min(stack.getMaxStackSize(), getSlotLimit(slot));

            if (!existing.isEmpty() && !ItemStack.isSameItemSameTags(existing, stack)) {
                return stack;
            }

            int currentCount = existing.isEmpty() ? 0 : existing.getCount();
            int space = Math.max(0, limit - currentCount);
            if (space <= 0) {
                return stack;
            }

            int toMove = Math.min(space, stack.getCount());
            if (!simulate) {
                ItemStack updated = stack.copy();
                updated.setCount(currentCount + toMove);
                setStackInSlot(slot, updated);
            }

            ItemStack remainder = stack.copy();
            remainder.shrink(toMove);
            return remainder;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount <= 0 || slot < 0 || slot >= getSlots()) {
                return ItemStack.EMPTY;
            }

            ItemStack existing = getStackInSlot(slot);
            if (existing.isEmpty()) {
                return ItemStack.EMPTY;
            }

            int toExtract = Math.min(amount, existing.getCount());
            ItemStack result = existing.copy();
            result.setCount(toExtract);

            if (!simulate) {
                ItemStack remainder = existing.copy();
                remainder.shrink(toExtract);
                setStackInSlot(slot, remainder);
            }

            return result;
        }

        @Override
        public int getSlotLimit(int slot) {
            ItemStack existing = getStackInSlot(slot);
            if (!existing.isEmpty()) {
                return existing.getMaxStackSize();
            }
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot >= 0 && slot < getSlots() && !stack.isEmpty();
        }

        private int getLoadoutSlotCount() {
            IPlayerLoadout loadout = getLoadout();
            return loadout == null ? 0 : loadout.getDynamicSlotCount();
        }

        private IPlayerLoadout getLoadout() {
            return player.getCapability(LoadoutCapability.PLAYER_LOADOUT).orElse(null);
        }

        private void setStackInSlot(int slot, ItemStack stack) {
            if (slot < HOTBAR_SLOTS) {
                player.getInventory().setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
                player.getInventory().setChanged();
                return;
            }

            IPlayerLoadout loadout = getLoadout();
            if (loadout == null) {
                return;
            }

            int loadoutIndex = slot - HOTBAR_SLOTS;
            if (loadoutIndex < 0 || loadoutIndex >= loadout.getDynamicSlotCount()) {
                return;
            }
            if (!loadout.isDynamicSlotActive(loadoutIndex)) {
                return;
            }
            loadout.setDynamicSlot(loadoutIndex, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }
    }
}
