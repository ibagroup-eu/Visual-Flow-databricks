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

package eu.ibagroup.vfdatabricks.dto.importing;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Import response DTO class.
 * Contains general information after completing import jobs and pipelines.
 * Contains information about not imported jobs and pipelines.
 * Contains information about errors, occurred during the import process.
 */
@NoArgsConstructor
@Getter
@Builder
@EqualsAndHashCode
@ToString
@Schema(description = "DTO that contains jobs/pipelines that were not imported")
public class ImportResponseDto {
    @ArraySchema(arraySchema = @Schema(description = "Ids of the jobs that were not imported"))
    private final Set<String> notImportedJobs = new HashSet<>();
    @ArraySchema(arraySchema = @Schema(description = "Ids of the pipelines that were not imported"))
    private final Set<String> notImportedPipelines = new HashSet<>();
    private final Map<String, List<String>> errorsInJobs = new HashMap<>();
    private final Map<String, List<String>> errorsInPipelines = new HashMap<>();
    private final Map<String, List<MissingParamDto>> missingProjectParams = new HashMap<>();
    private final Map<String, List<MissingParamDto>> missingProjectConnections = new HashMap<>();
}
