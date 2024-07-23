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

import eu.ibagroup.vfdatabricks.dto.jobs.CommonDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksJobRunDto;
import eu.ibagroup.vfdatabricks.dto.pipelines.PipelineOverviewDto;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

import static eu.ibagroup.vfdatabricks.dto.Constants.PENDING_VF_STATUS;
import static eu.ibagroup.vfdatabricks.dto.Constants.RUNNING_VF_STATUS;
import static eu.ibagroup.vfdatabricks.services.UtilsService.toFormattedString;

@Service
@RequiredArgsConstructor
public class AsyncJobCheckService {
    private final DatabricksJobService databricksApiService;
    private final MapperService mapperService;

    @Async
    public CompletableFuture<DatabricksJobRunDto> checkAndUpdateStatus(String projectId, CommonDto job) {
        if (StringUtils.equalsAnyIgnoreCase(job.getStatus(), PENDING_VF_STATUS, RUNNING_VF_STATUS)
                && job.getRunId() > 0) {

            DatabricksJobRunDto result = databricksApiService.checkJobStatus(projectId, job.getRunId());

            if (result != null && result.getState() != null) {
                String status = mapperService.mapStatus(result.getState());
                if (!status.equals(job.getStatus())) {
                    populate(job, status, result);
                    return CompletableFuture.completedFuture(result);

                }
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    private static void populate(CommonDto job, String status, DatabricksJobRunDto result) {
        job.setStatus(status);
        job.setStartedAt(toFormattedString(result.getStartTime()));
        if (result.getState().getResultState() != null) {
            if (job instanceof PipelineOverviewDto) {
                ((PipelineOverviewDto) job).setProgress(1);
            }
            job.setFinishedAt(toFormattedString(result.getEndTime()));
        }
    }

}
