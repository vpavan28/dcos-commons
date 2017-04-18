package com.mesosphere.sdk.offer.taskdata;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.google.common.collect.ImmutableMap;

/**
 * Implements common logic for write access to a task's data. Any access to specific values is (only) provided by
 * implementing classes.
 */
public class TaskDataWriter {

    private final Map<String, String> map;

    /**
     * Creates a new instance which is initialized with no data.
     */
    protected TaskDataWriter() {
        this(new TreeMap<>());
    }

    /**
     * Creates a new instance which is initialized with the provided data.
     */
    protected TaskDataWriter(Map<String, String> map) {
        this.map = map;
    }

    /**
     * Returns the requested value, or an empty Optional if the value was not found.
     */
    protected Optional<String> getOptional(String key) {
        return Optional.ofNullable(map.get(key));
    }

    /**
     * Sets the provided value, overwriting any previous value.
     */
    protected void put(String key, String val) {
        map.put(key, val);
    }

    /**
     * Removes the provided value, or does nothing if it was already not present.
     */
    protected void remove(String key) {
        map.remove(key);
    }

    /**
     * Returns a read-only copy of all contained entries.
     */
    protected ImmutableMap<String, String> map() {
        return ImmutableMap.copyOf(map);
    }
}
