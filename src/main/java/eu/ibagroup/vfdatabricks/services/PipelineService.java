/*
 * Copyright (c) 2021 IBA Group, a.s. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.ibagroup.vfdatabricks.services;


import eu.ibagroup.vfdatabricks.config.ApplicationConfigurationProperties;
import eu.ibagroup.vfdatabricks.dto.Constants;
import eu.ibagroup.vfdatabricks.dto.GraphDto;
import eu.ibagroup.vfdatabricks.dto.jobs.CommonDto;
import eu.ibagroup.vfdatabricks.dto.jobs.JobDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksJobRunDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksJobStorageRunDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksJobTask;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksRunIdDto;
import eu.ibagroup.vfdatabricks.dto.pipelines.PipelineDto;
import eu.ibagroup.vfdatabricks.dto.pipelines.PipelineOverviewDto;
import eu.ibagroup.vfdatabricks.dto.pipelines.PipelineOverviewListDto;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static eu.ibagroup.vfdatabricks.dto.Constants.*;
import static eu.ibagroup.vfdatabricks.services.JobService.TIMEOUT;


@Service
@Slf4j
public class PipelineService {
    private static final String BASE_URL = "%s/%s/%s/%s/pipeline";
    private static final String URL_STRING_FORMAT = BASE_URL + "/%s";

    private final RestTemplate restTemplate;
    private final ApplicationConfigurationProperties appProperties;
    private final MapperService mapperService;
    private final DatabricksAPIService databricksApiService;
    private final AsyncJobCheckService asyncJobCheckService;
    private final JobService jobService;
    private final CronCheckService cronCheckService;
    private final SchedulerService schedulerService;

    public PipelineService(@Qualifier("authRestTemplate") RestTemplate restTemplate,
                           ApplicationConfigurationProperties appProperties,
                           MapperService mapperService,
                           JobService jobService,
                           DatabricksAPIService databricksApiService,
                           AsyncJobCheckService asyncJobCheckService,
                           CronCheckService cronCheckService,
                           SchedulerService schedulerService) {
        this.restTemplate = restTemplate;
        this.appProperties = appProperties;
        this.mapperService = mapperService;
        this.jobService = jobService;
        this.databricksApiService = databricksApiService;
        this.asyncJobCheckService = asyncJobCheckService;
        this.cronCheckService = cronCheckService;
        this.schedulerService = schedulerService;
    }

    private static String getTaskKey(GraphDto.NodeDto node, String parentId) {
        return MapperService.toAlphaNumeric(node.getValue().get("name"))
                + PipelineTransformer.SEPARATOR_CHAR
                + PipelineTransformer.getId(parentId, node.getId());
    }

    public String create(String projectId, PipelineDto pipelineRequestDto) {
        String url = String.format(BASE_URL,
                appProperties.getJobStorage().getHost(),
                CONTEXT_PATH,
                JOB_STORAGE_API,
                projectId);
        ResponseEntity<String> created = restTemplate.postForEntity(
                url,
                pipelineRequestDto,
                String.class
        );
        return Objects.requireNonNull(created.getBody());
    }

    public PipelineDto getByIdAndFetchStatus(String projectId, String id) {
        PipelineDto body = getById(projectId, id);
        List<PipelineDto> pipelines = Collections.singletonList(body);
        checkAndUpdateStatus(projectId, pipelines);
        cronCheckService.checkAndUpdateCron(projectId, pipelines);
        return body;
    }

    PipelineDto getById(String projectId, String id) {
        ResponseEntity<PipelineDto> response = restTemplate.getForEntity(
                String.format(URL_STRING_FORMAT,
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        projectId,
                        id),
                PipelineDto.class
        );
        return Objects.requireNonNull(response.getBody());
    }

    public void copy(String projectId, String pipelineId) {
        restTemplate.postForEntity(
                String.format(String.format("%s/copy", URL_STRING_FORMAT),
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        projectId,
                        pipelineId),
                null,
                Void.class
        );
    }

    public void update(String projectId, String id, PipelineDto pipelineRequestDto) {
        restTemplate.put(
                String.format(URL_STRING_FORMAT,
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        projectId,
                        id),
                pipelineRequestDto
        );
    }

    public void patch(String projectId, CommonDto pipelineRequestDto) {
        restTemplate.patchForObject(
                String.format(URL_STRING_FORMAT,
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        projectId,
                        pipelineRequestDto.getId()),
                pipelineRequestDto,
                Void.class
        );
    }

    public void delete(String projectId, String id) {
        restTemplate.delete(
                String.format(URL_STRING_FORMAT,
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        projectId,
                        id)
        );

        schedulerService.deleteCron(projectId, id);

    }


    public PipelineOverviewListDto getAll(String projectId) {
        ResponseEntity<PipelineOverviewListDto> response = restTemplate.getForEntity(
                String.format(BASE_URL,
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        projectId),
                PipelineOverviewListDto.class
        );
        PipelineOverviewListDto body = Objects.requireNonNull(response.getBody());
        checkAndUpdateStatus(projectId, body.getPipelines());
        cronCheckService.checkAndUpdateCron(projectId, body.getPipelines());
        return body;
    }

    public void run(String projectId, String id) {
        PipelineDto pipelineDto = getByIdAndFetchStatus(projectId, id);

        if (StringUtils.equalsAnyIgnoreCase(pipelineDto.getStatus(), PENDING_VF_STATUS, RUNNING_VF_STATUS)) {
            throw new IllegalStateException("Already started");
        }

        DatabricksJobStorageRunDto databricksJobStorageRunDto = mapPipelineToDatabricksJobStorageRun(
                projectId,
                pipelineDto
        );

        pipelineDto.setStatus(PENDING_VF_STATUS);
        pipelineDto.setRunId(0);
        pipelineDto.setProgress(0);
        pipelineDto.setJobsStatuses(null);
        update(projectId, id, pipelineDto);

        databricksApiService.runJob(projectId, databricksJobStorageRunDto, pipelineDto.getParams())
                .whenComplete((DatabricksRunIdDto runIdDto, Throwable exception) -> {
                    if (exception != null) {
                        LOGGER.error("Pipeline run failed: {}", exception.getMessage(), exception);
                        pipelineDto.setStatus(Constants.FAILED_VF_STATUS);
                    } else {
                        pipelineDto.setRunId(runIdDto.getRunId());
                        pipelineDto.setStatus(PENDING_VF_STATUS);
                        pipelineDto.setFinishedAt(null);
                    }
                    update(projectId, id, pipelineDto);
                });

    }

    private DatabricksJobStorageRunDto mapPipelineToDatabricksJobStorageRun(String projectId, PipelineDto pipelineDto) {
        PipelineTransformer transformer = new PipelineTransformer(
                pipelineId -> getGraphDto(projectId, pipelineId),
                (String jobId) -> {
                    JobDto jobDto = jobService.getJob(projectId, jobId);
                    return mapperService.mapJobDtoToDatabricksJobTask(jobDto, projectId);
                }
        );
        List<DatabricksJobTask> tasks = transformer.transform(GraphDto.parseGraph(pipelineDto.getDefinition()));

        return DatabricksJobStorageRunDto.builder()
                .runName(pipelineDto.getName())
                .tasks(tasks)
                .emailNotifications(mapperService.mapEmailNotifications(pipelineDto.getParams().getEmail()))
                .build();
    }

    private GraphDto getGraphDto(String projectId, String pipelineId) {
        PipelineDto pipelineDto = this.getById(projectId, pipelineId);
        return GraphDto.parseGraph(pipelineDto.getDefinition());
    }

    @SneakyThrows
    private void checkAndUpdateStatus(String projectId, List<? extends PipelineOverviewDto> pipelines) {
        CompletableFuture<?>[] futures = pipelines.stream()
                .map(job -> checkAndUpdateStatus(projectId, job))
                .toArray(CompletableFuture[]::new);
        try {
            CompletableFuture.allOf(futures).get(TIMEOUT, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException e) {
            LOGGER.warn(e.getMessage(), e);
        }
    }

    private CompletableFuture<Void> checkAndUpdateStatus(String projectId, PipelineOverviewDto pipeline) {
        return asyncJobCheckService.checkAndUpdateStatus(projectId, pipeline)
                .thenAccept((DatabricksJobRunDto result) -> {
                    if (result != null) {
                        updateJobStatuses(projectId, pipeline, result);
                    }
                });
    }

    void updateJobStatuses(String projectId, PipelineOverviewDto pipeline, DatabricksJobRunDto result) {
        // update job statuses
        PipelineDto pipelineDto;
        if (!(pipeline instanceof PipelineDto)) {
            pipelineDto = getById(projectId, pipeline.getId());
        } else {
            pipelineDto = (PipelineDto) pipeline;
        }
        GraphDto graphDto = GraphDto.parseGraph(pipelineDto.getDefinition());
        pipeline.setJobsStatuses(getJobStatuses(projectId, result, graphDto));

        patch(projectId, pipeline);
        // saveHistory
    }

    private Map<String, String> getJobStatuses(String projectId, DatabricksJobRunDto result, GraphDto graphDto) {
        Map<String, String> statusByTaskKey = result.getTasks().stream()
                .collect(Collectors.toMap(
                        DatabricksJobTask::getTaskKey,
                        task -> mapperService.mapStatus(task.getState())
                ));
        MultiValueMap<String, String> taskKeyByNodeId = collectIdsByTaskKey(graphDto, projectId, null);
        return taskKeyByNodeId.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        (Map.Entry<String, List<String>> entry) -> {
                            List<String> taskKeys = entry.getValue();
                            Set<String> statuses = taskKeys.stream()
                                    .map(statusByTaskKey::get)
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toSet());
                            return MapperService.resolveOverallStatus(statuses);
                        }));
    }

    MultiValueMap<String, String> collectIdsByTaskKey(GraphDto graphDto, String projectId, String parentId) {
        MultiValueMap<String, String> result = new LinkedMultiValueMap<>();
        for (GraphDto.NodeDto node : graphDto.getNodes()) {
            String operation = node.getValue().get("operation");
            if ("JOB".equals(operation)) {
                result.add(node.getId(), getTaskKey(node, parentId));
            }
            if ("PIPELINE".equals(operation)) {
                GraphDto pipelineGraph = getGraphDto(projectId, node.getValue().get("pipelineId"));

                MultiValueMap<String, String> children = collectIdsByTaskKey(pipelineGraph, projectId,
                        PipelineTransformer.getId(parentId, node.getId()));
                result.addAll(node.getId(), children.values().stream().flatMap(Collection::stream).toList());
            }
        }
        return result;
    }

    public void terminate(String projectId, String id) {
        stop(projectId, id);
    }

    public void stop(String projectId, String id) {
        PipelineDto pipelineDto = getById(projectId, id);
        databricksApiService.cancelJob(projectId, pipelineDto.getRunId());
    }
}
