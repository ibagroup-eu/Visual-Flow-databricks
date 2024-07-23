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
import eu.ibagroup.vfdatabricks.dto.ParameterDto;
import eu.ibagroup.vfdatabricks.dto.ParameterOverviewDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DataBricksSecretDeleteDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DataBricksSecretPutDto;
import eu.ibagroup.vfdatabricks.model.Parameter;
import io.fabric8.kubernetes.api.model.Secret;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

import static eu.ibagroup.vfdatabricks.dto.Constants.*;
import static eu.ibagroup.vfdatabricks.services.UtilsService.*;

@Slf4j
@Service
public class ParameterService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RestTemplate databricksRestTemplate;
    private final KubernetesService kubernetesService;
    public ParameterService(@Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate,
                            ObjectMapper objectMapper,
                            @Qualifier("databricksRestTemplate") RestTemplate databricksRestTemplate,
                            KubernetesService kubernetesService) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.databricksRestTemplate = databricksRestTemplate;
        this.kubernetesService = kubernetesService;
    }

    private String wrapValue(String value) {
        return String.format("{\"text\":\"%s\"}", value);
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
        Secret project = kubernetesService.getSecret(projectId);
        HttpEntity<DataBricksSecretPutDto> databricksEntity = makeHttpEntity(
                decodeFromBase64(project.getData().get(TOKEN)), DataBricksSecretPutDto.builder()
                        .scope(projectId)
                        .key(parameter.getKey())
                        .stringValue(wrapValue(encodeToBase64(parameter.getValue().getText())))
                        .build());
        databricksRestTemplate.exchange(
                String.format("%s/%s/put",
                        decodeFromBase64(project.getData().get(HOST)),
                        DATABRICKS_SECRET_API),
                HttpMethod.POST,
                databricksEntity,
                Object.class
        );
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
        Secret project = kubernetesService.getSecret(projectId);
        HttpEntity<DataBricksSecretDeleteDto> databricksEntity = makeHttpEntity(
                decodeFromBase64(project.getData().get(TOKEN)), DataBricksSecretDeleteDto.builder()
                        .scope(projectId)
                        .key(parameterId)
                        .build());
        databricksRestTemplate.exchange(
                String.format("%s/%s/delete",
                        decodeFromBase64(project.getData().get(HOST)),
                        DATABRICKS_SECRET_API),
                HttpMethod.POST,
                databricksEntity,
                Object.class
        );

    }

    private Parameter jsonToParameter(String jobJson) throws JsonProcessingException {
        Parameter parameter = objectMapper.readValue(jobJson, Parameter.class);
        return parameter;
    }

}
