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
        ENV_VARS.set("ETCD_DISK_TYPE", "ROOT");
        ENV_VARS.set("ETCD_DISK_SIZE", "5120");
        ENV_VARS.set("ETCD_IMAGE", "mesosphere/etcd-mesos:latest");
        ENV_VARS.set("ETCD_NODE_ADVERTISE_PORT", "1026");
        ENV_VARS.set("ETCD_NODE_PEER_PORT", "1027");
        ENV_VARS.set("ETCD_PROXY_ADVERTISE_PORT", "2379");

        ENV_VARS.set("INFLUXDB_CPUS", "0.1");
        ENV_VARS.set("INFLUXDB_MEM", "512");
        ENV_VARS.set("INFLUXDB_DISK_TYPE", "ROOT");
        ENV_VARS.set("INFLUXDB_DISK_SIZE", "1024");
        ENV_VARS.set("INFLUXDB_IMAGE", "influxdb:latest");

        ENV_VARS.set("LIGHTHOUSE_CPUS", "0.1");
        ENV_VARS.set("LIGHTHOUSE_MEM", "512");
        ENV_VARS.set("LIGHTHOUSE_WEBUI_PORT", "8085");
        ENV_VARS.set("LIGHTHOUSE_IMAGE", "portworx/px-lighthouse:latest");
    }

    @Test
    public void testYmlBase() throws Exception {
        testYaml("svc.yml");
    }
}
