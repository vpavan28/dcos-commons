package com.mesosphere.sdk.zookeeper.scheduler;

import com.google.common.base.Joiner;
import com.mesosphere.sdk.scheduler.DefaultScheduler;
import com.mesosphere.sdk.scheduler.SchedulerFlags;
import com.mesosphere.sdk.specification.DefaultService;
import com.mesosphere.sdk.specification.DefaultServiceSpec;
import com.mesosphere.sdk.specification.yaml.RawServiceSpec;

import java.io.File;
import java.util.List;

/**
 * Template service.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            new DefaultService(createSchedulerBuilder(new File(args[0]))).run();
        }
    }

    private static DefaultScheduler.Builder createSchedulerBuilder(File pathToYamlSpecification)
            throws Exception {
        RawServiceSpec rawServiceSpec = RawServiceSpec.newBuilder(pathToYamlSpecification).build();
        SchedulerFlags schedulerFlags = SchedulerFlags.fromEnv();
        List<String> zkNodes = ZookeeperConfigUtils.getZookeeperNodes(rawServiceSpec.getName());
        DefaultScheduler.Builder schedulerBuilder = DefaultScheduler.newBuilder(
                DefaultServiceSpec.newGenerator(rawServiceSpec, schedulerFlags)
                    .setAllPodsEnv("ZOOKEEPER_SERVERS", Joiner.on('\n').join(zkNodes))
                    .build(),
                schedulerFlags)
                .setPlansFrom(rawServiceSpec);

        return schedulerBuilder;
    }
}
