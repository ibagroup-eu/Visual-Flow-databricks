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

import eu.ibagroup.vfdatabricks.dto.connections.ConnectionDto;
import eu.ibagroup.vfdatabricks.dto.databases.PingStatusDto;
import eu.ibagroup.vfdatabricks.services.DatabasesService;
import eu.ibagroup.vfdatabricks.services.auth.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for manipulations with DB-service.
 */
@Slf4j
@Tag(name = "Databases API", description = "Manage DB connections")
@RequiredArgsConstructor
@RestController
@RequestMapping("api/db")
public class DatabasesController {

    private final DatabasesService databaseService;
    private final AuthenticationService authenticationService;

    /**
     * Method for getting connection ping status by project id and connection ID.
     *
     * @param projectId    is project id.
     * @param connectionId is connection ID.
     * @return ping status DTO.
     */
    @Operation(summary = "Ping connection by its ID",
            description = "Get a connection ping status")
    @GetMapping("/{projectId}/connections/{connectionId}")
    public PingStatusDto ping(@PathVariable String projectId, @PathVariable String connectionId) {
        LOGGER.info(
                "{} - Receiving {} connection ping status for the '{}' project",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                connectionId,
                projectId);
        return databaseService.ping(projectId, connectionId);
    }

    /**
     * Method for getting connection ping status with provided parameters.
     *
     * @param projectId     is project id.
     * @param connectionDto is JSON containing user parameters.
     * @return ping status DTO.
     */
    @Operation(summary = "Ping connection with certain parameters",
            description = "Get a connection ping status")
    @PostMapping("/{projectId}/connections")
    public PingStatusDto ping(@PathVariable final String projectId,
                              @RequestBody @Valid final ConnectionDto connectionDto) {
        LOGGER.info(
                "{} - Receiving {} connection ping status for the '{}' project",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                connectionDto.getKey(),
                projectId);
        return databaseService.ping(projectId, connectionDto);
    }
}
