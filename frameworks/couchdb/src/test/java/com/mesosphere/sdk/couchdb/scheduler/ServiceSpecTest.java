package com.mesosphere.sdk.couchdb.scheduler;

import com.mesosphere.sdk.testing.BaseServiceSpecTest;
import org.junit.Test;

public class ServiceSpecTest extends BaseServiceSpecTest {

    public ServiceSpecTest() {
        super(
                "EXECUTOR_URI", "",
                "LIBMESOS_URI", "",
                "PORT_API", "8080",
                "FRAMEWORK_NAME", "portworx-couchdb",

                "COUCHDB_DOCKER_IMAGE", "couchdb:2.1.1",
                "NODE_COUNT", "3",
                "COUCHDB_CPUS", "0.5",
                "COUCHDB_MEM_MB", "2048",
                "COUCHDB_DISK_MB", "10240",
                "COUCHDB_DOCKER_VOLUME_NAME", "CouchDBVolume",
                "COUCHDB_PORT", "5984",
                "COUCHDB_NODE_PORT", "5986"
              );
    }

    @Test
    public void testYmlBase() throws Exception {
        testYaml("svc.yml");
    }
}
