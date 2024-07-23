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

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.ibagroup.vfdatabricks.dto.ParameterDto;
import eu.ibagroup.vfdatabricks.dto.ParameterOverviewDto;
import eu.ibagroup.vfdatabricks.services.ParameterService;
import eu.ibagroup.vfdatabricks.services.auth.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("api/project")
public class ParameterController {

    private final ParameterService parameterService;
    private final AuthenticationService authenticationService;

    @Operation(summary = "Create parameter", description = "Create new parameter in existing project")
    @PostMapping("/{projectId}/params/{paramId}")
    public ResponseEntity<String> create(@PathVariable String projectId,
                                         @PathVariable String paramId,
                                         @RequestBody final ParameterDto parameterDto) throws JsonProcessingException {
        LOGGER.info(
                "{} - Creating parameter",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo())
        );
        parameterService.create(projectId, paramId, parameterDto);
        LOGGER.info(
                "{} - Parameter '{}' successfully created",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                paramId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(paramId);
    }

    @Operation(summary = "Get information about the parameter", description = "Fetch parameter by id")
    @GetMapping("{projectId}/params/{paramId}")
    public ParameterDto get(@PathVariable String projectId, @PathVariable String paramId)
            throws JsonProcessingException {
        LOGGER.info(
                "{} - Receiving parameter '{}' in project '{}'",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                paramId,
                projectId
        );
        return parameterService.get(projectId, paramId);
    }

    @Operation(summary = "Get all parameters in a project",
            description = "Get information about all parameters in a project")
    @GetMapping("{projectId}/params")
    public ParameterOverviewDto getAll(@PathVariable String projectId) {
        LOGGER.info(
                "{} - Receiving all parameters in project '{}'",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                projectId
        );
        return parameterService.getAll(projectId);
    }

    @Operation(summary = "Update existing parameter", description = "Update existing parameter")
    @PutMapping("{projectId}/params/{paramId}")
    public ResponseEntity<Void> update(
            @PathVariable String projectId,
            @PathVariable String paramId,
            @RequestBody ParameterDto parameterDto) throws JsonProcessingException {
        LOGGER.info(
                "{} - Updating parameter '{}' in project '{}'",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                paramId,
                projectId
        );
        parameterService.update(projectId, paramId, parameterDto);
        LOGGER.info(
                "Parmeter '{}' in project '{}' successfully updated by {}",
                paramId,
                projectId,
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo())
                );
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * Deleting job in project by id.
     *
     * @param projectId project id
     * @param parameterId        parameter id
     */
    @Operation(summary = "Delete the parameter", description = "Delete existing parameter", responses = {
            @ApiResponse(responseCode = "204", description = "Indicates successful parameter deletion")})
    @DeleteMapping("{projectId}/params/{parameterId}")
    public ResponseEntity<Void> delete(@PathVariable String projectId, @PathVariable String parameterId) {
        LOGGER.info(
                "{} - Deleting '{}' parameter in project '{}'",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()),
                parameterId,
                projectId
        );
        parameterService.delete(projectId, parameterId);
        LOGGER.info(
                "Parameter '{}' in project '{}' successfully deleted by {}",
                parameterId,
                projectId,
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo())
        );
        return ResponseEntity.noContent().build();
    }
}
