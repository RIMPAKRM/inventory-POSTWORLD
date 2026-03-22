package org.inventory.inventory.domain;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

/**
 * Registry for ProtectionProfile with deterministic conflict resolution.
 */
public final class ProtectionProfileRegistry {

    private static final Map<ResourceLocation, List<ProtectionProfile>> BY_ITEM = new HashMap<>();

    private ProtectionProfileRegistry() {}

    public static synchronized void register(ResourceLocation itemId, ProtectionProfile profile) {
        if (itemId == null || profile == null) return;
        BY_ITEM.computeIfAbsent(itemId, k -> new ArrayList<>()).add(profile);
        BY_ITEM.get(itemId).sort(ProtectionProfile.ORDER);
    }

    public static synchronized void replaceSnapshot(Map<ResourceLocation, List<ProtectionProfile>> snapshot) {
        BY_ITEM.clear();
        for (Map.Entry<ResourceLocation, List<ProtectionProfile>> e : snapshot.entrySet()) {
            List<ProtectionProfile> sorted = new ArrayList<>(e.getValue());
            sorted.sort(ProtectionProfile.ORDER);
            BY_ITEM.put(e.getKey(), Collections.unmodifiableList(sorted));
        }
    }

    public static Optional<ProtectionProfile> resolve(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) return Optional.empty();
        return resolve(itemId);
    }

    public static Optional<ProtectionProfile> resolve(ResourceLocation itemId) {
        List<ProtectionProfile> profiles = BY_ITEM.get(itemId);
        if (profiles == null || profiles.isEmpty()) return Optional.empty();
        return Optional.of(profiles.get(0));
    }

    public static synchronized List<ProtectionProfile> allForItem(ResourceLocation itemId) {
        List<ProtectionProfile> profiles = BY_ITEM.get(itemId);
        if (profiles == null) return List.of();
        return profiles;
    }

    public static synchronized void clear() {
        BY_ITEM.clear();
    }
}

