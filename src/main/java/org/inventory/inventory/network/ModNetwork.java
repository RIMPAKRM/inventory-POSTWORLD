package org.inventory.inventory.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.inventory.inventory.Inventory;

/**
 * Central Forge SimpleChannel registry for all mod network packets.
 *
 * Call {@link #registerPackets()} once during {@code FMLCommonSetupEvent}.
 *
 * Discriminators (must be stable across versions):
 *   0 – C2SClickCustomSlotPacket
 *   1 – C2SRequestCraftPacket
 *   2 – C2SOpenInventoryPacket
 *   3 – S2CLoadoutSyncPacket
 *   4 – S2CActionRejectedPacket
 */
public final class ModNetwork {

    private static final String PROTOCOL_VERSION = "3"; // bump when packet layout changes
    private static int nextId = 0;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(Inventory.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private ModNetwork() {}

    /** Register all packets. Must be called on both client and server during setup. */
    public static void registerPackets() {
        CHANNEL.registerMessage(nextId++,
                C2SClickCustomSlotPacket.class,
                C2SClickCustomSlotPacket::encode,
                C2SClickCustomSlotPacket::decode,
                C2SClickCustomSlotPacket::handle);

        CHANNEL.registerMessage(nextId++,
                C2SRequestCraftPacket.class,
                C2SRequestCraftPacket::encode,
                C2SRequestCraftPacket::decode,
                C2SRequestCraftPacket::handle);

        CHANNEL.registerMessage(nextId++,
                C2SOpenInventoryPacket.class,
                C2SOpenInventoryPacket::encode,
                C2SOpenInventoryPacket::decode,
                C2SOpenInventoryPacket::handle);

        CHANNEL.registerMessage(nextId++,
                S2CLoadoutSyncPacket.class,
                S2CLoadoutSyncPacket::encode,
                S2CLoadoutSyncPacket::decode,
                S2CLoadoutSyncPacket::handle);

        CHANNEL.registerMessage(nextId++,
                S2CActionRejectedPacket.class,
                S2CActionRejectedPacket::encode,
                S2CActionRejectedPacket::decode,
                S2CActionRejectedPacket::handle);
    }
}

