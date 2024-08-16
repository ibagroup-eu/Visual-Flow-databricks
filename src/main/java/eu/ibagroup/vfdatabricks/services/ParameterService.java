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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ibagroup.vfdatabricks.dto.parameters.ParameterDto;
import eu.ibagroup.vfdatabricks.dto.parameters.ParameterOverviewDto;
import eu.ibagroup.vfdatabricks.model.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static eu.ibagroup.vfdatabricks.dto.Constants.PARAMETER_KEY_PREFIX;
import static eu.ibagroup.vfdatabricks.dto.Constants.PROJECT_KEY_PREFIX;

@Slf4j
@Service
public class ParameterService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final DatabricksAPIService databricksAPIService;
    public ParameterService(@Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate,
                            ObjectMapper objectMapper,
                            DatabricksAPIService databricksAPIService) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.databricksAPIService = databricksAPIService;
    }

    public void create(String projectId, String paramId, ParameterDto parameterDto)
            throws JsonProcessingException {
        String folderKey = PROJECT_KEY_PREFIX + projectId;
        Parameter parameter = Parameter.builder()
                .secret(parameterDto.isSecret())
                .key(paramId)
                .value(parameterDto.getValue())
                .build();
        String parameterKey = folderKey + PARAMETER_KEY_PREFIX + paramId;
        String parameterJson = objectMapper.writeValueAsString(parameter);
        redisTemplate.opsForHash().put(folderKey, parameterKey, parameterJson);
        databricksAPIService.addSecret(projectId, parameter);
    }

    public void update(String projectId, String paramId, ParameterDto parameterDto)
            throws JsonProcessingException {
        delete(projectId, paramId);
        create(projectId, parameterDto.getKey(), parameterDto);
    }

    public ParameterDto get(String projectId, String parameterId) throws JsonProcessingException {
        String folderKey = PROJECT_KEY_PREFIX + projectId;
        String parameterKey = folderKey + PARAMETER_KEY_PREFIX + parameterId;
        Parameter parameter = jsonToParameter((String) redisTemplate.opsForHash().get(folderKey, parameterKey));
        ParameterDto parameterDto = ParameterDto.builder()
                .secret(parameter.isSecret())
                .key(parameter.getKey())
                .value(parameter.getValue())
                .build();
        return parameterDto;
    }

    public ParameterOverviewDto getAll(String projectId) {
        List<Parameter> parameters = new ArrayList<>();
        String folderKey = PROJECT_KEY_PREFIX + projectId;
        redisTemplate.opsForHash().entries(folderKey).forEach((Object key, Object value) -> {
            try {
                parameters.add(jsonToParameter((String) value));
            } catch (JsonProcessingException e) {
                LOGGER.error("Error while executing getAll method: " + e.getMessage());
            }
        });
        List<ParameterDto> parameterDtos = new ArrayList<>();
        parameters.forEach(parameter -> parameterDtos.add(ParameterDto.builder()
                .key(parameter.getKey())
                .secret(parameter.isSecret())
                .value(parameter.getValue())
                .build()));

        return ParameterOverviewDto.builder()
                .params(parameterDtos)
                .editable(true)
                .build();
    }

    public void delete(String projectId, String parameterId) {
        String folderKey = PROJECT_KEY_PREFIX + projectId;
        String jobKey = folderKey + PARAMETER_KEY_PREFIX + parameterId;
        redisTemplate.opsForHash().delete(folderKey, jobKey);
        databricksAPIService.deleteSecret(projectId, parameterId);
    }

    private Parameter jsonToParameter(String jobJson) throws JsonProcessingException {
        Parameter parameter = objectMapper.readValue(jobJson, Parameter.class);
        return parameter;
    }

}
