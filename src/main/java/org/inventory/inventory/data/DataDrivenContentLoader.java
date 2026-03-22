package org.inventory.inventory.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.inventory.inventory.domain.*;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.function.Function;

/**
 * Loads Phase C data-driven content with soft-disable fallback.
 */
public final class DataDrivenContentLoader {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    private static final String DIR_CRAFT_CATEGORIES = "craft_categories";
    private static final String DIR_CRAFT_CARDS = "craft_cards";
    private static final String DIR_STORAGE_PROFILES = "storage_profiles";
    private static final String DIR_PROTECTION_PROFILES = "protection_profiles";

    private DataDrivenContentLoader() {}

    public static SimplePreparableReloadListener<Void> reloadListener() {
        return new SimplePreparableReloadListener<>() {
            @Override
            protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                return null;
            }

            @Override
            protected void apply(Void unused, ResourceManager resourceManager, ProfilerFiller profiler) {
                reloadAll(resourceManager);
            }
        };
    }

    public static LoadSummary reloadAll(ResourceManager resourceManager) {
        return reloadAll(
                readJsonDirectory(resourceManager, DIR_CRAFT_CATEGORIES),
                readJsonDirectory(resourceManager, DIR_CRAFT_CARDS),
                readJsonDirectory(resourceManager, DIR_STORAGE_PROFILES),
                readJsonDirectory(resourceManager, DIR_PROTECTION_PROFILES),
                ForgeRegistries.ITEMS::getValue
        );
    }

    /** Test-only entrypoint that bypasses ResourceManager and injects custom item resolver. */
    public static LoadSummary reloadAllForTests(Map<ResourceLocation, JsonObject> categoryJson,
                                                 Map<ResourceLocation, JsonObject> cardJson,
                                                 Map<ResourceLocation, JsonObject> storageJson,
                                                 Map<ResourceLocation, JsonObject> protectionJson,
                                                 Function<ResourceLocation, Item> itemResolver) {
        return reloadAll(categoryJson, cardJson, storageJson, protectionJson, itemResolver);
    }

    static LoadSummary reloadAll(Map<ResourceLocation, JsonObject> categoryJson,
                                 Map<ResourceLocation, JsonObject> cardJson,
                                 Map<ResourceLocation, JsonObject> storageJson,
                                 Map<ResourceLocation, JsonObject> protectionJson,
                                 Function<ResourceLocation, Item> itemResolver) {
        int issues = 0;

        List<CraftCategory> categories = new ArrayList<>();
        Map<ResourceLocation, CraftCategory> categoryById = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonObject> e : categoryJson.entrySet()) {
            ResourceLocation id = toDataObjectId(e.getKey(), DIR_CRAFT_CATEGORIES);
            if (id == null) {
                issues += issue(ContentErrorCode.INVALID_ID, e.getKey(), "invalid category resource path");
                continue;
            }

            JsonObject json = e.getValue();
            if (!json.has("displayName")) {
                issues += issue(ContentErrorCode.MISSING_FIELD, e.getKey(), "displayName");
                continue;
            }

            String displayName = GsonHelper.getAsString(json, "displayName", "").trim();
            if (displayName.isEmpty()) {
                issues += issue(ContentErrorCode.INVALID_VALUE, e.getKey(), "displayName is blank");
                continue;
            }

            int sortOrder = GsonHelper.getAsInt(json, "sortOrder", 0);
            CraftCategory category = new CraftCategory(id, displayName, sortOrder);
            if (categoryById.putIfAbsent(id, category) != null) {
                issues += issue(ContentErrorCode.DUPLICATE_ID, e.getKey(), "category id " + id);
                continue;
            }
            categories.add(category);
        }

        List<CraftCard> cards = new ArrayList<>();
        Set<ResourceLocation> cardIds = new HashSet<>();
        for (Map.Entry<ResourceLocation, JsonObject> e : cardJson.entrySet()) {
            ResourceLocation id = toDataObjectId(e.getKey(), DIR_CRAFT_CARDS);
            if (id == null) {
                issues += issue(ContentErrorCode.INVALID_ID, e.getKey(), "invalid craft card resource path");
                continue;
            }
            if (!cardIds.add(id)) {
                issues += issue(ContentErrorCode.DUPLICATE_ID, e.getKey(), "craft card id " + id);
                continue;
            }

            JsonObject json = e.getValue();
            ResourceLocation categoryId = parseIdField(json, "category");
            if (categoryId == null) {
                issues += issue(ContentErrorCode.MISSING_FIELD, e.getKey(), "category");
                continue;
            }
            if (!categoryById.containsKey(categoryId)) {
                issues += issue(ContentErrorCode.UNKNOWN_CATEGORY, e.getKey(), categoryId.toString());
                continue;
            }

            List<CraftIngredient> ingredients = parseIngredients(json, e.getKey(), itemResolver);
            if (ingredients == null || ingredients.isEmpty()) {
                continue;
            }

            ItemStack result = parseResult(json, e.getKey(), itemResolver);
            if (result == null || result.isEmpty()) {
                continue;
            }

            cards.add(new CraftCard(id, categoryId, ingredients, result));
        }

        Map<ResourceLocation, StorageProfile> storageByItem = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonObject> e : storageJson.entrySet()) {
            ResourceLocation profileId = toDataObjectId(e.getKey(), DIR_STORAGE_PROFILES);
            if (profileId == null) {
                issues += issue(ContentErrorCode.INVALID_ID, e.getKey(), "invalid storage profile resource path");
                continue;
            }

            JsonObject json = e.getValue();
            ResourceLocation itemId = parseIdField(json, "item");
            if (itemId == null) {
                issues += issue(ContentErrorCode.MISSING_FIELD, e.getKey(), "item");
                continue;
            }
            if (itemResolver.apply(itemId) == null) {
                issues += issue(ContentErrorCode.UNKNOWN_ITEM, e.getKey(), itemId.toString());
                continue;
            }

            String slotTypeRaw = GsonHelper.getAsString(json, "slotType", "").trim();
            EquipmentSlotType slotType;
            try {
                slotType = EquipmentSlotType.valueOf(slotTypeRaw.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                issues += issue(ContentErrorCode.UNKNOWN_SLOT_TYPE, e.getKey(), slotTypeRaw);
                continue;
            }

            int slotCount = GsonHelper.getAsInt(json, "slotCount", -1);
            if (slotCount < 0) {
                issues += issue(ContentErrorCode.INVALID_VALUE, e.getKey(), "slotCount must be >= 0");
                continue;
            }

            if (storageByItem.putIfAbsent(itemId, new StorageProfile(profileId, slotType, slotCount)) != null) {
                issues += issue(ContentErrorCode.DUPLICATE_ID, e.getKey(), "storage profile item " + itemId);
            }
        }

        Map<ResourceLocation, List<ProtectionProfile>> protectionByItem = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonObject> e : protectionJson.entrySet()) {
            ResourceLocation profileId = toDataObjectId(e.getKey(), DIR_PROTECTION_PROFILES);
            if (profileId == null) {
                issues += issue(ContentErrorCode.INVALID_ID, e.getKey(), "invalid protection profile resource path");
                continue;
            }

            JsonObject json = e.getValue();
            List<ResourceLocation> items = parseItemTargets(json, e.getKey());
            if (items == null || items.isEmpty()) {
                continue;
            }

            double armorValue = GsonHelper.getAsDouble(json, "armorValue", -1);
            if (armorValue < 0d) {
                issues += issue(ContentErrorCode.INVALID_VALUE, e.getKey(), "armorValue must be >= 0");
                continue;
            }

            double durabilityModifier = GsonHelper.getAsDouble(json, "durabilityModifier", 1d);
            if (durabilityModifier <= 0d) {
                issues += issue(ContentErrorCode.INVALID_VALUE, e.getKey(), "durabilityModifier must be > 0");
                continue;
            }

            String weightClass = GsonHelper.getAsString(json, "weightClass", "medium");
            int priority = GsonHelper.getAsInt(json, "priority", 0);
            List<ResourceLocation> tags = parseResourceLocationArray(json, "tags", e.getKey());
            if (tags == null) {
                continue;
            }

            ProtectionProfile profile = new ProtectionProfile(
                    profileId, armorValue, durabilityModifier, weightClass, tags, priority);

            for (ResourceLocation itemId : items) {
                if (itemResolver.apply(itemId) == null) {
                    issues += issue(ContentErrorCode.UNKNOWN_ITEM, e.getKey(), itemId.toString());
                    continue;
                }
                protectionByItem.computeIfAbsent(itemId, k -> new ArrayList<>()).add(profile);
            }
        }

        CraftCardRegistry.replaceSnapshot(categories, cards);
        StorageProfileRegistry.replaceSnapshot(storageByItem);
        ProtectionProfileRegistry.replaceSnapshot(protectionByItem);

        LoadSummary summary = new LoadSummary(categories.size(), cards.size(), storageByItem.size(), protectionByItem.size(), issues);
        LOGGER.info("[DataLoader] loaded categories={} cards={} storageProfiles={} protectionItems={} issues={}",
                summary.categoriesLoaded(), summary.cardsLoaded(), summary.storageProfilesLoaded(),
                summary.protectionItemsLoaded(), summary.issues());
        return summary;
    }

    private static List<ResourceLocation> parseItemTargets(JsonObject json, ResourceLocation fileKey) {
        List<ResourceLocation> items = parseResourceLocationArray(json, "items", fileKey);
        if (items == null) {
            return null;
        }
        if (items.isEmpty()) {
            issue(ContentErrorCode.EMPTY_ARRAY, fileKey, "items");
            return null;
        }
        return items;
    }

    private static List<CraftIngredient> parseIngredients(JsonObject json,
                                                           ResourceLocation fileKey,
                                                           Function<ResourceLocation, Item> itemResolver) {
        if (!json.has("ingredients") || !json.get("ingredients").isJsonArray()) {
            issue(ContentErrorCode.MISSING_FIELD, fileKey, "ingredients[]");
            return null;
        }

        JsonArray arr = json.getAsJsonArray("ingredients");
        if (arr.isEmpty()) {
            issue(ContentErrorCode.EMPTY_ARRAY, fileKey, "ingredients");
            return null;
        }

        List<CraftIngredient> result = new ArrayList<>();
        for (JsonElement element : arr) {
            if (!element.isJsonObject()) {
                issue(ContentErrorCode.INVALID_VALUE, fileKey, "ingredient entry must be object");
                return null;
            }
            JsonObject ingredientJson = element.getAsJsonObject();
            ResourceLocation itemId = parseIdField(ingredientJson, "item");
            if (itemId == null) {
                issue(ContentErrorCode.MISSING_FIELD, fileKey, "ingredient.item");
                return null;
            }
            Item item = itemResolver.apply(itemId);
            if (item == null) {
                issue(ContentErrorCode.UNKNOWN_ITEM, fileKey, itemId.toString());
                return null;
            }
            int count = GsonHelper.getAsInt(ingredientJson, "count", 0);
            if (count <= 0) {
                issue(ContentErrorCode.INVALID_VALUE, fileKey, "ingredient.count must be > 0");
                return null;
            }
            result.add(new CraftIngredient(item, count));
        }
        return result;
    }

    private static ItemStack parseResult(JsonObject json,
                                         ResourceLocation fileKey,
                                         Function<ResourceLocation, Item> itemResolver) {
        if (!json.has("result") || !json.get("result").isJsonObject()) {
            issue(ContentErrorCode.MISSING_FIELD, fileKey, "result");
            return null;
        }

        JsonObject result = json.getAsJsonObject("result");
        ResourceLocation itemId = parseIdField(result, "item");
        if (itemId == null) {
            issue(ContentErrorCode.MISSING_FIELD, fileKey, "result.item");
            return null;
        }

        Item item = itemResolver.apply(itemId);
        if (item == null) {
            issue(ContentErrorCode.UNKNOWN_ITEM, fileKey, itemId.toString());
            return null;
        }

        int count = GsonHelper.getAsInt(result, "count", 0);
        if (count <= 0) {
            issue(ContentErrorCode.INVALID_VALUE, fileKey, "result.count must be > 0");
            return null;
        }

        return new ItemStack(item, count);
    }

    private static List<ResourceLocation> parseResourceLocationArray(JsonObject json,
                                                                     String field,
                                                                     ResourceLocation fileKey) {
        if (!json.has(field)) {
            return List.of();
        }
        if (!json.get(field).isJsonArray()) {
            issue(ContentErrorCode.INVALID_VALUE, fileKey, field + " must be array");
            return null;
        }

        JsonArray arr = json.getAsJsonArray(field);
        List<ResourceLocation> values = new ArrayList<>();
        for (JsonElement e : arr) {
            if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) {
                issue(ContentErrorCode.INVALID_VALUE, fileKey, field + " entry must be string");
                return null;
            }
            ResourceLocation id = ResourceLocation.tryParse(e.getAsString());
            if (id == null) {
                issue(ContentErrorCode.INVALID_ID, fileKey, field + " entry " + e.getAsString());
                return null;
            }
            values.add(id);
        }
        return values;
    }

    private static ResourceLocation parseIdField(JsonObject json, String field) {
        if (!json.has(field)) return null;
        String raw = GsonHelper.getAsString(json, field, "");
        return ResourceLocation.tryParse(raw);
    }

    private static Map<ResourceLocation, JsonObject> readJsonDirectory(ResourceManager resourceManager, String directory) {
        Map<ResourceLocation, JsonObject> out = new LinkedHashMap<>();
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(directory, id -> id.getPath().endsWith(".json"));
        for (Map.Entry<ResourceLocation, Resource> e : resources.entrySet()) {
            try (Reader reader = e.getValue().openAsReader()) {
                JsonObject json = GsonHelper.fromJson(GSON, reader, JsonObject.class);
                if (json == null) {
                    issue(ContentErrorCode.INVALID_JSON, e.getKey(), "empty json");
                    continue;
                }
                out.put(e.getKey(), json);
            } catch (IOException | JsonParseException ex) {
                issue(ContentErrorCode.INVALID_JSON, e.getKey(), ex.getMessage());
            }
        }
        return out;
    }

    private static ResourceLocation toDataObjectId(ResourceLocation filePathId, String folder) {
        String prefix = folder + "/";
        String path = filePathId.getPath();
        if (!path.startsWith(prefix) || !path.endsWith(".json")) {
            return null;
        }
        String idPath = path.substring(prefix.length(), path.length() - ".json".length());
        if (idPath.isBlank()) {
            return null;
        }
        return ResourceLocation.fromNamespaceAndPath(filePathId.getNamespace(), idPath);
    }

    private static int issue(ContentErrorCode code, ResourceLocation file, String details) {
        LOGGER.warn("[DataLoader:{}] file={} details={}", code, file, details);
        return 1;
    }

    public record LoadSummary(int categoriesLoaded,
                              int cardsLoaded,
                              int storageProfilesLoaded,
                              int protectionItemsLoaded,
                              int issues) {
    }
}


