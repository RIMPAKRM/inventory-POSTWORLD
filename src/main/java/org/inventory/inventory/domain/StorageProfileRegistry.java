package org.inventory.inventory.domain;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Maps items to their StorageProfile.
 * Phase A: populated programmatically.
 * Phase C: replaced by data-driven loading from storage_profiles/*.json.
 */
public final class StorageProfileRegistry {

    private static final Map<ResourceLocation, StorageProfile> BY_ITEM = new HashMap<>();

    private StorageProfileRegistry() {}

    /** Register an item → StorageProfile mapping. */
    public static void register(Item item, StorageProfile profile) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        if (key != null) {
            BY_ITEM.put(key, profile);
        }
    }

    /** Register by item id (used by data-driven JSON loader). */
    public static void register(ResourceLocation itemId, StorageProfile profile) {
        if (itemId != null && profile != null) {
            BY_ITEM.put(itemId, profile);
        }
    }

    /** Atomically replace all mappings with validated JSON snapshot. */
    public static synchronized void replaceSnapshot(Map<ResourceLocation, StorageProfile> snapshot) {
        BY_ITEM.clear();
        BY_ITEM.putAll(snapshot);
    }

    /** Look up a StorageProfile for the given stack. Returns empty if none registered. */
    public static Optional<StorageProfile> lookup(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null) return Optional.empty();
        return Optional.ofNullable(BY_ITEM.get(key));
    }

    /** Clear all registered profiles (used in tests or data reload). */
    public static synchronized void clear() {
        BY_ITEM.clear();
    }
}

