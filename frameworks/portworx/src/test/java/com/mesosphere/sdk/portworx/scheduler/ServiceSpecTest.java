package com.mesosphere.sdk.portworx.scheduler;

import com.mesosphere.sdk.testing.BaseServiceSpecTest;
import org.junit.BeforeClass;
import org.junit.Test;

public class ServiceSpecTest extends BaseServiceSpecTest {

    @BeforeClass
    public static void beforeAll() {
        ENV_VARS.set("EXECUTOR_URI", "");
        ENV_VARS.set("LIBMESOS_URI", "");
        ENV_VARS.set("PORT_API", "8080");
        ENV_VARS.set("FRAMEWORK_NAME", "portworx");

        ENV_VARS.set("NODE_COUNT", "3");
        ENV_VARS.set("NODE_CPUS", "0.1");
        ENV_VARS.set("NODE_MEM", "512");

        ENV_VARS.set("ETCD_COUNT", "3");
        ENV_VARS.set("ETCD_CPUS", "0.1");
        ENV_VARS.set("ETCD_MEM", "512");

        ENV_VARS.set("INFLUXDB_CPUS", "0.1");
        ENV_VARS.set("INFLUXDB_MEM", "512");

        ENV_VARS.set("LIGHTHOUSE_CPUS", "0.1");
        ENV_VARS.set("LIGHTHOUSE_MEM", "512");
    }

    @Test
    public void testYmlBase() throws Exception {
        testYaml("svc.yml");
    }
}
