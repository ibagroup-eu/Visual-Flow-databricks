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

import eu.ibagroup.vfdatabricks.dto.jobs.databricks.JobLogDto;
import eu.ibagroup.vfdatabricks.dto.pipelines.CronPipelineDto;
import eu.ibagroup.vfdatabricks.dto.pipelines.PipelineDto;
import eu.ibagroup.vfdatabricks.dto.pipelines.PipelineHistoryResponseDto;
import eu.ibagroup.vfdatabricks.dto.pipelines.PipelineOverviewListDto;
import eu.ibagroup.vfdatabricks.services.HistoryService;
import eu.ibagroup.vfdatabricks.services.PipelineService;
import eu.ibagroup.vfdatabricks.services.SchedulerService;
import eu.ibagroup.vfdatabricks.services.auth.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Manage requests for pipelines.
 */
@Slf4j
@Tag(name = "Pipeline API", description = "Manage pipelines")
@RequiredArgsConstructor
@RequestMapping("api/project")
@RestController
public class PipelineController {

    private final PipelineService pipelineService;
    private final HistoryService historyService;
    private final AuthenticationService authenticationService;
    private final SchedulerService schedulerService;

    /**
     * Create pipeline.
     *
     * @param projectId          project id
     * @param pipelineRequestDto id and graph for pipeline
     * @return ResponseEntity
     */
    @Operation(summary = "Create a new pipeline", description = "Create a new pipeline in the project",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Id of a new pipeline")})
    @PostMapping(value = "{projectId}/pipeline")
    public ResponseEntity<String> create(
            @PathVariable String projectId, @Valid @RequestBody PipelineDto pipelineRequestDto) {
        LOGGER.info(
                "{} - Creating pipeline in project '{}'",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                projectId);
        String id = pipelineService.create(projectId, pipelineRequestDto);
        LOGGER.info(
                "{} - Pipeline '{}' in project '{}' successfully created",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                id,
                projectId);
        return ResponseEntity.status(HttpStatus.CREATED).body(id);
    }

    /**
     * Get pipeline.
     *
     * @param projectId project id
     * @param id        pipeline id
     * @return pipeline graph
     */
    @Operation(summary = "Get information about the pipeline", description = "Fetch pipeline's structure by id")
    @GetMapping(value = "{projectId}/pipeline/{id}")
    public PipelineDto get(@PathVariable String projectId, @PathVariable String id) {
        LOGGER.info(
                "{} - Receiving pipeline '{}' in project '{}'",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                id,
                projectId);
        return pipelineService.getByIdAndFetchStatus(projectId, id);
    }

    /**
     * Update pipeline.
     *
     * @param projectId          project id
     * @param id                 current pipeline id
     * @param pipelineRequestDto new id and graph for pipeline
     */
    @PostMapping(value = "{projectId}/pipeline/{id}")
    @Operation(summary = "Update existing pipeline", description = "Update existing pipeline with a new structure")
    public void update(
            @PathVariable String projectId,
            @PathVariable String id,
            @Valid @RequestBody PipelineDto pipelineRequestDto) {
        LOGGER.info(
                "{} - Updating pipeline '{}' in project '{}'",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                id,
                projectId);
        pipelineService.update(projectId, id, pipelineRequestDto);
        LOGGER.info(
                "{} - Pipeline '{}' in project '{}' successfully updated",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                id,
                projectId);

    }

    /**
     * Delete pipeline.
     *
     * @param projectId project id
     * @param id        pipeline id
     */
    @Operation(summary = "Delete the pipeline", description = "Delete existing pipeline", responses =
            {@ApiResponse(responseCode = "204", description = "Indicates successful pipeline deletion")})
    @DeleteMapping(value = "{projectId}/pipeline/{id}")
    public ResponseEntity<Void> delete(@PathVariable String projectId, @PathVariable String id) {
        LOGGER.info(
                "{} - Deleting pipeline '{}' in project '{}'",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                id,
                projectId);
        pipelineService.delete(projectId, id);
        LOGGER.info(
                "{} - Pipeline '{}' in project '{}' successfully deleted",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                id,
                projectId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get all pipelines in project.
     *
     * @param projectId project id
     * @return ResponseEntity with jobs graphs
     */
    @Operation(summary = "Get all pipelines in a project", description = "Get information about all pipelines in" +
            " a project")
    @GetMapping("{projectId}/pipeline")
    public PipelineOverviewListDto getAll(@PathVariable String projectId) {
        LOGGER.info(
                "{} - Receiving all pipelines in project '{}'",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                projectId);
        return pipelineService.getAll(projectId);
    }

    /**
     * Run pipeline.
     *
     * @param projectId project id
     * @param id        pipeline id
     */
    @Operation(summary = "Run the pipeline", description = "Create a new Workflow in order to run the pipeline")
    @PostMapping(value = "{projectId}/pipeline/{id}/run")
    public void run(@PathVariable String projectId, @PathVariable String id) {
        LOGGER.info(
                "{} - Running pipeline '{}' in project '{}'",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                id,
                projectId);
        pipelineService.run(projectId, id);
        LOGGER.info(
                "{} - Pipeline '{}' in project '{}' successfully started",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                id,
                projectId);

    }

    /**
     * Terminate pipeline.
     *
     * @param projectId project id
     * @param id        pipeline id
     */
    @Operation(summary = "Terminate the pipeline", description = "Terminate the Workflow associated with " +
            "specific pipeline")
    @PostMapping(value = "{projectId}/pipeline/{id}/terminate")
    public void terminate(@PathVariable String projectId, @PathVariable String id) {
        LOGGER.info(
                "{} - Terminating pipeline '{}' in project '{}'",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                id,
                projectId);
        pipelineService.terminate(projectId, id);
        LOGGER.info(
                "{} - Pipeline '{}' in project '{}' has been terminated successfully ",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                id,
                projectId);

    }

    /**
     * Stop pipeline.
     *
     * @param projectId project id
     * @param id        pipeline id
     */
    @Operation(summary = "Stop the pipeline", description = "Stop the Workflow associated with specific pipeline")
    @PostMapping(value = "{projectId}/pipeline/{id}/stop")
    public void stop(@PathVariable String projectId, @PathVariable String id) {
        LOGGER.info(
                "{} - Stopping pipeline '{}' in project '{}'",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                id,
                projectId);
        pipelineService.stop(projectId, id);
        LOGGER.info(
                "{} - Pipeline '{}' in project '{}' has been stopped successfully ",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                id,
                projectId);

    }


    /**
     * Getting pipeline history.
     *
     * @param projectId  project id
     * @param pipelineId pipeline id
     * @return List of pipeline history response DTO
     */
    @Operation(summary = "Get pipeline history", description = "Get history for a specific pipeline")
    @GetMapping("{projectId}/pipeline/{pipelineId}/history")
    public List<PipelineHistoryResponseDto> getPipelineHistory(
            @PathVariable String projectId, @PathVariable String pipelineId) {
        LOGGER.info(
                "{} - Receiving pipeline '{}' history in project '{}'",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                pipelineId,
                projectId);
        return historyService.getPipelineHistory(pipelineId);
    }


    /**
     * Getting pipeline logs.
     *
     * @param projectId  project id
     * @param pipeLineId pipeline id
     * @param jobName    job name
     * @return ResponseEntity with list of history objects
     */
    @GetMapping("{projectId}/pipeline/{pipeLineId}/jobName/{jobName}/logs")
    public List<JobLogDto> getLogs(@PathVariable String projectId, @PathVariable String pipeLineId,
                                   @PathVariable String jobName) {
        LOGGER.info(
                "{} - Pipeline '{}' in project '{}' trying to receive logs",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                pipeLineId, projectId
        );
        return historyService.getPipelineLogs(projectId, pipeLineId, jobName);
    }

    /**
     * Copies pipeline.
     *
     * @param projectId  project id
     * @param pipelineId pipelineId id
     */
    @Operation(summary = "Copy the pipeline", description = "Make a pipeline copy within the same project")
    @PostMapping("{projectId}/pipeline/{pipelineId}/copy")
    public void copy(@PathVariable String projectId, @PathVariable String pipelineId) {
        LOGGER.info(
                "{} - Copying pipeline '{}' in project '{}'",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                pipelineId, projectId
        );
        pipelineService.copy(projectId, pipelineId);
    }

    /**
     * Create CRON pipeline.
     *
     * @param projectId       project id
     * @param id              pipeline id
     * @param cronPipelineDto cron data
     */
    @Operation(summary = "Create a scheduled pipeline", description = "Create a scheduled CronWorkflow based on " +
            "existing pipeline")
    @PostMapping(value = "{projectId}/pipeline/{id}/cron")
    public void createCron(
            @PathVariable String projectId,
            @PathVariable String id,
            @Valid @RequestBody CronPipelineDto cronPipelineDto) {
        LOGGER.info(
                "{} - Creating cron on pipeline '{}' in project '{}'",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                id,
                projectId);
        schedulerService.createCron(projectId, id, cronPipelineDto);
        LOGGER.info(
                "{} - Cron on pipeline '{}' in project '{}' successfully created",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                id,
                projectId);
    }

    /**
     * Stop CRON pipeline.
     *
     * @param projectId project id
     * @param id        pipeline id
     */
    @Operation(summary = "Delete a scheduled pipeline", description = "Delete a scheduled CronWorkflow bound to " +
            "existing pipeline", responses = {@ApiResponse(responseCode = "204", description = "Indicates successful" +
            " pipeline deletion")})
    @DeleteMapping(value = "{projectId}/pipeline/{id}/cron")
    public ResponseEntity<Void> deleteCron(@PathVariable String projectId, @PathVariable String id) {
        LOGGER.info(
                "{} - Deleting cron on pipeline '{}' in project '{}' ",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                id,
                projectId);
        schedulerService.deleteCron(projectId, id);
        LOGGER.info(
                "{} - Cron on pipeline '{}' in project '{}' successfully deleted",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                id,
                projectId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get cron pipeline.
     *
     * @param projectId project id
     * @param id        pipeline id
     * @return pipeline graph
     */
    @Operation(summary = "Get a scheduled pipeline", description = "Get a scheduled CronWorkflow bound to " +
            "existing pipeline")
    @GetMapping(value = "{projectId}/pipeline/{id}/cron", produces = "application/json")
    public CronPipelineDto getCronPipeline(@PathVariable String projectId, @PathVariable String id) {
        LOGGER.info(
                "{} - Receiving cron on pipeline '{}' in project '{}'",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                id,
                projectId);
        return schedulerService.getCron(projectId, id);
    }

    /**
     * Update CRON pipeline.
     *
     * @param projectId       project id
     * @param id              pipeline id
     * @param cronPipelineDto cron data
     */
    @Operation(summary = "Update a scheduled pipeline", description = "Update a scheduled CronWorkflow based on " +
            "existing pipeline")
    @PutMapping(value = "{projectId}/pipeline/{id}/cron")
    public void updateCron(
            @PathVariable String projectId,
            @PathVariable String id,
            @Valid @RequestBody CronPipelineDto cronPipelineDto) {
        LOGGER.info(
                "{} - Updating cron on pipeline '{}' in project '{}'",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                id,
                projectId);
        schedulerService.updateCron(projectId, id, cronPipelineDto);
        LOGGER.info(
                "{} - Cron on pipeline '{}' in project '{}' successfully updated",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                id,
                projectId);
    }
}
