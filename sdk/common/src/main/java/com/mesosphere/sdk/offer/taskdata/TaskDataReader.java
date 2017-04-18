package com.mesosphere.sdk.offer.taskdata;

import java.util.Map;
import java.util.Optional;

import com.mesosphere.sdk.offer.TaskException;

/**
 * Implements common logic for read access to task data. Any access to component-specific values is (only) provided by
 * implementing classes.
 */
public class TaskDataReader {

    private final String taskName;
    private final String keyType;
    private final Map<String, String> map;

    /**
     * Creates a new instance for the provided data.
     *
     * @param taskName the name of the task, only used in error messages if there are problems with the label content
     * @param keyType the type of data in the map (e.g. 'label' or 'envvar')
     * @param map the data to be read from
     */
    protected TaskDataReader(String taskName, String keyType, Map<String, String> map) {
        this.taskName = taskName;
        this.keyType = keyType;
        this.map = map;
    }

    /**
     * Returns the requested label value, or throws an exception if the value was not found.
     */
    protected String getOrThrow(String key) throws TaskException {
        String value = map.get(key);
        if (value == null) {
            throw new TaskException(String.format(
                    "Task %s is missing %s %s. Current %ss are: %s", taskName, keyType, key, keyType, map));
        }
        return value;
    }

    /**
     * Returns the requested label value, or an empty Optional if the value was not found.
     */
    protected Optional<String> getOptional(String key) {
        return Optional.ofNullable(map.get(key));
    }
}
