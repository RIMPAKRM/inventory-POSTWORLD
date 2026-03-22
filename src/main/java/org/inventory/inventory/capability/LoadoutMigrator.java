package org.inventory.inventory.capability;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import org.slf4j.Logger;

/**
 * Handles NBT schema migrations for PlayerLoadout.
 *
 * Every time the NBT schema changes, add a migrateStep(nbt, fromV, toV) branch.
 * The migrator is applied automatically during deserialization.
 */
public final class LoadoutMigrator {

    private static final Logger LOGGER = LogUtils.getLogger();

    private LoadoutMigrator() {}

    /**
     * Migrate nbt from fromVersion to toVersion, applying each step in order.
     * Returns the original tag unchanged if versions match.
     */
    public static CompoundTag migrate(CompoundTag nbt, int fromVersion, int toVersion) {
        if (fromVersion == toVersion) return nbt;

        CompoundTag result = nbt.copy();
        for (int v = fromVersion; v < toVersion; v++) {
            LOGGER.info("[LoadoutMigrator] PlayerLoadout schema v{} → v{}", v, v + 1);
            result = migrateStep(result, v, v + 1);
        }
        return result;
    }

    /** Applies a single version step. Add new cases as the schema evolves. */
    private static CompoundTag migrateStep(CompoundTag nbt, int from, int to) {
        CompoundTag result = nbt.copy();
        // v0 → v1: initial schema, no structural changes needed
        // Future migrations will be added here as additional if/else blocks.
        result.putInt("schemaVersion", to);
        return result;
    }
}

