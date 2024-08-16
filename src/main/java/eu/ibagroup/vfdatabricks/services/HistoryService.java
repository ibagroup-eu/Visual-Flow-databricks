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
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksJobClusterDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksJobLogDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.JobLogDto;
import eu.ibagroup.vfdatabricks.dto.pipelines.PipelineDto;
import eu.ibagroup.vfdatabricks.dto.pipelines.PipelineHistoryResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static eu.ibagroup.vfdatabricks.dto.Constants.CONTEXT_PATH_HISTORY;
import static eu.ibagroup.vfdatabricks.dto.Constants.HISTORY_SERVICE_API;
import static eu.ibagroup.vfdatabricks.services.UtilsService.decodeFromBase64;
import static eu.ibagroup.vfdatabricks.services.UtilsService.getParsedDBLogs;

@Service
@Slf4j
public class HistoryService {
    private final RestTemplate restTemplate;
    private final ApplicationConfigurationProperties appProperties;
    private final PipelineService pipelineService;
    private final DatabricksAPIService databricksAPIService;


    public HistoryService(@Qualifier("authRestTemplate") RestTemplate restTemplate,
                          ApplicationConfigurationProperties appProperties,
                          PipelineService pipelineService,
                          DatabricksAPIService databricksAPIService) {
        this.restTemplate = restTemplate;
        this.appProperties = appProperties;
        this.pipelineService = pipelineService;
        this.databricksAPIService = databricksAPIService;
    }

    public List<PipelineHistoryResponseDto> getPipelineHistory(String pipelineId) {
        return Arrays.asList(Objects.requireNonNull(restTemplate.getForEntity(
                String.format("%s/%s/%s/history/pipeline/%s",
                        appProperties.getHistoryService().getHost(),
                        CONTEXT_PATH_HISTORY,
                        HISTORY_SERVICE_API,
                        pipelineId),
                PipelineHistoryResponseDto[].class).getBody()));
    }

    public List<JobLogDto> getPipelineLogs(String projectId, String pipeLineId, String jobName) {
        List<JobLogDto> jobLogDtos = new ArrayList<>();
        PipelineDto pipelineDto = pipelineService.getById(projectId, pipeLineId);
        DatabricksJobClusterDto databricksJobClusterDto = databricksAPIService.getClusterInfo(projectId,
                pipelineDto.getRunId());
        databricksJobClusterDto.getTasks().stream().filter(job -> job.getJobName().equals(jobName)).findFirst()
                .ifPresent((DatabricksJobClusterDto.Task task) -> {
                    try {
                        DatabricksJobLogDto databricksJobLogDto = databricksAPIService.getJobLogs(projectId,
                                task.getClusterInstance().getClusterId());
                        jobLogDtos.addAll(getParsedDBLogs(decodeFromBase64(databricksJobLogDto.getData())));
                    } catch (RuntimeException e) {
                        LOGGER.info("Error:", e);
                        LOGGER.info("No provided logs from Databricks");
                    }
                });

        return jobLogDtos;
    }
}
