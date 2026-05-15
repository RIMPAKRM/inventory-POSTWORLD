package org.inventory.inventory.client.screen;

/**
 * Pure layout helper for container browser screens.
 *
 * The screen itself stays thin; all geometry decisions live here so they can
 * be unit-tested without Minecraft client classes.
 */
public final class ContainerBrowserLayout {

    public static final int SLOT_SIZE = 18;
    public static final int SCREEN_WIDTH = 176;
    public static final int TITLE_TOP = 6;
    public static final int STORAGE_TOP = 26;
    public static final int STORAGE_GAP = 18;
    public static final int PLAYER_GRID_HEIGHT = 96;
    public static final int BOTTOM_PADDING = 8;
    public static final int PLAYER_INVENTORY_SLOTS = 36;

    private ContainerBrowserLayout() {}

    public static boolean isSupportedContainer(net.minecraft.world.inventory.AbstractContainerMenu menu) {
        return menu != null && menu.slots.size() > PLAYER_INVENTORY_SLOTS;
    }

    public static Layout create(int storageSlotCount) {
        int columns = chooseColumns(storageSlotCount);
        int rows = storageSlotCount <= 0 ? 0 : (storageSlotCount + columns - 1) / columns;
        int storageLeft = Math.max(8, (SCREEN_WIDTH - columns * SLOT_SIZE) / 2);
        int playerTop = STORAGE_TOP + rows * SLOT_SIZE + STORAGE_GAP;
        int screenHeight = playerTop + PLAYER_GRID_HEIGHT + BOTTOM_PADDING;
        return new Layout(SCREEN_WIDTH, screenHeight, columns, rows, storageLeft, STORAGE_TOP, playerTop);
    }

    public static int chooseColumns(int storageSlotCount) {
        if (storageSlotCount <= 0) {
            return 1;
        }
        if (storageSlotCount <= 9) {
            return storageSlotCount;
        }
        if (storageSlotCount <= 18) {
            return 6;
        }
        return 9;
    }

    public record Layout(
            int screenWidth,
            int screenHeight,
            int storageColumns,
            int storageRows,
            int storageLeft,
            int storageTop,
            int playerTop) {
    }
}