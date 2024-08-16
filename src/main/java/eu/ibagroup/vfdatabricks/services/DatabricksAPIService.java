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

import com.google.common.cache.LoadingCache;
import eu.ibagroup.vfdatabricks.config.ApplicationConfigurationProperties;
import eu.ibagroup.vfdatabricks.dto.NodeTypeList;
import eu.ibagroup.vfdatabricks.dto.Params;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DataBricksSecretDeleteDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DataBricksSecretPutDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DataBricksSecretScopeDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksJobClusterDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksJobLogDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksJobRunDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksJobStorageRunDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksRunIdDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksSecretScopeDeleteDto;
import eu.ibagroup.vfdatabricks.dto.projects.DatabricksAuthentication;
import eu.ibagroup.vfdatabricks.exceptions.ForRetryRestTemplateException;
import eu.ibagroup.vfdatabricks.model.Parameter;
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
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static eu.ibagroup.vfdatabricks.dto.Constants.*;
import static eu.ibagroup.vfdatabricks.services.UtilsService.*;
import static org.springframework.http.HttpHeaders.USER_AGENT;

@Slf4j
@Service
public class DatabricksAPIService {
    private static final long MILLISECONDS_MULTIPLIER = 1000L;
    private final KubernetesService kubernetesService;
    private final RestTemplate databricksRestTemplate;
    private final ApplicationConfigurationProperties appProperties;
    private final LoadingCache<String, String> tokenCache;

    public DatabricksAPIService(KubernetesService kubernetesService,
                                @Qualifier("databricksRestTemplate") RestTemplate databricksRestTemplate,
                                ApplicationConfigurationProperties appProperties,
                                @Qualifier("tokenCache") LoadingCache<String, String> tokenCache) {
        this.kubernetesService = kubernetesService;
        this.databricksRestTemplate = databricksRestTemplate;
        this.appProperties = appProperties;
        this.tokenCache = tokenCache;
    }

    @Async
    public CompletableFuture<DatabricksRunIdDto> runJob(String projectId,
                                                        DatabricksJobStorageRunDto body,
                                                        Params params) {
        RetryTemplate retryTemplate = getRetryTemplate(params);
        DatabricksRunIdDto response;
        try {
            response = retryTemplate.execute((RetryCallback<DatabricksRunIdDto,
                    ForRetryRestTemplateException>) (RetryContext context)-> {
                LOGGER.info("STARTED RETRY");
                return sendRequest(projectId,
                        String.format("/%s/runs/submit", DATABRICKS_JOBS_API),
                        HttpMethod.POST,
                        body,
                        DatabricksRunIdDto.class,
                        false);
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
        sendRequest(projectId,
                String.format("/%s/runs/cancel", DATABRICKS_JOBS_API),
                HttpMethod.POST,
                DatabricksRunIdDto.builder().runId(runId).build(),
                Void.class,
                true);
    }

    public DatabricksJobRunDto checkJobStatus(String projectId, long runId) {
        return sendRequest(projectId,
                String.format("/%s/runs/get?run_id=%s", DATABRICKS_JOBS_API, runId),
                HttpMethod.GET,
                null,
                DatabricksJobRunDto.class,
                true);
    }

    public DatabricksJobClusterDto getClusterInfo(String projectId, long runId) {
        return sendRequest(projectId,
                String.format("/%s/runs/get?run_id=%s", DATABRICKS_JOBS_API, runId),
                HttpMethod.GET,
                null,
                DatabricksJobClusterDto.class,
                true);
    }

    public DatabricksJobLogDto getJobLogs(String projectId, String clusterId) {
        String path = "/logStore/log/" + clusterId + "/driver/log4j-active.log";
        return sendRequest(projectId,
                String.format("/%s/dbfs/read?path=%s", DATABRICKS_JOBS_API_20, path),
                HttpMethod.GET,
                null,
                DatabricksJobLogDto.class,
                true);
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
            httpPut.setHeader("Authorization", "Bearer " + getToken(project));
            httpPut.setHeader("Content-Type", "application/octet-stream");
            httpPut.setHeader(USER_AGENT, String.format("%s/%s", appProperties.getDatabricks().getIsv().getName(),
                    appProperties.getDatabricks().getIsv().getVersion()));

            httpPut.setEntity(new ByteArrayEntity(fileBytes, ContentType.APPLICATION_OCTET_STREAM));
            LOGGER.info("File upload for {} started", projectId);
            try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
                if (response.getCode() == HttpStatus.NO_CONTENT.value()) {
                    LOGGER.info("File for project '{}' uploaded successfully", projectId);
                } else {
                    LOGGER.info("Error while uploading file for project '{}': " + response.getCode(), projectId);
                }
            }

        } catch (Exception e) {
            LOGGER.error("Error while uploading file", e);
        }
    }

    public void createDirectory(String projectId, String path) {
        sendRequest(projectId,
                String.format(URL_CREATE_DIRECTORY_FORMAT, path),
                HttpMethod.PUT,
                null,
                Object.class,
                true);
    }

    public void createSecretScope(String projectId) {
        sendRequest(projectId,
                DATABRICKS_CREATE_SECRET_SCOPE_API,
                HttpMethod.POST,
                DataBricksSecretScopeDto.builder().scope(projectId).scopeBackendType("DATABRICKS").build(),
                Object.class,
                true);
        LOGGER.info("secret scope created for {}", projectId);
    }

    public void deleteSecretScope(String projectId) {
        sendRequest(projectId,
                DATABRICKS_DELETE_SECRET_SCOPE_API,
                HttpMethod.POST,
                DatabricksSecretScopeDeleteDto.builder().scope(projectId).build(),
                Object.class,
                true);
        LOGGER.info("Secret scope deleted for {}", projectId);
    }

    public void addSecret(String projectId, Parameter parameter) {
        DataBricksSecretPutDto body = DataBricksSecretPutDto.builder()
                .scope(projectId)
                .key(parameter.getKey())
                .stringValue(wrapValue(encodeToBase64(parameter.getValue().getText())))
                .build();
        sendRequest(projectId,
                String.format("/%s/put", DATABRICKS_SECRET_API),
                HttpMethod.POST,
                body,
                Object.class,
                true);
    }

    public void deleteSecret(String projectId, String parameterId) {
        DataBricksSecretDeleteDto body = DataBricksSecretDeleteDto.builder()
                .scope(projectId)
                .key(parameterId)
                .build();
        sendRequest(projectId,
                String.format("/%s/delete", DATABRICKS_SECRET_API),
                HttpMethod.POST,
                body,
                Object.class,
                true);
    }

    /**
     * Getting cluster config.
     *
     * @param projectId for getting connection settings for databricks
     * @return Map of params
     */
    public Map<String, Object> getDatabricksClusterConfigFields(String projectId) {
        Map<String, Object> result = new HashMap<>();

        Secret project = kubernetesService.getSecret(projectId);
        String cloud = decodeFromBase64(project.getData().get(CLOUD));

        Map<?, ?> response = sendRequest(projectId,
                String.format("%s/policies/clusters/list", DATABRICKS_JOBS_API_20),
                HttpMethod.GET,
                null,
                Map.class,
                true);
        List<LinkedHashMap<String, String>> policies = (List<LinkedHashMap<String, String>>) response.get("policies");
        result.put("policies", policies
                .stream()
                .filter(policy -> !policy.containsKey("policy_family_id")
                        || "job-cluster".equals(policy.get("policy_family_id"))).toList());

        response = sendRequest(projectId, String.format("%s/clusters/spark-versions", DATABRICKS_JOBS_API_20),
                HttpMethod.GET,
                null,
                Map.class,
                true);
        result.put("versions", response.get("versions"));

        NodeTypeList responseNodeType = sendRequest(projectId,
                String.format("%s/clusters/list-node-types", DATABRICKS_JOBS_API_20),
                HttpMethod.GET,
                null,
                NodeTypeList.class,
                true);
        result.put("node_types", responseNodeType.getNodeTypes());

        if ("AWS".equals(cloud)) {
            response = sendRequest(projectId,
                    String.format("%s/clusters/list-zones", DATABRICKS_JOBS_API_20),
                    HttpMethod.GET,
                    null,
                    Map.class,
                    true);
            result.put(ZONES_FIELD, response.get(ZONES_FIELD));
        } else {
            result.put(ZONES_FIELD, new String[]{"auto"});
        }

        if ("AWS".equals(cloud)) {
            response = sendRequest(projectId, String.format("%s/instance-profiles/list", DATABRICKS_JOBS_API_20),
                    HttpMethod.GET,
                    null,
                    Map.class,
                    true);
            result.put(INSTANCE_PROFILES_FIELD, response.get(INSTANCE_PROFILES_FIELD));
        } else {
            result.put(INSTANCE_PROFILES_FIELD, Collections.emptyMap());
        }

        return result;
    }

    private String wrapValue(String value) {
        return String.format("{\"text\":\"%s\"}", value);
    }

    private String getToken(Secret project) {
        return switch (DatabricksAuthentication.AuthenticationType.valueOf(
                decodeFromBase64(project.getData().get(AUTHENTICATION_TYPE)))) {
            case PAT -> decodeFromBase64(project.getData().get(TOKEN));
            case OAUTH -> {
                try {
                    yield tokenCache.get(project.getMetadata().getName());
                } catch (ExecutionException e) {
                    throw new RuntimeException("Error while getting token from cache", e);
                }
            }
        };
    }

    /**
     * Sends an HTTP request to the Databricks server.
     *
     * @param projectId - the project ID
     * @param apiUrlWithoutHost - the API path without the host
     * @param httpMethod - the HTTP method for the request
     * @param body - the body of the request
     * @param responseType - the response type from HttpResponse
     * @return the object from the response
     */
    private <T> T sendRequest(String projectId,
                              String apiUrlWithoutHost,
                              HttpMethod httpMethod,
                              @Nullable Object body,
                              Class<T> responseType,
                              boolean enableRetry) {
        Secret project = kubernetesService.getSecret(projectId);
        StringBuilder url = new StringBuilder();
        url.append(decodeFromBase64(project.getData().get(HOST)));
        if (!apiUrlWithoutHost.startsWith("/")) {
            url.append("/");
        }
        url.append(apiUrlWithoutHost);
        HttpEntity<Object> httpEntity = makeHttpEntity(
                getToken(project),
                body
        );
        Params params;
        if (enableRetry) {
            params = Params.builder()
                        .intervals(appProperties.getDatabricks().getRetry().getIntervals())
                        .upTo(appProperties.getDatabricks().getRetry().getUpTo())
                        .build();
        } else {
            params = Params.builder().intervals("1").upTo("0").build();
        }
        RetryTemplate retryTemplate = getRetryTemplate(params);
        ResponseEntity<T> response;
        try {
            response = retryTemplate.execute((RetryCallback<ResponseEntity<T>,
                    ForRetryRestTemplateException>) (RetryContext context)->
                    databricksRestTemplate.exchange(
                            url.toString(),
                            httpMethod,
                            httpEntity,
                            responseType)
            );
        } catch (ForRetryRestTemplateException e) {
            LOGGER.info("Retry error", e);
            throw e;
        }
        if (response.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(403))) {
            tokenCache.invalidate(projectId);
            tokenCache.refresh(projectId);
            return sendRequest(projectId, apiUrlWithoutHost, httpMethod, body, responseType, enableRetry);
        } else {
            return response.getBody();
        }
    }

}
