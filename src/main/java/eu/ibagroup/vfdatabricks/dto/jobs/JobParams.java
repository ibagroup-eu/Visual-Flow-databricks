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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.util.Map;

import static eu.ibagroup.vfdatabricks.dto.Constants.*;

/**
 * Parameters for jobs.
 */
@SuperBuilder
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class JobParams extends BaseJobParams {
    @Serial
    private static final long serialVersionUID = 1;

    @JsonProperty(POLICY)
    private String policy;
    @JsonProperty(INSTANCE_PROFILE)
    private String instanceProfile;
    @JsonProperty(CLUSTER_TAGS)
    private String clusterTags;
    @JsonProperty(ON_DEMAND_SPOT)
    private int onDemandSpot;
    @JsonProperty(IS_ON_DEMAND_SPOT)
    private boolean isOnDemandSpot;
    @JsonProperty(ENABLE_CREDENTIAL)
    private boolean enableCredential;
    @JsonProperty(AVAILABILITY_ZONE)
    private String availabilityZone;
    @JsonProperty(MAX_SPOT_PRICE)
    private int maxSpotPrice;
    @JsonProperty(EBS_VOLUME_TYPE)
    private String ebsVolumeType;
    @JsonProperty(VOLUMES)
    private String volumes;
    @JsonProperty(DB_SIZE)
    private String dbSize;
    @JsonProperty(SPARK_CONFIG)
    private String sparkConfig;
    @JsonProperty(ENV_VAR)
    private String envVar;
    @JsonProperty(DESTINATION)
    private String destination;
    @JsonProperty(LOG_PATH)
    private String logPath;
    @JsonProperty(REGION)
    private String region;
    @JsonProperty(SSH_PUBLIC_KEY)
    private String sshPublicKey;
    @JsonProperty(NODE_TYPE_PARAM)
    private String nodeType;
    @JsonProperty(CLUSTER_SCRIPTS)
    private String clusterScripts;
    @JsonProperty(CLUSTER_DATABRICKS_SCHEMA)
    private transient Map<String, Object> clusterDatabricksSchema;
}
