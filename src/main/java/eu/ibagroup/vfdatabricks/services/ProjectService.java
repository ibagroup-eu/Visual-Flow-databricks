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
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DataBricksSecretScopeDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksSecretScopeDeleteDto;
import eu.ibagroup.vfdatabricks.dto.projects.ProjectOverviewDto;
import eu.ibagroup.vfdatabricks.dto.projects.ProjectOverviewListDto;
import eu.ibagroup.vfdatabricks.dto.projects.ProjectRequestDto;
import eu.ibagroup.vfdatabricks.dto.projects.ProjectResponseDto;
import io.fabric8.kubernetes.api.model.Secret;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

import static eu.ibagroup.vfdatabricks.dto.Constants.*;
import static eu.ibagroup.vfdatabricks.services.UtilsService.decodeFromBase64;
import static eu.ibagroup.vfdatabricks.services.UtilsService.makeHttpEntity;

/**
 * ProjectService class.
 */
@Slf4j
@Service
public class ProjectService {
    private final ApplicationConfigurationProperties appProperties;
    private final KubernetesService kubernetesService;
    private final RestTemplate databricksRestTemplate;
    private final AsyncDeleteProjectDataService asyncDeleteProjectDataService;
    private final AsyncUploadJarService asyncUploadJarService;

    public ProjectService(ApplicationConfigurationProperties appProperties,
                          KubernetesService kubernetesService,
                          @Qualifier("databricksRestTemplate") RestTemplate databricksRestTemplate,
                          AsyncDeleteProjectDataService asyncDeleteProjectDataService,
                          AsyncUploadJarService asyncUploadJarService) {
        this.appProperties = appProperties;
        this.kubernetesService = kubernetesService;
        this.databricksRestTemplate = databricksRestTemplate;
        this.asyncDeleteProjectDataService = asyncDeleteProjectDataService;
        this.asyncUploadJarService = asyncUploadJarService;
    }

    private String withNamespacePrefix(final String name) {
        return appProperties.getNamespace().getPrefix() + name;
    }

    /**
     * Creates project.
     *
     * @param projectDto project transfer object.
     */
    public String create(@Valid final ProjectRequestDto projectDto) throws IOException {
        String id = withNamespacePrefix(UtilsService.getValidK8sName(projectDto.getName()));
        Secret secret = projectDto.toSecret();
        Path path = Paths.get(appProperties.getJarHash());
        String jarHashFromFileSystem = Files.readString(path);
        secret.getStringData().put(HASH, jarHashFromFileSystem);
        secret.getStringData().put(UPDATING, "true");
        kubernetesService.createSecret(id, secret);
        LOGGER.info("Project {} successfully created", id);
        Secret project = kubernetesService.getSecret(id);
        HttpEntity<DataBricksSecretScopeDto> databricksEntity = makeHttpEntity(
                decodeFromBase64(project.getData().get(TOKEN)), DataBricksSecretScopeDto.builder()
                        .scope(id)
                        .scopeBackendType("DATABRICKS").build());
        databricksRestTemplate.exchange(
                String.format("%s/%s/create",
                        decodeFromBase64(project.getData().get(HOST)),
                        DATABRICKS_SECRET_SCOPE_API),
                HttpMethod.POST,
                databricksEntity,
                Object.class
        );
        asyncUploadJarService.uploadJarFileToDatabricks(id, projectDto.getPathToFile())
                .whenComplete((Object object, Throwable exception) -> {
                    if (exception != null) {
                        LOGGER.error("Jar upload failed: {}", exception.getMessage(), exception);
                    } else {
                        try {
                            update(id, projectDto);
                        } catch (IOException e) {
                            LOGGER.error(e.getMessage(), e);
                        }
                    }
                });

        return id;
    }

    /**
     * Gets project by id.
     *
     * @param id project id.
     * @return project transfer object.
     */
    public ProjectResponseDto get(final String id) {
        Secret project = kubernetesService.getSecret(id);
        String pathToFile = "";
        if (project.getData().get(PATH_TO_FILE) != null) {
            pathToFile = decodeFromBase64(project.getData().get(PATH_TO_FILE));
        }
        String cloud = "";
        if (project.getData().get(CLOUD) != null) {
            cloud = decodeFromBase64(project.getData().get(CLOUD));
        }
        return ProjectResponseDto.builder()
                .id(project.getMetadata().getName())
                .name(project.getMetadata().getAnnotations().get(NAME))
                .description(project.getMetadata().getAnnotations().get(DESCRIPTION))
                .host(decodeFromBase64(project.getData().get(HOST)))
                .token(decodeFromBase64(project.getData().get(TOKEN)))
                .pathToFile(pathToFile)
                .cloud(cloud)
                .editable(true)
                .locked(false)
                .demo(false)
                .isUpdating(decodeFromBase64(project.getData().get(UPDATING)))
                .build();
    }

    /**
     * Gets all project names.
     *
     * @return list of project names.
     */
    public ProjectOverviewListDto getAll() {
        return ProjectOverviewListDto.builder()
                .projects(
                        kubernetesService.getSecretsByLabels(Map.of(TYPE, PROJECT))
                                .stream()
                                .map( (Secret project) -> {
                                            String pathToFile = "";
                                            if (project.getData().get(PATH_TO_FILE) != null) {
                                                pathToFile = decodeFromBase64(project.getData().get(PATH_TO_FILE));
                                            }
                                            String cloud = "";
                                            if (project.getData().get(CLOUD) != null) {
                                                cloud = decodeFromBase64(project.getData().get(CLOUD));
                                            }
                                            return ProjectOverviewDto.builder()
                                                    .id(project.getMetadata().getName())
                                                    .name(project.getMetadata().getAnnotations().get(NAME))
                                                    .description(project.getMetadata()
                                                            .getAnnotations().get(DESCRIPTION))
                                                    .pathToFile(pathToFile)
                                                    .cloud(cloud)
                                                    .isLocked(false)
                                                    .host(decodeFromBase64(project.getData().get(HOST)))
                                                    .jarHash(decodeFromBase64(project.getData().get(HASH)))
                                                    .token(decodeFromBase64(project.getData().get(TOKEN)))
                                                    .isUpdating(decodeFromBase64(project.getData().get(UPDATING)))
                                                    .build();
                                        }
                                )
                                .collect(Collectors.toList())
                )
                .editable(true)
                .build();
    }

    /**
     * Updates project.
     *
     * @param id         project id.
     * @param projectDto new project params.
     */
    public void update(final String id, @Valid final ProjectRequestDto projectDto) throws IOException {
        Secret secret = projectDto.toSecret();
        Path path = Paths.get(appProperties.getJarHash());
        String jarHashFromFileSystem = Files.readString(path);
        secret.getStringData().put(HASH, jarHashFromFileSystem);
        kubernetesService.updateSecret(id, secret);
    }

    /**
     * Deletes project by id.
     *
     * @param id project id.
     */
    public void delete(final String id) {
        Secret project = kubernetesService.getSecret(id);
        kubernetesService.deleteSecret(id);
        asyncDeleteProjectDataService.deleteProjectData(id);
        HttpEntity<DatabricksSecretScopeDeleteDto> databricksEntity = makeHttpEntity(
                decodeFromBase64(project.getData().get(TOKEN)),
                DatabricksSecretScopeDeleteDto.builder().scope(id).build());
        databricksRestTemplate.exchange(
                String.format("%s/%s/delete",
                        decodeFromBase64(project.getData().get(HOST)),
                        DATABRICKS_SECRET_SCOPE_API),
                HttpMethod.POST,
                databricksEntity,
                Object.class
        );

    }
}
