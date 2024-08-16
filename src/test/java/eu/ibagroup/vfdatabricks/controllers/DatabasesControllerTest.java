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
import eu.ibagroup.vfdatabricks.model.auth.UserInfo;
import eu.ibagroup.vfdatabricks.services.DatabasesService;
import eu.ibagroup.vfdatabricks.services.auth.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabasesControllerTest {

    @Mock
    private DatabasesService databasesService;
    @Mock
    private AuthenticationService authenticationService;
    private DatabasesController controller;

    @BeforeEach
    public void setUp() {
        controller = new DatabasesController(databasesService, authenticationService);
        UserInfo expected = new UserInfo();
        expected.setName("name");
        expected.setId("id");
        expected.setUsername("username");
        expected.setEmail("email");
        when(authenticationService.getUserInfo()).thenReturn(expected);
    }

    @Test
    void testPing() {
        PingStatusDto dto = PingStatusDto
                .builder()
                .status(false)
                .message("Test error")
                .build();
        when(databasesService.ping("projectId", "conId")).thenReturn(dto);
        PingStatusDto response = controller.ping("projectId", "conId");
        assertEquals(dto, response, "Response must be equal to dto");
        verify(databasesService).ping(anyString(), anyString());
    }

    @Test
    void testPingWithParams() {
        PingStatusDto dto = PingStatusDto
                .builder()
                .status(false)
                .message("Test error")
                .build();
        ConnectionDto connectionDto = ConnectionDto.builder()
                .key("key")
                .value(Map.of())
                .build();
        when(databasesService.ping("projectId", connectionDto)).thenReturn(dto);
        PingStatusDto response = controller.ping("projectId", connectionDto);
        assertEquals(dto, response, "Response must be equal to dto");
        verify(databasesService).ping(anyString(), any(ConnectionDto.class));
    }
}
