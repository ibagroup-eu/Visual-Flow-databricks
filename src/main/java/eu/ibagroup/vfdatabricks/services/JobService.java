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
import eu.ibagroup.vfdatabricks.dto.jobs.CommonDto;
import eu.ibagroup.vfdatabricks.dto.jobs.JobDto;
import eu.ibagroup.vfdatabricks.dto.jobs.JobHistoryDto;
import eu.ibagroup.vfdatabricks.dto.jobs.JobOverviewListDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksJobClusterDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksJobLogDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksJobRunDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksJobStorageRunDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksRunIdDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.JobLogDto;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static eu.ibagroup.vfdatabricks.dto.Constants.*;
import static eu.ibagroup.vfdatabricks.services.UtilsService.*;

/**
 * JobService class.
 */
@Slf4j
@Setter
@Service
@Getter
public class JobService {
    protected static final long TIMEOUT = 3L;
    private final MapperService mapperService;
    private final DatabricksJobService databricksApiService;
    private final ApplicationConfigurationProperties appProperties;
    private final RestTemplate restTemplate;
    private final AsyncJobCheckService asyncJobCheckService;

    public JobService(
            MapperService mapperService,
            DatabricksJobService databricksApiService,
            ApplicationConfigurationProperties appProperties,
            @Qualifier("authRestTemplate") RestTemplate restTemplate,
            AsyncJobCheckService asyncJobCheckService) {
        this.mapperService = mapperService;
        this.databricksApiService = databricksApiService;
        this.appProperties = appProperties;
        this.restTemplate = restTemplate;
        this.asyncJobCheckService = asyncJobCheckService;
    }

    public JobOverviewListDto getAll(final String projectId) throws InterruptedException {
        JobOverviewListDto response = Objects.requireNonNull(
                restTemplate.getForEntity(
                        String.format("%s/%s/%s/%s/job",
                                appProperties.getJobStorage().getHost(),
                                CONTEXT_PATH,
                                JOB_STORAGE_API,
                                projectId),
                        JobOverviewListDto.class
                ).getBody()
        );

        CompletableFuture<?>[] futures = response.getJobs()
                .stream()
                .map(job -> checkAndUpdateStatus(projectId, job))
                .toArray(CompletableFuture[]::new);
        try {
            CompletableFuture.allOf(futures).get(TIMEOUT, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException e) {
            LOGGER.warn(e.getMessage(), e);
        }
        return response;
    }

    private CompletableFuture<Void> checkAndUpdateStatus(String projectId, CommonDto job) {
        return asyncJobCheckService.checkAndUpdateStatus(projectId, job)
                .thenAccept((DatabricksJobRunDto result) -> {
                    if (result != null) {
                        updateJobStatus(projectId, job);
                        if (result.getState().getResultState() != null) {
                            saveHistory(result, job, projectId);
                        }
                    }
                });
    }

    public void updateJobStatus(String projectId, CommonDto job) {
        restTemplate.postForEntity(
                String.format("%s/%s/%s/%s/job/%s/status?status=%s",
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        projectId,
                        job.getId(),
                        job.getStatus()),
                job,
                Void.class
        );
    }

    public JobDto getAndFetchStatus(final String projectId, final String jobId) throws InterruptedException {
        JobDto job = getJob(projectId, jobId);
        CompletableFuture<Void> future = checkAndUpdateStatus(projectId, job);
        try {
            future.get(TIMEOUT, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException e) {
            LOGGER.warn(e.getMessage(), e);
        }

        return job;
    }

    JobDto getJob(String projectId, String jobId) {
        ResponseEntity<JobDto> job = restTemplate.getForEntity(
                String.format(URL_STRING_FORMAT,
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        projectId,
                        jobId),
                JobDto.class
        );
        return job.getBody();
    }

    public String create(final String projectId, @Valid final JobDto jobDto) {
        jobDto.setLastModified(toFormattedString(Instant.now().toEpochMilli()));
        ResponseEntity<String> created = restTemplate.postForEntity(
                String.format("%s/%s/%s/%s/job",
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        projectId),
                jobDto,
                String.class
        );
        return Objects.requireNonNull(created.getBody());
    }

    public void update(final String projectId, final String jobId, @Valid final JobDto jobDto) {
        jobDto.setLastModified(toFormattedString(Instant.now().toEpochMilli()));
        restTemplate.postForEntity(
                String.format(URL_STRING_FORMAT,
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        projectId,
                        jobId),
                jobDto,
                Void.class
        );
    }

    public void delete(final String projectId, final String jobId) {
        restTemplate.delete(String.format("%s/%s/%s/history/job/%s",
                appProperties.getHistoryService().getHost(),
                CONTEXT_PATH_HISTORY,
                HISTORY_SERVICE_API,
                jobId));
        restTemplate.delete(
                String.format(URL_STRING_FORMAT,
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        projectId,
                        jobId),
                Object.class
        );
    }

    public void run(final String projectId, final String jobId) {
        JobDto jobDto = Objects.requireNonNull(
                restTemplate.getForEntity(
                        String.format(URL_STRING_FORMAT,
                                appProperties.getJobStorage().getHost(),
                                CONTEXT_PATH,
                                JOB_STORAGE_API,
                                projectId,
                                jobId),
                        JobDto.class
                ).getBody()
        );

        if (StringUtils.equalsAnyIgnoreCase(jobDto.getStatus(), PENDING_VF_STATUS, RUNNING_VF_STATUS)) {
            throw new IllegalStateException("Already started");
        }

        DatabricksJobStorageRunDto databricksJobStorageRunDto = mapperService.mapRequestToJobRun(jobDto, projectId);

        jobDto.setStatus(PENDING_VF_STATUS);
        jobDto.setRunId(-1L);
        update(projectId, jobId, jobDto);

        databricksApiService.runJob(projectId, databricksJobStorageRunDto, jobDto.getParams())
                .whenComplete((DatabricksRunIdDto runIdDto, Throwable exception) -> {
                    if (exception != null) {
                        LOGGER.error("Job run failed: {}", exception.getMessage(), exception);
                        saveCustomJobLog(jobId,
                                List.of(JobLogDto.builder().level("ERROR").message(exception.getMessage()).build()));
                        update(projectId, jobId, JobDto.builder()
                                .id(jobId)
                                .name(jobDto.getName())
                                .definition(jobDto.getDefinition())
                                .params(jobDto.getParams())
                                .status("Failed")
                                .build());
                    } else {
                        update(projectId, jobId, JobDto.builder()
                                .id(jobId)
                                .name(jobDto.getName())
                                .definition(jobDto.getDefinition())
                                .params(jobDto.getParams())
                                .runId(runIdDto.getRunId())
                                .finishedAt(null)
                                .status(PENDING_VF_STATUS)
                                .build());
                    }
                });

    }

    public void stop(final String projectId, final String jobId) {
        JobDto jobDto = Objects.requireNonNull(restTemplate.getForEntity(
                String.format(URL_STRING_FORMAT,
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        projectId,
                        jobId),
                JobDto.class
        ).getBody());

        databricksApiService.cancelJob(projectId, jobDto.getRunId());
    }

    public void copy(String projectId, String jobId) {
        restTemplate.postForEntity(
                String.format(String.format("%s/copy", URL_STRING_FORMAT),
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        projectId,
                        jobId),
                null,
                Void.class
        );
    }

    public List<JobHistoryDto> getJobHistory(final String jobId) {
        return Arrays.asList(Objects.requireNonNull(restTemplate.getForEntity(
                String.format("%s/%s/%s/history/job/%s",
                        appProperties.getHistoryService().getHost(),
                        CONTEXT_PATH_HISTORY,
                        HISTORY_SERVICE_API,
                        jobId),
                JobHistoryDto[].class).getBody()));

    }


    public List<JobLogDto> getJobLogs(final String projectId, final String jobId) {
        JobDto jobDto = getJob(projectId, jobId);
        if (List.of(SUCCEEDED_VF_STATUS, FAILED_VF_STATUS).contains(jobDto.getStatus())) {
            return Arrays.asList(Objects.requireNonNull(restTemplate.getForEntity(
                    String.format("%s/%s/%s/history/job/%s/log/last",
                            appProperties.getHistoryService().getHost(),
                            CONTEXT_PATH_HISTORY,
                            HISTORY_SERVICE_API,
                            jobId),
                    JobLogDto[].class).getBody()));
        } else {
            return getLogsFromDatabricks(projectId, jobId);
        }
    }

    public List<JobLogDto> getLogsFromDatabricks(String projectId, String jobId) {
        JobDto jobDto = getJob(projectId, jobId);
        DatabricksJobClusterDto databricksJobClusterDto = databricksApiService.getClusterInfo(projectId,
                jobDto.getRunId());
        String clusterId = databricksJobClusterDto.getTasks().get(0).getClusterInstance().getClusterId();
        try {
            DatabricksJobLogDto databricksJobLogDto = databricksApiService.getJobLogs(projectId, clusterId);
            return getParsedDBLogs(decodeFromBase64(databricksJobLogDto.getData()));
        } catch (RuntimeException e) {
            LOGGER.info("Error:", e);
            LOGGER.info("No provided logs from Databricks");
        }
        return List.of();
    }


    public void saveHistory(DatabricksJobRunDto runDto, CommonDto job, String projectId) {
        String logId = saveJobLogs(projectId, job.getId());
        JobHistoryDto jobHistoryDto = JobHistoryDto.builder()
                .jobId(job.getId())
                .jobName(job.getName())
                .startedAt(toFormattedString(runDto.getStartTime()))
                .finishedAt(toFormattedString(runDto.getEndTime()))
                .startedBy(runDto.getCreatorUserName())
                .type(JOB_TYPE)
                .status(mapperService.mapStatus(runDto.getState()))
                .logId(logId)
                .build();
        restTemplate.postForEntity(
                String.format("%s/%s/%s/history/job",
                        appProperties.getHistoryService().getHost(),
                        CONTEXT_PATH_HISTORY,
                        HISTORY_SERVICE_API),
                jobHistoryDto,
                String.class
        );
    }

    public String saveJobLogs(String projectId, String jobId) {
        List<JobLogDto> logs = getLogsFromDatabricks(projectId, jobId);
        return restTemplate.postForEntity(
                String.format("%s/%s/%s/history/job/%s/log",
                        appProperties.getHistoryService().getHost(),
                        CONTEXT_PATH_HISTORY,
                        HISTORY_SERVICE_API,
                        jobId),
                logs,
                String.class
        ).getBody();
    }

    public void saveCustomJobLog(String jobId, List<JobLogDto> logs) {
        restTemplate.postForEntity(
                String.format("%s/%s/%s/history/job/%s/log",
                        appProperties.getHistoryService().getHost(),
                        CONTEXT_PATH_HISTORY,
                        HISTORY_SERVICE_API,
                        jobId),
                logs,
                String.class
        );
    }

    public List<JobLogDto> getJobLogsHistory(String jobId, String logId) {
        return Arrays.asList(Objects.requireNonNull(restTemplate.getForEntity(
                String.format("%s/%s/%s/history/job/%s/log/%s",
                        appProperties.getHistoryService().getHost(),
                        CONTEXT_PATH_HISTORY,
                        HISTORY_SERVICE_API,
                        jobId,
                        logId),
                JobLogDto[].class).getBody()));
    }

}
