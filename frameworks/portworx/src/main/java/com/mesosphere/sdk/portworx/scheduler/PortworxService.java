package com.mesosphere.sdk.portworx.scheduler;

import com.mesosphere.sdk.portworx.api.*;
import com.mesosphere.sdk.scheduler.DefaultScheduler;
import com.mesosphere.sdk.scheduler.SchedulerFlags;
import com.mesosphere.sdk.specification.DefaultService;
import com.mesosphere.sdk.specification.yaml.RawServiceSpec;
import com.mesosphere.sdk.specification.yaml.YAMLServiceSpecFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * Portworx Service.
 */
public class PortworxService extends DefaultService {
    protected static final Logger LOGGER = LoggerFactory.getLogger(PortworxService.class);

    public PortworxService(File pathToYamlSpecification) throws Exception {
        RawServiceSpec rawServiceSpec = YAMLServiceSpecFactory.generateRawSpecFromYAML(pathToYamlSpecification);
        SchedulerFlags schedulerFlags = SchedulerFlags.fromEnv();
        DefaultScheduler.Builder schedulerBuilder = DefaultScheduler.newBuilder(
                YAMLServiceSpecFactory.generateServiceSpec(rawServiceSpec, schedulerFlags), schedulerFlags)
                .setPlansFrom(rawServiceSpec);

        schedulerBuilder.setCustomResources(getResources(schedulerBuilder.getServiceSpec().getName()));
        initService(schedulerBuilder);
    }

    private Collection<Object> getResources(String serviceName) {
        final Collection<Object> apiResources = new ArrayList<>();
        apiResources.add(new PortworxResource(serviceName));

        return apiResources;
    }
}
