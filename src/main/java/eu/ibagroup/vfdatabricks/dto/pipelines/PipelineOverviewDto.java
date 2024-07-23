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

import eu.ibagroup.vfdatabricks.dto.jobs.CommonDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

/**
 * Pipeline response DTO class.
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "DTO with basic info about the pipeline")
public class PipelineOverviewDto extends CommonDto {
    @Schema(description = "Pipeline's completion progress")
    private double progress;
    @Schema(description = "Whether pipeline is represented by scheduled workflow")
    private boolean cron;
    @Schema(description = "If true Workflow scheduling will not occur")
    private boolean cronSuspend;
    @Schema(description = "Pipeline's list of tags")
    private List<String> tags;
    @Schema(description = "Pipeline's list of dependencies")
    private List<String> dependentPipelineIds;
    private Map<String, String> jobsStatuses;
}
