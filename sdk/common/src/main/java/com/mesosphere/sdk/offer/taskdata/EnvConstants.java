package com.mesosphere.sdk.offer.taskdata;

/**
 * Environment variables to be set inside launched tasks themselves.
 * Unlike with Labels, these are accessible to the tasks themselves via the container environment.
 */
class EnvConstants {

    private EnvConstants() {
        // do not instantiate
    }

    /** Provides the Task/Pod index of the instance, starting at 0. */
    static final String POD_INSTANCE_INDEX_TASKENV = "POD_INSTANCE_INDEX";
    /** Prefix used for port environment variables which advertise reserved ports by their name. */
    static final String PORT_NAME_TASKENV_PREFIX = "PORT_";
    /** Prefix used for config file templates to be handled by the 'bootstrap' utility executable. */
    static final String CONFIG_TEMPLATE_TASKENV_PREFIX = "CONFIG_TEMPLATE_";
    /** Provides the configured name of the framework/service. */
    static final String FRAMEWORK_NAME_TASKENV = "FRAMEWORK_NAME";
    /** Provides the name of the pod/task within the service. */
    static final String TASK_NAME_TASKENV = "TASK_NAME";
}
