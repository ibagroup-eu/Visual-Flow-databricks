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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JarUpdateService {

    private final ProjectService projectService;
    private final ApplicationConfigurationProperties appProperties;
    private final AsyncUploadJarService asyncUploadJarService;

    public JarUpdateService(ProjectService projectService, ApplicationConfigurationProperties appProperties,
                            AsyncUploadJarService asyncUploadJarService) {
        this.projectService = projectService;
        this.appProperties = appProperties;
        this.asyncUploadJarService = asyncUploadJarService;
    }

    public void updateJar() throws IOException {
        Path path = Paths.get(appProperties.getJarHash());
        if (Files.exists(path)) {
            String jarHashFromFileSystem = Files.readString(path);
            ProjectOverviewListDto projectOverviewListDto = projectService.getAll();
            List<ProjectOverviewDto> uniqueProjects = projectOverviewListDto.getProjects().stream()
                    .collect(Collectors.toMap((ProjectOverviewDto
                            project)-> project.getHost() + ":" + project.getPathToFile(),
                            Function.identity(),
                            (existing, replacement) -> existing
                    ))
                    .values()
                    .stream()
                    .toList();

            if (!uniqueProjects.isEmpty() && (uniqueProjects.get(0).getJarHash() == null
                    || !uniqueProjects.get(0).getJarHash().equals(jarHashFromFileSystem))) {
                LOGGER.info("The process of updating jar files has started");
                projectOverviewListDto.getProjects().forEach((ProjectOverviewDto project )-> {
                    try {
                        projectService.update(project.getId(),
                                ProjectRequestDto
                                        .builder()
                                        .name(project.getName())
                                        .host(project.getHost())
                                        .cloud(project.getCloud())
                                        .token(project.getToken())
                                        .description(project.getDescription())
                                        .pathToFile(project.getPathToFile())
                                        .isUpdating("true")
                                        .build());
                    } catch (IOException e) {
                       LOGGER.info("Error while updating project", e);
                    }
                });
                CompletableFuture<?>[] futures = uniqueProjects
                        .stream()
                        .map(project -> asyncUploadJarService.uploadJarFileToDatabricks(project.getId(),
                                project.getPathToFile()))
                        .toArray(CompletableFuture[]::new);
                CompletableFuture.allOf(futures).whenComplete((Object object, Throwable exception) -> {
                    if (exception != null) {
                        LOGGER.error("Jar upload failed: {}", exception.getMessage(), exception);
                    } else {
                        projectOverviewListDto.getProjects().forEach((ProjectOverviewDto project)-> {
                            try {
                                projectService.update(project.getId(),
                                        ProjectRequestDto
                                                .builder()
                                                .name(project.getName())
                                                .host(project.getHost())
                                                .cloud(project.getCloud())
                                                .token(project.getToken())
                                                .description(project.getDescription())
                                                .pathToFile(project.getPathToFile())
                                                .isUpdating("false")
                                                .build());
                            } catch (IOException e) {
                                LOGGER.info("Error while updating project", e);
                            }
                        });
                        LOGGER.info("Jar files updated successfully");
                    }
                });
            } else {
                LOGGER.info("The files in project are up to date");
            }
        } else {
            LOGGER.info("File not found: " + path + ". Skipping jar update");
        }
    }

}
