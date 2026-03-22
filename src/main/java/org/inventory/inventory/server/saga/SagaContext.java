package org.inventory.inventory.server.saga;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Shared mutable state bag for a single Saga execution.
 *
 * Steps read and write well-known keys via {@link #get}/{@link #set}.
 * Compile-time safety is traded for flexibility; keys are documented
 * per-saga in the concrete Saga classes.
 */
public final class SagaContext {

    /** Unique operation ID — matches the outer OpContext.opId for tracing. */
    public final UUID opId;

    /** The player on whose behalf this saga is executing. */
    public final ServerPlayer player;

    /** Typed key→value bag for inter-step communication. */
    private final Map<String, Object> data = new HashMap<>();

    public SagaContext(UUID opId, ServerPlayer player) {
        this.opId   = opId;
        this.player = player;
    }

    /** Retrieve a value; returns {@code null} if not set. */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    /** Retrieve a value with a default fallback. */
    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(String key, T defaultValue) {
        T value = (T) data.get(key);
        return value != null ? value : defaultValue;
    }

    /** Store a value under a key. */
    public void set(String key, Object value) {
        data.put(key, value);
    }

    /** True if the key has been set. */
    public boolean has(String key) {
        return data.containsKey(key);
    }
}

