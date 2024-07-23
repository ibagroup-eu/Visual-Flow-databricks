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

package eu.ibagroup.vfdatabricks.dto.pipelines;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.ibagroup.vfdatabricks.dto.Params;
import eu.ibagroup.vfdatabricks.dto.notifications.EmailNotification;
import eu.ibagroup.vfdatabricks.dto.notifications.SlackNotification;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.util.Set;

/**
 * Workflow parameters.
 */

@SuperBuilder
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PipelineParams extends Params {
    @Serial
    private static final long serialVersionUID = 1;

    @JsonProperty("EMAIL")
    private EmailNotification email;
    @JsonProperty("SLACK")
    private SlackNotification slack;
    @JsonProperty("DEPENDENT_PIPELINE_IDS")
    private Set<String> dependentPipelineIds;
}
