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

import eu.ibagroup.vfdatabricks.util.CronExpressionUtils;
import eu.ibagroup.vfdatabricks.dto.pipelines.CronPipelineDto;
import eu.ibagroup.vfdatabricks.services.quartz.CronJob;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService {
    private final SchedulerFactoryBean schedulerFactoryBean;


    private static JobKey getJobKey(String projectId, String id) {
        return JobKey.jobKey("JobName-" + id, "JobGroup" + projectId);
    }

    private static TriggerKey getTriggerKey(String projectId, String id) {
        return TriggerKey.triggerKey("TriggerName-" + id, "TriggerGroup" + projectId);
    }

    private static Trigger createTrigger(CronPipelineDto cronPipelineDto, TriggerKey triggerKey) {
        String expression = CronExpressionUtils.unixToQuartz(cronPipelineDto.getSchedule());
        return TriggerBuilder.newTrigger()
                .withIdentity(triggerKey)
                .withSchedule(CronScheduleBuilder.cronSchedule(expression)
                        .inTimeZone(TimeZone.getTimeZone("UTC")))
                .startNow()
                .build();
    }


    private boolean isPaused(TriggerKey triggerKey) throws SchedulerException {
        return schedulerFactoryBean.getScheduler().getTriggerState(triggerKey) == Trigger.TriggerState.PAUSED;
    }

    @SneakyThrows
    public boolean deleteCron(String projectId, String id) {
        JobKey jobKey = getJobKey(projectId, id);
        return schedulerFactoryBean.getScheduler().deleteJob(jobKey);
    }

    @SneakyThrows
    @Async
    public CompletableFuture<Boolean> exists(String projectId, String id) {
        JobKey jobKey = getJobKey(projectId, id);
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        boolean exists = scheduler.checkExists(jobKey);
        return CompletableFuture.completedFuture(exists);
    }

    @SneakyThrows
    public void createCron(String projectId, String id, CronPipelineDto cronPipelineDto) {
        JobDetail jobDetail = JobBuilder.newJob(CronJob.class)
                .withIdentity(getJobKey(projectId, id))
                .usingJobData("projectId", projectId)
                .usingJobData("pipelineId", id)
                .requestRecovery(true)
                .storeDurably()
                .build();

        Trigger trigger = createTrigger(cronPipelineDto, getTriggerKey(projectId, id));

        Date date = schedulerFactoryBean.getScheduler().scheduleJob(jobDetail, trigger);
        LOGGER.info("Job scheduled successfully with id: {} and date: {}", jobDetail.getKey(), date);
    }

    @SneakyThrows
    public void updateCron(String projectId, String id, CronPipelineDto cronPipelineDto) {

        TriggerKey triggerKey = getTriggerKey(projectId, id);
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        CronTrigger existingTrigger = (CronTrigger) scheduler.getTrigger(triggerKey);
        if (StringUtils.isNotBlank(cronPipelineDto.getSchedule())
                && !Objects.equals(
                cronPipelineDto.getSchedule(),
                CronExpressionUtils.quartzToUnix(existingTrigger.getCronExpression())
        )) {
            Trigger trigger = createTrigger(cronPipelineDto, triggerKey);
            Date date = scheduler.rescheduleJob(triggerKey, trigger);
            LOGGER.info("Job rescheduled successfully with id: {} and date: {}", triggerKey, date);
        }
        if (cronPipelineDto.isSuspend() != isPaused(triggerKey)) {
            if (cronPipelineDto.isSuspend()) {
                schedulerFactoryBean.getScheduler().pauseTrigger(triggerKey);
                LOGGER.info("Job suspended successfully with id: {}", triggerKey);
            } else {
                schedulerFactoryBean.getScheduler().resumeTrigger(triggerKey);
                LOGGER.info("Job resumed successfully with id: {}", triggerKey);
            }
        }

    }

    @SneakyThrows
    public CronPipelineDto getCron(String projectId, String id) {
        TriggerKey triggerKey = getTriggerKey(projectId, id);

        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        Trigger trigger = scheduler.getTrigger(triggerKey);
        if (trigger == null) {
            return null;
        }
        String expression = CronExpressionUtils.quartzToUnix(((CronTrigger) trigger).getCronExpression());
        return CronPipelineDto.builder()
                .schedule(expression)
                .suspend(isPaused(triggerKey))
                .build();

    }
}
