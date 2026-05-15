package org.inventory.inventory.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.inventory.inventory.domain.ProtectionProfileRegistry;
import org.inventory.inventory.domain.StorageProfileRegistry;

import java.util.List;
import java.util.Locale;

public class GearTooltipItem extends Item {

    public GearTooltipItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        int maxDamage = stack.getMaxDamage();
        if (maxDamage > 0) {
            int remaining = Math.max(0, maxDamage - stack.getDamageValue());
            tooltip.add(Component.translatable("tooltip.inventory.durability", remaining, maxDamage));
        }

        var protectionOpt = ProtectionProfileRegistry.resolve(stack);
        if (protectionOpt.isPresent()) {
            protectionOpt.ifPresent(profile -> tooltip.add(Component.translatable(
                    "tooltip.inventory.armor",
                    formatNumber(profile.getArmorValue())
            )));

            protectionOpt
                    .filter(profile -> profile.getArmorToughness() > 0)
                    .ifPresent(profile -> tooltip.add(Component.translatable(
                        "tooltip.inventory.toughness",
                        formatNumber(profile.getArmorToughness())
                    )));
        }

        StorageProfileRegistry.lookup(stack)
                .filter(profile -> profile.getSlotCount() > 0)
                .ifPresent(profile -> tooltip.add(Component.translatable(
                        "tooltip.inventory.slots",
                        profile.getSlotCount()
                )));

        super.appendHoverText(stack, level, tooltip, flag);
    }

    private static String formatNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 1.0E-6d) {
            return Integer.toString((int) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}