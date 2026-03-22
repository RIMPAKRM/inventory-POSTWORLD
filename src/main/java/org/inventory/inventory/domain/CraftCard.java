package org.inventory.inventory.domain;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

/**
 * A recipe card: defines what ingredients are consumed and what item is produced.
 *
 * Phase B: created programmatically via {@link CraftCardRegistry#register}.
 * Phase C: loaded from {@code data/inventory/craft_cards/*.json} with
 *          JSON schema validation and soft-disable fallback.
 */
public final class CraftCard {

    private final ResourceLocation id;
    private final ResourceLocation categoryId;
    private final List<CraftIngredient> ingredients;
    private final ItemStack result;

    public CraftCard(ResourceLocation id,
                     ResourceLocation categoryId,
                     List<CraftIngredient> ingredients,
                     ItemStack result) {
        if (id == null)          throw new IllegalArgumentException("CraftCard.id must not be null");
        if (categoryId == null)  throw new IllegalArgumentException("CraftCard.categoryId must not be null");
        if (ingredients == null || ingredients.isEmpty())
            throw new IllegalArgumentException("CraftCard.ingredients must not be empty");
        if (result == null || result.isEmpty())
            throw new IllegalArgumentException("CraftCard.result must not be empty");

        this.id          = id;
        this.categoryId  = categoryId;
        this.ingredients = Collections.unmodifiableList(ingredients);
        this.result      = result.copy();
    }

    public ResourceLocation getId()               { return id; }
    public ResourceLocation getCategoryId()       { return categoryId; }
    public List<CraftIngredient> getIngredients() { return ingredients; }
    public ItemStack getResult()                  { return result.copy(); }

    @Override
    public String toString() {
        return "CraftCard{id=" + id + ", result=" + result.getItem().getDescriptionId() + "×" + result.getCount() + "}";
    }
}

