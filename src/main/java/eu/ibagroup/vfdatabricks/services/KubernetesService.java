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
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static eu.ibagroup.vfdatabricks.dto.Constants.*;

@Slf4j
@Service
public class KubernetesService {
    protected final String appName;
    protected final String appNameLabel;
    protected final KubernetesClient client;

    @Autowired
    public KubernetesService(
            ApplicationConfigurationProperties appProperties,
            final KubernetesClient client) {
        this.appName = appProperties.getNamespace().getApp();
        this.appNameLabel = appProperties.getNamespace().getLabel();
        this.client = client;
    }


    /**
     * Creates secret.
     *
     * @param secretName name of secret.
     * @param secret    secret.
     */
    public void createSecret(final String secretName, final Secret secret) {
        client.secrets().inNamespace(appName).resource(new SecretBuilder(secret)
                        .editMetadata()
                        .withName(secretName)
                        .addToLabels(APP, appNameLabel)
                        .addToLabels(TYPE, PROJECT)
                        .endMetadata()
                        .build())
                .create();
    }

    /**
     * Updates secret.
     *
     * @param secretName name of secret.
     * @param secret    secret.
     */
    public void updateSecret(final String secretName, final Secret secret) {
        client.secrets().inNamespace(appName).withName(secretName).edit(
                s -> new SecretBuilder(secret)
                        .editMetadata()
                        .withName(secretName)
                        .addToLabels(APP, appNameLabel)
                        .addToLabels(TYPE, PROJECT)
                        .endMetadata()
                        .build()
        );
    }

    /**
     * Gets secret.
     *
     * @param secretName name of the secret.
     * @return secret.
     */
    public Secret getSecret(final String secretName) {
        return client.secrets().inNamespace(appName).withName(secretName).get();
    }

    /**
     * Get secrets based on certain labels.
     *
     * @param labels    labels
     * @return list of secrets
     */
    public List<Secret> getSecretsByLabels(final Map<String, String> labels) {
        return client.secrets().inNamespace(appName).withLabels(labels).list().getItems();
    }

    /**
     * Delete secret.
     *
     * @param secretName name of the secret.
     */
    public void deleteSecret(final String secretName) {
        client.secrets().inNamespace(appName).withName(secretName).delete();
    }

}
