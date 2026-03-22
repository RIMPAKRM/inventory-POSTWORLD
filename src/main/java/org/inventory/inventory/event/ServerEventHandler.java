package org.inventory.inventory.event;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.GameRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.inventory.inventory.Inventory;
import org.inventory.inventory.capability.IPlayerLoadout;
import org.inventory.inventory.capability.LoadoutCapability;
import org.inventory.inventory.domain.EquipmentSlotType;
import org.inventory.inventory.data.DataDrivenContentLoader;
import org.inventory.inventory.domain.StorageProfileRegistry;
import org.inventory.inventory.server.ArmorAttributeService;
import org.inventory.inventory.server.InventoryTransactionService;
import org.inventory.inventory.server.LoadoutSyncScheduler;
import org.inventory.inventory.server.MetricsService;
import org.inventory.inventory.server.OpContext;
import org.inventory.inventory.server.OverflowService;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Server-side Forge EVENT_BUS handler for Phase B.
 *
 * Responsibilities:
 *  - Drive {@link LoadoutSyncScheduler#tick} on every server tick so
 *    debounced S2C_LoadoutSync packets are flushed on time.
 *  - Log a periodic metrics summary every 20 min (24_000 ticks).
 */
@Mod.EventBusSubscriber(modid = Inventory.MODID)
public final class ServerEventHandler {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int METRICS_LOG_INTERVAL = 24_000; // ~20 min at 20 TPS
    private static long tickCount = 0;

    private ServerEventHandler() {}

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        LoadoutSyncScheduler.tick(event.getServer());

        if (++tickCount % METRICS_LOG_INTERVAL == 0) {
            MetricsService.logSummary();
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.isCreative() || player.isSpectator()) return;

        net.minecraft.world.entity.player.Inventory inv = player.getInventory();
        // Vanilla main inventory slots (9..35) are disabled by design.
        for (int slotIndex = 9; slotIndex <= 35; slotIndex++) {
            ItemStack stack = inv.getItem(slotIndex);
            if (stack.isEmpty()) continue;

            ItemStack remainder = moveToAllowedStorage(player, stack.copy());
            inv.setItem(slotIndex, ItemStack.EMPTY);

            if (!remainder.isEmpty()) {
                player.drop(remainder, false);
                LOGGER.debug("[ServerEventHandler] dropped overflow from disabled vanilla[{}] player={}",
                        slotIndex, player.getName().getString());
            }
        }
    }

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isCreative() || player.isSpectator()) return;

        ItemStack incoming = event.getItem().getItem();
        if (incoming.isEmpty()) return;

        if (!hasCapacityForWholeStack(player, incoming)) {
            // Reject pickup early: prevents "pickup then forced drop" behavior.
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isCreative() || player.isSpectator()) return;

        ItemStack inHand = event.getItemStack();
        if (inHand.isEmpty()) return;
        if (!(inHand.getItem() instanceof ArmorItem armorItem)) return;

        EquipmentSlotType customSlot = mapVanillaArmorSlot(armorItem.getEquipmentSlot());
        if (customSlot == null) return;

        IPlayerLoadout loadout = player.getCapability(LoadoutCapability.PLAYER_LOADOUT).orElse(null);
        if (loadout == null) return;

        // Never allow vanilla auto-equip path for armor when custom inventory is active.
        if (!loadout.getEquipment(customSlot).isEmpty()) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        Optional<OpContext> ctxOpt = InventoryTransactionService.beginLoadoutOp(player, loadout.getLoadoutVersion());
        if (ctxOpt.isEmpty()) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        OpContext ctx = ctxOpt.get();
        boolean success = false;
        try {
            ItemStack equipped = inHand.copyWithCount(1);
            loadout.setEquipment(customSlot, equipped);
            inHand.shrink(1);
            player.setItemInHand(event.getHand(), inHand);

            if (customSlot.providesStorage()) {
                var displaced = loadout.recalculateStorageSlots(StorageProfileRegistry::lookup);
                if (!displaced.isEmpty()) {
                    OverflowService.applyOverflow(player, displaced, ctx.opId);
                }
            }

            success = true;
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        } finally {
            InventoryTransactionService.endLoadoutOp(ctx, success);
            if (success) {
                ArmorAttributeService.applyLoadoutArmor(player, loadout);
                LoadoutSyncScheduler.sendImmediately(player);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;

        IPlayerLoadout loadout = player.getCapability(LoadoutCapability.PLAYER_LOADOUT).orElse(null);
        if (loadout == null) return;

        for (ItemStack stack : extractAndClearDeathDrops(loadout)) {
            ItemEntity drop = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack);
            event.getDrops().add(drop);
        }
        ArmorAttributeService.clearLoadoutArmor(player);
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(DataDrivenContentLoader.reloadListener());
        LOGGER.info("[ServerEventHandler] registered data reload listener");
    }

    private static ItemStack moveToAllowedStorage(ServerPlayer player, ItemStack stack) {
        ItemStack rem = mergeIntoHotbar(player, stack);
        if (rem.isEmpty()) {
            return ItemStack.EMPTY;
        }

        LazyOptional<IPlayerLoadout> cap = player.getCapability(LoadoutCapability.PLAYER_LOADOUT);
        IPlayerLoadout loadout = cap.orElse(null);
        if (loadout != null) rem = mergeIntoDynamic(loadout, rem);
        return rem;
    }

    private static ItemStack mergeIntoHotbar(ServerPlayer player, ItemStack stack) {
        ItemStack rem = stack.copy();
        net.minecraft.world.entity.player.Inventory inv = player.getInventory();

        for (int i = 0; i <= 8 && !rem.isEmpty(); i++) {
            ItemStack slot = inv.getItem(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameTags(slot, rem)) {
                int move = Math.min(slot.getMaxStackSize() - slot.getCount(), rem.getCount());
                if (move > 0) {
                    slot.grow(move);
                    rem.shrink(move);
                }
            }
        }

        for (int i = 0; i <= 8 && !rem.isEmpty(); i++) {
            if (inv.getItem(i).isEmpty()) {
                int place = Math.min(rem.getMaxStackSize(), rem.getCount());
                ItemStack placed = rem.copy();
                placed.setCount(place);
                inv.setItem(i, placed);
                rem.shrink(place);
            }
        }
        return rem;
    }

    private static ItemStack mergeIntoDynamic(IPlayerLoadout loadout, ItemStack stack) {
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

    private static boolean hasCapacityForWholeStack(ServerPlayer player, ItemStack incoming) {
        int capacity = freeCapacityInHotbar(player, incoming);

        IPlayerLoadout loadout = player.getCapability(LoadoutCapability.PLAYER_LOADOUT).orElse(null);
        if (loadout != null) {
            capacity += freeCapacityInDynamic(loadout, incoming);
        }

        return capacity >= incoming.getCount();
    }

    private static int freeCapacityInHotbar(ServerPlayer player, ItemStack incoming) {
        int free = 0;
        net.minecraft.world.entity.player.Inventory inv = player.getInventory();

        for (int i = 0; i <= 8; i++) {
            ItemStack slot = inv.getItem(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameTags(slot, incoming)) {
                free += Math.max(0, slot.getMaxStackSize() - slot.getCount());
            }
        }

        int emptySlotCapacity = incoming.getMaxStackSize();
        for (int i = 0; i <= 8; i++) {
            if (inv.getItem(i).isEmpty()) {
                free += emptySlotCapacity;
            }
        }
        return free;
    }

    private static int freeCapacityInDynamic(IPlayerLoadout loadout, ItemStack incoming) {
        int free = 0;

        for (int i = 0; i < loadout.getDynamicSlotCount(); i++) {
            if (!loadout.isDynamicSlotActive(i)) continue;
            ItemStack slot = loadout.getDynamicSlot(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameTags(slot, incoming)) {
                free += Math.max(0, slot.getMaxStackSize() - slot.getCount());
            }
        }

        int emptySlotCapacity = incoming.getMaxStackSize();
        for (int i = 0; i < loadout.getDynamicSlotCount(); i++) {
            if (!loadout.isDynamicSlotActive(i)) continue;
            if (loadout.getDynamicSlot(i).isEmpty()) {
                free += emptySlotCapacity;
            }
        }
        return free;
    }

    private static java.util.List<ItemStack> extractAndClearDeathDrops(IPlayerLoadout loadout) {
        java.util.List<ItemStack> drops = new java.util.ArrayList<>();

        for (EquipmentSlotType slotType : EquipmentSlotType.values()) {
            ItemStack equipped = loadout.getEquipment(slotType);
            if (!equipped.isEmpty()) {
                drops.add(equipped.copy());
                loadout.setEquipment(slotType, ItemStack.EMPTY);
            }
        }

        for (int i = 0; i < loadout.getDynamicSlotCount(); i++) {
            if (!loadout.isDynamicSlotActive(i)) continue;
            ItemStack stack = loadout.getDynamicSlot(i);
            if (!stack.isEmpty()) {
                drops.add(stack.copy());
                loadout.setDynamicSlot(i, ItemStack.EMPTY);
            }
        }

        return drops;
    }

    private static EquipmentSlotType mapVanillaArmorSlot(EquipmentSlot vanillaSlot) {
        return switch (vanillaSlot) {
            case HEAD -> EquipmentSlotType.HEAD;
            case CHEST -> EquipmentSlotType.CHEST;
            case LEGS -> EquipmentSlotType.LEGS;
            case FEET -> EquipmentSlotType.FEET;
            default -> null;
        };
    }
}

