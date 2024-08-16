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
import eu.ibagroup.vfdatabricks.dto.projects.ProjectOverviewDto;
import eu.ibagroup.vfdatabricks.dto.projects.ProjectOverviewListDto;
import eu.ibagroup.vfdatabricks.dto.projects.ProjectRequestDto;
import eu.ibagroup.vfdatabricks.dto.projects.ProjectResponseDto;
import io.fabric8.kubernetes.api.model.Secret;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

import static eu.ibagroup.vfdatabricks.dto.Constants.*;

/**
 * ProjectService class.
 */
@Slf4j
@Service
public class ProjectService {
    private final ApplicationConfigurationProperties appProperties;
    private final KubernetesService kubernetesService;
    private final AsyncDeleteProjectDataService asyncDeleteProjectDataService;
    private final AsyncUploadJarService asyncUploadJarService;
    private final DatabricksAPIService databricksAPIService;

    public ProjectService(ApplicationConfigurationProperties appProperties,
                          KubernetesService kubernetesService,
                          AsyncDeleteProjectDataService asyncDeleteProjectDataService,
                          AsyncUploadJarService asyncUploadJarService,
                          DatabricksAPIService databricksAPIService) {
        this.appProperties = appProperties;
        this.kubernetesService = kubernetesService;
        this.asyncDeleteProjectDataService = asyncDeleteProjectDataService;
        this.asyncUploadJarService = asyncUploadJarService;
        this.databricksAPIService = databricksAPIService;
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
        databricksAPIService.createSecretScope(id);
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
        return ProjectResponseDto.fromSecret(project);
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
                                .map(ProjectOverviewDto::fromSecret)
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
        try {
            databricksAPIService.deleteSecretScope(id);
        } catch (RuntimeException e) {
            LOGGER.info("Can't delete secret scope for {}", id, e);
        }
        kubernetesService.deleteSecret(id);
        asyncDeleteProjectDataService.deleteProjectData(id);
    }
}
