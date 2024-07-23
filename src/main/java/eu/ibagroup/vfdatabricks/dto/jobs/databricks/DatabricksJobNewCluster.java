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

package eu.ibagroup.vfdatabricks.dto.jobs.databricks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Map;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabricksJobNewCluster {
    @JsonProperty("num_workers")
    private Integer numWorkers;
    @JsonProperty("autoscale")
    private AutoScale autoScale;
    @JsonProperty("spark_version")
    private String sparkVersion;
    @JsonProperty("spark_conf")
    private Map<String, Object> sparkConf;
    @JsonProperty("aws_attributes")
    private AWSAttributes awsAttributes;
    @JsonProperty("node_type_id")
    private String nodeTypeId;
    @JsonProperty("driver_node_type_id")
    private String driverNodeTypeId;
    @JsonProperty("ssh_public_keys")
    private String[] sshPublicKeys;
    @JsonProperty("custom_tags")
    private Map<String, Object> customTags;
    @JsonProperty("cluster_log_conf")
    private ClusterLogConfig clusterLogConfig;
    @JsonProperty("init_scripts")
    private Object[] initScripts;
    @JsonProperty("spark_env_vars")
    private Map<String, String> sparkEnvVars;
    @JsonProperty("autotermination_minutes")
    private Integer autoTerminationMinutes;
    @JsonProperty("enable_elastic_disk")
    private Boolean enableElasticDisk;
    @JsonProperty("instance_pool_id")
    private String instancePoolId;
    @JsonProperty("policy_id")
    private String policyId;
    @JsonProperty("enable_local_disk_encryption")
    private String enableLocalDiskEncryption;
    @JsonProperty("driver_instance_pool_id")
    private String driverInstancePoolId;
    @JsonProperty("workload_type")
    private WorkloadType workloadType;
    @JsonProperty("runtime_engine")
    private String runtimeEngine;
    @JsonProperty("docker_image")
    private DockerImage dockerImage;
    @JsonProperty("data_security_mode")
    private String dataSecurityMode;
    @JsonProperty("single_user_name")
    private String singleUserName;
    @JsonProperty("apply_policy_default_values")
    private Boolean applyPolicyDefaultValues;


    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AutoScale {
        @JsonProperty("min_workers")
        private Integer minWorkers;
        @JsonProperty("max_workers")

        private Integer maxWorkers;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AWSAttributes {
        @JsonProperty("first_on_demand")
        private Integer firstOnDemand;
        @JsonProperty("availability")
        private String availability;
        @JsonProperty("zone_id")
        private String zoneId;
        @JsonProperty("instance_profile_arn")
        private String instanceProfileArn;
        @JsonProperty("spot_bid_price_percent")
        private Integer spotBidPricePercent;
        @JsonProperty("ebs_volume_type")
        private String ebsVolumeType;
        @JsonProperty("ebs_volume_count")
        private Integer ebsVolumeCount;
        @JsonProperty("ebs_volume_size")
        private Integer ebsVolumeSize;
        @JsonProperty("ebs_volume_iops")
        private Integer ebsVolumeIOPS;
        @JsonProperty("ebs_volume_throughput")
        private Integer ebsVolumeThroughput;

    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ClusterLogConfig {
        @JsonProperty("dbfs")
        private DBFS dbfs;
        @JsonProperty("s3")
        private S3 s3;


        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class DBFS {
            @JsonProperty("destination")
            private String destination;
        }

        @AllArgsConstructor
        @NoArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class S3 {
            @JsonProperty("destination")
            private String destination;
            @JsonProperty("region")
            private String region;
            @JsonProperty("endpoint")
            private String endpoint;
            @JsonProperty("enable_encryption")
            private Boolean enableEncryption;
            @JsonProperty("encryption_type")
            private String encryptionType;
            @JsonProperty("kms_key")
            private String kmsKey;
            @JsonProperty("canned_acl")
            private String cannedAcl;
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class WorkloadType {
        @JsonProperty("clients")
        private Clients clients;

        @AllArgsConstructor
        @NoArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Clients {
            @JsonProperty("notebooks")
            private Boolean notebooks;
            @JsonProperty("jobs")
            private Boolean jobs;
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DockerImage {
        @JsonProperty("url")
        private String url;
        @JsonProperty("basic_auth")
        private BasicAuth basic_auth;

        @AllArgsConstructor
        @NoArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class BasicAuth {
            @JsonProperty("username")
            private String username;
            @JsonProperty("password")
            private String password;
        }
    }

}
