package org.inventory.inventory.client.screen;

/**
 * Layout helper for browser-style storage screens.
 *
 * Storage is rendered vertically, while the player's loadout and hotbar stay
 * in the familiar positions on the left and bottom.
 */
public final class StorageBrowserLayout {

    public static final int SLOT_SIZE = 18;
    public static final int LOADOUT_LEFT_X = 8;
    public static final int LOADOUT_TOP_Y = 26;
    public static final int STORAGE_TOP_Y = 27;
    public static final int STORAGE_LEFT_X = 210;
    public static final int HOTBAR_LEFT_X = 8;
    public static final int HOTBAR_MIN_Y = 202;
    public static final int PANEL_MARGIN = 8;
    public static final int PLAYER_INVENTORY_WIDTH = 162;
    public static final int LOADOUT_WIDTH = 180;
    public static final int STANDARD_STORAGE_ROWS = 9;
    public static final int WIDE_STORAGE_COLUMNS = 6;

    private StorageBrowserLayout() {}

    public static Layout create(int storageSlotCount) {
        int columns = chooseStorageColumns(storageSlotCount);
        int rows = storageSlotCount <= 0 ? 0 : (storageSlotCount + columns - 1) / columns;
        int storageWidth = columns * SLOT_SIZE;
        int storageRight = STORAGE_LEFT_X + storageWidth;
        int hotbarY = Math.max(HOTBAR_MIN_Y, STORAGE_TOP_Y + rows * SLOT_SIZE + 22);
        int screenHeight = hotbarY + 26;
        int screenWidth = Math.max(LOADOUT_LEFT_X + LOADOUT_WIDTH + 16, storageRight + PANEL_MARGIN);
        return new Layout(screenWidth, screenHeight, columns, rows, STORAGE_LEFT_X, STORAGE_TOP_Y, hotbarY);
    }

    public static int chooseStorageColumns(int storageSlotCount) {
        if (storageSlotCount <= 0) {
            return 1;
        }
        if (storageSlotCount <= STANDARD_STORAGE_ROWS * 3) {
            return Math.max(1, (storageSlotCount + STANDARD_STORAGE_ROWS - 1) / STANDARD_STORAGE_ROWS);
        }
        return WIDE_STORAGE_COLUMNS;
    }

    public record Layout(
            int screenWidth,
            int screenHeight,
            int storageColumns,
            int storageRows,
            int storageLeft,
            int storageTop,
            int hotbarY) {
    }
}