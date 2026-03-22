package org.inventory.inventory.domain;

/**
 * Types of equipment slots available to the player.
 * CHEST, VEST, BACKPACK and LEGS are independent storage layers.
 * FACE and GLOVES do NOT provide storage capacity.
 */
public enum EquipmentSlotType {
    HEAD(false),
    FACE(false),
    CHEST(true),
    VEST(true),
    BACKPACK(true),
    GLOVES(false),
    LEGS(true),
    FEET(false);

    private final boolean providesStorage;

    EquipmentSlotType(boolean providesStorage) {
        this.providesStorage = providesStorage;
    }

    /** True if items equipped in this slot can grant dynamic storage slots. */
    public boolean providesStorage() {
        return providesStorage;
    }

    public static final int COUNT = values().length;
}

