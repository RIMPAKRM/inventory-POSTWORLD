package org.inventory.inventory.domain;

import net.minecraft.resources.ResourceLocation;

/**
 * Describes how many dynamic storage slots an equipped item grants.
 * Phase C will load these from data/inventory/storage_profiles/*.json.
 */
public class StorageProfile {

    public static final StorageProfile EMPTY = new StorageProfile(
            ResourceLocation.fromNamespaceAndPath("inventory", "empty"), null, 0);

    private final ResourceLocation id;
    private final EquipmentSlotType slotType;
    private final int slotCount;

    public StorageProfile(ResourceLocation id, EquipmentSlotType slotType, int slotCount) {
        this.id = id;
        this.slotType = slotType;
        this.slotCount = Math.max(0, slotCount);
    }

    public ResourceLocation getId() { return id; }
    public EquipmentSlotType getSlotType() { return slotType; }
    public int getSlotCount() { return slotCount; }

    @Override
    public String toString() {
        return "StorageProfile{id=" + id + ", slotType=" + slotType + ", slots=" + slotCount + "}";
    }
}


