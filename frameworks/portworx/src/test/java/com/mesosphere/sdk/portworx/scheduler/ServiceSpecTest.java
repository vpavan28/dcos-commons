package com.mesosphere.sdk.portworx.scheduler;

import com.mesosphere.sdk.testing.BaseServiceSpecTest;
import org.junit.Test;

public class ServiceSpecTest extends BaseServiceSpecTest {

    public ServiceSpecTest() {
        super(
                "EXECUTOR_URI", "",
                "LIBMESOS_URI", "",
                "PORT_API", "8080",
                "FRAMEWORK_NAME", "portworx",

                "NODE_COUNT", "3",
                "NODE_CPUS", "0.1",
                "NODE_MEM", "512",

                "ETCD_COUNT", "3",
                "ETCD_CPUS", "0.1",
                "ETCD_MEM", "512",
                "ETCD_DISK_TYPE", "ROOT",
                "ETCD_DISK_SIZE", "5120",
                "ETCD_IMAGE", "mesosphere/etcd-mesos:latest",
                "ETCD_NODE_ADVERTISE_PORT", "1026",
                "ETCD_NODE_PEER_PORT", "1027",
                "ETCD_PROXY_ADVERTISE_PORT", "2379",

                "INFLUXDB_CPUS", "0.1",
                "INFLUXDB_MEM", "512",
                "INFLUXDB_DISK_TYPE", "ROOT",
                "INFLUXDB_DISK_SIZE", "1024",
                "INFLUXDB_IMAGE", "influxdb:latest",

                "LIGHTHOUSE_CPUS", "0.1",
                "LIGHTHOUSE_MEM", "512",
                "LIGHTHOUSE_WEBUI_PORT", "8085",
                "LIGHTHOUSE_IMAGE", "portworx/px-lighthouse:latest");
    }

    @Test
    public void testYmlBase() throws Exception {
        testYaml("svc.yml");
    }
}
