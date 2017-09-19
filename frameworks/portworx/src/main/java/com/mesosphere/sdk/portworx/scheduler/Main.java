package com.mesosphere.sdk.portworx.scheduler;

import com.mesosphere.sdk.config.validate.TaskEnvCannotChange;
import com.mesosphere.sdk.portworx.api.*;
import com.mesosphere.sdk.scheduler.DefaultScheduler;
import com.mesosphere.sdk.scheduler.SchedulerFlags;
import com.mesosphere.sdk.specification.DefaultService;
import com.mesosphere.sdk.specification.DefaultServiceSpec;
import com.mesosphere.sdk.specification.ServiceSpec;
import com.mesosphere.sdk.specification.yaml.RawServiceSpec;

import java.io.File;
import java.util.*;

/**
 * Portworx service.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        new DefaultService(createSchedulerBuilder(new File(args[0]))).run();
    }

    private static DefaultScheduler.Builder createSchedulerBuilder(File pathToYamlSpecification)
            throws Exception {
        RawServiceSpec rawServiceSpec = RawServiceSpec.newBuilder(pathToYamlSpecification).build();
        SchedulerFlags schedulerFlags = SchedulerFlags.fromEnv();
        DefaultScheduler.Builder schedulerBuilder = DefaultScheduler.newBuilder(
                DefaultServiceSpec.newGenerator(rawServiceSpec, schedulerFlags).build(),
                schedulerFlags)
                .setCustomConfigValidators(Arrays.asList(
                        new TaskEnvCannotChange("etcd-cluster", "node", "ETCD_ENABLED"),
                        new TaskEnvCannotChange("etcd-proxy", "node", "ETCD_ENABLED"),
                        new TaskEnvCannotChange("lighthouse", "start", "LIGHTHOUSE_ENABLED")))
                .setPlansFrom(rawServiceSpec);

        schedulerBuilder.setCustomResources(getResources(schedulerBuilder.getServiceSpec()));
        return schedulerBuilder;
    }

    private static Collection<Object> getResources(ServiceSpec serviceSpec) {
        final Collection<Object> apiResources = new ArrayList<>();
        apiResources.add(new PortworxResource(serviceSpec));

        return apiResources;
    }
}
