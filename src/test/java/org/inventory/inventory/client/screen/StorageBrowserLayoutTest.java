package org.inventory.inventory.client.screen;

import org.junit.jupiter.api.Test;
import org.inventory.inventory.domain.EquipmentSlotType;
import org.inventory.inventory.menu.StorageBrowserMenu;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StorageBrowserLayoutTest {

    @Test
    void fiveSlotStorageStaysInSingleColumn() {
        StorageBrowserLayout.Layout layout = StorageBrowserLayout.create(5);

        assertEquals(1, layout.storageColumns());
        assertEquals(5, layout.storageRows());
        assertEquals(210, layout.storageLeft());
        assertEquals(228, layout.screenHeight());
    }

    @Test
    void fifteenSlotStorageUsesTwoVerticalColumns() {
        StorageBrowserLayout.Layout layout = StorageBrowserLayout.create(15);

        assertEquals(2, layout.storageColumns());
        assertEquals(8, layout.storageRows());
        assertEquals(210, layout.storageLeft());
        assertEquals(228, layout.screenHeight());
    }

    @Test
    void thirtySlotStorageUsesWideLayout() {
        StorageBrowserLayout.Layout layout = StorageBrowserLayout.create(30);

        assertEquals(6, layout.storageColumns());
        assertEquals(5, layout.storageRows());
        assertEquals(210, layout.storageLeft());
        assertEquals(326, layout.screenWidth());
        assertEquals(228, layout.screenHeight());
    }

    @Test
    void fiftyFourSlotStorageUsesSixByNineLayout() {
        StorageBrowserLayout.Layout layout = StorageBrowserLayout.create(54);

        assertEquals(6, layout.storageColumns());
        assertEquals(9, layout.storageRows());
        assertEquals(210, layout.storageLeft());
        assertEquals(326, layout.screenWidth());
        assertEquals(237, layout.screenHeight());
    }

    @Test
    void browserLowerRowIsRaisedCloserToChestRow() {
        assertEquals(62, StorageBrowserMenu.getDynamicProviderY(EquipmentSlotType.VEST));
        assertEquals(62, StorageBrowserMenu.getDynamicProviderY(EquipmentSlotType.LEGS));
    }
}