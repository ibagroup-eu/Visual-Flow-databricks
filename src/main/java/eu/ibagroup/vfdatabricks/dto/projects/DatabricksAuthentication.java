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

import eu.ibagroup.vfdatabricks.validators.ValidDatabricksAuthentication;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import static eu.ibagroup.vfdatabricks.dto.Constants.MAX_TOKEN_LENGTH;

/**
 * Databricks' authentication data.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
@ValidDatabricksAuthentication
public class DatabricksAuthentication {
    @Size(max = MAX_TOKEN_LENGTH)
    private String token;
    private String clientId;
    private String secret;
    @NotNull
    private AuthenticationType authenticationType;


    public enum AuthenticationType {
        PAT,
        OAUTH
    }

}
