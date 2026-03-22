package org.inventory.inventory.domain;

import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.List;

/**
 * Data-driven defense profile applied to one or more items.
 *
 * Conflict resolution is deterministic:
 *  1) higher priority first
 *  2) then lexicographical ResourceLocation id
 */
public final class ProtectionProfile {

    public static final Comparator<ProtectionProfile> ORDER =
            Comparator.comparingInt(ProtectionProfile::getPriority).reversed()
                    .thenComparing(p -> p.getId().toString());

    private final ResourceLocation id;
    private final double armorValue;
    private final double durabilityModifier;
    private final String weightClass;
    private final List<ResourceLocation> tags;
    private final int priority;

    public ProtectionProfile(ResourceLocation id,
                             double armorValue,
                             double durabilityModifier,
                             String weightClass,
                             List<ResourceLocation> tags,
                             int priority) {
        if (id == null) throw new IllegalArgumentException("ProtectionProfile.id must not be null");
        if (armorValue < 0d) throw new IllegalArgumentException("armorValue must be >= 0");
        if (durabilityModifier <= 0d) throw new IllegalArgumentException("durabilityModifier must be > 0");

        this.id = id;
        this.armorValue = armorValue;
        this.durabilityModifier = durabilityModifier;
        this.weightClass = (weightClass == null || weightClass.isBlank()) ? "medium" : weightClass;
        this.tags = tags == null ? List.of() : List.copyOf(tags);
        this.priority = priority;
    }

    public ResourceLocation getId() { return id; }
    public double getArmorValue() { return armorValue; }
    public double getDurabilityModifier() { return durabilityModifier; }
    public String getWeightClass() { return weightClass; }
    public List<ResourceLocation> getTags() { return tags; }
    public int getPriority() { return priority; }
}

