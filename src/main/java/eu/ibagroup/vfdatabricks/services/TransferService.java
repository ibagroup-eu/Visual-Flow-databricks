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
import eu.ibagroup.vfdatabricks.dto.exporting.ExportRequestDto;
import eu.ibagroup.vfdatabricks.dto.exporting.ExportResponseDto;
import eu.ibagroup.vfdatabricks.dto.importing.ImportRequestDto;
import eu.ibagroup.vfdatabricks.dto.importing.ImportResponseDto;
import eu.ibagroup.vfdatabricks.dto.jobs.CommonDto;
import eu.ibagroup.vfdatabricks.dto.pipelines.CronPipelineDto;
import eu.ibagroup.vfdatabricks.dto.pipelines.PipelineOverviewDto;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static eu.ibagroup.vfdatabricks.dto.Constants.CONTEXT_PATH;
import static eu.ibagroup.vfdatabricks.dto.Constants.JOB_STORAGE_API;

@Service
public class TransferService {

    private final RestTemplate restTemplate;
    private final ApplicationConfigurationProperties appProperties;
    private final SchedulerService schedulerService;
    private final PipelineService pipelineService;
    private final CronCheckService cronCheckService;

    public TransferService(@Qualifier("authRestTemplate") RestTemplate restTemplate,
                           ApplicationConfigurationProperties appProperties,
                           SchedulerService schedulerService, PipelineService pipelineService,
                           CronCheckService cronCheckService) {
        this.restTemplate = restTemplate;
        this.appProperties = appProperties;
        this.schedulerService = schedulerService;
        this.pipelineService = pipelineService;
        this.cronCheckService = cronCheckService;
    }

    /**
     * Export jobs and pipelines.
     *
     * @param projectId project id
     * @return pipelines and jobs in json format
     */
    public ResponseEntity<ExportResponseDto> exporting(String projectId, ExportRequestDto exportRequestDto) {
        ResponseEntity<ExportResponseDto> result = restTemplate.postForEntity(
                String.format("%s/%s/%s/%s/exportResources",
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        projectId),
                exportRequestDto,
                ExportResponseDto.class
        );
        cronCheckService.checkAndUpdateCron(projectId, Objects.requireNonNull(result.getBody()).getPipelines());
        return result;
    }

    /**
     * Import jobs and pipelines.
     * Nested jobs will be imported as well.
     *
     * @param projectId     is project ID.
     * @param importRequest is an object, contains information about imported jobs and pipelines.
     * @return importing results.
     */
    public ResponseEntity<ImportResponseDto> importing(String projectId, ImportRequestDto importRequest) {
        ResponseEntity<ImportResponseDto> resultResponse = restTemplate.postForEntity(
                String.format("%s/%s/%s/%s/importResources",
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        projectId),
                importRequest,
                ImportResponseDto.class
        );
        ImportResponseDto result = Objects.requireNonNull(resultResponse.getBody());
        Map<String, String> pipelinesNamesWithCron = importRequest.getPipelines().stream()
                .filter(pipeline -> !result.getNotImportedPipelines().contains(pipeline.getName()) &&
                        pipeline.isCron() && StringUtils.isNotBlank(pipeline.getCronExpression()))
                .collect(Collectors.toMap(CommonDto::getName, PipelineOverviewDto::getCronExpression,
                        (p1, p2) -> p1));
        if (MapUtils.isNotEmpty(pipelinesNamesWithCron)) {
            pipelineService.getAll(projectId, pipelinesNamesWithCron.keySet()).getPipelines()
                    .forEach((PipelineOverviewDto pipeline) ->
                            schedulerService.exists(projectId, pipeline.getId())
                                    .thenAccept((Boolean exists) -> {
                                        if (exists) {
                                            schedulerService.updateCron(projectId, pipeline.getId(),
                                                    new CronPipelineDto(
                                                            pipelinesNamesWithCron.get(pipeline.getName())
                                                    ));
                                        } else {
                                            schedulerService.createCron(projectId, pipeline.getId(),
                                                    new CronPipelineDto(
                                                            pipelinesNamesWithCron.get(pipeline.getName())
                                                    ));
                                        }
                                    }));
        }
        return resultResponse;
    }
}
