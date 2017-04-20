package com.mesosphere.sdk.offer;

import com.google.protobuf.TextFormat;
import com.mesosphere.sdk.api.ArtifactResource;
import com.mesosphere.sdk.offer.taskdata.SchedulerLabelReader;
import com.mesosphere.sdk.offer.taskdata.SchedulerLabelWriter;
import com.mesosphere.sdk.offer.taskdata.SchedulerTaskEnvWriter;
import com.mesosphere.sdk.scheduler.SchedulerFlags;
import com.mesosphere.sdk.scheduler.plan.PodInstanceRequirement;
import com.mesosphere.sdk.specification.*;
import com.mesosphere.sdk.specification.util.RLimit;
import com.mesosphere.sdk.state.StateStore;
import org.apache.mesos.Protos;
import org.apache.mesos.Protos.TaskInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A default implementation of the OfferRequirementProvider interface.
 */
public class DefaultOfferRequirementProvider implements OfferRequirementProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultOfferRequirementProvider.class);

    private static final String CONFIG_TEMPLATE_DOWNLOAD_DIR = "config-templates/";

    private final StateStore stateStore;
    private final String serviceName;
    private final UUID targetConfigurationId;
    private final SchedulerFlags schedulerFlags;

    /**
     * Creates a new instance which relies on the provided {@link StateStore} for storing known tasks, and which
     * updates tasks which are not tagged with the provided {@code targetConfigurationId}.
     */
    public DefaultOfferRequirementProvider(
            StateStore stateStore, String serviceName, UUID targetConfigurationId, SchedulerFlags schedulerFlags) {
        this.stateStore = stateStore;
        this.serviceName = serviceName;
        this.targetConfigurationId = targetConfigurationId;
        this.schedulerFlags = schedulerFlags;
    }

    @Override
    public OfferRequirement getNewOfferRequirement(PodInstanceRequirement podInstanceRequirement)
            throws InvalidRequirementException {
        PodInstance podInstance = podInstanceRequirement.getPodInstance();
        return OfferRequirement.create(
                podInstance.getPod().getType(),
                podInstance.getIndex(),
                getNewTaskRequirements(
                        podInstance,
                        podInstanceRequirement.getTasksToLaunch(),
                        podInstanceRequirement.getParameters()),
                getExecutorRequirement(podInstance, serviceName, targetConfigurationId),
                podInstance.getPod().getPlacementRule());
    }

    private Collection<TaskRequirement> getNewTaskRequirements(
            PodInstance podInstance,
            Collection<String> tasksToLaunch,
            Map<String, String> parameters) throws InvalidRequirementException{
        LOGGER.info("Getting new TaskRequirements for tasks: {}", tasksToLaunch);

        ArrayList<String> usedResourceSets = new ArrayList<>();
        List<TaskRequirement> taskRequirements = new ArrayList<>();

        // Generating TaskRequirements for evaluation.
        for (TaskSpec taskSpec : podInstance.getPod().getTasks()) {
            if (!tasksToLaunch.contains(taskSpec.getName())) {
                continue;
            }

            if (!usedResourceSets.contains(taskSpec.getResourceSet().getId())) {
                LOGGER.info("Generating taskInfo to launch for: {}, with resource set: {}",
                        taskSpec.getName(), taskSpec.getResourceSet().getId());
                usedResourceSets.add(taskSpec.getResourceSet().getId());
                taskRequirements.add(getNewTaskRequirement(podInstance, taskSpec, parameters, false));
            }
        }

        // Generating TaskRequirements to complete Pod footprint.
        for (TaskSpec taskSpec : podInstance.getPod().getTasks()) {
            if (tasksToLaunch.contains(taskSpec.getName())) {
                continue;
            }

            if (!usedResourceSets.contains(taskSpec.getResourceSet().getId())) {
                LOGGER.info("Generating transient taskInfo to complete pod footprint for: {}, with resource set: {}",
                        taskSpec.getName(), taskSpec.getResourceSet().getId());
                TaskRequirement taskRequirement = getNewTaskRequirement(podInstance, taskSpec, parameters, true);
                usedResourceSets.add(taskSpec.getResourceSet().getId());
                taskRequirements.add(taskRequirement);
            }
        }

        return taskRequirements;
    }

    private TaskRequirement getNewTaskRequirement(
            PodInstance podInstance,
            TaskSpec taskSpec,
            Map<String, String> parameters,
            boolean isTransient) throws InvalidRequirementException {
        TaskInfo taskInfo = createTaskInfo(
                podInstance, taskSpec, parameters, getNewResources(taskSpec), serviceName, targetConfigurationId, isTransient);

        Collection<ResourceRequirement> resourceRequirements =
                getResourceRequirements(taskSpec, Collections.emptyList());

        return new TaskRequirement(taskInfo, resourceRequirements);
    }

    private static TaskInfo createTaskInfo(
            PodInstance podInstance,
            TaskSpec taskSpec,
            Map<String, String> parameters,
            Collection<Protos.Resource> resources,
            String serviceName,
            UUID targetConfigurationId,
            boolean isTransient) {
        Protos.TaskInfo.Builder taskInfoBuilder = Protos.TaskInfo.newBuilder()
                .addAllResources(resources)
                .setName(TaskSpec.getInstanceName(podInstance, taskSpec))
                .setTaskId(CommonIdUtils.emptyTaskId())
                .setSlaveId(CommonIdUtils.emptyAgentId());
        SchedulerLabelWriter labelWriter = new SchedulerLabelWriter()
                .setTargetConfiguration(targetConfigurationId)
                .setGoalState(taskSpec.getGoal())
                .setType(podInstance.getPod().getType())
                .setIndex(podInstance.getIndex());
        if (isTransient) {
            labelWriter.setTransient();
        }
        taskInfoBuilder.setLabels(labelWriter.toProto());

        SchedulerTaskEnvWriter envWriter = new SchedulerTaskEnvWriter();
        if (taskSpec.getCommand().isPresent()) {
            envWriter.setEnv(
                    serviceName,
                    podInstance,
                    taskSpec,
                    taskSpec.getCommand().get(),
                    CONFIG_TEMPLATE_DOWNLOAD_DIR,
                    parameters);
            taskInfoBuilder.getCommandBuilder().setEnvironment(envWriter.getTaskEnv());
        }

        if (taskSpec.getDiscovery().isPresent()) {
            taskInfoBuilder.setDiscovery(getDiscoveryInfo(taskSpec.getDiscovery().get(), podInstance.getIndex()));
        }

        setHealthCheck(taskInfoBuilder, taskSpec, envWriter);
        setReadinessCheck(taskInfoBuilder, taskSpec, envWriter);

        return taskInfoBuilder.build();
    }

    private static Collection<ResourceRequirement> getResourceRequirements(
            TaskSpec taskSpec, Collection<Protos.Resource> resources) {
        ResourceSet resourceSet = taskSpec.getResourceSet();

        Map<String, Protos.Resource> resourceMap =
                resources.stream()
                        .filter(resource -> !resource.hasDisk())
                        .collect(Collectors.toMap(r -> r.getName(), Function.identity()));

        Map<String, Protos.Resource> volumeMap =
                resources.stream()
                        .filter(resource -> resource.hasDisk())
                        .filter(resource -> resource.getDisk().hasVolume())
                        .collect(Collectors.toMap(
                                r -> r.getDisk().getVolume().getContainerPath(),
                                Function.identity()));

        List<ResourceRequirement> resourceRequirements = new ArrayList<>();

        for (ResourceSpec r : resourceSet.getResources()) {
            resourceRequirements.add(r.getResourceRequirement(resourceMap.get(r.getName())));
        }

        for (VolumeSpec v : resourceSet.getVolumes()) {
            resourceRequirements.add(v.getResourceRequirement(volumeMap.get(v.getContainerPath())));
        }

        return resourceRequirements;
    }

    private ExecutorRequirement getExecutorRequirement(
            PodInstance podInstance,
            String serviceName,
            UUID targetConfigurationId) throws InvalidRequirementException {
        List<Protos.TaskInfo> podTasks = TaskUtils.getPodTasks(podInstance, stateStore);

        for (Protos.TaskInfo taskInfo : podTasks) {
            Optional<Protos.TaskStatus> taskStatusOptional = stateStore.fetchStatus(taskInfo.getName());
            if (taskStatusOptional.isPresent()
                    && taskStatusOptional.get().getState() == Protos.TaskState.TASK_RUNNING) {
                LOGGER.info(
                        "Reusing executor from task '{}': {}",
                        taskInfo.getName(),
                        TextFormat.shortDebugString(taskInfo.getExecutor()));
                return ExecutorRequirement.create(taskInfo.getExecutor());
            }
        }

        LOGGER.info("Creating new executor for pod {}, as no RUNNING tasks were found", podInstance.getName());
        return ExecutorRequirement.create(getNewExecutorInfo(
                podInstance.getPod(), serviceName, targetConfigurationId, schedulerFlags));
    }

    @Override
    public OfferRequirement getExistingOfferRequirement(PodInstanceRequirement podInstanceRequirement)
            throws InvalidRequirementException {
        PodInstance podInstance = podInstanceRequirement.getPodInstance();
        List<TaskSpec> taskSpecs = podInstance.getPod().getTasks().stream()
                .filter(taskSpec -> podInstanceRequirement.getTasksToLaunch().contains(taskSpec.getName()))
                .collect(Collectors.toList());
        List<TaskRequirement> taskRequirements = new ArrayList<>();

        for (TaskSpec taskSpec : taskSpecs) {
            Optional<Protos.TaskInfo> taskInfoOptional =
                    stateStore.fetchTask(TaskSpec.getInstanceName(podInstance, taskSpec));
            Collection<Protos.Resource> taskResources;
            if (taskInfoOptional.isPresent()) {
                taskResources = new ArrayList<>();
                List<Protos.Resource> resourcesToUpdate = new ArrayList<>();
                for (Protos.Resource resource : taskInfoOptional.get().getResourcesList()) {
                    if (resource.hasDisk()) {
                        // Disk resources may not be changed:
                        taskResources.add(resource);
                    } else {
                        resourcesToUpdate.add(resource);
                    }
                }

                Map<String, Protos.Resource> oldResourceMap = resourcesToUpdate.stream()
                        .collect(Collectors.toMap(resource -> resource.getName(), resource -> resource));
                List<Protos.Resource> updatedResources = new ArrayList<>();
                for (ResourceSpec resourceSpec : taskSpec.getResourceSet().getResources()) {
                    Protos.Resource oldResource = oldResourceMap.get(resourceSpec.getName());
                    if (oldResource != null) {
                        // Update existing resource
                        try {
                            updatedResources.add(ResourceUtils.updateResource(oldResource, resourceSpec));
                        } catch (IllegalArgumentException e) {
                            LOGGER.error("Failed to update Resources with exception: ", e);
                            // On failure to update resources, keep the old resources.
                            updatedResources.add(oldResource);
                        }
                    } else {
                        // Add newly added resource
                        updatedResources.add(ResourceUtils.getExpectedResource(resourceSpec));
                    }
                }

                taskResources.addAll(coalesceResources(updatedResources));
            } else {
                Collection<String> tasksWithResourceSet = podInstance.getPod().getTasks().stream()
                        .filter(taskSpec1 -> taskSpec.getResourceSet().getId().equals(taskSpec1.getResourceSet().getId()))
                        .map(taskSpec1 -> TaskSpec.getInstanceName(podInstance, taskSpec1))
                        .distinct()
                        .collect(Collectors.toList());

                Collection<TaskInfo> taskInfosForPod = stateStore.fetchTasks().stream()
                        .filter(taskInfo -> {
                            try {
                                return TaskUtils.isSamePodInstance(taskInfo, podInstance);
                            } catch (TaskException e) {
                                return false;
                            }
                        })
                        .collect(Collectors.toList());

                Optional<TaskInfo> matchingTaskInfoOptional = taskInfosForPod.stream()
                        .filter(taskInfo -> tasksWithResourceSet.contains(taskInfo.getName()))
                        .findFirst();

                if (matchingTaskInfoOptional.isPresent()) {
                    taskResources = matchingTaskInfoOptional.get().getResourcesList();
                } else {
                    LOGGER.error("Failed to find a Task with resource set: {}", taskSpec.getResourceSet().getId());
                    taskResources = Collections.emptyList();
                }
            }

            Protos.TaskInfo taskInfo = createTaskInfo(
                    podInstance,
                    taskSpec,
                    podInstanceRequirement.getParameters(),
                    taskResources,
                    serviceName,
                    targetConfigurationId,
                    false);
            taskRequirements.add(new TaskRequirement(
                    taskInfo, getResourceRequirements(taskSpec, taskInfo.getResourcesList())));
        }
        validateTaskRequirements(taskRequirements);

        return OfferRequirement.create(
                podInstance.getPod().getType(),
                podInstance.getIndex(),
                taskRequirements,
                ExecutorRequirement.create(getExecutor(podInstance, serviceName, targetConfigurationId)),
                // Do not add placement rules to getExistingOfferRequirement
                Optional.empty());
    }

    private static Protos.DiscoveryInfo getDiscoveryInfo(DiscoverySpec discoverySpec, int index) {
        Protos.DiscoveryInfo.Builder builder = Protos.DiscoveryInfo.newBuilder();
        if (discoverySpec.getPrefix().isPresent()) {
            builder.setName(String.format("%s-%d", discoverySpec.getPrefix().get(), index));
        }
        if (discoverySpec.getVisibility().isPresent()) {
            builder.setVisibility(discoverySpec.getVisibility().get());
        } else {
            builder.setVisibility(Protos.DiscoveryInfo.Visibility.CLUSTER);
        }

        return builder.build();
    }

    private static void validateTaskRequirements(List<TaskRequirement> taskRequirements)
            throws InvalidRequirementException {
        if (taskRequirements.isEmpty()) {
            throw new InvalidRequirementException("Failed to generate any TaskRequirements.");
        }

        String taskType = "";
        try {
            taskType = new SchedulerLabelReader(taskRequirements.get(0).getTaskInfo()).getType();
        } catch (TaskException e) {
            throw new InvalidRequirementException(e);
        }

        for (TaskRequirement taskRequirement : taskRequirements) {
            try {
                String localTaskType = new SchedulerLabelReader(taskRequirement.getTaskInfo()).getType();
                if (!localTaskType.equals(taskType)) {
                    throw new InvalidRequirementException("TaskRequirements must have TaskTypes.");
                }
            } catch (TaskException e) {
                throw new InvalidRequirementException(e);
            }
        }
    }

    private static Collection<Protos.Resource> getNewResources(TaskSpec taskSpec)
            throws InvalidRequirementException {
        ResourceSet resourceSet = taskSpec.getResourceSet();
        Collection<Protos.Resource> resources = new ArrayList<>();

        for (ResourceSpec resourceSpec : resourceSet.getResources()) {
            resources.add(ResourceUtils.getExpectedResource(resourceSpec));
        }

        for (VolumeSpec volumeSpec : resourceSet.getVolumes()) {
            switch (volumeSpec.getType()) {
                case ROOT:
                    resources.add(
                            ResourceUtils.getDesiredRootVolume(
                                    volumeSpec.getRole(),
                                    volumeSpec.getPrincipal(),
                                    volumeSpec.getValue().getScalar().getValue(),
                                    volumeSpec.getContainerPath()));
                    break;
                case MOUNT:
                    resources.add(
                            ResourceUtils.getDesiredMountVolume(
                                    volumeSpec.getRole(),
                                    volumeSpec.getPrincipal(),
                                    volumeSpec.getValue().getScalar().getValue(),
                                    volumeSpec.getContainerPath()));
                    break;
                default:
                    LOGGER.error("Encountered unsupported disk type: " + volumeSpec.getType());
            }
        }

        return coalesceResources(resources);
    }

    private static List<Protos.Resource> coalesceResources(Collection<Protos.Resource> resources) {
        List<Protos.Resource> portResources = new ArrayList<>();
        List<Protos.Resource> otherResources = new ArrayList<>();
        for (Protos.Resource r : resources) {
            if (isPortResource(r)) {
                portResources.add(r);
            } else {
                otherResources.add(r);
            }
        }

        if (!portResources.isEmpty()) {
            otherResources.add(coalescePorts(portResources));
        }

        return otherResources;
    }

    private static boolean isPortResource(Protos.Resource resource) {
        return resource.getName().equals(Constants.PORTS_RESOURCE_TYPE);
    }

    private static Protos.Resource coalescePorts(List<Protos.Resource> resources) {
        // Within the SDK, each port is handled as its own resource, since they can have extra meta-data attached, but
        // we can't have multiple "ports" resources on a task info, so we combine them here. Since ports are also added
        // back onto TaskInfos during the evaluation stage (since they may be dynamic) we actually just clear the ranges
        // from that resource here to make the bookkeeping easier.
        // TODO(mrb): instead of clearing ports, keep them in OfferRequirement and build up actual TaskInfos elsewhere
        return resources.get(0).toBuilder().clearRanges().build();
    }

    /**
     * Returns the ExecutorInfo of a PodInstance if it is still running so it may be re-used, otherwise
     * it returns a new ExecutorInfo.
     * @param podInstance A PodInstance
     * @return The appropriate ExecutorInfo.
     */
    private Protos.ExecutorInfo getExecutor(
            PodInstance podInstance, String serviceName, UUID targetConfigurationId) {
        List<Protos.TaskInfo> podTasks = TaskUtils.getPodTasks(podInstance, stateStore);

        for (Protos.TaskInfo taskInfo : podTasks) {
            Optional<Protos.TaskStatus> taskStatusOptional = stateStore.fetchStatus(taskInfo.getName());
            if (taskStatusOptional.isPresent()
                    && taskStatusOptional.get().getState() == Protos.TaskState.TASK_RUNNING) {
                LOGGER.info(
                        "Reusing executor from task '{}': {}",
                        taskInfo.getName(),
                        TextFormat.shortDebugString(taskInfo.getExecutor()));
                return taskInfo.getExecutor();
            }
        }

        LOGGER.info("Creating new executor for pod {}, as no RUNNING tasks were found", podInstance.getName());
        return getNewExecutorInfo(podInstance.getPod(), serviceName, targetConfigurationId, schedulerFlags);
    }

    private static Protos.ContainerInfo getContainerInfo(PodSpec podSpec) {
        if (!podSpec.getImage().isPresent() && podSpec.getNetworks().isEmpty() && podSpec.getRLimits().isEmpty()) {
            return null;
        }

        Protos.ContainerInfo.Builder containerInfo = Protos.ContainerInfo.newBuilder()
                .setType(Protos.ContainerInfo.Type.MESOS);

        if (podSpec.getImage().isPresent()) {
            containerInfo.getMesosBuilder()
            .setImage(Protos.Image.newBuilder()
                    .setType(Protos.Image.Type.DOCKER)
                    .setDocker(Protos.Image.Docker.newBuilder()
                            .setName(podSpec.getImage().get())));
        }

        if (!podSpec.getNetworks().isEmpty()) {
            containerInfo.addAllNetworkInfos(
                    podSpec.getNetworks().stream().map(n -> getNetworkInfo(n)).collect(Collectors.toList()));
        }

        if (!podSpec.getRLimits().isEmpty()) {
            containerInfo.setRlimitInfo(getRLimitInfo(podSpec.getRLimits()));
        }

        return containerInfo.build();
    }

    private static Protos.NetworkInfo getNetworkInfo(NetworkSpec networkSpec) {
        LOGGER.info("Loading NetworkInfo for network named \"{}\"", networkSpec.getName());
        Protos.NetworkInfo.Builder netInfoBuilder = Protos.NetworkInfo.newBuilder();
        netInfoBuilder.setName(networkSpec.getName());

        if (!networkSpec.getPortMappings().isEmpty()) {
            for (Map.Entry<Integer, Integer> e : networkSpec.getPortMappings().entrySet()) {
                Integer hostPort = e.getKey();
                Integer containerPort = e.getValue();
                netInfoBuilder.addPortMappings(Protos.NetworkInfo.PortMapping.newBuilder()
                        .setHostPort(hostPort)
                        .setContainerPort(containerPort)
                        .build());
            }
        }

        if (!networkSpec.getNetgroups().isEmpty()) {
            netInfoBuilder.addAllGroups(networkSpec.getNetgroups());
        }

        if (!networkSpec.getIpAddresses().isEmpty()) {
            for (String ipAddressString : networkSpec.getIpAddresses()) {
                netInfoBuilder.addIpAddresses(
                        Protos.NetworkInfo.IPAddress.newBuilder()
                                .setIpAddress(ipAddressString)
                                .setProtocol(Protos.NetworkInfo.Protocol.IPv4)
                                .build());
            }
        }

        return netInfoBuilder.build();
    }

    private static Protos.RLimitInfo getRLimitInfo(Collection<RLimit> rlimits) {
        Protos.RLimitInfo.Builder rLimitInfoBuilder = Protos.RLimitInfo.newBuilder();

        for (RLimit rLimit : rlimits) {
            Optional<Long> soft = rLimit.getSoft();
            Optional<Long> hard = rLimit.getHard();
            Protos.RLimitInfo.RLimit.Builder rLimitsBuilder = Protos.RLimitInfo.RLimit.newBuilder()
                    .setType(rLimit.getEnum());

            // RLimit itself validates that both or neither of these are present.
            if (soft.isPresent() && hard.isPresent()) {
                rLimitsBuilder.setSoft(soft.get()).setHard(hard.get());
            }
            rLimitInfoBuilder.addRlimits(rLimitsBuilder);
        }

        return rLimitInfoBuilder.build();
    }

    private static Protos.ExecutorInfo getNewExecutorInfo(
            PodSpec podSpec,
            String serviceName,
            UUID targetConfigurationId,
            SchedulerFlags schedulerFlags) throws IllegalStateException {
        Protos.ExecutorInfo.Builder executorInfoBuilder = Protos.ExecutorInfo.newBuilder()
                .setName(podSpec.getType())
                .setExecutorId(Protos.ExecutorID.newBuilder().setValue("").build()); // Set later by ExecutorRequirement
        // Populate ContainerInfo with the appropriate information from PodSpec
        Protos.ContainerInfo containerInfo = getContainerInfo(podSpec);
        if (containerInfo != null) {
            executorInfoBuilder.setContainer(containerInfo);
        }

        // command and user:
        Protos.CommandInfo.Builder executorCommandBuilder = executorInfoBuilder.getCommandBuilder().setValue(
                "export LD_LIBRARY_PATH=$MESOS_SANDBOX/libmesos-bundle/lib:$LD_LIBRARY_PATH && " +
                "export MESOS_NATIVE_JAVA_LIBRARY=$(ls $MESOS_SANDBOX/libmesos-bundle/lib/libmesos-*.so) && " +
                "export JAVA_HOME=$(ls -d $MESOS_SANDBOX/jre*/) && " +
                "$MESOS_SANDBOX/executor/bin/executor");

        if (podSpec.getUser().isPresent()) {
            executorCommandBuilder.setUser(podSpec.getUser().get());
        }

        // Required URIs from the scheduler environment:
        executorCommandBuilder.addUrisBuilder().setValue(schedulerFlags.getLibmesosURI());
        executorCommandBuilder.addUrisBuilder().setValue(schedulerFlags.getJavaURI());

        // Any URIs defined in PodSpec itself.
        for (URI uri : podSpec.getUris()) {
            executorCommandBuilder.addUrisBuilder().setValue(uri.toString());
        }

        // Finally any URIs for config templates defined in TaskSpecs.
        for (TaskSpec taskSpec : podSpec.getTasks()) {
            for (ConfigFileSpec config : taskSpec.getConfigFiles()) {
                executorCommandBuilder.addUrisBuilder()
                        .setValue(ArtifactResource.getTemplateUrl(
                                serviceName,
                                targetConfigurationId,
                                podSpec.getType(),
                                taskSpec.getName(),
                                config.getName()))
                        .setOutputFile(CONFIG_TEMPLATE_DOWNLOAD_DIR + config.getName())
                        .setExtract(false);
            }
        }

        return executorInfoBuilder.build();
    }

    private static void setHealthCheck(
            Protos.TaskInfo.Builder taskInfo, TaskSpec taskSpec, SchedulerTaskEnvWriter envWriter) {
        if (!taskSpec.getHealthCheck().isPresent()) {
            LOGGER.debug("No health check defined for taskSpec: {}", taskSpec.getName());
            return;
        }
        HealthCheckSpec healthCheckSpec = taskSpec.getHealthCheck().get();
        Protos.HealthCheck.Builder builder = Protos.HealthCheck.newBuilder()
                .setDelaySeconds(healthCheckSpec.getDelay())
                .setIntervalSeconds(healthCheckSpec.getInterval())
                .setTimeoutSeconds(healthCheckSpec.getTimeout())
                .setConsecutiveFailures(healthCheckSpec.getMaxConsecutiveFailures())
                .setGracePeriodSeconds(healthCheckSpec.getGracePeriod());
        builder.getCommandBuilder()
                .setValue(healthCheckSpec.getCommand())
                .setEnvironment(envWriter.getHealthCheckEnv());
        taskInfo.setHealthCheck(builder);
    }

    private static void setReadinessCheck(
            Protos.TaskInfo.Builder taskInfoBuilder, TaskSpec taskSpec, SchedulerTaskEnvWriter envWriter) {
        if (!taskSpec.getReadinessCheck().isPresent()) {
            LOGGER.debug("No readiness check defined for taskSpec: {}", taskSpec.getName());
            return;
        }
        ReadinessCheckSpec readinessCheckSpec = taskSpec.getReadinessCheck().get();
        Protos.HealthCheck.Builder builder = Protos.HealthCheck.newBuilder()
                .setDelaySeconds(readinessCheckSpec.getDelay())
                .setIntervalSeconds(readinessCheckSpec.getInterval())
                .setTimeoutSeconds(readinessCheckSpec.getTimeout())
                .setConsecutiveFailures(0)
                .setGracePeriodSeconds(0);
        builder.getCommandBuilder()
                .setValue(readinessCheckSpec.getCommand())
                .setEnvironment(envWriter.getHealthCheckEnv());
        taskInfoBuilder.setLabels(new SchedulerLabelWriter(taskInfoBuilder)
                .setReadinessCheck(builder.build())
                .toProto());
    }
}
