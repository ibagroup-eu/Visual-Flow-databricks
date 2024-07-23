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
import eu.ibagroup.vfdatabricks.dto.exporting.ExportRequestDto;
import eu.ibagroup.vfdatabricks.dto.exporting.ExportResponseDto;
import eu.ibagroup.vfdatabricks.dto.importing.ImportRequestDto;
import eu.ibagroup.vfdatabricks.dto.importing.ImportResponseDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import static eu.ibagroup.vfdatabricks.dto.Constants.CONTEXT_PATH;
import static eu.ibagroup.vfdatabricks.dto.Constants.JOB_STORAGE_API;

@Service
public class TransferService {

    private final RestTemplate restTemplate;
    private final ApplicationConfigurationProperties appProperties;

    public TransferService(@Qualifier("authRestTemplate") RestTemplate restTemplate,
                           ApplicationConfigurationProperties appProperties) {
        this.restTemplate = restTemplate;
        this.appProperties = appProperties;
    }

    /**
     * Export jobs and pipelines.
     *
     * @param projectId project id
     * @return pipelines and jobs in json format
     */
    public ResponseEntity<ExportResponseDto> exporting(String projectId, ExportRequestDto exportRequestDto) {
        return restTemplate.postForEntity(
                String.format("%s/%s/%s/%s/exportResources",
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        projectId),
                exportRequestDto,
                ExportResponseDto.class
        );
    }

    /**
     * Import jobs and pipelines.
     * Nested jobs will be imported as well.
     * @param projectId is project ID.
     * @param importRequest is an object, contains information about imported jobs and pipelines.
     * @return importing results.
     */
    public ResponseEntity<ImportResponseDto> importing(String projectId, ImportRequestDto importRequest) {
        return restTemplate.postForEntity(
                String.format("%s/%s/%s/%s/importResources",
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        projectId),
                importRequest,
                ImportResponseDto.class
        );
    }
}
