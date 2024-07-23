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

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ibagroup.vfdatabricks.dto.GraphDto;
import eu.ibagroup.vfdatabricks.dto.jobs.HistoryResponseDto;
import eu.ibagroup.vfdatabricks.dto.jobs.JobDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksEmailNotifications;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksJobNewCluster;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksJobRunDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksJobRunListDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksJobSparkJarTask;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksJobState;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksJobStorageRunDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksJobTask;
import eu.ibagroup.vfdatabricks.dto.notifications.EmailNotification;
import io.fabric8.kubernetes.api.model.Secret;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RegExUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import static eu.ibagroup.vfdatabricks.dto.Constants.*;
import static eu.ibagroup.vfdatabricks.services.UtilsService.decodeFromBase64;
import static eu.ibagroup.vfdatabricks.services.UtilsService.encodeToBase64;


/**
 * MapperService class.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class MapperService {
    private static final Pattern PROHIBITED_CHARS = Pattern.compile("\\W+");
    private final KubernetesService kubernetesService;
    private final ObjectMapper objectMapper;

    public static String toAlphaNumeric(String name) {
        return RegExUtils.replaceAll(name, PROHIBITED_CHARS, "_");
    }

    private static String toFormattedString(long millis) {
        return DATE_TIME_FORMATTER.format(
                ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
        );
    }

    static String resolveOverallStatus(Collection<String> statuses) {
        String result;
        if (statuses.contains(RUNNING_VF_STATUS)) {
            result = RUNNING_VF_STATUS;
        } else if (statuses.contains(PENDING_VF_STATUS)) {
            result = PENDING_VF_STATUS;
        } else if (statuses.contains(FAILED_VF_STATUS)) {
            result = FAILED_VF_STATUS;
        } else if (statuses.stream().allMatch(status -> status.equals(SUCCEEDED_VF_STATUS))) {
            result = SUCCEEDED_VF_STATUS;
        } else {
            result = DRAFT_VF_STATUS;
        }
        return result;
    }

    public DatabricksJobStorageRunDto mapRequestToJobRun(@Valid JobDto jobDto, String projectId) {
        return DatabricksJobStorageRunDto.builder()
                .runName(jobDto.getName())
                .tasks(List.of(
                        mapJobDtoToDatabricksJobTask(jobDto, projectId)
                ))
                .build();
    }

    public DatabricksJobTask mapJobDtoToDatabricksJobTask(JobDto jobDto, String projectId) {
        Secret project = kubernetesService.getSecret(projectId);
        DatabricksJobNewCluster newCluster =
                objectMapper.convertValue(jobDto.getParams().getClusterDatabricksSchema(),
                        DatabricksJobNewCluster.class);
        newCluster.setSparkEnvVars(
                prepareSparkEnv(jobDto, projectId, newCluster.getSparkEnvVars()));
        newCluster.setClusterLogConfig(DatabricksJobNewCluster.ClusterLogConfig.builder()
                .dbfs(DatabricksJobNewCluster.ClusterLogConfig.DBFS
                        .builder()
                        .destination("dbfs:/logStore/log")
                        .build())
                .build());

        return DatabricksJobTask.builder()
                .taskKey(jobDto.getName())
                .newCluster(newCluster)
                .sparkJarTask(
                        DatabricksJobSparkJarTask.builder()
                                .mainClassName("by.iba.vf.spark.transformation.TransformationJob")
                                .build()
                )
                .libraries(List.of(DatabricksJobTask
                        .Library
                        .builder()
                        .jar(decodeFromBase64(project.getData().get(PATH_TO_FILE)) + "/" + JAR_FILE_NAME)
                        .build()))
                .build();
    }

    public Map<String, String> prepareSparkEnv(JobDto jobDto, String projectId, Map<String, String> initialVars) {
        GraphDto graphDto = GraphDto.parseGraph(jobDto.getDefinition());

        Map<String, String> result = new HashMap<>();

        result.put(JOB_CONFIG_FIELD, encodeToBase64(graphDto.toString()));
        result.put(JOB_DEFINITION_FIELD, encodeToBase64(jobDto.getDefinition().toString()));
        result.put(UP_TO, jobDto.getParams().getUpTo());
        result.put(INTERVALS, jobDto.getParams().getIntervals());
        result.put(VISUAL_FLOW_CONFIGURATION_TYPE, "Databricks");
        result.put(VISUAL_FLOW_DATABRICKS_SECRET_SCOPE, projectId);
        result.put(JNAME, "zulu11-ca-amd64");
        if (initialVars != null) {
            result.putAll(initialVars);
        }
        return result;
    }

    public String mapStatus(DatabricksJobState state) {
        String status = DRAFT_VF_STATUS;
        if (state != null) {
            switch (state.getLifeCycleState()) {
                case PENDING_DB_STATUS, BLOCKED_DB_STATUS:
                    status = PENDING_VF_STATUS;
                    break;
                case RUNNING_DB_STATUS:
                    status = RUNNING_VF_STATUS;
                    break;
                default:
                    if (Objects.equals(state.getResultState(), SUCCESS_DB_STATUS)) {
                        status = SUCCEEDED_VF_STATUS;
                    } else {
                        status = FAILED_VF_STATUS;
                    }
            }
        }
        return status;
    }

    public List<HistoryResponseDto> mapJobRunsToHistory(DatabricksJobRunListDto jobRuns) {
        return CollectionUtils.emptyIfNull(jobRuns.getRuns())
                .stream()
                .map(this::mapJobRunToHistory)
                .toList();
    }

    public HistoryResponseDto mapJobRunToHistory(DatabricksJobRunDto jobRun) {
        return HistoryResponseDto.builder()
                .id(String.valueOf(jobRun.getJobId()))
                .type(JOB_TYPE)
                .startedBy(jobRun.getCreatorUserName())
                .startedAt(toFormattedString(jobRun.getStartTime()))
                .finishedAt(toFormattedString(jobRun.getEndTime()))
                .status(mapStatus(jobRun.getState()))
                .logId(String.valueOf(jobRun.getRunId()))
                .build();
    }

    public DatabricksEmailNotifications mapEmailNotifications(EmailNotification emailNotification) {
        if (emailNotification == null) {
            return null;
        }
        DatabricksEmailNotifications.DatabricksEmailNotificationsBuilder builder =
                DatabricksEmailNotifications.builder();
        if (emailNotification.getFailureNotify() == Boolean.TRUE) {
            builder.onSuccess(emailNotification.getRecipients());
        }
        if (emailNotification.getFailureNotify() == Boolean.TRUE) {
            builder.onFailure(emailNotification.getRecipients());
        }
        return builder.build();
    }
}
