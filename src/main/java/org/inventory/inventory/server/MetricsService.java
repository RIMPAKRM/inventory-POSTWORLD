package org.inventory.inventory.server;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple in-process metrics counters for Phase B monitoring.
 *
 * All methods are thread-safe (AtomicLong).
 * Values reset on server restart. For persistent metrics, export to a log file
 * via {@link #logSummary()} or wire to a monitoring system in Phase D.
 */
public final class MetricsService {

    private static final Logger LOGGER = LogUtils.getLogger();

    // ---- Counters ----

    private static final AtomicLong overflows           = new AtomicLong();
    private static final AtomicLong rollbacks           = new AtomicLong();
    private static final AtomicLong dedupRejections     = new AtomicLong();
    private static final AtomicLong versionRejections   = new AtomicLong();
    private static final AtomicLong validationRejections= new AtomicLong();
    private static final AtomicLong syncsSent           = new AtomicLong();
    private static final AtomicLong craftRequests       = new AtomicLong();
    private static final AtomicLong craftSuccesses      = new AtomicLong();

    private MetricsService() {}

    // ---- Increment helpers (called from services / packet handlers) ----

    public static void incrementOverflows()            { overflows.incrementAndGet(); }
    public static void incrementRollbacks()            { rollbacks.incrementAndGet(); }
    public static void incrementDedupRejections()      { dedupRejections.incrementAndGet(); }
    public static void incrementVersionRejections()    { versionRejections.incrementAndGet(); }
    public static void incrementValidationRejections() { validationRejections.incrementAndGet(); }
    public static void incrementCraftRequests()        { craftRequests.incrementAndGet(); }
    public static void incrementCraftSuccesses()       { craftSuccesses.incrementAndGet(); }

    public static void recordSyncSent(UUID ignored)    { syncsSent.incrementAndGet(); }

    // ---- Snapshot reads ----

    public static long getOverflows()            { return overflows.get(); }
    public static long getRollbacks()            { return rollbacks.get(); }
    public static long getDedupRejections()      { return dedupRejections.get(); }
    public static long getVersionRejections()    { return versionRejections.get(); }
    public static long getValidationRejections() { return validationRejections.get(); }
    public static long getSyncsSent()            { return syncsSent.get(); }
    public static long getCraftRequests()        { return craftRequests.get(); }
    public static long getCraftSuccesses()       { return craftSuccesses.get(); }

    /** Log a one-line summary to the server log. */
    public static void logSummary() {
        LOGGER.info("[Metrics] overflow={} rollback={} dedup={} versionMismatch={} " +
                        "validationFail={} syncs={} craft(req/ok)={}/{}",
                overflows.get(), rollbacks.get(), dedupRejections.get(),
                versionRejections.get(), validationRejections.get(), syncsSent.get(),
                craftRequests.get(), craftSuccesses.get());
    }

    /** Reset all counters (for tests). */
    public static void resetAll() {
        overflows.set(0); rollbacks.set(0);
        dedupRejections.set(0); versionRejections.set(0); validationRejections.set(0);
        syncsSent.set(0); craftRequests.set(0); craftSuccesses.set(0);
    }
}

