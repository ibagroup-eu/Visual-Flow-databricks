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

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Project response DTO class.
 * Contains namespace information from Kubernetes.
 * Determines, wherever a project is for demo and
 * add limitations depending on demo flag.
 */
@Slf4j
@Getter
@Setter
@EqualsAndHashCode
@Builder(toBuilder = true)
@ToString
public class ProjectResponseDto {
    private String name;
    private String id;
    private String description;
    private String host;
    private String token;
    private String pathToFile;
    private String cloud;
    private boolean editable;
    private boolean demo;
    private boolean locked;
    private String isUpdating;
}
