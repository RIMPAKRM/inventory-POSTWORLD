package org.inventory.inventory.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.inventory.inventory.domain.EquipmentSlotType;
import org.inventory.inventory.domain.StorageProfile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Core implementation of IPlayerLoadout.
 *
 * Canonical slot mapping (see CanonicalSlotMapping):
 *   Equipment indices: 100 – 107  (one per EquipmentSlotType ordinal)
 *   Dynamic indices:   200 – 327  (up to MAX_DYNAMIC_SLOTS)
 *   Vanilla indices:   0   – 40   (never modified here)
 */
public class PlayerLoadout implements IPlayerLoadout {

    public static final int SCHEMA_VERSION = 1;
    public static final int MAX_DYNAMIC_SLOTS = 128;
    public static final EquipmentSlotType[] STORAGE_PROVIDERS = new EquipmentSlotType[] {
            EquipmentSlotType.CHEST,
            EquipmentSlotType.VEST,
            EquipmentSlotType.BACKPACK,
            EquipmentSlotType.LEGS
    };
    public static final int SLOTS_PER_PROVIDER = 36;

    // Internal storage — indexed by EquipmentSlotType.ordinal()
    private final ItemStack[] equipment = new ItemStack[EquipmentSlotType.COUNT];
    // Internal dynamic slot storage
    private final ItemStack[] dynamicSlots = new ItemStack[MAX_DYNAMIC_SLOTS];
    private final boolean[] slotActive = new boolean[MAX_DYNAMIC_SLOTS];
    private final EquipmentSlotType[] dynamicProviders = new EquipmentSlotType[MAX_DYNAMIC_SLOTS];
    private int activeDynamicSlotCount = 0;

    private long loadoutVersion = 0L;
    private CompoundTag pendingOverflow = null;

    public PlayerLoadout() {
        Arrays.fill(equipment, ItemStack.EMPTY);
        Arrays.fill(dynamicSlots, ItemStack.EMPTY);
        Arrays.fill(slotActive, false);
        for (int i = 0; i < MAX_DYNAMIC_SLOTS; i++) {
            dynamicProviders[i] = providerForDynamicIndex(i);
        }
    }

    public static EquipmentSlotType providerForDynamicIndex(int localIndex) {
        if (localIndex < 0 || localIndex >= MAX_DYNAMIC_SLOTS) return null;
        int providerIndex = localIndex / SLOTS_PER_PROVIDER;
        if (providerIndex < 0 || providerIndex >= STORAGE_PROVIDERS.length) return null;
        return STORAGE_PROVIDERS[providerIndex];
    }

    public static int providerIndex(EquipmentSlotType provider) {
        for (int i = 0; i < STORAGE_PROVIDERS.length; i++) {
            if (STORAGE_PROVIDERS[i] == provider) return i;
        }
        return -1;
    }

    public static int providerOffset(int localIndex) {
        if (localIndex < 0) return -1;
        return localIndex % SLOTS_PER_PROVIDER;
    }

    // ---- Versioning ----

    @Override
    public int getCurrentSchemaVersion() { return SCHEMA_VERSION; }

    @Override
    public long getLoadoutVersion() { return loadoutVersion; }

    @Override
    public long incrementAndGetLoadoutVersion() { return ++loadoutVersion; }

    // ---- Equipment ----

    @Override
    public ItemStack getEquipment(EquipmentSlotType slot) {
        return equipment[slot.ordinal()].copy();
    }

    @Override
    public ItemStack setEquipment(EquipmentSlotType slot, ItemStack newItem) {
        ItemStack old = equipment[slot.ordinal()];
        equipment[slot.ordinal()] = newItem.isEmpty() ? ItemStack.EMPTY : newItem.copy();
        return old;
    }

    // ---- Dynamic storage ----

    @Override
    public int getDynamicSlotCount() { return activeDynamicSlotCount; }

    @Override
    public ItemStack getDynamicSlot(int localIndex) {
        if (localIndex < 0 || localIndex >= MAX_DYNAMIC_SLOTS) return ItemStack.EMPTY;
        return dynamicSlots[localIndex].copy();
    }

    @Override
    public void setDynamicSlot(int localIndex, ItemStack item) {
        if (localIndex < 0 || localIndex >= MAX_DYNAMIC_SLOTS) return;
        if (!slotActive[localIndex]) return; // Invariant: cannot write to inactive slot
        dynamicSlots[localIndex] = item.isEmpty() ? ItemStack.EMPTY : item.copy();
    }

    @Override
    public boolean isDynamicSlotActive(int localIndex) {
        if (localIndex < 0 || localIndex >= MAX_DYNAMIC_SLOTS) return false;
        return slotActive[localIndex];
    }

    @Override
    public EquipmentSlotType getDynamicSlotProvider(int localIndex) {
        if (localIndex < 0 || localIndex >= MAX_DYNAMIC_SLOTS) return null;
        return providerForDynamicIndex(localIndex);
    }

    @Override
    public List<ItemStack> recalculateStorageSlots(Function<ItemStack, Optional<StorageProfile>> profileLookup) {
        Map<EquipmentSlotType, Integer> capacityByProvider = new LinkedHashMap<>();
        for (EquipmentSlotType type : STORAGE_PROVIDERS) {
            capacityByProvider.put(type, 0);
        }

        for (EquipmentSlotType type : EquipmentSlotType.values()) {
            if (!type.providesStorage()) continue;
            ItemStack equipped = equipment[type.ordinal()];
            if (!equipped.isEmpty()) {
                int slots = profileLookup.apply(equipped)
                        .map(StorageProfile::getSlotCount)
                        .orElse(0);
                capacityByProvider.put(type, Math.max(0, slots));
            }
        }

        List<ItemStack> displaced = new ArrayList<>();

        int scanLimit = 0;
        for (EquipmentSlotType provider : STORAGE_PROVIDERS) {
            int providerIdx = providerIndex(provider);
            if (providerIdx < 0) continue;

            int base = providerIdx * SLOTS_PER_PROVIDER;
            int cap = Math.min(SLOTS_PER_PROVIDER, Math.max(0, capacityByProvider.getOrDefault(provider, 0)));

            for (int offset = 0; offset < SLOTS_PER_PROVIDER && base + offset < MAX_DYNAMIC_SLOTS; offset++) {
                int idx = base + offset;
                dynamicProviders[idx] = provider;
                boolean shouldBeActive = offset < cap;
                if (shouldBeActive) {
                    scanLimit = Math.max(scanLimit, idx + 1);
                }
                if (!shouldBeActive && !dynamicSlots[idx].isEmpty()) {
                    displaced.add(dynamicSlots[idx].copy());
                    dynamicSlots[idx] = ItemStack.EMPTY;
                }
                slotActive[idx] = shouldBeActive;
            }
        }

        int usedByProviders = STORAGE_PROVIDERS.length * SLOTS_PER_PROVIDER;
        for (int i = usedByProviders; i < MAX_DYNAMIC_SLOTS; i++) {
            if (slotActive[i] && !dynamicSlots[i].isEmpty()) {
                displaced.add(dynamicSlots[i].copy());
            }
            slotActive[i] = false;
            dynamicProviders[i] = null;
            dynamicSlots[i] = ItemStack.EMPTY;
        }

        activeDynamicSlotCount = Math.min(scanLimit, MAX_DYNAMIC_SLOTS);
        return displaced;
    }

    // ---- Disconnect recovery ----

    @Override public boolean hasPendingOverflow() { return pendingOverflow != null; }
    @Override public CompoundTag getPendingOverflow() { return pendingOverflow; }
    @Override public void setPendingOverflow(CompoundTag tag) { this.pendingOverflow = tag; }
    @Override public void clearPendingOverflow() { this.pendingOverflow = null; }

    // ---- NBT ----

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("schemaVersion", SCHEMA_VERSION);
        nbt.putLong("loadoutVersion", loadoutVersion);

        // Equipment
        CompoundTag equipTag = new CompoundTag();
        for (EquipmentSlotType type : EquipmentSlotType.values()) {
            ItemStack stack = equipment[type.ordinal()];
            if (!stack.isEmpty()) {
                equipTag.put(type.name(), stack.save(new CompoundTag()));
            }
        }
        nbt.put("equipment", equipTag);

        // Dynamic slots
        nbt.putInt("activeDynamicSlotCount", activeDynamicSlotCount);
        ListTag dynList = new ListTag();
        for (int i = 0; i < activeDynamicSlotCount; i++) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("idx", i);
            entry.putBoolean("active", slotActive[i]);
            if (dynamicProviders[i] != null) {
                entry.putString("provider", dynamicProviders[i].name());
            }
            if (!dynamicSlots[i].isEmpty()) {
                entry.put("item", dynamicSlots[i].save(new CompoundTag()));
            }
            dynList.add(entry);
        }
        nbt.put("dynamicSlots", dynList);

        // Pending overflow
        if (pendingOverflow != null) {
            nbt.put("pendingOverflow", pendingOverflow.copy());
        }

        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        int savedVersion = nbt.getInt("schemaVersion");
        // Apply migrations if necessary
        nbt = LoadoutMigrator.migrate(nbt, savedVersion, SCHEMA_VERSION);

        loadoutVersion = nbt.getLong("loadoutVersion");

        // Equipment
        Arrays.fill(equipment, ItemStack.EMPTY);
        CompoundTag equipTag = nbt.getCompound("equipment");
        for (EquipmentSlotType type : EquipmentSlotType.values()) {
            if (equipTag.contains(type.name())) {
                equipment[type.ordinal()] = ItemStack.of(equipTag.getCompound(type.name()));
            }
        }

        // Dynamic slots
        Arrays.fill(dynamicSlots, ItemStack.EMPTY);
        Arrays.fill(slotActive, false);
        Arrays.fill(dynamicProviders, null);
        activeDynamicSlotCount = nbt.getInt("activeDynamicSlotCount");
        ListTag dynList = nbt.getList("dynamicSlots", Tag.TAG_COMPOUND);
        for (int i = 0; i < dynList.size(); i++) {
            CompoundTag entry = dynList.getCompound(i);
            int idx = entry.getInt("idx");
            if (idx >= 0 && idx < MAX_DYNAMIC_SLOTS) {
                slotActive[idx] = entry.getBoolean("active");
                if (entry.contains("provider")) {
                    try {
                        dynamicProviders[idx] = EquipmentSlotType.valueOf(entry.getString("provider"));
                    } catch (IllegalArgumentException ignored) {
                        dynamicProviders[idx] = null;
                    }
                }
                if (entry.contains("item")) {
                    dynamicSlots[idx] = ItemStack.of(entry.getCompound("item"));
                }
            }
        }

        // Pending overflow
        pendingOverflow = nbt.contains("pendingOverflow") ? nbt.getCompound("pendingOverflow") : null;
    }
}

