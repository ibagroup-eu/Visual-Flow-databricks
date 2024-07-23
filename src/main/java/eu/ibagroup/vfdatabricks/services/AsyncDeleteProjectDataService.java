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
import eu.ibagroup.vfdatabricks.dto.jobs.JobOverviewListDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static eu.ibagroup.vfdatabricks.dto.Constants.*;

@Service
public class AsyncDeleteProjectDataService {
    private final RedisTemplate<String, String> redisTemplate;
    private final RestTemplate restTemplate;
    private final ApplicationConfigurationProperties appProperties;

    public AsyncDeleteProjectDataService(@Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate,
                                         @Qualifier("authRestTemplate") RestTemplate restTemplate,
                                         ApplicationConfigurationProperties appProperties) {
        this.redisTemplate = redisTemplate;
        this.restTemplate = restTemplate;
        this.appProperties = appProperties;
    }

    @Async
    public void deleteProjectData(String projectId) {
        // deleting jobs and histories
        Optional.ofNullable(restTemplate.getForEntity(
                        String.format("%s/%s/%s/%s/job",
                                appProperties.getJobStorage().getHost(),
                                CONTEXT_PATH,
                                JOB_STORAGE_API,
                                projectId),
                        JobOverviewListDto.class
                ).getBody())
                .ifPresent(body -> body.getJobs().forEach((CommonDto job) -> {
                            restTemplate.delete(String.format("%s/%s/%s/history/job/%s",
                                    appProperties.getHistoryService().getHost(),
                                    CONTEXT_PATH_HISTORY,
                                    HISTORY_SERVICE_API,
                                    job.getId()));
                            restTemplate.delete(
                                    String.format(URL_STRING_FORMAT,
                                            appProperties.getJobStorage().getHost(),
                                            CONTEXT_PATH,
                                            JOB_STORAGE_API,
                                            projectId,
                                            job.getId()));
                        })
                );
        // TODO delete pipelines
        String folderKey = PROJECT_KEY_PREFIX + projectId;
        // deleting parameters
        redisTemplate.opsForHash().entries(folderKey).keySet()
                .forEach(key -> redisTemplate.opsForHash().delete(folderKey, key));
        // deleting connections
        restTemplate.delete(
                String.format("%s/%s/%s/%s/connections",
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        projectId),
                Object.class
        );
    }

}
