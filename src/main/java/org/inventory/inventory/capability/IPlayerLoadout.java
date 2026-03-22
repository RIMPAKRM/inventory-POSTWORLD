package org.inventory.inventory.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.inventory.inventory.domain.EquipmentSlotType;
import org.inventory.inventory.domain.StorageProfile;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Interface for per-player loadout state (equipment + dynamic storage).
 *
 * Server is the sole source of truth. All mutations must go through
 * InventoryTransactionService to maintain transactional guarantees.
 */
public interface IPlayerLoadout {

    // ---- Versioning ----

    /** Schema version embedded in NBT for migration. */
    int getCurrentSchemaVersion();

    /**
     * Optimistic concurrency version. Incremented on every committed operation.
     * C2S packets must match this value; mismatch causes rejection.
     */
    long getLoadoutVersion();

    /** Increments and returns the new loadout version. Called by the transaction service on commit. */
    long incrementAndGetLoadoutVersion();

    // ---- Equipment ----

    /** Returns a copy of the item in the given equipment slot (EMPTY if nothing equipped). */
    ItemStack getEquipment(EquipmentSlotType slot);

    /**
     * Replaces the item in the given equipment slot with newItem.
     * Returns the previously equipped item.
     * Does NOT trigger storage recalculation — call recalculateStorageSlots() after.
     */
    ItemStack setEquipment(EquipmentSlotType slot, ItemStack newItem);

    // ---- Dynamic storage slots ----

    /** Number of currently active dynamic storage slots. */
    int getDynamicSlotCount();

    /** Returns a copy of the item in local dynamic slot index (EMPTY if empty). */
    ItemStack getDynamicSlot(int localIndex);

    /** Sets item in local dynamic slot index. Ignored if slot is inactive. */
    void setDynamicSlot(int localIndex, ItemStack item);

    /** True if the dynamic slot at localIndex is currently active. */
    boolean isDynamicSlotActive(int localIndex);

    /**
     * Provider equipment slot for a dynamic slot.
     * Returns null only when provider is unknown for this index.
     */
    EquipmentSlotType getDynamicSlotProvider(int localIndex);

    /**
     * Recalculates active dynamic slot count from current equipment via profileLookup.
     * Slots that are deactivated have their items displaced (returned in the list).
     * Newly activated slots start empty.
     *
     * @param profileLookup maps an equipped ItemStack to its StorageProfile
     * @return items displaced from deactivated slots (must be handled by OverflowService)
     */
    List<ItemStack> recalculateStorageSlots(Function<ItemStack, Optional<StorageProfile>> profileLookup);

    // ---- Disconnect recovery ----

    boolean hasPendingOverflow();
    CompoundTag getPendingOverflow();
    void setPendingOverflow(CompoundTag tag);
    void clearPendingOverflow();

    // ---- NBT ----

    CompoundTag serializeNBT();
    void deserializeNBT(CompoundTag nbt);
}

