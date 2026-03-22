package org.inventory.inventory.domain;

import net.minecraft.world.item.Item;

/**
 * A single ingredient requirement for a {@link CraftCard}.
 *
 * @param item  the required Item type
 * @param count how many of that item are needed
 */
public record CraftIngredient(Item item, int count) {

    public CraftIngredient {
        if (item == null) throw new IllegalArgumentException("CraftIngredient.item must not be null");
        if (count <= 0)   throw new IllegalArgumentException("CraftIngredient.count must be > 0");
    }
}

