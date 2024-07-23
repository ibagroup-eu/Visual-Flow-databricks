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

package eu.ibagroup.vfdatabricks.services.quartz;

import eu.ibagroup.vfdatabricks.services.PipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.PersistJobDataAfterExecution;
import org.springframework.scheduling.quartz.QuartzJobBean;

@Slf4j
@RequiredArgsConstructor
@PersistJobDataAfterExecution
public class CronJob extends QuartzJobBean {
    private final PipelineService pipelineService;

    @Override
    protected void executeInternal(org.quartz.JobExecutionContext context) {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        JobKey key = context.getJobDetail().getKey();
        LOGGER.info("Executing job {}", key);
        String projectId = dataMap.getString("projectId");
        String pipelineId = dataMap.getString("pipelineId");
        pipelineService.run(projectId, pipelineId);
    }

}
