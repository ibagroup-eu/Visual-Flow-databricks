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

package eu.ibagroup.vfdatabricks.dto.projects;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

import static eu.ibagroup.vfdatabricks.dto.Constants.*;

/**
 * Project request DTO class.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class ProjectRequestDto {
    @Pattern(regexp = NAME_PATTERN)
    private String name;
    @NotNull
    @Size(max = MAX_DESCRIPTION_LENGTH)
    private String description;
    @Pattern(regexp = HOST_PATTERN)
    private String host;
    @Valid
    private DatabricksAuthentication authentication;
    private String pathToFile;
    private String cloud;
    private String isUpdating;
    private String jarHash;

    public Secret toSecret() {
        if (pathToFile == null) {
            pathToFile = "";
        }
        if (isUpdating == null) {
            isUpdating = "false";
        }
        if (jarHash == null) {
            jarHash = "temp";
        }
        String token = switch (authentication.getAuthenticationType()) {
            case PAT -> authentication.getToken();
            case OAUTH -> getAuthentication().getClientId() + ":" + authentication.getSecret();
        };

        return new SecretBuilder()
                .addToStringData(Map.of(HOST, host,
                        TOKEN, token,
                        AUTHENTICATION_TYPE, authentication.getAuthenticationType().name(),
                        PATH_TO_FILE, pathToFile,
                        CLOUD, cloud,
                        UPDATING, isUpdating,
                        HASH, jarHash))
                .withNewMetadata()
                .withAnnotations(Map.of(NAME, name, DESCRIPTION, description))
                .endMetadata()
                .build();
    }
}
