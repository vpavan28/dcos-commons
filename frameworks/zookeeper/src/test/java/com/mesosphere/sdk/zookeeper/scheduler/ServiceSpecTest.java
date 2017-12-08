package com.mesosphere.sdk.zookeeper.scheduler;

import com.mesosphere.sdk.testing.BaseServiceSpecTest;
import org.junit.Test;

public class ServiceSpecTest extends BaseServiceSpecTest {

    public ServiceSpecTest() {
        super(
                "EXECUTOR_URI", "http://executor.uri",
                "BOOTSTRAP_URI", "http://bootstrap.uri",
                "SCHEDULER_URI", "http://scheduler.uri",
                "LIBMESOS_URI", "http://libmesos.uri",
                "ZOOKEEPER_URI", "http://zookeeper.uri",

                "PORT_API", "8080",
                "FRAMEWORK_NAME", "portworx-zookeeper",

                "ZOOKEEPER_VERSION", "3.4.11",
                "TASKCFG_ALL_ZOOKEEPER_CLIENT_PORT", "2182",

                "NODE_COUNT", "3",
                "NODE_CPUS", "0.1",
                "ZOOKEEPER_MEM_MB", "512",
                "ZOOKEEPER_DISK_MB", "2048",

                "ZOOKEEPER_DOCKER_VOLUME_NAME", "ZookeeperVolume"
              );
    }

    @Test
    public void testYmlBase() throws Exception {
        testYaml("svc.yml");
    }
}
