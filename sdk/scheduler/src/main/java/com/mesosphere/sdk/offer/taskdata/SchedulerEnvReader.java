package com.mesosphere.sdk.offer.taskdata;

import java.util.Map;
import java.util.Optional;
import org.apache.mesos.Protos.ExecutorInfo;
import org.apache.mesos.Protos.TaskInfo;

/**
 * Provides read access to task labels which are (only) read by the Scheduler.
 */
public class SchedulerEnvReader extends TaskDataReader {

    /**
     * @see TaskDataReader#TaskDataReader(String, String, java.util.Map)
     */
    public SchedulerEnvReader(TaskInfo taskInfo) {
        this(taskInfo.getName(), EnvUtils.toMap(taskInfo.getCommand().getEnvironment()));
    }

    /**
     * @see TaskDataReader#TaskDataReader(String, String, java.util.Map)
     */
    public SchedulerEnvReader(TaskInfo.Builder taskInfoBuilder) {
        this(taskInfoBuilder.getName(), EnvUtils.toMap(taskInfoBuilder.getCommand().getEnvironment()));
    }

    /**
     * @see TaskDataReader#TaskDataReader(String, String, java.util.Map)
     */
    public SchedulerEnvReader(ExecutorInfo executorInfo) {
        this(executorInfo.getName(), EnvUtils.toMap(executorInfo.getCommand().getEnvironment()));
    }

    /**
     * @see TaskDataReader#TaskDataReader(String, String, java.util.Map)
     */
    public SchedulerEnvReader(ExecutorInfo.Builder executorInfoBuilder) {
        this(executorInfoBuilder.getName(), EnvUtils.toMap(executorInfoBuilder.getCommand().getEnvironment()));
    }

    private SchedulerEnvReader(String name, Map<String, String> env) {
        super(name, "envvar", env);
    }

    /**
     * Returns the port value for the specified port settings, or an empty string if no matching entry was found.
     */
    public Optional<String> getPort(String portName, Optional<String> customEnvKey) {
        return getOptional(EnvUtils.getPortEnvironmentVariable(portName, customEnvKey));
    }
}
