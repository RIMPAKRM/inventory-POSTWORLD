package org.inventory.inventory.phasea;

import org.inventory.inventory.domain.EquipmentSlotType;
import org.inventory.inventory.capability.PlayerLoadout;
import org.inventory.inventory.server.CanonicalSlotMapping;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerLoadoutCoreTest {

    @Test
    void canonicalSlotMappingKeepsVanillaAndCustomRangesStable() {
        assertEquals(100, CanonicalSlotMapping.equipmentSlotToIndex(EquipmentSlotType.HEAD));
        assertEquals(104, CanonicalSlotMapping.equipmentSlotToIndex(EquipmentSlotType.BACKPACK));
        assertEquals(EquipmentSlotType.VEST, CanonicalSlotMapping.indexToEquipmentSlot(103));

        assertEquals(200, CanonicalSlotMapping.dynamicSlotToIndex(0));
        assertEquals(327, CanonicalSlotMapping.dynamicSlotToIndex(127));
        assertEquals(5, CanonicalSlotMapping.indexToDynamicSlot(205));

        assertTrue(CanonicalSlotMapping.isVanillaSlot(0));
        assertTrue(CanonicalSlotMapping.isVanillaSlot(40));
        assertFalse(CanonicalSlotMapping.isVanillaSlot(41));
        assertTrue(CanonicalSlotMapping.isEquipmentSlot(100));
        assertTrue(CanonicalSlotMapping.isDynamicSlot(200));
    }

    @Test
    void canonicalIndicesDoNotOverlapVanillaRange() {
        assertTrue(CanonicalSlotMapping.EQUIPMENT_SLOT_BASE > CanonicalSlotMapping.VANILLA_OFFHAND);
        assertTrue(CanonicalSlotMapping.DYNAMIC_SLOT_BASE > CanonicalSlotMapping.EQUIPMENT_SLOT_BASE + EquipmentSlotType.COUNT - 1);
        assertTrue(CanonicalSlotMapping.DYNAMIC_SLOT_MAX >= CanonicalSlotMapping.DYNAMIC_SLOT_BASE);
    }

    @Test
    void legsAreIndependentStorageProviderWithOwnDynamicRange() {
        assertTrue(EquipmentSlotType.LEGS.providesStorage());
        assertEquals(EquipmentSlotType.CHEST, PlayerLoadout.providerForDynamicIndex(0));
        assertEquals(EquipmentSlotType.VEST, PlayerLoadout.providerForDynamicIndex(PlayerLoadout.SLOTS_PER_PROVIDER));
        assertEquals(EquipmentSlotType.BACKPACK, PlayerLoadout.providerForDynamicIndex(PlayerLoadout.SLOTS_PER_PROVIDER * 2));
        assertEquals(EquipmentSlotType.LEGS, PlayerLoadout.providerForDynamicIndex(PlayerLoadout.SLOTS_PER_PROVIDER * 3));
    }
}







