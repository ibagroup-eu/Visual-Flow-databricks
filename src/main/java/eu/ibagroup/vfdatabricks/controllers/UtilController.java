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

import eu.ibagroup.vfdatabricks.services.UtilsService;
import eu.ibagroup.vfdatabricks.services.auth.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Util controller class.
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("api/util")
public class UtilController {

    private final UtilsService utilsService;
    private final AuthenticationService authenticationService;

    /**
     * Get additional job params fields from Databricks.
     */
    @GetMapping("{projectId}/cluster/config/fields")
    public Map<String, Object> getClusterConfigFields(@PathVariable String projectId) {
        LOGGER.info(
                "{} - Receiving databricks job cluster config fields",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo())
        );
        return utilsService.getDatabricksClusterConfigFields(projectId);
    }
}
