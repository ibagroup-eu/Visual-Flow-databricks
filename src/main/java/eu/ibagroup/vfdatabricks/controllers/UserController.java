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

import eu.ibagroup.vfdatabricks.model.auth.UserInfo;
import eu.ibagroup.vfdatabricks.services.auth.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * User controller class.
 */
@Slf4j
@Tag(name = "User API", description = "Get information about app users")
@RequiredArgsConstructor
@RestController
@RequestMapping("api")
public class UserController {

    private final AuthenticationService authenticationService;

    /**
     * Retrieves current user information.
     *
     * @return user information.
     */
    @Operation(summary = "Get current user's information", description = "Get essential information about " +
            "current user")
    @GetMapping("/user")
    public UserInfo getUser() {
        LOGGER.info(
                "{} - Receiving information about current user",
                AuthenticationService.getFormattedUserInfo(authenticationService.getUserInfo()));
        return authenticationService.getUserInfo();
    }
}
