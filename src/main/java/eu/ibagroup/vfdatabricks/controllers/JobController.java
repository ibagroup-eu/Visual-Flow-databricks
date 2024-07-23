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

package eu.ibagroup.vfdatabricks.controllers;

import eu.ibagroup.vfdatabricks.dto.jobs.JobDto;
import eu.ibagroup.vfdatabricks.dto.jobs.JobHistoryDto;
import eu.ibagroup.vfdatabricks.dto.jobs.JobOverviewListDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.JobLogDto;
import eu.ibagroup.vfdatabricks.services.JobService;
import eu.ibagroup.vfdatabricks.services.auth.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Job controller class.
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("api/project")
public class JobController {
    private final JobService jobService;
    private final AuthenticationService authenticationService;

    /**
     * Get all jobs in project.
     *
     * @param projectId project id
     * @return ResponseEntity with jobs graphs
     */
    @GetMapping("{projectId}/job")
    public JobOverviewListDto getAll(@PathVariable String projectId) throws InterruptedException {
        LOGGER.info(
            "{} - Receiving all jobs in project '{}'",
            AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
            projectId
        );
        return jobService.getAll(projectId);
    }

    /**
     * Getting job in project by id.
     *
     * @param projectId project id
     * @param id        job id
     * @return ResponseEntity with job graph
     */
    @GetMapping("{projectId}/job/{id}")
    public JobDto get(@PathVariable String projectId, @PathVariable String id) throws InterruptedException {
        LOGGER.info(
                "{} - Receiving job '{}' in project '{}'",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                id,
                projectId
        );
        return jobService.getAndFetchStatus(projectId, id);
    }

    /**
     * Creating new job in project.
     *
     * @param projectId     project id
     * @param jobDto object with name and graph
     * @return ResponseEntity with id of new job
     */
    @PostMapping("{projectId}/job")
    public ResponseEntity<String> create(
        @PathVariable String projectId, @Valid @RequestBody JobDto jobDto) {
        LOGGER.info(
            "{} - Creating new job in project '{}'",
            AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
            projectId
        );
        String id = jobService.create(projectId, jobDto);
        LOGGER.info(
            "{} - Job '{}' in project '{}' successfully created",
            AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
            id,
            projectId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(id);
    }

    /**
     * Updating job in project by id.
     *
     * @param projectId     project id
     * @param id            job id
     * @param jobDto object with name and graph
     */
    @PostMapping("{projectId}/job/{id}")
    public void update(
        @PathVariable String projectId, @PathVariable String id, @Valid @RequestBody JobDto jobDto) {
        LOGGER.info(
            "{} - Updating job '{}' in project '{}'",
            AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
            id,
            projectId
        );
        jobService.update(projectId, id, jobDto);
        LOGGER.info(
            "{} - Job '{}' in project '{}' successfully updated",
            AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
            id,
            projectId
        );
    }

    /**
     * Deleting job in project by id.
     *
     * @param projectId project id
     * @param id        job id
     */
    @DeleteMapping("{projectId}/job/{id}")
    public ResponseEntity<Void> delete(@PathVariable String projectId, @PathVariable String id) {
        LOGGER.info(
            "{} - Deleting '{}' job in project '{}'",
            AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
            id,
            projectId
        );
        jobService.delete(projectId, id);
        LOGGER.info(
            "{} - Job '{}' in project '{}' successfully deleted",
            AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
            id,
            projectId
        );
        return ResponseEntity.noContent().build();
    }

    /**
     * Run job.
     *
     * @param projectId project id
     * @param id        job id
     */
    @PostMapping("{projectId}/job/{id}/run")
    public void run(@PathVariable String projectId, @PathVariable String id) {
        LOGGER.info(
                "{} - Running job '{}' in project '{}'",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                id,
                projectId
        );
        jobService.run(projectId, id);
        LOGGER.info(
                "{} - Job '{}' in project '{}' started",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                id,
                projectId
        );
    }

    /**
     * Stop job.
     *
     * @param projectId project id
     * @param id        job id
     */
    @PostMapping("{projectId}/job/{id}/stop")
    public void stop(@PathVariable String projectId, @PathVariable String id) {
        LOGGER.info(
                "{} - Stopping job '{}' in project '{}'",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                id,
                projectId
        );
        jobService.stop(projectId, id);
        LOGGER.info(
                "{} - Job '{}' in project '{}' successfully stopped",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                id,
                projectId
        );
    }

    /**
     * Getting job history.
     *
     * @param id        job id
     * @return ResponseEntity with list of history objects
     */
    @GetMapping("{projectId}/job/{id}/history")
    public List<JobHistoryDto> getHistory(@PathVariable String projectId, @PathVariable String id) {
        LOGGER.info(
                "{} - Job '{}' in project '{}' history received successfully",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                id, projectId
        );
        return jobService.getJobHistory(id);
    }

    /**
     * Getting job logs.
     *
     * @param projectId project id
     * @param jobId        job id
     * @return ResponseEntity with list of history objects
     */
    @GetMapping("{projectId}/job/{jobId}/logs")
    public List<JobLogDto> getLogs(@PathVariable String projectId, @PathVariable String jobId) {
        LOGGER.info(
                "{} - Job '{}' in project '{}' trying to receive logs",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                jobId, projectId
        );
        return jobService.getJobLogs(projectId, jobId);
    }

    /**
     * Getting job logs.
     *
     * @param projectId project id
     * @param jobId        job id
     * @return ResponseEntity with list of history objects
     */
    @GetMapping("{projectId}/job/{jobId}/logsHistory/{logId}")
    public List<JobLogDto> getLogsHistory(@PathVariable String projectId,
                                   @PathVariable String jobId,
                                   @PathVariable String logId) {
        LOGGER.info(
                "{} - Job '{}' in project '{}' trying to receive logs history by log id '{}'",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                jobId, projectId, logId
        );
        return jobService.getJobLogsHistory(jobId, logId);
    }

    /**
     * Copies job.
     *
     * @param projectId project id
     * @param jobId     job id
     */
    @Operation(summary = "Copy the job", description = "Make a job copy within the same project")
    @PostMapping("{projectId}/job/{jobId}/copy")
    public void copy(@PathVariable String projectId, @PathVariable String jobId) {
        LOGGER.info(
                "{} - Copying job '{}' in project '{}'",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                jobId, projectId
        );
        jobService.copy(projectId, jobId);
    }
}
