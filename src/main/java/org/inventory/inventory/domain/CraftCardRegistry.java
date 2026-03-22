package org.inventory.inventory.domain;

import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * In-memory registry for {@link CraftCard}s and {@link CraftCategory}s.
 *
 * Phase B: populated programmatically (e.g., in mod initialisation or tests).
 * Phase C: replaced by a data-driven JSON loader with soft-disable fallback.
 *
 * Thread safety: registrations must happen during mod setup (single-threaded).
 * After setup, all public methods are read-only and safe to call from any thread.
 */
public final class CraftCardRegistry {

    private static final Map<ResourceLocation, CraftCard>     CARDS      = new LinkedHashMap<>();
    private static final Map<ResourceLocation, CraftCategory> CATEGORIES = new LinkedHashMap<>();

    private CraftCardRegistry() {}

    // ---- Registration ----

    public static void register(CraftCard card) {
        if (CARDS.containsKey(card.getId())) {
            throw new IllegalStateException("Duplicate CraftCard id: " + card.getId());
        }
        CARDS.put(card.getId(), card);
    }

    public static void registerCategory(CraftCategory category) {
        if (CATEGORIES.containsKey(category.getId())) {
            throw new IllegalStateException("Duplicate CraftCategory id: " + category.getId());
        }
        CATEGORIES.put(category.getId(), category);
    }

    /**
     * Atomically replace all craft categories/cards with a new validated snapshot.
     *
     * Used by Phase C data reload to avoid partial updates while the server is live.
     */
    public static synchronized void replaceSnapshot(Collection<CraftCategory> categories,
                                                    Collection<CraftCard> cards) {
        CATEGORIES.clear();
        CARDS.clear();

        for (CraftCategory category : categories) {
            if (CATEGORIES.containsKey(category.getId())) {
                continue;
            }
            CATEGORIES.put(category.getId(), category);
        }

        for (CraftCard card : cards) {
            if (CARDS.containsKey(card.getId())) {
                continue;
            }
            CARDS.put(card.getId(), card);
        }
    }

    // ---- Lookups ----

    public static Optional<CraftCard> findCard(ResourceLocation id) {
        return Optional.ofNullable(CARDS.get(id));
    }

    public static Optional<CraftCategory> findCategory(ResourceLocation id) {
        return Optional.ofNullable(CATEGORIES.get(id));
    }

    /** All registered cards, in registration order. */
    public static Collection<CraftCard> allCards() {
        return Collections.unmodifiableCollection(CARDS.values());
    }

    /** All cards belonging to the given category, in registration order. */
    public static List<CraftCard> cardsInCategory(ResourceLocation categoryId) {
        List<CraftCard> result = new ArrayList<>();
        for (CraftCard card : CARDS.values()) {
            if (card.getCategoryId().equals(categoryId)) {
                result.add(card);
            }
        }
        return result;
    }

    /** All registered categories, sorted by {@link CraftCategory#getSortOrder()}. */
    public static List<CraftCategory> allCategoriesSorted() {
        return CATEGORIES.values().stream()
                .sorted(Comparator.comparingInt(CraftCategory::getSortOrder))
                .toList();
    }

    /** Clear all entries. For tests only. */
    public static synchronized void clearAll() {
        CARDS.clear();
        CATEGORIES.clear();
    }
}

