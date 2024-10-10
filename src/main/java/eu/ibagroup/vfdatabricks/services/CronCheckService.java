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

import eu.ibagroup.vfdatabricks.dto.pipelines.CronPipelineDto;
import eu.ibagroup.vfdatabricks.dto.pipelines.PipelineOverviewDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
@Service
public class CronCheckService {
    private final SchedulerService schedulerService;


    private void updateCronDetails(String projectId, PipelineOverviewDto pipelineOverviewDto, Boolean exists) {
        if (exists) {
            CronPipelineDto cron = schedulerService.getCron(projectId, pipelineOverviewDto.getId());
            pipelineOverviewDto.setCron(true);
            pipelineOverviewDto.setCronExpression(cron.getSchedule());
            pipelineOverviewDto.setCronSuspend(cron.isSuspend());
        }
    }

    public void checkAndUpdateCron(String projectId, Collection<? extends PipelineOverviewDto> pipelines) {
        CompletableFuture<?>[] futures = pipelines.stream()
                .map(pipelineOverviewDto -> schedulerService.exists(projectId, pipelineOverviewDto.getId())
                        .thenAccept(exists -> updateCronDetails(projectId, pipelineOverviewDto, exists)))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).join();
    }


}
