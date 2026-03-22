package org.inventory.inventory.server.saga;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.ItemStack;
import org.inventory.inventory.capability.IPlayerLoadout;
import org.inventory.inventory.capability.LoadoutCapability;
import org.inventory.inventory.domain.CraftCard;
import org.inventory.inventory.domain.CraftIngredient;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Craft Saga: atomically consumes ingredients and grants the crafted result.
 *
 * Context keys (set before execute by CraftService):
 *   "craftCard"   → {@link CraftCard}        the recipe to execute
 *
 * Steps:
 *   1. validateRecipe        — verify the recipe exists and conditions are met
 *   2. checkIngredients      — verify the player has all required items
 *   3. consumeIngredients    — remove the ingredients from the player's inventory
 *      compensation → returnIngredients (add items back)
 *   4. grantResult           — add the crafted item to the player's inventory
 *      compensation → removeGranted (remove the result if placed)
 *
 * The per-player lock held by InventoryTransactionService prevents concurrent
 * operations from interleaving between steps, so no separate reservation token
 * is needed for Phase B.
 */
public final class CraftSaga {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Context key: the recipe to craft. */
    public static final String KEY_CRAFT_CARD = "craftCard";
    /** Context key: snapshot of consumed stacks (for compensation). */
    public static final String KEY_CONSUMED   = "consumedStacks";
    /** Context key: granted result stack (for compensation). */
    public static final String KEY_GRANTED    = "grantedStack";
    private static final int VANILLA_HOTBAR_START = 0;
    private static final int VANILLA_HOTBAR_END = 8;

    private CraftSaga() {}

    public static List<SagaStep> steps() {
        return List.of(
                new ValidateRecipeStep(),
                new CheckIngredientsStep(),
                new ConsumeIngredientsStep(),
                new GrantResultStep()
        );
    }

    // ---- Step implementations ----

    private static class ValidateRecipeStep implements SagaStep {
        @Override public String name() { return "validateRecipe"; }

        @Override
        public boolean execute(SagaContext ctx) {
            CraftCard card = ctx.get(KEY_CRAFT_CARD);
            if (card == null) {
                LOGGER.warn("[CraftSaga] no craftCard in context opId={}", ctx.opId);
                return false;
            }
            LOGGER.debug("[CraftSaga] validateRecipe id={} opId={}", card.getId(), ctx.opId);
            return true;
        }

        @Override public void compensate(SagaContext ctx) {}
    }

    private static class CheckIngredientsStep implements SagaStep {
        @Override public String name() { return "checkIngredients"; }

        @Override
        public boolean execute(SagaContext ctx) {
            CraftCard card = ctx.get(KEY_CRAFT_CARD);
            IPlayerLoadout loadout = ctx.player.getCapability(LoadoutCapability.PLAYER_LOADOUT).orElse(null);

            for (CraftIngredient ingredient : card.getIngredients()) {
                int available = countItem(ctx, ingredient, loadout);
                if (available < ingredient.count()) {
                    LOGGER.debug("[CraftSaga] missing ingredient {} (have={} need={}) opId={}",
                            ingredient.item().getDescriptionId(), available, ingredient.count(), ctx.opId);
                    return false;
                }
            }
            return true;
        }

        @Override public void compensate(SagaContext ctx) {}
    }

    private static class ConsumeIngredientsStep implements SagaStep {
        @Override public String name() { return "consumeIngredients"; }

        @Override
        public boolean execute(SagaContext ctx) {
            CraftCard card = ctx.get(KEY_CRAFT_CARD);
            IPlayerLoadout loadout = ctx.player.getCapability(LoadoutCapability.PLAYER_LOADOUT).orElse(null);

            List<ItemStack> consumed = new ArrayList<>();
            for (CraftIngredient ingredient : card.getIngredients()) {
                List<ItemStack> taken = removeItems(ctx, ingredient, loadout);
                if (taken == null) {
                    // Failed mid-way — store partial consumed for compensation
                    ctx.set(KEY_CONSUMED, consumed);
                    return false;
                }
                consumed.addAll(taken);
            }
            ctx.set(KEY_CONSUMED, consumed);
            return true;
        }

        @Override
        public void compensate(SagaContext ctx) {
            // Return consumed ingredients to the player
            List<ItemStack> consumed = ctx.getOrDefault(KEY_CONSUMED, new ArrayList<>());
            if (consumed.isEmpty()) return;

            LOGGER.debug("[CraftSaga] returning {} consumed stacks opId={}", consumed.size(), ctx.opId);
            for (ItemStack stack : consumed) {
                if (!stack.isEmpty()) {
                    if (!ctx.player.getInventory().add(stack)) {
                        ctx.player.drop(stack, false);
                    }
                }
            }
        }
    }

    private static class GrantResultStep implements SagaStep {
        @Override public String name() { return "grantResult"; }

        @Override
        public boolean execute(SagaContext ctx) {
            CraftCard card = ctx.get(KEY_CRAFT_CARD);
            ItemStack result = card.getResult().copy();
            IPlayerLoadout loadout = ctx.player.getCapability(LoadoutCapability.PLAYER_LOADOUT).orElse(null);

            ItemStack remainder = placeIntoHotbar(ctx, result.copy());
            if (!remainder.isEmpty() && loadout != null) {
                remainder = placeIntoDynamic(loadout, remainder);
            }

            if (!remainder.isEmpty()) {
                // Inventory full — drop to world but still count as success
                ctx.player.drop(remainder, false);
                LOGGER.debug("[CraftSaga] inventory full, dropped result opId={}", ctx.opId);
            } else {
                ctx.set(KEY_GRANTED, result);
            }
            LOGGER.debug("[CraftSaga] granted {}×{} opId={}",
                    card.getResult().getItem().getDescriptionId(), card.getResult().getCount(), ctx.opId);
            return true;
        }

        @Override
        public void compensate(SagaContext ctx) {
            ItemStack granted = ctx.get(KEY_GRANTED);
            if (granted == null || granted.isEmpty()) return;

            LOGGER.debug("[CraftSaga] removing granted item for rollback opId={}", ctx.opId);
            IPlayerLoadout loadout = ctx.player.getCapability(LoadoutCapability.PLAYER_LOADOUT).orElse(null);
            int remaining = removeFromHotbar(ctx, granted.copy());
            if (remaining > 0 && loadout != null) {
                remaining = removeFromDynamic(loadout, granted.copyWithCount(remaining));
            }
            boolean removed = remaining <= 0;
            if (!removed) {
                LOGGER.warn("[CraftSaga] could not remove granted item during rollback opId={}", ctx.opId);
            }
        }
    }

    // ---- Helpers ----

    private static int countItem(SagaContext ctx, CraftIngredient ingredient, IPlayerLoadout loadout) {
        int total = 0;
        // Count only in vanilla hotbar (0..8). Main grid is disabled in this mod.
        for (int i = VANILLA_HOTBAR_START; i <= VANILLA_HOTBAR_END; i++) {
            ItemStack slot = ctx.player.getInventory().getItem(i);
            if (!slot.isEmpty() && slot.is(ingredient.item())) {
                total += slot.getCount();
            }
        }
        // Count in dynamic loadout slots
        if (loadout != null) {
            for (int i = 0; i < loadout.getDynamicSlotCount(); i++) {
                if (!loadout.isDynamicSlotActive(i)) continue;
                ItemStack slot = loadout.getDynamicSlot(i);
                if (!slot.isEmpty() && slot.is(ingredient.item())) {
                    total += slot.getCount();
                }
            }
        }
        return total;
    }

    /**
     * Remove {@code ingredient.count()} matching items from the player's inventory.
     * Returns a list of the stacks that were removed, or {@code null} on failure.
     */
    private static List<ItemStack> removeItems(SagaContext ctx, CraftIngredient ingredient, IPlayerLoadout loadout) {
        int remaining = ingredient.count();
        List<ItemStack> removed = new ArrayList<>();

        // Remove from vanilla hotbar first
        for (int i = VANILLA_HOTBAR_START; i <= VANILLA_HOTBAR_END && remaining > 0; i++) {
            ItemStack slot = ctx.player.getInventory().getItem(i);
            if (!slot.isEmpty() && slot.is(ingredient.item())) {
                int take = Math.min(remaining, slot.getCount());
                removed.add(slot.copyWithCount(take));
                slot.shrink(take);
                remaining -= take;
                if (slot.isEmpty()) ctx.player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
        // Then from dynamic loadout slots
        if (loadout != null) {
            for (int i = 0; i < loadout.getDynamicSlotCount() && remaining > 0; i++) {
                if (!loadout.isDynamicSlotActive(i)) continue;
                ItemStack slot = loadout.getDynamicSlot(i);
                if (!slot.isEmpty() && slot.is(ingredient.item())) {
                    int take = Math.min(remaining, slot.getCount());
                    removed.add(slot.copyWithCount(take));
                    ItemStack updated = slot.copy();
                    updated.shrink(take);
                    loadout.setDynamicSlot(i, updated.isEmpty() ? ItemStack.EMPTY : updated);
                    remaining -= take;
                }
            }
        }

        if (remaining > 0) return null; // not enough
        return removed;
    }

    private static ItemStack placeIntoHotbar(SagaContext ctx, ItemStack stack) {
        ItemStack rem = stack.copy();

        for (int i = VANILLA_HOTBAR_START; i <= VANILLA_HOTBAR_END && !rem.isEmpty(); i++) {
            ItemStack slot = ctx.player.getInventory().getItem(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameTags(slot, rem)) {
                int move = Math.min(slot.getMaxStackSize() - slot.getCount(), rem.getCount());
                if (move > 0) {
                    slot.grow(move);
                    rem.shrink(move);
                }
            }
        }

        for (int i = VANILLA_HOTBAR_START; i <= VANILLA_HOTBAR_END && !rem.isEmpty(); i++) {
            if (ctx.player.getInventory().getItem(i).isEmpty()) {
                int place = Math.min(rem.getMaxStackSize(), rem.getCount());
                ItemStack placed = rem.copy();
                placed.setCount(place);
                ctx.player.getInventory().setItem(i, placed);
                rem.shrink(place);
            }
        }

        return rem;
    }

    private static ItemStack placeIntoDynamic(IPlayerLoadout loadout, ItemStack stack) {
        ItemStack rem = stack.copy();

        for (int i = 0; i < loadout.getDynamicSlotCount() && !rem.isEmpty(); i++) {
            if (!loadout.isDynamicSlotActive(i)) continue;
            ItemStack slot = loadout.getDynamicSlot(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameTags(slot, rem)) {
                int move = Math.min(slot.getMaxStackSize() - slot.getCount(), rem.getCount());
                if (move > 0) {
                    ItemStack updated = slot.copy();
                    updated.grow(move);
                    loadout.setDynamicSlot(i, updated);
                    rem.shrink(move);
                }
            }
        }

        for (int i = 0; i < loadout.getDynamicSlotCount() && !rem.isEmpty(); i++) {
            if (!loadout.isDynamicSlotActive(i)) continue;
            if (loadout.getDynamicSlot(i).isEmpty()) {
                int place = Math.min(rem.getMaxStackSize(), rem.getCount());
                ItemStack placed = rem.copy();
                placed.setCount(place);
                loadout.setDynamicSlot(i, placed);
                rem.shrink(place);
            }
        }

        return rem;
    }

    private static int removeFromHotbar(SagaContext ctx, ItemStack target) {
        int remaining = target.getCount();
        for (int i = VANILLA_HOTBAR_START; i <= VANILLA_HOTBAR_END && remaining > 0; i++) {
            ItemStack slot = ctx.player.getInventory().getItem(i);
            if (slot.isEmpty() || !ItemStack.isSameItemSameTags(slot, target)) continue;
            int take = Math.min(remaining, slot.getCount());
            slot.shrink(take);
            remaining -= take;
            if (slot.isEmpty()) {
                ctx.player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
        return remaining;
    }

    private static int removeFromDynamic(IPlayerLoadout loadout, ItemStack target) {
        int remaining = target.getCount();
        for (int i = 0; i < loadout.getDynamicSlotCount() && remaining > 0; i++) {
            if (!loadout.isDynamicSlotActive(i)) continue;
            ItemStack slot = loadout.getDynamicSlot(i);
            if (slot.isEmpty() || !ItemStack.isSameItemSameTags(slot, target)) continue;

            int take = Math.min(remaining, slot.getCount());
            ItemStack updated = slot.copy();
            updated.shrink(take);
            loadout.setDynamicSlot(i, updated.isEmpty() ? ItemStack.EMPTY : updated);
            remaining -= take;
        }
        return remaining;
    }
}

