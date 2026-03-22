package org.inventory.inventory.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Client-side cache of the latest server-authoritative loadoutVersion.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientLoadoutState {

    private static long latestServerVersion = 0L;

    private ClientLoadoutState() {}

    public static void onServerSync(long serverVersion) {
        if (serverVersion > latestServerVersion) {
            latestServerVersion = serverVersion;
        }
    }

    public static long currentVersion() {
        return latestServerVersion;
    }

    public static long versionForRequest(long capabilityVersionFallback) {
        return Math.max(latestServerVersion, capabilityVersionFallback);
    }

    public static void reset() {
        latestServerVersion = 0L;
    }
}

