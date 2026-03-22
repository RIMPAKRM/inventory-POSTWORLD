package org.inventory.inventory.phasec;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.inventory.inventory.data.DataDrivenContentLoader;
import org.inventory.inventory.domain.CraftCardRegistry;
import org.inventory.inventory.domain.ProtectionProfileRegistry;
import org.inventory.inventory.domain.StorageProfileRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DataDrivenContentLoaderTest {

    @AfterEach
    void cleanup() {
        CraftCardRegistry.clearAll();
        StorageProfileRegistry.clear();
        ProtectionProfileRegistry.clear();
    }

    @Test
    void validCategoryLoadsAndBrokenCardIsSoftDisabled() {

        Map<ResourceLocation, JsonObject> categories = Map.of(
                ResourceLocation.parse("inventory:craft_categories/survival.json"),
                json("{\"displayName\":\"Survival\",\"sortOrder\":0}")
        );

        Map<ResourceLocation, JsonObject> cards = Map.of(
                ResourceLocation.parse("inventory:craft_cards/bad_missing_result.json"),
                json("{" +
                        "\"category\":\"inventory:survival\"," +
                        "\"ingredients\":[]" +
                        "}")
        );

        DataDrivenContentLoader.LoadSummary summary = DataDrivenContentLoader.reloadAllForTests(
                categories,
                cards,
                Map.of(),
                Map.of(),
                ignored -> null);

        assertEquals(1, summary.categoriesLoaded());
        assertEquals(0, summary.cardsLoaded(), "broken card must be soft-disabled");
        assertEquals(0, summary.storageProfilesLoaded());
        assertEquals(0, summary.protectionItemsLoaded());
        assertEquals(1, CraftCardRegistry.allCategoriesSorted().size());
        assertEquals(0, CraftCardRegistry.allCards().size());
    }

    private static JsonObject json(String raw) {
        return com.google.gson.JsonParser.parseString(raw).getAsJsonObject();
    }
}



