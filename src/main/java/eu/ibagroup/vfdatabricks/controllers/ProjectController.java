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

package eu.ibagroup.vfdatabricks.controllers;

import eu.ibagroup.vfdatabricks.dto.projects.ProjectOverviewListDto;
import eu.ibagroup.vfdatabricks.dto.projects.ProjectRequestDto;
import eu.ibagroup.vfdatabricks.dto.projects.ProjectResponseDto;
import eu.ibagroup.vfdatabricks.services.ProjectService;
import eu.ibagroup.vfdatabricks.services.auth.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;


/**
 * Project controller class.
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("api/project")
@Validated
public class ProjectController {
    private final ProjectService projectService;
    private final AuthenticationService authenticationService;

    /**
     * Creates new project.
     *
     * @param projectDto object that contains initial data
     * @return ResponseEntity with status code
     */
    @PostMapping
    @PreAuthorize("hasAuthority('SUPERUSER')")
    public ResponseEntity<String> create(@RequestBody @Valid final ProjectRequestDto projectDto) throws IOException {
        LOGGER.info(
                "{} - Creating project",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo())
        );
        String id = projectService.create(projectDto);
        LOGGER.info(
                "{} - Project '{}' successfully created",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                id
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(id);
    }

    /**
     * Gets project by id.
     *
     * @param projectId project id.
     * @return ResponseEntity with status code and project date (ProjectDto).
     */
    @GetMapping("/{projectId}")
    @PreAuthorize("hasAuthority('SUPERUSER')")
    public ProjectResponseDto get(@PathVariable final String projectId) {
        LOGGER.info(
                "{} - Receiving project '{}' ",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                projectId
        );
        return projectService.get(projectId);
    }

    /**
     * Gets projects list.
     *
     * @return project list.
     */
    @GetMapping
    public ProjectOverviewListDto getAll() {
        LOGGER.info(
                "{} - Receiving list of projects",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo())
        );
        ProjectOverviewListDto projectOverviewListDto = projectService.getAll();
        if (!authenticationService.getUserInfo().isSuperuser()) {
            projectOverviewListDto.setEditable(false);
            projectOverviewListDto.getProjects().forEach(p -> p.setLocked(true));
        }
        return projectOverviewListDto;
    }

    /**
     * Change project params.
     *
     * @param projectId  project id.
     * @param projectDto new project params.
     */
    @PostMapping("/{projectId}")
    @PreAuthorize("hasAuthority('SUPERUSER')")
    public void update(
            @PathVariable final String projectId, @RequestBody @Valid final ProjectRequestDto projectDto)
            throws IOException {
        LOGGER.info(
                "{} - Updating project '{}'",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                projectId
        );
        projectService.update(projectId, projectDto);
        LOGGER.info(
                "{} - Project '{}' description and resource quota successfully updated",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                projectId
        );
    }

    /**
     * Deletes project by id.
     *
     * @param projectId project id.
     * @return ResponseEntity with 204 status code.
     */
    @DeleteMapping("/{projectId}")
    @PreAuthorize("hasAuthority('SUPERUSER')")
    public ResponseEntity<Void> delete(@PathVariable final String projectId) {
        LOGGER.info(
                "{} - Deleting project '{}'",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                projectId
        );
        projectService.delete(projectId);
        LOGGER.info(
                "{} - Project '{}' successfully deleted",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                projectId
        );
        return ResponseEntity.noContent().build();
    }
}
