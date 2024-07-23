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

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Optional;

/**
 * Pipeline DTO class.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "DTO with pipeline's information, including it's definition and graph")
@SuperBuilder
@NoArgsConstructor
public class PipelineDto extends PipelineOverviewDto {

    @NotNull
    private JsonNode definition;
    @Schema(description = "Whether a current user can modify the pipeline")
    private boolean editable;
    private PipelineParams params;

    public boolean isEditable() {
        return true;
    }

    @Override
    public boolean isRunnable() {
        return Optional.ofNullable(this.getDefinition())
                .map(v -> v.get("graph"))
                .map(v -> !v.isEmpty())
                .orElse(false);
    }
}
