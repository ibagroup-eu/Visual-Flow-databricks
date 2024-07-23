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

import eu.ibagroup.vfdatabricks.dto.jobs.JobDto;
import eu.ibagroup.vfdatabricks.dto.pipelines.PipelineDto;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Import request DTO class.
 * Mainly used for importing jobs and pipelines.
 * Need to be merged with {@link eu.ibagroup.vfdatabricks.dto.exporting.ExportResponseDto},
 * since contains the same information.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Schema(description = "DTO with structure of jobs/pipelines in serialized JSON format that will be imported")
public class ImportRequestDto {
    @NotNull
    @ArraySchema(arraySchema = @Schema(description = "List of pipelines' structures"))
    private List<PipelineDto> pipelines;
    @NotNull
    @ArraySchema(arraySchema = @Schema(description = "List of jobs' structures"))
    private List<JobDto> jobs;
}
