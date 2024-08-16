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
import eu.ibagroup.vfdatabricks.dto.Constants;
import eu.ibagroup.vfdatabricks.dto.connections.ConnectionDto;
import eu.ibagroup.vfdatabricks.dto.databases.PingStatusDto;
import eu.ibagroup.vfdatabricks.dto.parameters.ParameterDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * Service class for manipulations with data, will be sent to DB-Service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DatabasesService {

    private final ConnectionService connectionService;
    private final ParameterService parameterService;
    private final RestTemplate restTemplate;
    private final ApplicationConfigurationProperties appProperties;

    /**
     * Method for getting connection ping status by project id and connection ID.
     *
     * @param projectId    is project id.
     * @param connectionId is connection ID.
     * @return ping status DTO.
     */
    public PingStatusDto ping(String projectId, String connectionId) {
        ResponseEntity<PingStatusDto> result = restTemplate
                .postForEntity(appProperties.getDbService().getHost(),
                        getConnection(projectId, connectionId),
                        PingStatusDto.class);
        return Objects.requireNonNull(result.getBody());
    }

    /**
     * Method for getting connection ping status with provided parameters.
     *
     * @param projectId     is project id.
     * @param connectionDto is JSON containing user parameters.
     * @return ping status DTO.
     */
    public PingStatusDto ping(String projectId, ConnectionDto connectionDto) {
        ResponseEntity<PingStatusDto> result = restTemplate
                .postForEntity(appProperties.getDbService().getHost(),
                        replaceParams(projectId, connectionDto),
                        PingStatusDto.class);
        return Objects.requireNonNull(result.getBody());
    }

    /**
     * Method for getting the {@link ConnectionDto connection object}, will be sent to db-service.
     * In addition, parses connection params and replaces them to their values.
     *
     * @param id   is the project id.
     * @param name is the connection name.
     * @return parsed connection object with filled params.
     */
    public ConnectionDto getConnection(String id, String name) {
        return replaceParams(id, connectionService.get(id, name));
    }

    /**
     * Method for replacing keys by values for connections.
     * In addition, parses connection params and replaces them to their values.
     *
     * @param projectId  is the project id.
     * @param connection is the connection.
     * @return parsed connection object with filled params.
     */
    public ConnectionDto replaceParams(String projectId, ConnectionDto connection) {
        List<ParameterDto> params = parameterService.getAll(projectId).getParams();
        Map<String, String> paramsMap = params.stream().collect(Collectors.toMap(ParameterDto::getKey,
                param -> param.getValue().getText()));
        connection.getValue().forEach((String key, String value) -> {
            Matcher matcher = Constants.PARAM_MATCH_PATTERN.matcher(value);
            while (matcher.find()) {
                String found = matcher.group(1);
                if (paramsMap.containsKey(found)) {
                    value = value.replace(matcher.group(), paramsMap.get(found));
                    matcher = Constants.PARAM_MATCH_PATTERN.matcher(value);
                }
            }
        });
        return connection;
    }
}
