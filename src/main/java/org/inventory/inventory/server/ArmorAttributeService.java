package org.inventory.inventory.server;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.inventory.inventory.capability.IPlayerLoadout;
import org.inventory.inventory.domain.EquipmentSlotType;
import org.inventory.inventory.domain.ProtectionProfileRegistry;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * Applies custom armor attribute modifiers to the player based on
 * items equipped in custom loadout slots.
 *
 * Each custom modifier is identified by a fixed UUID so it can be removed
 * before recalculation. Vanilla armor items use their built-in armorValue
 * when not registered in ProtectionProfileRegistry.
 *
 * Call {@link #applyLoadoutArmor(ServerPlayer, IPlayerLoadout)} after any
 * equipment change (equip, unequip, death, login).
 */
public final class ArmorAttributeService {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Fixed UUID for the custom ARMOR modifier from loadout slots. */
    private static final UUID LOADOUT_ARMOR_UUID =
            UUID.fromString("7b3e1d9a-4f6c-4b2a-8e0d-3c5a7f8b9e1c");

    /** Fixed UUID for the custom ARMOR_TOUGHNESS modifier from loadout slots. */
    private static final UUID LOADOUT_TOUGHNESS_UUID =
            UUID.fromString("2a4f8c1e-7d3b-4e5a-9c6f-1b8d2e4a7c0f");

    private static final String MODIFIER_NAME = "inventory:loadout_armor";

    private ArmorAttributeService() {}

    /**
     * Recalculates and re-applies armor and toughness attribute modifiers
     * from custom loadout equipment. Safe to call on every equipment change.
     */
    public static void applyLoadoutArmor(ServerPlayer player, IPlayerLoadout loadout) {
        double totalArmor = 0;
        double totalToughness = 0;

        for (EquipmentSlotType slotType : EquipmentSlotType.values()) {
            ItemStack equipped = loadout.getEquipment(slotType);
            if (equipped.isEmpty()) continue;

            // 1. Check data-driven ProtectionProfile first
            var profileOpt = ProtectionProfileRegistry.resolve(equipped);
            if (profileOpt.isPresent()) {
                totalArmor += profileOpt.get().getArmorValue();
                LOGGER.debug("[ArmorService] {} +{} armor from ProtectionProfile slot={}",
                        equipped.getItem().getDescriptionId(), profileOpt.get().getArmorValue(), slotType);
                continue;
            }

            // 2. Fallback: read vanilla ArmorItem defense values
            if (equipped.getItem() instanceof ArmorItem armorItem) {
                totalArmor    += armorItem.getDefense();
                totalToughness += armorItem.getToughness();
                LOGGER.debug("[ArmorService] {} +{} armor (vanilla fallback) slot={}",
                        equipped.getItem().getDescriptionId(), armorItem.getDefense(), slotType);
            }
        }

        applyModifier(player, Attributes.ARMOR,          LOADOUT_ARMOR_UUID,    totalArmor);
        applyModifier(player, Attributes.ARMOR_TOUGHNESS, LOADOUT_TOUGHNESS_UUID, totalToughness);

        LOGGER.debug("[ArmorService] applied armor={} toughness={} player={}",
                totalArmor, totalToughness, player.getName().getString());
    }

    /** Remove all loadout armor modifiers (called on death / reset). */
    public static void clearLoadoutArmor(ServerPlayer player) {
        removeModifier(player, Attributes.ARMOR,          LOADOUT_ARMOR_UUID);
        removeModifier(player, Attributes.ARMOR_TOUGHNESS, LOADOUT_TOUGHNESS_UUID);
    }

    // ---- Internal helpers ----

    private static void applyModifier(ServerPlayer player,
                                      net.minecraft.world.entity.ai.attributes.Attribute attribute,
                                      UUID uuid, double value) {
        var instance = player.getAttribute(attribute);
        if (instance == null) return;

        instance.removeModifier(uuid);

        if (value > 0) {
            instance.addTransientModifier(new AttributeModifier(
                    uuid, MODIFIER_NAME, value, AttributeModifier.Operation.ADDITION));
        }
    }

    private static void removeModifier(ServerPlayer player,
                                        net.minecraft.world.entity.ai.attributes.Attribute attribute,
                                        UUID uuid) {
        var instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(uuid);
        }
    }
}

