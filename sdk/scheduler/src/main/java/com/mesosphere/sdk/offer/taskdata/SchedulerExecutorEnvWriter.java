package com.mesosphere.sdk.offer.taskdata;

import java.util.Optional;

import org.apache.mesos.Protos.Environment;
import org.apache.mesos.Protos.ExecutorInfo;

/**
 * Provides write access to executor environment variables which are (only) written by the Scheduler.
 */
public class SchedulerExecutorEnvWriter extends TaskDataWriter {

    /**
     * @see TaskDataWriter#TaskDataWriter(java.util.Map)
     */
    public SchedulerExecutorEnvWriter(ExecutorInfo executorInfo) {
        super(EnvUtils.toMap(executorInfo.getCommand().getEnvironment()));
    }

    /**
     * @see TaskDataWriter#TaskDataWriter(java.util.Map)
     */
    public SchedulerExecutorEnvWriter(ExecutorInfo.Builder executorInfoBuilder) {
        super(EnvUtils.toMap(executorInfoBuilder.getCommand().getEnvironment()));
    }

    public SchedulerExecutorEnvWriter setPortEnvvar(String portName, Optional<String> customEnvKey, long port) {
        put(EnvUtils.getPortEnvironmentVariable(portName, customEnvKey), Long.toString(port));
        return this;
    }

    public Environment toProto() {
        return EnvUtils.toProto(map());
    }
}
