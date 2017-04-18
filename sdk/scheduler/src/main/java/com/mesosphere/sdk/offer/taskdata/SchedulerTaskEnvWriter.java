package com.mesosphere.sdk.offer.taskdata;

import java.util.Optional;

import org.apache.mesos.Protos.CommandInfo;
import org.apache.mesos.Protos.HealthCheck;
import org.apache.mesos.Protos.TaskInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mesosphere.sdk.offer.TaskException;

/**
 * Provides write access to task environment variables which are (only) written by the Scheduler.
 */
public class SchedulerTaskEnvWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerTaskEnvWriter.class);

    private final TaskDataWriter taskEnv;
    private final Optional<TaskDataWriter> healthCheckEnv;

    /**
     * @see TaskDataWriter#TaskDataWriter(java.util.Map)
     */
    public SchedulerTaskEnvWriter(TaskInfo taskInfo) {
        this(
                taskInfo.getCommand(),
                taskInfo.hasHealthCheck() ? Optional.of(taskInfo.getHealthCheck()) : Optional.empty());
    }

    /**
     * @see TaskDataWriter#TaskDataWriter(java.util.Map)
     */
    public SchedulerTaskEnvWriter(TaskInfo.Builder taskInfoBuilder) {
        this(
                taskInfoBuilder.getCommand(),
                taskInfoBuilder.hasHealthCheck() ? Optional.of(taskInfoBuilder.getHealthCheck()) : Optional.empty());
    }

    private SchedulerTaskEnvWriter(CommandInfo taskCmd, Optional<HealthCheck> healthCheck) {
        taskEnv = new TaskDataWriter(EnvUtils.toMap(taskCmd.getEnvironment()));
        if (healthCheck.isPresent()) {
            healthCheckEnv = Optional.of(new TaskDataWriter(
                    EnvUtils.toMap(healthCheck.get().getCommand().getEnvironment())));
        } else {
            healthCheckEnv = Optional.empty();
        }
    }

    public SchedulerTaskEnvWriter setPortEnvvar(String portName, Optional<String> customEnvKey, long port) {
        taskBuilder.setCommand(new SchedulerTaskEnvWriter(taskBuilder).setPort(portName, customEnvKey, port).toProto());
        setPortEnvironmentVariable(taskBuilder.getCommandBuilder(), port);

        // Add port to the health check (if defined)
        if (healthCheckEnv.isPresent()) {
            healthCheckEnv.get().put(EnvUtils.getPortEnvironmentVariable(portName, customEnvKey), Long.toString(port));
        }

        // Add port to the readiness check (if a readiness check is defined)
        try {
            taskBuilder.setLabels(new SchedulerLabelWriter(taskBuilder)
                    .setReadinessCheckPortEnvvar(portName, customEnvKey, Long.toString(port))
                    .toProto());
        } catch (TaskException e) {
            LOGGER.error("Got exception while adding PORT env var to ReadinessCheck", e);
        }

        return this; // TODO
    }
}
