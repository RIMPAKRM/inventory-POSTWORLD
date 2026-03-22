package org.inventory.inventory.server;

import org.inventory.inventory.domain.EquipmentSlotType;

/**
 * Fixed canonical slot index ranges.
 *
 * Rules:
 *  - Vanilla ranges (0–40) are NEVER shifted or reused by this mod.
 *  - Equipment slots occupy a fixed range starting at EQUIPMENT_SLOT_BASE.
 *  - Dynamic storage slots occupy a separate range starting at DYNAMIC_SLOT_BASE.
 *  - Adding new ranges must not overlap existing ones and requires a capability migration.
 */
public final class CanonicalSlotMapping {

    private CanonicalSlotMapping() {}

    // ---- Vanilla player inventory (mirrors net.minecraft.world.entity.player.Inventory) ----
    public static final int VANILLA_HOTBAR_FIRST  = 0;
    public static final int VANILLA_HOTBAR_LAST   = 8;
    public static final int VANILLA_MAIN_FIRST    = 9;
    public static final int VANILLA_MAIN_LAST     = 35;
    public static final int VANILLA_ARMOR_FIRST   = 36;
    public static final int VANILLA_ARMOR_LAST    = 39;
    public static final int VANILLA_OFFHAND       = 40;

    // ---- Custom equipment slots (one per EquipmentSlotType ordinal) ----
    public static final int EQUIPMENT_SLOT_BASE   = 100;
    // HEAD=100, FACE=101, CHEST=102, VEST=103, BACKPACK=104, GLOVES=105, LEGS=106, FEET=107

    // ---- Dynamic storage slots (up to PlayerLoadout.MAX_DYNAMIC_SLOTS = 128) ----
    public static final int DYNAMIC_SLOT_BASE     = 200;
    public static final int DYNAMIC_SLOT_MAX      = DYNAMIC_SLOT_BASE + 127; // 327

    // ---- Conversion helpers ----

    public static int equipmentSlotToIndex(EquipmentSlotType type) {
        return EQUIPMENT_SLOT_BASE + type.ordinal();
    }

    /** Returns null if index is not in the equipment slot range. */
    public static EquipmentSlotType indexToEquipmentSlot(int index) {
        int ordinal = index - EQUIPMENT_SLOT_BASE;
        EquipmentSlotType[] values = EquipmentSlotType.values();
        if (ordinal < 0 || ordinal >= values.length) return null;
        return values[ordinal];
    }

    public static int dynamicSlotToIndex(int localIndex) {
        return DYNAMIC_SLOT_BASE + localIndex;
    }

    public static int indexToDynamicSlot(int index) {
        return index - DYNAMIC_SLOT_BASE;
    }

    // ---- Range checks ----

    public static boolean isVanillaSlot(int index) {
        return index >= VANILLA_HOTBAR_FIRST && index <= VANILLA_OFFHAND;
    }

    public static boolean isEquipmentSlot(int index) {
        return index >= EQUIPMENT_SLOT_BASE
                && index < EQUIPMENT_SLOT_BASE + EquipmentSlotType.COUNT;
    }

    public static boolean isDynamicSlot(int index) {
        return index >= DYNAMIC_SLOT_BASE && index <= DYNAMIC_SLOT_MAX;
    }
}

