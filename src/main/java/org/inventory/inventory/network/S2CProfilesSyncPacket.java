package org.inventory.inventory.network;

import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import org.inventory.inventory.domain.ProtectionProfile;
import org.inventory.inventory.domain.ProtectionProfileRegistry;
import org.inventory.inventory.domain.StorageProfile;
import org.inventory.inventory.domain.StorageProfileRegistry;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Supplier;

/**
 * S2C packet to sync protection and storage profiles from server to client.
 * Sent on player join to ensure client has all profile data for tooltip rendering.
 */
public class S2CProfilesSyncPacket {

    private static final Logger LOGGER = LogUtils.getLogger();

    private Map<ResourceLocation, List<ProtectionProfile>> protectionProfiles;
    private Map<ResourceLocation, StorageProfile> storageProfiles;

    public S2CProfilesSyncPacket() {
        this.protectionProfiles = new HashMap<>();
        this.storageProfiles = new HashMap<>();
    }

    public S2CProfilesSyncPacket(Map<ResourceLocation, List<ProtectionProfile>> protectionProfiles,
                                 Map<ResourceLocation, StorageProfile> storageProfiles) {
        this.protectionProfiles = protectionProfiles;
        this.storageProfiles = storageProfiles;
    }

    public void encode(FriendlyByteBuf buf) {
        // Encode protection profiles
        buf.writeInt(this.protectionProfiles.size());
        for (Map.Entry<ResourceLocation, List<ProtectionProfile>> entry : this.protectionProfiles.entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            buf.writeInt(entry.getValue().size());
            for (ProtectionProfile profile : entry.getValue()) {
                buf.writeUtf(profile.getId().toString());
                buf.writeDouble(profile.getArmorValue());
                buf.writeDouble(profile.getArmorToughness());
                buf.writeDouble(profile.getDurabilityModifier());
                buf.writeUtf(profile.getWeightClass());
                buf.writeInt(profile.getPriority());
            }
        }

        // Encode storage profiles
        buf.writeInt(this.storageProfiles.size());
        for (Map.Entry<ResourceLocation, StorageProfile> entry : this.storageProfiles.entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            buf.writeUtf(entry.getValue().getId().toString());
            buf.writeUtf(entry.getValue().getSlotType().toString());
            buf.writeInt(entry.getValue().getSlotCount());
        }
    }

    public static S2CProfilesSyncPacket decode(FriendlyByteBuf buf) {
        S2CProfilesSyncPacket packet = new S2CProfilesSyncPacket();

        // Decode protection profiles
        int protectionCount = buf.readInt();
        for (int i = 0; i < protectionCount; i++) {
            ResourceLocation itemId = buf.readResourceLocation();
            int profileCount = buf.readInt();
            List<ProtectionProfile> profiles = new ArrayList<>();
            for (int j = 0; j < profileCount; j++) {
                ResourceLocation profileId = ResourceLocation.parse(buf.readUtf());
                double armor = buf.readDouble();
                double toughness = buf.readDouble();
                double durability = buf.readDouble();
                String weightClass = buf.readUtf();
                int priority = buf.readInt();
                profiles.add(new ProtectionProfile(profileId, armor, toughness, durability, weightClass, List.of(), priority));
            }
            packet.protectionProfiles.put(itemId, profiles);
        }

        // Decode storage profiles
        int storageCount = buf.readInt();
        for (int i = 0; i < storageCount; i++) {
            ResourceLocation itemId = buf.readResourceLocation();
            ResourceLocation profileId = ResourceLocation.parse(buf.readUtf());
            String slotTypeStr = buf.readUtf();
            int slotCount = buf.readInt();
            org.inventory.inventory.domain.EquipmentSlotType slotType =
                    org.inventory.inventory.domain.EquipmentSlotType.valueOf(slotTypeStr);
            packet.storageProfiles.put(itemId, new StorageProfile(profileId, slotType, slotCount));
        }

        return packet;
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) {
                handleOnClient();
            }
        });
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleOnClient() {
        LOGGER.debug("[S2CProfilesSync] received {} protection profiles and {} storage profiles", 
                protectionProfiles.size(), storageProfiles.size());
        ProtectionProfileRegistry.replaceSnapshot(protectionProfiles);
        StorageProfileRegistry.replaceSnapshot(storageProfiles);
    }
}
