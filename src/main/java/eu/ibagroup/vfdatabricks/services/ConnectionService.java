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
import eu.ibagroup.vfdatabricks.dto.connections.ConnectionDto;
import eu.ibagroup.vfdatabricks.dto.connections.ConnectionOverviewDto;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

import static eu.ibagroup.vfdatabricks.dto.Constants.CONTEXT_PATH;
import static eu.ibagroup.vfdatabricks.dto.Constants.JOB_STORAGE_API;

@Slf4j
@Setter
@Service
@Getter
public class ConnectionService {
    private final ApplicationConfigurationProperties appProperties;
    private final RestTemplate restTemplate;

    public ConnectionService(ApplicationConfigurationProperties appProperties,
                             @Qualifier("authRestTemplate") RestTemplate restTemplate) {
        this.appProperties = appProperties;
        this.restTemplate = restTemplate;
    }

    public String create(final String projectId, final ConnectionDto connectionDto) {
        ResponseEntity<String> created = restTemplate.postForEntity(
                String.format("%s/%s/%s/%s/connection",
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        projectId),
                connectionDto,
                String.class
        );
        return Objects.requireNonNull(created.getBody());
    }

    public ConnectionOverviewDto getAll(final String projectId) {
        return Objects.requireNonNull(
                restTemplate.getForEntity(
                        String.format("%s/%s/%s/%s/connections",
                                appProperties.getJobStorage().getHost(),
                                CONTEXT_PATH,
                                JOB_STORAGE_API,
                                projectId),
                        ConnectionOverviewDto.class
                ).getBody());
    }

    public void update(final String projectId, final ConnectionDto connectionDto) {
        restTemplate.put(
                String.format("%s/%s/%s/%s/connections/%s",
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        projectId,
                        connectionDto.getKey()),
                connectionDto,
                Void.class
        );
    }

    public void delete(final String projectId, final String connectionId) {
        restTemplate.delete(
                String.format("%s/%s/%s/%s/connections/%s",
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        projectId,
                        connectionId),
                Object.class
        );
    }
}
