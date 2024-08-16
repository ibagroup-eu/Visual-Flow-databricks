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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import static eu.ibagroup.vfdatabricks.dto.Constants.*;
import static eu.ibagroup.vfdatabricks.dto.Constants.UPDATING;
import static eu.ibagroup.vfdatabricks.services.UtilsService.decodeFromBase64;

/**
 * Project overview DTO class.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ProjectOverviewDto {
    private String name;
    private String id;
    private String description;
    private String pathToFile;
    private String cloud;
    private boolean isLocked;
    private String host;
    private String jarHash;
    private String isUpdating;
    private DatabricksAuthentication authentication;

    public static ProjectOverviewDto fromSecret(Secret project) {
        String pathToFile = "";
        if (project.getData().get(PATH_TO_FILE) != null) {
            pathToFile = decodeFromBase64(project.getData().get(PATH_TO_FILE));
        }
        String cloud = "";
        if (project.getData().get(CLOUD) != null) {
            cloud = decodeFromBase64(project.getData().get(CLOUD));
        }
        DatabricksAuthentication authentication = DatabricksAuthentication.builder()
                .authenticationType(DatabricksAuthentication.AuthenticationType
                        .valueOf(decodeFromBase64(project.getData().get(AUTHENTICATION_TYPE))))
                .build();
        if (DatabricksAuthentication.AuthenticationType.PAT == (authentication.getAuthenticationType())) {
            authentication.setToken(decodeFromBase64(project.getData().get(TOKEN)));
        } else {
            authentication.setClientId(decodeFromBase64(project.getData().get(TOKEN)).split(":")[0]);
            authentication.setSecret(decodeFromBase64(project.getData().get(TOKEN)).split(":")[1]);
        }
        return ProjectOverviewDto.builder()
                .id(project.getMetadata().getName())
                .name(project.getMetadata().getAnnotations().get(NAME))
                .description(project.getMetadata().getAnnotations().get(DESCRIPTION))
                .pathToFile(pathToFile)
                .cloud(cloud)
                .isLocked(false)
                .host(decodeFromBase64(project.getData().get(HOST)))
                .jarHash(decodeFromBase64(project.getData().get(HASH)))
                .authentication(authentication)
                .isUpdating(decodeFromBase64(project.getData().get(UPDATING)))
                .build();
    }
}
