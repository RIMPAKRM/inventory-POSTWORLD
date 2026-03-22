package org.inventory.inventory.domain;

import net.minecraft.resources.ResourceLocation;

/**
 * A category that groups related {@link CraftCard}s in the Craft UI.
 *
 * Phase C: loaded from {@code data/inventory/craft_categories/*.json}.
 * For Phase B, instances are created programmatically via
 * {@link CraftCardRegistry#registerCategory}.
 */
public final class CraftCategory {

    private final ResourceLocation id;
    private final String displayName;
    private final int sortOrder;

    public CraftCategory(ResourceLocation id, String displayName, int sortOrder) {
        this.id          = id;
        this.displayName = displayName;
        this.sortOrder   = sortOrder;
    }

    public ResourceLocation getId()      { return id; }
    public String getDisplayName()       { return displayName; }
    public int getSortOrder()            { return sortOrder; }

    @Override
    public String toString() {
        return "CraftCategory{id=" + id + ", sortOrder=" + sortOrder + "}";
    }
}

