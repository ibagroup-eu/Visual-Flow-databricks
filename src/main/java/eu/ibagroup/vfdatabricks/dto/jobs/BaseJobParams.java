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

package eu.ibagroup.vfdatabricks.dto.jobs;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.ibagroup.vfdatabricks.dto.Params;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static eu.ibagroup.vfdatabricks.dto.Constants.*;

@SuperBuilder
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public abstract class BaseJobParams extends Params {
    @JsonProperty(DRIVER_CORES)
    private String driverCores;
    @JsonProperty(DRIVER_MEMORY)
    private String driverMemory;
    @JsonProperty(DRIVER_REQUEST_CORES)
    private String driverRequestCores;
    @JsonProperty(EXECUTOR_CORES)
    private String executorCores;
    @JsonProperty(EXECUTOR_INSTANCES)
    private String executorInstances;
    @JsonProperty(EXECUTOR_MEMORY)
    private String executorMemory;
    @JsonProperty(EXECUTOR_REQUEST_CORES)
    private String executorRequestCores;
    @JsonProperty(SHUFFLE_PARTITIONS)
    private String shufflePartitions;
    @JsonProperty(CLUSTER_NAME)
    private String clusterName;
    @JsonProperty(NODES)
    private String nodes;
    @JsonProperty(ACCESS_MODE)
    private String accessMode;
    @JsonProperty(DATABRICKS_RUNTIME_VERSION)
    private String databricksRuntimeVersion;
    @JsonProperty(USE_PHOTON_ACCELERATION)
    private boolean usePhotonAcceleration;
    @JsonProperty(WORKER_TYPE)
    private String workerType;
    @JsonProperty(WORKERS)
    private int workers;
    @JsonProperty(MIN_WORKERS)
    private int minWorkers;
    @JsonProperty(MAX_WORKERS)
    private int maxWorkers;
    @JsonProperty(DRIVER_TYPE)
    private String driverType;
    @JsonProperty(AUTOSCALING_WORKERS)
    private boolean autoscalingWorkers;
    @JsonProperty(AUTOSCALING_STORAGE)
    private boolean autoscalingStorage;
}
