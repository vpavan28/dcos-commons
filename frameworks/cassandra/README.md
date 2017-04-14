# DC/OS Apache Cassandra Service Documentation

## Table of Contents

- [Overview](#overview)
  - Features
- [Quick Start](#quick-start)
- [Installing and Customizing](#installing-and-customizing)
  - Installation from CLI
  - Installation from Web
  - Service Settings
    - Service Name
    - Data Center
    - Rack
    - Remote Seeds
  - Cassandra Settings
  - Node Settings
    - Node Count
    - CPU
    - Memory
    - JMX Port
    - Storage Port
    - SSL Storage Port
    - Native Transport Port
    - RPC Port
    - Disk Type
    - Placement Constraints
- [Uninstalling](#uninstalling)
- [Connecting Clients](#connecting-clients)
  - Discovering Endpoints
  - Connecting Clients to Endpoints
- [Managing](#managing)
  - Updating Configuration
    - Adding a Node
    - Resizing a Node
    - Updating Placement Constraints
  - Restarting Nodes
  - Replacing Nodes
  - Configuring Multi-data-center Deployments
- [Disaster Recovery](#disaster-recovery)
  - Backup
  - Restore
- [Deployment Best Practices](#deploy-best-practices)
- [Troubleshooting](#troubleshooting)
  - Accessing Logs
  - Replacing a Permanently Failed Node
- [Limitations](#limitations)
  - Accessibility of nodes in multi-data-center deployments
- [Support](#support)
  - Supported Versions

<a name="overview"></a>
# Overview

DC/OS Apache Cassandra is an automated service that makes it easy to deploy and manage Apache Cassandra on [Mesosphere DC/OS](http://dcos.io). Apache Cassandra is a distributed, NoSQL database offering high availability, fault tolerance and scalability across data centers.

For more information on Apache Cassandra, see the Apache Cassandra [documentation](http://cassandra.apache.org/doc/latest/).

## Features

*   Easy installation
*   Simple horizontal scaling of Cassandra nodes
*   Straightforward backup and restore of data out of the box
*   Multi-datacenter replication support
*   Rack-aware scheduling with placement constraints

<a name="quick-start"></a>
# Quick Start

1. Get a DC/OS cluster. If you don't have one yet, head over to [DC/OS Docs](https://dcos.io/docs/latest) for instructions.
2. Install the Service in your DC/OS cluster, either via the [DC/OS Dashboard](https://docs.mesosphere.com/latest/usage/webinterface/) or via the [DC/OS CLI](https://docs.mesosphere.com/latest/usage/cli/) as shown here:
```
dcos config set core.dcos_url http://your-cluster.com
dcos config set core.ssl_verify False # optional
dcos auth login
```
```
dcos package install cassandra
```
3. The service will now deploy with a default configuration. You can monitor its deployment via the Services UI in the DC/OS Dashboard.
4. Get the address of a Cassandra node in your running cluster:
```
dcos cassandra endpoints node
{
  "address": [
    "10.0.1.125:9042",
    "10.0.2.152:9042",
    "10.0.1.22:9042"
  ],
  "dns": [
    "node-1-server.cassandra.mesos:9042",
    "node-0-server.cassandra.mesos:9042",
    "node-2-server.cassandra.mesos:9042"
  ],
  "vip": "node.cassandra.l4lb.thisdcos.directory:9042"
}
```
5. Write some data to your cluster:
```
dcos node ssh --master-proxy --leader
core@ip-10-0-6-153 ~ docker run -it cassandra:3.0.10 cqlsh node-0-server.cassandra.mesos
> CREATE KEYSPACE space1 WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 3 };
> USE space1;
> CREATE TABLE testtable1 (key varchar, value varchar, PRIMARY KEY(key));
> INSERT INTO space1.testtable1(key, value) VALUES('testkey1', 'testvalue1');
> SELECT * FROM testtable1;
```

<a name="installing-and-customizing"></a>
# Installing and Customizing

When installing the service without any additional customizations, reasonable defaults are provided, but different configurations are recommended depending on the context of the deployment. The defaults are reasonable for trying out the service but not necessarily for production use.

## Installation from CLI

From the DC/OS CLI, DC/OS Apache Cassandra may be installed with a default testing/non-production configuration as follows:
```
dcos package install beta-cassandra
```

A custom configuration may be specified in an `options.json` file and passed to the the DC/OS CLI as follows:
```
$ dcos package install beta-cassandra --options=your-options.json
```

For more information about building the options.json file, see the [DC/OS documentation](https://docs.mesosphere.com/latest/usage/managing-services/config-universe-service/) for service configuration access.

## Installation from Web

From the DC/OS Dashboard webpage, DC/OS Apache Cassandra may be installed with a default configuration as follows:
1. Visit http://yourcluster.com/ to view the DC/OS Dashboard.
1. Navigate to `Universe` => `Packages` and find the `beta-cassandra` package.
1. Click `Install`, then in the pop up dialog click `Install` again to use default settings.

A customized installation may be performed from the DC/OS Dashboard as follows:
1. Visit http://yourcluster.com/ to view the DC/OS Dashboard.
1. Navigate to `Universe` => `Packages` and find the `beta-cassandra` package.
1. Click `Install`, then in the pop up dialog click `Advanced` to see the customization dialog.
1. Make your changes to the default configuration in the customization dialog, then click `Review`.
1. Examine the configuration summary for any needed changes. Click `Back` to make changes, or `Install` to confirm the settings and install the service.

## Service Settings

### Service Name

Each instance of DC/OS Apache Cassandra in a given DC/OS cluster must be configured with a different service name. You can configure the service name in the **service** section of the install settings. The default service name (used in many examples here) is `cassandra`.

*   **In DC/OS CLI options.json**: `name`: string (default: `cassandra`)
*   **DC/OS web interface**: The service name cannot be changed after the cluster has started.

### Data Center

Configure the name of the logical data center that this Cassandra cluster runs in. This sets the `dc` property in the `cassandra-rackdc.properties` file.

*   **In DC/OS CLI options.json**: `data_center`: string (default: `dc1`)
*   **DC/OS web interface**: `CASSANDRA_LOCATION_DATA_CENTER`: `string`

### Rack

Configure the name of the rack that this Cassandra cluster runs in. This sets the `rack` property in the `cassandra-rackdc.properties` file.

*   **In DC/OS CLI options.json**: `rack`: string (default: `rac1`)
*   **DC/OS web interface**: `CASSANDRA_LOCATION_RACK`: `string`

### Remote Seeds

Configure the remote seeds from another Cassandra cluster that this cluster should communicate with to establish cross-data-center replication. This should be a comma-separated list of node hostnames, such as `node-0-server.cassandra.mesos,node-1-server.cassandra.mesos`. For more information on multi-data-center configuration, see #_multi-dc_.

*   **In DC/OS CLI options.json**: `remote_seeds`: string (default: `""`)
*   **DC/OS web interface**: `TASKCFG_ALL_REMOTE_SEEDS`: `string`

## Cassandra Settings

Most of the settings exposed in Apache Cassandra's `cassandra.yaml` configuration file are configurable via DC/OS. For information about these settings, see the Apache Cassandra [documentation](http://cassandra.apache.org/doc/latest/configuration/cassandra_config_file.html). Settings that can be configured via DC/OS include:

*   `cluster_name`
*   `num_tokens`
*   `hinted_handoff_enabled`
*   `max_hint_window_in_ms`
*   `hinted_handoff_throttle_in_kb`
*   `max_hints_delivery_threads`
*   `hints_flush_period_in_ms`
*   `max_hints_file_size_in_mb`
*   `batchlog_replay_throttle_in_kb`
*   `authenticator`
*   `authorizer`
*   `partitioner`
*   `key_cache_save_period`
*   `row_cache_size_in_mb`
*   `row_cache_save_period`
*   `commitlog_sync_period_in_ms`
*   `commitlog_segment_size_in_mb`
*   `commitlog_total_space_in_mb`
*   `concurrent_reads`
*   `concurrent_writes`
*   `concurrent_counter_writes`
*   `concurrent_materialized_view_writes`
*   `memtable_allocation_type`
*   `index_summary_resize_interval_in_minutes`
*   `storage_port`
*   `ssl_storage_port`
*   `start_native_transport`
*   `native_transport_port`
*   `start_rpc`
*   `rpc_port`
*   `rpc_keepalive`
*   `thrift_framed_transport_size_in_mb`
*   `tombstone_warn_threshold`
*   `tombstone_failure_threshold`
*   `column_index_size_in_kb`
*   `batch_size_warn_threshold_in_kb`
*   `batch_size_fail_threshold_in_kb`
*   `compaction_throughput_mb_per_sec`
*   `sstable_preemptive_open_interval_in_mb`
*   `read_request_timeout_in_ms`
*   `range_request_timeout_in_ms`
*   `write_request_timeout_in_ms`
*   `counter_write_request_timeout_in_ms`
*   `internode_compression`
*   `cas_contention_timeout_in_ms`
*   `truncate_request_timeout_in_ms`
*   `request_timeout_in_ms`
*   `dynamic_snitch_update_interval_in_ms`
*   `dynamic_snitch_reset_interval_in_ms`
*   `dynamic_snitch_badness_threshold`
*   `roles_update_interval_in_ms`
*   `permissions_update_interval_in_ms`
*   `key_cache_keys_to_save`
*   `row_cache_keys_to_save`
*   `counter_cache_keys_to_save`
*   `file_cache_size_in_mb`
*   `memtable_heap_space_in_mb`
*   `memtable_offheap_space_in_mb`
*   `memtable_cleanup_threshold`
*   `memtable_flush_writers`
*   `listen_on_broadcast_address`
*   `internode_authenticator`
*   `native_transport_max_threads`
*   `native_transport_max_frame_size_in_mb`
*   `native_transport_max_concurrent_connections`
*   `native_transport_max_concurrent_connections_per_ip`
*   `rpc_min_threads`
*   `rpc_max_threads`
*   `rpc_send_buff_size_in_bytes`
*   `rpc_recv_buff_size_in_bytes`
*   `concurrent_compactors`
*   `stream_throughput_outbound_megabits_per_sec`
*   `inter_dc_stream_throughput_outbound_megabits_per_sec`
*   `streaming_socket_timeout_in_ms`
*   `phi_convict_threshold`
*   `buffer_pool_use_heap_if_exhausted`
*   `disk_optimization_strategy`
*   `max_value_size_in_mb`
*   `otc_coalescing_strategy`

## Node Settings

The following settings may be adjusted to customize the amount of resources allocated to each node. Apache Cassandra's [resource requirements](http://cassandra.apache.org/doc/latest/operating/hardware.html) must be taken into consideration when adjusting these values. Reducing these values below those requirements may result in adverse performance and/or failures while using the service.

Each of the following settings may be customized under the **node** configuration section.

### Node Count

The number of Cassandra nodes running in your DC/OS Apache Cassandra cluster. After installation, this number can be increased, with new Cassandra nodes automatically joining the cluster. Decreasing the number of nodes is not supported.

*   **In DC/OS CLI options.json**: `count`: integer (default: `3`)
*   **DC/OS web interface**: `NODES`: `integer`

### CPU

The amount of CPU allocated to each node may be customized. A value of `1.0` equates to one full CPU core on a machine. This value may be customized by editing the **cpus** value under the **node** configuration section. Turning this too low will result in throttled tasks.

*   **In DC/OS CLI options.json**: `cpus`: number (default: `0.5`)
*   **DC/OS web interface**: `CASSANDRA_CPUS`: `number`

### Memory

The amount of RAM allocated to each node may be customized. This value may be customized by editing the **mem** value (in MB) under the **node** configuration section. Turning this too low will result in out of memory errors. The `heap.size` setting must also be less than this value to prevent out of memory errors resulting from the Java Virtual Machine attempting to allocate more memory than is available to the Cassandra process.

*   **In DC/OS CLI options.json**: `mem`: integer (default: `10240`)
*   **DC/OS web interface**: `CASSANDRA_MEMORY_MB`: `integer`

### JMX Port

The port that Apache Cassandra listens on for JMX requests, such as those issued by `nodetool`.

*   **In DC/OS CLI options.json**: `jmx_port`: integer (default: `7199`)
*   **DC/OS web interface**: `TASKCFG_ALL_JMX_PORT`: `integer`

### Storage Port

The port that Apache Cassandra listens on for inter-node communication.

*   **In DC/OS CLI options.json**: `storage_port`: integer (default: `7000`)
*   **DC/OS web interface**: `TASKCFG_ALL_CASSANDRA_STORAGE_PORT`: `integer`

### SSL Storage Port

The port that Apache Cassandra listens on for inter-node communication over SSL.

*   **In DC/OS CLI options.json**: `ssl_storage_port`: integer (default: `7001`)
*   **DC/OS web interface**: `TASKCFG_ALL_CASSANDRA_SSL_STORAGE_PORT`: `integer`

### Native Transport Port

The port that Apache Cassandra listens on for CQL queries.

*   **In DC/OS CLI options.json**: `native_transport_port`: integer (default: `9042`)
*   **DC/OS web interface**: `TASKCFG_ALL_CASSANDRA_NATIVE_TRANSPORT_PORT`: `integer`

### RPC Port

The port that Apache Cassandra listens on for Thrift RPC requests.

*   **In DC/OS CLI options.json**: `rpc_port`: integer (default: `9160`)
*   **DC/OS web interface**: `TASKCFG_ALL_CASSANDRA_RPC_PORT`: `integer`

### Disk Type

The type of disks that can be used for storing broker data are: `ROOT` (default) and `MOUNT`.  The type of disk may only be specified at install time.

* `ROOT`: node data is stored on the same volume as the agent work directory. Cassandra tasks will use the configured amount of disk space. These are used by default, since they do not require the [extra configuration](https://dcos.io/docs/1.8/administration/storage/mount-disk-resources/) necessary to enable MOUNT volumes.
* `MOUNT`: Node data will be stored on a dedicated volume attached to the agent. Dedicated MOUNT volumes have performance advantages, since they are dedicated to the Cassandra task that reserves them.

To configure the disk type:
*   **In DC/OS CLI options.json**: `disk_type`: string (default: `ROOT`)
*   **DC/OS web interface**: `CASSANDRA_DISK_TYPE`: `string`

### Placement Constraints

Placement constraints allow you to customize where Apache Cassandra nodes are deployed in the DC/OS cluster. Placement constraints support all [Marathon operators (reference)](http://mesosphere.github.io/marathon/docs/constraints.html) with this syntax: `field:OPERATOR[:parameter]`. For example, if the reference lists `[["hostname", "UNIQUE"]]`, you should  use `hostname:UNIQUE`.

*   **In DC/OS CLI options.json**: `placement_constraint`: string (default: `""`)
*   **DC/OS web interface**: `PLACEMENT_CONSTRAINT`: `string`

**MULTI DC SUPPORT**
** OTHER CUSTOM PLANS**

<a name="uninstalling"></a>
# Uninstalling

Follow these steps to uninstall the service.

1. Stop the service. From the DC/OS CLI, enter `dcos package uninstall`.
1. Clean up remaining reserved resources with the framework cleaner script, `janitor.py`. [More information about the framework cleaner script](https://docs.mesosphere.com/1.8/usage/managing-services/uninstall/#framework-cleaner).

To uninstall an instance named `cassandra` (the default), run:
```
MY_SERVICE_NAME=cassandra
dcos package uninstall --app-id=$MY_SERVICE_NAME cassandra
dcos node ssh --master-proxy --leader "docker run mesosphere/janitor /janitor.py \
    -r $MY_SERVICE_NAME-role \
    -p $MY_SERVICE_NAME-principal \
    -z dcos-service-$MY_SERVICE_NAME"
```

<a name="connecting-clients"></a>
# Connecting Clients

Clients communicating with Apache Cassandra use the Cassandra Query Language (CQL) to issue queries and write data to the cluster. CQL client libraries exist in many languages, and Apache Cassandra ships with a utility called `cqlsh` that enables you to issue queries against an Apache Cassandra cluster from the command line.

## Discovering Endpoints

Once the service is running, you may view information about its endpoints via either of the following methods:
- CLI:
  - List endpoint types: `dcos cassandra endpoints`
  - View endpoints for an endpoint type: `dcos cassandra endpoints <endpoint>`
- Web:
  - List endpoint types: `https://yourcluster.com/service/cassandra/v1/endpoints`
  - View endpoints for an endpoint type: `https://yourcluster.com/service/cassandra/v1/endpoints/<endpoint>`

The DC/OS Apache Cassandra Service currently exposes only the `node` endpoint type, which shows the locations for all Cassandra nodes in the cluster. To see node addresses, run `dcos cassandra endpoints node`. A typical response will look like the following:

```json
{
  "address": [
    "10.0.0.49:9042",
    "10.0.2.253:9042",
    "10.0.1.27:9042"
  ],
  "dns": [
    "node-2-server.cassandra.mesos:9042",
    "node-0-server.cassandra.mesos:9042",
    "node-1-server.cassandra.mesos:9042"
  ],
  "vip": "node.cassandra.l4lb.thisdcos.directory:9042"
}
```

In general, the `.mesos` endpoints will only work from within the same DC/OS cluster. From outside the cluster you may either use the direct IPs, or set up a proxy service which acts as a frontend to your DC/OS Apache Cassandra instance. For development and testing purposes, you may use [DC/OS Tunnel](https://docs.mesosphere.com/latest/administration/access-node/tunnel/) to access services from outside the cluster, but this option is not suitable for production use.

## Connecting Clients to Endpoints

To connect to a DC/OS Apache Cassandra cluster using `cqlsh`, first SSH into a host in your DC/OS cluster:
```
dcos node ssh --leader --master-proxy
```

Then, use the `cassandra` Docker image to run `cqlsh`, passing as an argument the address of one of the Apache Cassandra nodes in the cluster:
```
docker run cassandra:3.0.10 cqlsh node-0-server.cassandra.mesos
```

This will open an interactive shell from which you can issue queries and write to the cluster. To ensure that the `cqlsh` client and your cluster are using the same CQL version, be sure to use the version of the `cassandra` Docker image that corresponds to the version of Apache Cassandra being run in your cluster. The version installed by the DC/OS Apache Cassandra Service is 3.0.10.

<a name="managing"></a>
# Managing

## Updating Configuration

Configuration changes may be performed by editing the runtime environment of the Scheduler. After making a change, the scheduler will be restarted, and it will automatically deploy any detected changes to the service, one node at a time. For example a given change will first be applied to the `node-0` pod, then `node-1`, and so on.

Nodes are configured with a "Readiness check" to ensure that the underlying service appears to be in a healthy state before continuing with applying a given change to the next node in the sequence. However this basic check is not foolproof and reasonable care should be taken to ensure that a given configuration change will not negatively affect the behavior of the service.

Some changes, such as decreasing the number of nodes or changing volume requirements, are not supported after initial deployment. See [Limitations](#limitations).

To see a full listing of available options, run `dcos package describe --config cassandra` in the CLI, or browse the DC/OS Apache Cassandra Service install dialog in the DC/OS Dashboard.

### Adding a Node

The service deploys 3 nodes by default. This may be customized at initial deployment or after the cluster is already running via the `NODES` environment variable. Shrinking the cluster is not supported. If you decrease this value, the scheduler will complain about the configuration change until it's reverted back to its original value or a larger one.

### Resizing a Node

The CPU and Memory requirements of each node may be increased or decreased as follows:
- CPU (1.0 = 1 core): `CASSANDRA_CPUS`
- Memory (in MB): `CASSANDRA_MEMORY_MB`. To prevent out of memory errors, you must ensure that the `TASKCFG_ALL_CASSANDRA_HEAP_SIZE` environment variable is less than `$CASSANDRA_MEMORY_MB`.

Note that volume requirements (type and/or size) may not be changed after initial deployment.

### Updating Placement Constraints

Placement constraints may be updated after initial deployment using the following procedure. See [Service Settings](#service-settings) above for more information on placement constraints.

Let's say we have the following deployment of our nodes

- Placement constraint of: `hostname:LIKE:10.0.10.3|10.0.10.8|10.0.10.26|10.0.10.28|10.0.10.84`
- Tasks:
```
10.0.10.3: node-0
10.0.10.8: node-1
10.0.10.26: node-2
10.0.10.28: empty
10.0.10.84: empty
```

`10.0.10.8` is being decommissioned and we should move away from it. Steps:

1. Remove the decommissioned IP and add a new IP to the placement rule whitelist by editing `PLACEMENT_CONSTRAINT`:

	```
	hostname:LIKE:10.0.10.3|10.0.10.26|10.0.10.28|10.0.10.84|10.0.10.123
	```
1. Redeploy `node-1` from the decommissioned node to somewhere within the new whitelist: `dcos cassandra pods replace node-1`
1. Wait for `node-1` to be up and healthy before continuing with any other replacement operations.

## Restarting Nodes

This operation will restart a node, while keeping it at its current location and with its current persistent volume data. This may be thought of as similar to restarting a system process, but it also deletes any data which isn't in a persistent volume, via the magic of containers.

1. Run `dcos cassandra pods restart node-<NUM>`, e.g. `node-2`.

## Replacing Nodes

This operation will move a node to a new system, and will discard the persistent volumes at the prior system to be rebuilt at the new system. Perform this operation if a given system is about to be offlined or has already been offlined. Note that nodes are not moved automatically; you must manually perform the following steps to move nodes to new systems. You may build your own automation to perform node replacement automatically according to your own preferences.

1. Run `dcos cassandra pods replace node-<NUM>` to halt the current instance with id `<NUM>` (if still running) and launch a new instance elsewhere.

For example, let's say `node-2`'s host system has died and `node-2` needs to be moved.
```
dcos cassandra pods replace node-2
```

## Configuring Multi-data-center Deployments

To replicate data across data centers, Apache Cassandra requires that you configure each cluster with the addresses of the seed nodes from every remote cluster. Here's what starting a multi-data-center Apache Cassandra deployment would like, running inside of a single DC/OS cluster.

Launch the first cluster with the default configuration:
```
dcos package install cassandra
```

Create an options.json file for the second cluster that specifies a different service name and data center name:
```json
{
  "service": {
    "name": "cassandra2",
    "data_center": "dc2"
  }
}
```

Launch the second cluster with these custom options:
```
dcos package install cassandra --options=options.json
```

Get the list of seed node addresses for the first cluster from the scheduler HTTP API:
```json
DCOS_AUTH_TOKEN=$(dcos config show core.dcos_acs_token)
DCOS_URL=$(dcos config show core.dcos_url)
curl -H "authorization:token=$DCOS_AUTH_TOKEN" $DCOS_URL/service/cassandra/v1/seeds
{"seeds": ["10.0.0.1", "10.0.0.2"]}
```

In the DC/OS UI, go to the configuration dialog for the second cluster (whose service name is `cassandra2`) and update the `TASKCFG_ALL_REMOTE_SEEDS` environment variable to `10.0.0.1,10.0.0.2`. This environment variable may not already be present in a fresh install. To add it, click the plus sign at the bottom of the list of environment variables, and then fill in its name and value in the new row that appears.

Get the seed node addresses for the second cluster the same way:
```
curl -H "authorization:token=$DCOS_AUTH_TOKEN" $DCOS_URL/service/cassandra2/v1/seeds
{"seeds": ["10.0.0.3", "10.0.0.4"]}
```

In the DC/OS UI, go to the configuration dialog for the first cluster (whose service name is `cassandra`) and update the `TASKCFG_ALL_REMOTE_SEEDS` environment variable to `10.0.0.3,10.0.0.4`, again adding the variable with the plus sign if it's not already present.

Both schedulers will restart after the configuration update, and each cluster will communicate with the seed nodes from the other cluster to establish a multi-data-center topology. Repeat this process for each new cluster you add, appending a comma-separated list of that cluster's seeds to the `TASKCFG_ALL_REMOTE_SEEDS` environment variable for each existing cluster, and adding a comma-separated list of each existing cluster's seeds to the newly-added cluster's `TASKCFG_ALL_REMOTE_SEEDS` environment variable.

<a name="disaster-recovery"></a>
# Disaster Recovery

## Backup

You can backup an entire cluster's data and schema to Amazon S3 using the `backup-s3` plan. This plan requires the following parameters to run:
- `SNAPSHOT_NAME`: the name of this snapshot. Snapshots for individual nodes will be stored as gzipped tarballs with the name `$SNAPSHOT_NAME-<POD_INDEX>`.
- `CASSANDRA_KEYSPACES`: the Cassandra keyspaces to backup. The entire keyspace, as well as its schema, will be backed up for each keyspace specified.
- `AWS_ACCESS_KEY_ID`: the access key ID for the AWS IAM user running this backup.
- `AWS_SECRET_ACCESS_KEY`: the secret access key for the AWS IAM user running this backup.
- `AWS_REGION`: the region of the S3 bucket being used to store this backup.
- `S3_BUCKET_NAME`: the name of the S3 bucket to store this backup in.

This plan can be initiated from the command line:
```
SNAPSHOT_NAME=<my_snapshot>
CASSANDRA_KEYSPACES="\"space1 space2\""
AWS_ACCESS_KEY_ID=<my_access_key_id>
AWS_SECRET_ACCESS_KEY=<my_secret_access_key>
AWS_REGION=us-west-2
S3_BUCKET_NAME=backups
dcos cassandra plan start backup-s3 "SNAPSHOT_NAME=$SNAPSHOT_NAME,CASSANDRA_KEYSPACES=$CASSANDRA_KEYSPACES,AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID,AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY,AWS_REGION=$AWS_REGION,S3_BUCKET_NAME=$S3_BUCKET_NAME"
```

If multiple keyspaces are being backed up, it is necessary to wrap them in escaped quotation marks (`\"`) for the backup to run successfully.

**IMPORTANT**: To ensure that sensitive information such as your AWS secret access key remains secure, make sure that you've set the `core.dcos_url` configuration property in the DC/OS CLI to an HTTPS URL.

## Restore

Restoring cluster data is similar to backing it up. The `restore-s3` plan assumes that your data is stored in an S3 bucket in the format that `backup-s3` uses. The restore plan has the following parameters:
- `SNAPSHOT_NAME`: the snapshot name from the `backup-s3` plan.
- `AWS_ACCESS_KEY_ID`: the access key ID for the AWS IAM user running this restore.
- `AWS_SECRET_ACCESS_KEY`: the secret access key for the AWS IAM user running this restore.
- `AWS_REGION`: the region of the S3 bucket being used to store the backup being restored.
- `S3_BUCKET_NAME`: the name of the S3 bucket where the backup is stored.

To initiate this plan from the command line:
```
SNAPSHOT_NAME=<my_snapshot>
AWS_ACCESS_KEY_ID=<my_access_key_id>
AWS_SECRET_ACCESS_KEY=<my_secret_access_key>
AWS_REGION=us-west-2
S3_BUCKET_NAME=backups
dcos cassandra plan start backup-s3 "SNAPSHOT_NAME=$SNAPSHOT_NAME,AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID,AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY,AWS_REGION=$AWS_REGION,S3_BUCKET_NAME=$S3_BUCKET_NAME"
```

This will restore the schema from every keyspace backed up with the `backup-s3` plan and populate those keyspaces with the data they contained at the time the snapshot was taken. This plan assumes that the keyspaces being restored do not already exist in the current cluster, and will fail if any keyspace with the same name is present.

<a name="troubleshooting"></a>
# Troubleshooting

## Accessing Logs

Logs for the Scheduler and all service nodes may be browsed via the DC/OS Dashboard.

- Scheduler logs are useful for determining why a node isn't being launched (this is under the purview of the Scheduler).
- Node logs are useful for examining problems in the service itself.

In all cases, logs are generally piped to files named `stdout` and/or `stderr`.

To view logs for a given node, perform the following steps:
1. Visit http://yourcluster.com/ to view the DC/OS Dashboard.
1. Navigate to `Services` and click on the service to be examined (default `cassandra`).
1. In the list of tasks for the service, click on the task to be examined (scheduler is named after the service, nodes are each `node-<NUM>-server`).
1. In the task details, click on the `Logs` tab to go into the log viewer. By default you will see `stdout`, but `stderr` is also useful. Use the pull-down in the upper right to select the file to be examined.

## Replacing a Permanently Failed Node

If a machine has permanently failed, manual intervention is required to replace the node or nodes that resided on that machine. Because DC/OS Apache Cassandra uses persistent volumes, the service continuously attempts to replace nodes where their data has been persisted. In the case where a machine has permanently failed, use the DC/OS Apache Cassandra CLI to replace the nodes.

In the example below, the node with id `0` will be replaced on a new machine as long as cluster resources are sufficient to satisfy the service’s placement constraints and resource requirements.

    $ dcos cassandra broker replace 0

<a name="limitations"></a>
# Limitations

- For multi-data-center configurations, the hostnames for the seed nodes in each cluster must be routable from every other cluster. Typically, DC/OS hosts are members of a private subnet that is not routable from external hosts, so further network configuration is required to achieve this.

<a name="support"></a>
# Support

## Supported Versions

The DC/OS Apache Cassandra Service runs Cassandra v3.0.10. It supports DC/OS version 1.8 and later.
