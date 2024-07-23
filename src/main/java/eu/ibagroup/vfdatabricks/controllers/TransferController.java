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

import eu.ibagroup.vfdatabricks.dto.exporting.ExportRequestDto;
import eu.ibagroup.vfdatabricks.dto.exporting.ExportResponseDto;
import eu.ibagroup.vfdatabricks.dto.importing.ImportRequestDto;
import eu.ibagroup.vfdatabricks.dto.importing.ImportResponseDto;
import eu.ibagroup.vfdatabricks.services.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("api/project")
public class TransferController {

    private final TransferService transferService;

    /**
     * Export.
     *
     * @param projectId        project id
     * @param exportRequestDto dto with job ids and pipelines for export
     * @return object with exported jobs and pipelines
     */
    @Operation(summary = "Export pipelines/jobs", description = "Export existing pipelines/jobs into JSON file")
    @PostMapping("{projectId}/exportResources")
    public ResponseEntity<ExportResponseDto> exporting(
            @PathVariable String projectId, @RequestBody @Valid ExportRequestDto exportRequestDto) {
        return transferService.exporting(projectId, exportRequestDto);
    }

    /**
     * Import.
     *
     * @param projectId        project id
     * @param importRequestDto dto with jobs ids and pipelines ids for export
     * @return object witch contains not imported ids of pipelines and jobs
     */
    @Operation(summary = "Import pipelines/jobs", description = "Import pipelines/jobs into a specific project " +
            "from JSON structure")
    @PostMapping("{projectId}/importResources")
    public ResponseEntity<ImportResponseDto> importing(
            @PathVariable String projectId, @RequestBody @Valid ImportRequestDto importRequestDto) {
        return transferService.importing(projectId, importRequestDto);
    }
}
