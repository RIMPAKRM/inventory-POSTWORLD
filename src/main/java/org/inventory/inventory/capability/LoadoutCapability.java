package org.inventory.inventory.capability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.inventory.inventory.Inventory;
import org.inventory.inventory.network.RequestDedup;
import org.inventory.inventory.server.LoadoutSyncScheduler;
import org.inventory.inventory.server.OverflowService;
import org.inventory.inventory.server.ArmorAttributeService;
import org.inventory.inventory.server.PlayerLockService;

/**
 * Registers and manages the PLAYER_LOADOUT capability.
 *
 * MOD bus: onRegisterCapabilities — called from Inventory constructor.
 * FORGE bus (via @Mod.EventBusSubscriber): entity attach, clone, login, logout.
 */
@Mod.EventBusSubscriber(modid = Inventory.MODID)
public final class LoadoutCapability {

    /** The capability token. Populated by Forge during RegisterCapabilitiesEvent. */
    public static final Capability<IPlayerLoadout> PLAYER_LOADOUT =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static final ResourceLocation LOADOUT_KEY =
            ResourceLocation.fromNamespaceAndPath(Inventory.MODID, "player_loadout");

    private LoadoutCapability() {}

    // ---- MOD bus — called manually from Inventory constructor ----

    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IPlayerLoadout.class);
    }

    // ---- FORGE bus ----

    /** Attach the capability to every Player entity. */
    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (!(event.getObject() instanceof Player)) return;
        if (event.getObject().getCapability(PLAYER_LOADOUT).isPresent()) return;

        PlayerLoadoutProvider provider = new PlayerLoadoutProvider();
        event.addCapability(LOADOUT_KEY, provider);
        event.addListener(provider::invalidate);
    }

    /**
     * Copy loadout on player respawn or dimension change.
     * Uses isDeath flag: if true (death), we transfer loadout; if false (dimension), always copy.
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player newPlayer = event.getEntity();

        boolean keepInventory = newPlayer.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
        boolean shouldCopy = !event.isWasDeath() || keepInventory;
        if (!shouldCopy) {
            return;
        }

        original.reviveCaps();
        original.getCapability(PLAYER_LOADOUT).ifPresent(src ->
                newPlayer.getCapability(PLAYER_LOADOUT).ifPresent(dst ->
                        dst.deserializeNBT(src.serializeNBT())
                )
        );
        original.invalidateCaps();
    }

    /**
     * On login: recover any in-progress overflow transaction left from a disconnect.
     * Phase B: also send immediate S2C_LoadoutSync so the client has the current version.
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            OverflowService.recoverPendingOverflow(serverPlayer);
            LoadoutSyncScheduler.sendImmediately(serverPlayer);
            serverPlayer.getCapability(PLAYER_LOADOUT).ifPresent(loadout ->
                    ArmorAttributeService.applyLoadoutArmor(serverPlayer, loadout));
        }
    }

    /** On logout: release per-player lock, cancel pending sync, and clean up dedup cache. */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PlayerLockService.removeLock(event.getEntity().getUUID());
        LoadoutSyncScheduler.cancelSync(event.getEntity().getUUID());
        RequestDedup.clearPlayer(event.getEntity().getUUID());
    }
}




