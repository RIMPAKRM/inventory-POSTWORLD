package org.inventory.inventory.event;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

/**
 * Reduces natural health regeneration for players by a configurable factor.
 */
public final class HealthRegenHandler {

    // Multiply heal amount by this factor (0.3 = three times slower)
    private static final float SCALE = 0.3f;
    private static final boolean HAS_ROUGH_TWEAKS =
            ModList.get().isLoaded("roughtweaks") || ModList.get().isLoaded("rough_tweaks");

    @SubscribeEvent
    public void onLivingHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return; // run only on server

        // Do not touch healing that originates from RoughTweaks internals.
        if (HAS_ROUGH_TWEAKS && isRoughTweaksHealing()) return;

        // Skip potion-based Regeneration (keep its intended strength)
        if (player.hasEffect(MobEffects.REGENERATION)) return;

        // Scale down the heal amount
        float amt = event.getAmount() * SCALE;
        event.setAmount(amt);
    }

    private static boolean isRoughTweaksHealing() {
        return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(stream -> stream.anyMatch(frame -> {
                    String className = frame.getClassName().toLowerCase();
                    return className.contains("roughtweaks") || className.contains("rough_tweaks");
                }));
    }
}
