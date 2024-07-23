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

import eu.ibagroup.vfdatabricks.dto.NodeTypeList;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.JobLogDto;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Matcher;

import static eu.ibagroup.vfdatabricks.dto.Constants.*;

/**
 * Util class for application.
 */
@Service
@Slf4j
public class UtilsService {

    private final RestTemplate databricksRestTemplate;
    private final KubernetesService kubernetesService;

    public UtilsService(@Qualifier("databricksRestTemplate") RestTemplate databricksRestTemplate,
                        KubernetesService kubernetesService) {
        this.databricksRestTemplate = databricksRestTemplate;
        this.kubernetesService = kubernetesService;
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
        HttpEntity<Void> databricksEntity = makeHttpEntity(
                decodeFromBase64(project.getData().get(TOKEN)),
                null
        );
        String baseUrl = decodeFromBase64(project.getData().get(HOST));

        Map<?, ?> response = getResponse(databricksEntity, String.format("%s/%s/policies/clusters/list",
                baseUrl,
                DATABRICKS_JOBS_API_20), Map.class);
        List<LinkedHashMap<String, String>> policies = (List<LinkedHashMap<String, String>>) response.get("policies");
        result.put("policies", policies
                .stream()
                .filter(policy -> !policy.containsKey("policy_family_id")
                        || "job-cluster".equals(policy.get("policy_family_id"))).toList());

        response = getResponse(databricksEntity, String.format("%s/%s/clusters/spark-versions",
                baseUrl,
                DATABRICKS_JOBS_API_20), Map.class);
        result.put("versions", response.get("versions"));

        NodeTypeList responseNodeType = getResponse(databricksEntity,
                String.format("%s/%s/clusters/list-node-types",
                        baseUrl,
                        DATABRICKS_JOBS_API_20),
                NodeTypeList.class
        );
        result.put("node_types", responseNodeType.getNodeTypes());

        if ("GCP".equals(cloud)) {
            result.put(ZONES_FIELD, new String[]{"auto"});
        } else {
            response = getResponse(databricksEntity, String.format("%s/%s/clusters/list-zones",
                    baseUrl,
                    DATABRICKS_JOBS_API_20), Map.class);
            result.put(ZONES_FIELD, response.get(ZONES_FIELD));
        }

        if ("AWS".equals(cloud)) {
            response = getResponse(databricksEntity, String.format("%s/%s/instance-profiles/list",
                    baseUrl,
                    DATABRICKS_JOBS_API_20), Map.class);
            result.put(INSTANCE_PROFILES_FIELD, response.get(INSTANCE_PROFILES_FIELD));
        } else {
            result.put(INSTANCE_PROFILES_FIELD, Collections.emptyMap());
        }

        return result;
    }

    private <T> T getResponse(HttpEntity<Void> databricksEntity, String url, Class<T> responseType) {
        return Objects.requireNonNull(databricksRestTemplate.exchange(
                url,
                HttpMethod.GET,
                databricksEntity,
                responseType
        ).getBody());
    }

    /**
     * Getting valid K8s name.
     *
     * @param name invalid name
     * @return valid name
     */
    static String getValidK8sName(final String name) {
        return NON_WORD_PATTERN.matcher(name).replaceAll("-").toLowerCase(Locale.getDefault());
    }

    /**
     * Encoding to base64.
     *
     * @param value string to be encoded
     * @return encoded value
     */
    static String encodeToBase64(final String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decoding base64 value.
     *
     * @param encodedString Base64 encoded string
     * @return decoded value
     */
    static String decodeFromBase64(final String encodedString) {
        return new String(Base64.getDecoder().decode(encodedString), StandardCharsets.UTF_8);
    }

    /**
     * Creating HttpEntity
     *
     * @param token token for request
     * @param body body for request
     * @return HttpEntity
     */
    static <T> HttpEntity<T> makeHttpEntity(final String token, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }

    static String toFormattedString(long millis) {
        return DATE_TIME_FORMATTER.format(
                ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
        );
    }

    /**
     * Parsing logs
     *
     * @param logs
     * @return
     */
    static List<JobLogDto> getParsedDBLogs(String logs) {
        return getParsedLogs(() -> logs);
    }

    static List<JobLogDto> getParsedLogs(Supplier<String> logSupplier) {
        try {
            String logs = Objects.toString(logSupplier.get(), "");
            String[] logItems = logs.split("\n");
            List<JobLogDto> logResults = checkLogItems(logItems);

            if (logResults.isEmpty()) {
                logResults.add(JobLogDto.builder().message(logs).build());
            }

            return logResults;
        } catch (ResourceNotFoundException e) {
            LOGGER.info("Error:", e);
            return Collections.emptyList();
        }
    }
    static List<JobLogDto> checkLogItems(String[] logItems) {
        List<JobLogDto> logResults = new ArrayList<>();
        int logIndex = 0;
        for (String logItem : logItems) {
            Matcher matcher = LOG_PATTERN.matcher(logItem);
            if (matcher.matches()) {
                logResults.add(JobLogDto.fromMatcher(matcher));
                logIndex++;
            } else {
                if (logIndex != 0) {
                    JobLogDto lastLog = logResults.get(logIndex - 1);
                    logResults.set(logIndex - 1, lastLog.withMessage(lastLog.getMessage() + "\n" + logItem));
                }
            }
        }
        return logResults;
    }
}
