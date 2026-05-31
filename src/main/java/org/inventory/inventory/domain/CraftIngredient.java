package org.inventory.inventory.domain;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;

/**
 * A single ingredient requirement for a {@link CraftCard}.
 *
 * @param item  the required Item type
 * @param count how many of that item are needed
 * @param tag   optional tag for group of items
 */
public record CraftIngredient(Item item, int count, ResourceLocation tag) {

    public CraftIngredient {
        if (item == null) throw new IllegalArgumentException("CraftIngredient.item must not be null");
        if (count <= 0)   throw new IllegalArgumentException("CraftIngredient.count must be > 0");
    }

    // Конструктор без тега
    public CraftIngredient(Item item, int count) {
        this(item, count, null);
    }

    // Проверка соответствия ItemStack
    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (tag != null) {
            // Превращаем ResourceLocation в TagKey<Item>
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tag);
            return stack.is(tagKey);
        } else {
            return stack.is(item);
        }
    }
}
