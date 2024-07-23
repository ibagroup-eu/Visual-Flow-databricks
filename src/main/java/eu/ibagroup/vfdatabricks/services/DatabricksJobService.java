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
import eu.ibagroup.vfdatabricks.dto.Params;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksJobClusterDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksJobLogDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksJobRunDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksJobStorageRunDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksRunIdDto;
import eu.ibagroup.vfdatabricks.exceptions.ForRetryRestTemplateException;
import io.fabric8.kubernetes.api.model.Secret;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;

import static eu.ibagroup.vfdatabricks.dto.Constants.*;
import static eu.ibagroup.vfdatabricks.services.UtilsService.decodeFromBase64;
import static eu.ibagroup.vfdatabricks.services.UtilsService.makeHttpEntity;
import static org.springframework.http.HttpHeaders.USER_AGENT;

@Slf4j
@Service
public class DatabricksJobService {
    private static final long MILLISECONDS_MULTIPLIER = 1000L;
    private final KubernetesService kubernetesService;
    private final RestTemplate databricksRestTemplate;
    private final ApplicationConfigurationProperties appProperties;

    public DatabricksJobService(KubernetesService kubernetesService,
                                @Qualifier("databricksRestTemplate") RestTemplate databricksRestTemplate,
                                ApplicationConfigurationProperties appProperties) {
        this.kubernetesService = kubernetesService;
        this.databricksRestTemplate = databricksRestTemplate;
        this.appProperties = appProperties;
    }

    @Async
    public CompletableFuture<DatabricksRunIdDto> runJob(String projectId,
                                                        DatabricksJobStorageRunDto body,
                                                        Params params) {
        RetryTemplate retryTemplate = getRetryTemplate(params);
        Secret project = kubernetesService.getSecret(projectId);
        HttpEntity<DatabricksJobStorageRunDto> databricksEntity = makeHttpEntity(
                decodeFromBase64(project.getData().get(TOKEN)),
                body
        );
        DatabricksRunIdDto response;
        try {
            response = retryTemplate.execute((RetryCallback<DatabricksRunIdDto,
                    ForRetryRestTemplateException>) (RetryContext context)-> {
                LOGGER.info("STARTED RETRY");
                return databricksRestTemplate.exchange(
                        String.format("%s/%s/runs/submit",
                                decodeFromBase64(project.getData().get(HOST)), DATABRICKS_JOBS_API),
                        HttpMethod.POST,
                        databricksEntity,
                        DatabricksRunIdDto.class
                ).getBody();
            });
        } catch (ForRetryRestTemplateException e) {
            LOGGER.info("Retry error", e);
            throw e;
        }
        return CompletableFuture.completedFuture(response);
    }

    private static RetryTemplate getRetryTemplate(Params params) {
        RetryTemplate retryTemplate = new RetryTemplate();

        int upTo = Integer.parseInt(params.getUpTo());
        int intervals = Integer.parseInt(params.getIntervals());

        int attempts = upTo / intervals;
        attempts++;

        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(intervals * MILLISECONDS_MULTIPLIER);

        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
        retryTemplate.setRetryPolicy(new SimpleRetryPolicy(attempts));
        return retryTemplate;
    }

    public void cancelJob(String projectId, long runId) {
        Secret project = kubernetesService.getSecret(projectId);
        HttpEntity<DatabricksRunIdDto> databricksEntity = makeHttpEntity(
                decodeFromBase64(project.getData().get(TOKEN)),
                DatabricksRunIdDto.builder().runId(runId).build()
        );
        databricksRestTemplate.exchange(
                String.format("%s/%s/runs/cancel",
                        decodeFromBase64(project.getData().get(HOST)),
                        DATABRICKS_JOBS_API),
                HttpMethod.POST,
                databricksEntity,
                Void.class
        );
    }

    public DatabricksJobRunDto checkJobStatus(String projectId, long runId) {
        Secret project = kubernetesService.getSecret(projectId);

        HttpEntity<DatabricksJobStorageRunDto> entity = makeHttpEntity(
                decodeFromBase64(project.getData().get(TOKEN)),
                null
        );
        ResponseEntity<DatabricksJobRunDto> result = databricksRestTemplate.exchange(
                String.format("%s/%s/runs/get?run_id=%s",
                        decodeFromBase64(project.getData().get(HOST)),
                        DATABRICKS_JOBS_API, runId),
                HttpMethod.GET,
                entity,
                DatabricksJobRunDto.class
        );
        return result.getBody();
    }

    public DatabricksJobClusterDto getClusterInfo(String projectId, long runId) {
        Secret project = kubernetesService.getSecret(projectId);

        HttpEntity<DatabricksJobStorageRunDto> entity = makeHttpEntity(
                decodeFromBase64(project.getData().get(TOKEN)),
                null
        );
        ResponseEntity<DatabricksJobClusterDto> result = databricksRestTemplate.exchange(
                String.format("%s/%s/runs/get?run_id=%s",
                        decodeFromBase64(project.getData().get(HOST)),
                        DATABRICKS_JOBS_API, runId),
                HttpMethod.GET,
                entity,
                DatabricksJobClusterDto.class
        );
        return result.getBody();
    }

    public DatabricksJobLogDto getJobLogs(String projectId, String clusterId) {
        String path = "/logStore/log/" + clusterId + "/driver/log4j-active.log";
        Secret project = kubernetesService.getSecret(projectId);

        HttpEntity<DatabricksJobStorageRunDto> entity = makeHttpEntity(
                decodeFromBase64(project.getData().get(TOKEN)),
                null
        );
        ResponseEntity<DatabricksJobLogDto> result = databricksRestTemplate.exchange(
                String.format("%s/%s/dbfs/read?path=%s",
                        decodeFromBase64(project.getData().get(HOST)),
                        DATABRICKS_JOBS_API_20, path),
                HttpMethod.GET,
                entity,
                DatabricksJobLogDto.class
        );
        return result.getBody();
    }

    public void uploadFile(String projectId, String path, byte[] fileBytes, String fileName) {
        createDirectory(projectId, path);
        Secret project = kubernetesService.getSecret(projectId);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            String uploadUrl = String.format(URL_UPLOAD_FILE_FORMAT,
                    decodeFromBase64(project.getData().get(HOST)),
                    path,
                    fileName);
            HttpPut httpPut = new HttpPut(uploadUrl);
            httpPut.setHeader("Authorization", "Bearer " + decodeFromBase64(project.getData().get(TOKEN)));
            httpPut.setHeader("Content-Type", "application/octet-stream");
            httpPut.setHeader(USER_AGENT, String.format("%s/%s", appProperties.getDatabricks().getIsv().getName(),
                    appProperties.getDatabricks().getIsv().getVersion()));

            httpPut.setEntity(new ByteArrayEntity(fileBytes, ContentType.APPLICATION_OCTET_STREAM));
            LOGGER.info("File upload for {} started", projectId);
            try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
                if (response.getCode() == HttpStatus.NO_CONTENT.value()) {
                    LOGGER.info("File uploaded successfully");
                } else {
                    LOGGER.info("Error while uploading file: " + response.getCode());
                }
            }

        } catch (Exception e) {
            LOGGER.error("Error while uploading file", e);
        }
    }

    public void createDirectory(String projectId, String path) {
        Secret project = kubernetesService.getSecret(projectId);
        HttpEntity<Object> entity = makeHttpEntity(
                decodeFromBase64(project.getData().get(TOKEN)),
                null
        );
        databricksRestTemplate.exchange(
                String.format(URL_CREATE_DIRECTORY_FORMAT,
                        decodeFromBase64(project.getData().get(HOST)),
                        path),
                HttpMethod.PUT,
                entity,
                Object.class
        );
    }

}
