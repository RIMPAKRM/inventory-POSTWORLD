package org.inventory.inventory.phasec;

import net.minecraft.resources.ResourceLocation;
import org.inventory.inventory.domain.ProtectionProfile;
import org.inventory.inventory.domain.ProtectionProfileRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtectionProfileRegistryTest {

    @AfterEach
    void cleanup() {
        ProtectionProfileRegistry.clear();
    }

    @Test
    void higherPriorityProfileWinsAndArmorValuesDifferPerItem() {
        ResourceLocation itemA = ResourceLocation.parse("inventory:item_a");
        ResourceLocation itemB = ResourceLocation.parse("inventory:item_b");

        ProtectionProfile low = new ProtectionProfile(
                ResourceLocation.parse("inventory:low"), 1.0, 1.0, "light", List.of(), 5);
        ProtectionProfile high = new ProtectionProfile(
                ResourceLocation.parse("inventory:high"), 3.5, 1.2, "heavy", List.of(), 20);
        ProtectionProfile bOnly = new ProtectionProfile(
                ResourceLocation.parse("inventory:b_only"), 7.0, 0.8, "heavy", List.of(), 1);

        ProtectionProfileRegistry.replaceSnapshot(Map.of(
                itemA, List.of(low, high),
                itemB, List.of(bOnly)
        ));

        assertTrue(ProtectionProfileRegistry.resolve(itemA).isPresent());
        assertTrue(ProtectionProfileRegistry.resolve(itemB).isPresent());
        assertEquals(3.5, ProtectionProfileRegistry.resolve(itemA).orElseThrow().getArmorValue());
        assertEquals(7.0, ProtectionProfileRegistry.resolve(itemB).orElseThrow().getArmorValue());
    }
}

