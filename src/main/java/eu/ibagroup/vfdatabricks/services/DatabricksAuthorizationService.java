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

import eu.ibagroup.vfdatabricks.dto.DatabricksOAuthResponseDto;
import io.fabric8.kubernetes.api.model.Secret;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import static eu.ibagroup.vfdatabricks.dto.Constants.*;
import static eu.ibagroup.vfdatabricks.services.UtilsService.decodeFromBase64;

@Slf4j
@Service
public class DatabricksAuthorizationService {

    private final KubernetesService kubernetesService;
    private final RestTemplate databricksRestTemplate;


    public DatabricksAuthorizationService(KubernetesService kubernetesService,
                                @Qualifier("databricksRestTemplate") RestTemplate databricksRestTemplate) {
        this.kubernetesService = kubernetesService;
        this.databricksRestTemplate = databricksRestTemplate;
    }

    public String getOAuthToken(String projectId) {
        Secret project = kubernetesService.getSecret(projectId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + project.getData().get(TOKEN));

        HttpEntity<String> entity = new HttpEntity<>(DATABRICKS_OAUTH_REQUEST_BODY, headers);

        ResponseEntity<DatabricksOAuthResponseDto> response =
                databricksRestTemplate.exchange(String.format("%s/%s",
                                decodeFromBase64(project.getData().get(HOST)),
                                DATABRICKS_OAUTH_REQUEST_URL),
                        HttpMethod.POST,
                        entity,
                        DatabricksOAuthResponseDto.class);
        LOGGER.info("Received OAuth token for {}", projectId);
        return response.getBody().getAccessToken();
    }
}
