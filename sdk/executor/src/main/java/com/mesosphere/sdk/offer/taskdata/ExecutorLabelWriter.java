package com.mesosphere.sdk.offer.taskdata;

import org.apache.mesos.Protos.Labels;

/**
 * Provides write access to task labels which are (only) written by the Executor.
 */
public class ExecutorLabelWriter extends TaskDataWriter {

    public ExecutorLabelWriter() {
        super();
    }

    /**
     * Indicates to the Scheduler that an Executor's readiness check has passed.
     */
    public ExecutorLabelWriter setReadinessCheckPassed() {
        put(LabelConstants.READINESS_CHECK_PASSED_LABEL, LabelConstants.READINESS_CHECK_PASSED_LABEL_VALUE);
        return this;
    }

    /**
     * Returns a Protobuf representation of all contained entries.
     */
    public Labels toProto() {
        return LabelUtils.toProto(map());
    }
}
