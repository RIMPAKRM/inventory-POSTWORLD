package org.inventory.inventory.client.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContainerBrowserLayoutTest {

    @Test
    void fiveSlotStorageUsesSingleRow() {
        ContainerBrowserLayout.Layout layout = ContainerBrowserLayout.create(5);

        assertEquals(5, layout.storageColumns());
        assertEquals(1, layout.storageRows());
        assertEquals(43, layout.storageLeft());
        assertEquals(166, layout.screenHeight());
    }

    @Test
    void fifteenSlotStorageUsesSixColumnsAndThreeRows() {
        ContainerBrowserLayout.Layout layout = ContainerBrowserLayout.create(15);

        assertEquals(6, layout.storageColumns());
        assertEquals(3, layout.storageRows());
        assertEquals(34, layout.storageLeft());
        assertEquals(202, layout.screenHeight());
    }

    @Test
    void thirtySlotStorageUsesNineColumnsAndFourRows() {
        ContainerBrowserLayout.Layout layout = ContainerBrowserLayout.create(30);

        assertEquals(9, layout.storageColumns());
        assertEquals(4, layout.storageRows());
        assertEquals(8, layout.storageLeft());
        assertEquals(220, layout.screenHeight());
    }
}