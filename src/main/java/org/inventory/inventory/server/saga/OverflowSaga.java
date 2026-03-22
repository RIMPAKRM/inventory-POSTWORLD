package org.inventory.inventory.server.saga;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.inventory.inventory.capability.IPlayerLoadout;
import org.inventory.inventory.capability.LoadoutCapability;
import org.inventory.inventory.server.MetricsService;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Overflow Saga: safely redistributes items displaced when equipment is removed.
 *
 * Context keys used (all set by PersistJournalStep):
 *   "displaced"   → {@code List<ItemStack>} items to redistribute
 *
 * Steps:
 *   1. persistJournal    — write pendingOverflow NBT (crash/disconnect recovery)
 *   2. moveToAvailable   — distribute into vanilla + active dynamic slots
 *   3. dropRemainder     — drop whatever didn't fit
 *   4. clearJournal      — remove pendingOverflow NBT
 *
 * Compensations: steps 1-3 have no safe inverse (item movements cannot be
 * easily reversed without complex rollback logic). Instead, the journal (step 1)
 * persists across crashes; on relog, OverflowService.recoverPendingOverflow()
 * re-runs the saga from the persisted state.
 */
public final class OverflowSaga {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Context key: items displaced from deactivated slots. */
    public static final String KEY_DISPLACED = "displaced";
    /** Context key: items that didn't fit anywhere (to be dropped). */
    public static final String KEY_REMAINDER  = "remainder";
    private static final int VANILLA_HOTBAR_START = 0;
    private static final int VANILLA_HOTBAR_END = 8;

    private OverflowSaga() {}

    public static List<SagaStep> steps() {
        return List.of(
                new PersistJournalStep(),
                new MoveToAvailableStep(),
                new DropRemainderStep(),
                new ClearJournalStep()
        );
    }

    // ---- Step implementations ----

    private static class PersistJournalStep implements SagaStep {
        @Override public String name() { return "persistJournal"; }

        @Override
        public boolean execute(SagaContext ctx) {
            List<ItemStack> displaced = ctx.get(KEY_DISPLACED);
            if (displaced == null || displaced.isEmpty()) {
                // Nothing to overflow — skip remaining steps via context flag
                ctx.set("overflow.empty", true);
                return true;
            }
            ctx.player.getCapability(LoadoutCapability.PLAYER_LOADOUT).ifPresent(loadout ->
                    loadout.setPendingOverflow(buildPendingTag(displaced, ctx))
            );
            MetricsService.incrementOverflows();
            return true;
        }

        @Override public void compensate(SagaContext ctx) { /* journal removal is done in ClearJournalStep */ }
    }

    private static class MoveToAvailableStep implements SagaStep {
        @Override public String name() { return "moveToAvailable"; }

        @Override
        public boolean execute(SagaContext ctx) {
            if (Boolean.TRUE.equals(ctx.get("overflow.empty"))) return true;

            List<ItemStack> displaced = ctx.get(KEY_DISPLACED);
            IPlayerLoadout loadout = ctx.player.getCapability(LoadoutCapability.PLAYER_LOADOUT).orElse(null);
            if (loadout == null) {
                ctx.set(KEY_REMAINDER, new ArrayList<>(displaced));
                return true; // continue — drop all in next step
            }

            List<ItemStack> remainder = moveToAvailable(ctx.player, loadout, displaced, ctx);
            ctx.set(KEY_REMAINDER, remainder);
            return true;
        }

        @Override public void compensate(SagaContext ctx) { /* item moves cannot be cleanly reversed */ }
    }

    private static class DropRemainderStep implements SagaStep {
        @Override public String name() { return "dropRemainder"; }

        @Override
        public boolean execute(SagaContext ctx) {
            if (Boolean.TRUE.equals(ctx.get("overflow.empty"))) return true;

            List<ItemStack> remainder = ctx.getOrDefault(KEY_REMAINDER, new ArrayList<>());
            for (ItemStack item : remainder) {
                if (!item.isEmpty()) {
                    LOGGER.debug("[OverflowSaga] drop {}×{} opId={}",
                            item.getItem().getDescriptionId(), item.getCount(), ctx.opId);
                    ctx.player.drop(item, false);
                }
            }
            return true;
        }

        @Override public void compensate(SagaContext ctx) { /* drops to world cannot be reversed */ }
    }

    private static class ClearJournalStep implements SagaStep {
        @Override public String name() { return "clearJournal"; }

        @Override
        public boolean execute(SagaContext ctx) {
            ctx.player.getCapability(LoadoutCapability.PLAYER_LOADOUT)
                    .ifPresent(IPlayerLoadout::clearPendingOverflow);
            return true;
        }

        @Override public void compensate(SagaContext ctx) { /* nothing */ }
    }

    // ---- Internal helpers ----

    private static List<ItemStack> moveToAvailable(
            ServerPlayer player, IPlayerLoadout loadout,
            List<ItemStack> displaced, SagaContext ctx) {

        List<ItemStack> remainder = new ArrayList<>();
        for (ItemStack stack : displaced) {
            ItemStack rem = stack.copy();
            rem = mergeIntoVanilla(player, rem, ctx);
            if (!rem.isEmpty()) {
                rem = mergeIntoDynamic(loadout, rem, ctx);
            }
            if (!rem.isEmpty()) {
                remainder.add(rem);
            }
        }
        return remainder;
    }

    private static ItemStack mergeIntoVanilla(ServerPlayer player, ItemStack stack, SagaContext ctx) {
        ItemStack rem = stack.copy();

        // Pass 1: merge into existing stacks (hotbar only)
        for (int i = VANILLA_HOTBAR_START; i <= VANILLA_HOTBAR_END && !rem.isEmpty(); i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameTags(slot, rem)) {
                int space = slot.getMaxStackSize() - slot.getCount();
                int move  = Math.min(space, rem.getCount());
                if (move > 0) {
                    slot.grow(move);
                    rem.shrink(move);
                    LOGGER.debug("[OverflowSaga] merged {} into vanilla[{}] opId={}", move, i, ctx.opId);
                }
            }
        }
        // Pass 2: empty slots (hotbar only)
        for (int i = VANILLA_HOTBAR_START; i <= VANILLA_HOTBAR_END && !rem.isEmpty(); i++) {
            if (player.getInventory().getItem(i).isEmpty()) {
                int place  = Math.min(rem.getMaxStackSize(), rem.getCount());
                ItemStack placed = rem.copy();
                placed.setCount(place);
                player.getInventory().setItem(i, placed);
                rem.shrink(place);
                LOGGER.debug("[OverflowSaga] placed {} into vanilla[{}] opId={}", place, i, ctx.opId);
            }
        }
        return rem;
    }

    private static ItemStack mergeIntoDynamic(IPlayerLoadout loadout, ItemStack stack, SagaContext ctx) {
        ItemStack rem = stack.copy();

        // Pass 1: merge into existing
        for (int i = 0; i < loadout.getDynamicSlotCount() && !rem.isEmpty(); i++) {
            if (!loadout.isDynamicSlotActive(i)) continue;
            ItemStack slot = loadout.getDynamicSlot(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameTags(slot, rem)) {
                int space = slot.getMaxStackSize() - slot.getCount();
                int move  = Math.min(space, rem.getCount());
                if (move > 0) {
                    ItemStack updated = slot.copy();
                    updated.grow(move);
                    loadout.setDynamicSlot(i, updated);
                    rem.shrink(move);
                    LOGGER.debug("[OverflowSaga] merged {} into dynamic[{}] opId={}", move, i, ctx.opId);
                }
            }
        }
        // Pass 2: empty slots
        for (int i = 0; i < loadout.getDynamicSlotCount() && !rem.isEmpty(); i++) {
            if (!loadout.isDynamicSlotActive(i)) continue;
            if (loadout.getDynamicSlot(i).isEmpty()) {
                int place  = Math.min(rem.getMaxStackSize(), rem.getCount());
                ItemStack placed = rem.copy();
                placed.setCount(place);
                loadout.setDynamicSlot(i, placed);
                rem.shrink(place);
                LOGGER.debug("[OverflowSaga] placed {} into dynamic[{}] opId={}", place, i, ctx.opId);
            }
        }
        return rem;
    }

    private static CompoundTag buildPendingTag(List<ItemStack> items, SagaContext ctx) {
        CompoundTag tag = new CompoundTag();
        tag.putString("opId", ctx.opId.toString());
        ListTag list = new ListTag();
        for (ItemStack item : items) {
            if (!item.isEmpty()) list.add(item.save(new CompoundTag()));
        }
        tag.put("items", list);
        return tag;
    }

    /** Deserialise items from a persisted pendingOverflow tag (used in recovery). */
    public static List<ItemStack> readItemsFromTag(CompoundTag tag) {
        List<ItemStack> items = new ArrayList<>();
        if (tag.contains("items")) {
            ListTag list = tag.getList("items", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                items.add(ItemStack.of(list.getCompound(i)));
            }
        }
        return items;
    }
}

