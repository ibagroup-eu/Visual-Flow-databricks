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

package eu.ibagroup.vfdatabricks.dto;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * Class for constants.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Constants {
    public static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");
    public static final String NAME_PATTERN = "[A-Za-z0-9 \\-_]{3,40}";
    public static final String HOST_PATTERN =
            "http[s]*\\:\\/\\/(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)" +
                    "*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])";
    public static final Pattern NON_WORD_PATTERN = Pattern.compile("[^A-Za-z0-9]+");
    public static final Pattern PARAM_MATCH_PATTERN = Pattern.compile("#(.+?)#");
    public static final int MAX_DESCRIPTION_LENGTH = 500;
    public static final int MAX_TOKEN_LENGTH = 38;
    public static final String JOB_CONFIG_FIELD = "JOB_CONFIG";
    public static final String JOB_DEFINITION_FIELD = "JOB_DEFINITION";
    public static final String APP = "app";
    public static final String TYPE = "type";
    public static final String PROJECT = "project";
    public static final String NAME = "name";
    public static final String HOST = "host";
    public static final String UPDATING = "updating";
    public static final String HASH = "hash";
    public static final String TOKEN = "token";
    public static final String PATH_TO_FILE = "pathToFile";
    public static final String AUTHENTICATION_TYPE = "authType";
    public static final String CLOUD = "cloud";
    public static final String DESCRIPTION = "description";
    public static final String EXECUTOR_CORES = "EXECUTOR_CORES";
    public static final String EXECUTOR_INSTANCES = "EXECUTOR_INSTANCES";
    public static final String EXECUTOR_MEMORY = "EXECUTOR_MEMORY";
    public static final String EXECUTOR_REQUEST_CORES = "EXECUTOR_REQUEST_CORES";
    public static final String SHUFFLE_PARTITIONS = "SHUFFLE_PARTITIONS";
    public static final String UP_TO = "UP_TO";
    public static final String INTERVALS = "INTERVALS";
    public static final String VISUAL_FLOW_CONFIGURATION_TYPE = "VISUAL_FLOW_CONFIGURATION_TYPE";
    public static final String VISUAL_FLOW_DATABRICKS_SECRET_SCOPE = "VISUAL_FLOW_DATABRICKS_SECRET_SCOPE";
    public static final String JNAME = "JNAME";
    public static final String TAGS = "TAGS";
    public static final String DRIVER_CORES = "DRIVER_CORES";
    public static final String DRIVER_MEMORY = "DRIVER_MEMORY";
    public static final String DRIVER_REQUEST_CORES = "DRIVER_REQUEST_CORES";
    public static final String DRAFT_VF_STATUS = "Draft";
    public static final String SUCCEEDED_VF_STATUS = "Succeeded";
    public static final String RUNNING_VF_STATUS = "Running";
    public static final String PENDING_VF_STATUS = "Pending";
    public static final String FAILED_VF_STATUS = "Failed";
    public static final String SUCCESS_DB_STATUS = "SUCCESS";
    public static final String RUNNING_DB_STATUS = "RUNNING";
    public static final String PENDING_DB_STATUS = "PENDING";
    public static final String BLOCKED_DB_STATUS = "BLOCKED";
    public static final String JOB_TYPE = "job";
    public static final String DATABRICKS_JOBS_API = "api/2.1/jobs";
    public static final String DATABRICKS_JOBS_API_20 = "api/2.0";
    public static final String JOB_STORAGE_API = "api/project";
    public static final String DATABRICKS_CREATE_SECRET_SCOPE_API = "api/2.0/secrets/scopes/create";
    public static final String DATABRICKS_DELETE_SECRET_SCOPE_API = "api/2.0/secrets/scopes/delete";
    public static final String DATABRICKS_SECRET_API = "api/2.0/secrets";
    public static final String HISTORY_SERVICE_API = "api/databricks";
    public static final String CONTEXT_PATH = "vf/be";
    public static final String CONTEXT_PATH_HISTORY = "vf/be/history";
    public static final String URL_STRING_FORMAT = "%s/%s/%s/%s/job/%s";
    public static final String URL_UPLOAD_FILE_FORMAT = "%s/api/2.0/fs/files%s/%s";
    public static final String URL_CREATE_DIRECTORY_FORMAT = "/api/2.0/fs/directories%s";
    public static final String JAR_FILE_NAME = "spark-transformations-0.1-jar-with-dependencies.jar";
    public static final String PROJECT_KEY_PREFIX = "projectParams:";
    public static final String PARAMETER_KEY_PREFIX = ":params:";
    public static final String CLUSTER_NAME = "CLUSTER_NAME";
    public static final String POLICY = "POLICY";
    public static final String NODES = "NODES";
    public static final String ACCESS_MODE = "ACCESS_MODE";
    public static final String DATABRICKS_RUNTIME_VERSION = "DATABRICKS_RUNTIME_VERSION";
    public static final String USE_PHOTON_ACCELERATION = "USE_PHOTON_ACCELERATION";
    public static final String WORKER_TYPE = "WORKER_TYPE";
    public static final String WORKERS = "WORKERS";
    public static final String MIN_WORKERS = "MIN_WORKERS";
    public static final String MAX_WORKERS = "MAX_WORKERS";
    public static final String DRIVER_TYPE = "DRIVER_TYPE";
    public static final String AUTOSCALING_WORKERS = "AUTOSCALING_WORKERS";
    public static final String AUTOSCALING_STORAGE = "AUTOSCALING_STORAGE";
    public static final String INSTANCE_PROFILE = "INSTANCE_PROFILE";
    public static final String CLUSTER_TAGS = "CLUSTER_TAGS";
    public static final String ON_DEMAND_SPOT = "ON_DEMAND_SPOT";
    public static final String IS_ON_DEMAND_SPOT = "IS_ON_DEMAND_SPOT";
    public static final String ENABLE_CREDENTIAL = "ENABLE_CREDENTIAL";
    public static final String AVAILABILITY_ZONE = "AVAILABILITY_ZONE";
    public static final String MAX_SPOT_PRICE = "MAX_SPOT_PRICE";
    public static final String EBS_VOLUME_TYPE = "EBS_VOLUME_TYPE";
    public static final String VOLUMES = "VOLUMES";
    public static final String DB_SIZE = "DB_SIZE";
    public static final String SPARK_CONFIG = "SPARK_CONFIG";
    public static final String ENV_VAR = "ENV_VAR";
    public static final String DESTINATION = "DESTINATION";
    public static final String LOG_PATH = "LOG_PATH";
    public static final String REGION = "REGION";
    public static final String SSH_PUBLIC_KEY = "SSH_PUBLIC_KEY";
    public static final String NODE_TYPE_PARAM = "NODE_TYPE";
    public static final String CLUSTER_DATABRICKS_SCHEMA = "CLUSTER_DATABRICKS_SCHEMA";
    public static final String CLUSTER_SCRIPTS = "CLUSTER_SCRIPTS";
    public static final String ZONES_FIELD = "zones";
    public static final String INSTANCE_PROFILES_FIELD = "instance_profiles";
    public static final int JAR_FILE_CACHE_EXPIRE_MINUTES = 30;
    public static final int TOKEN_CACHE_EXPIRE_MINUTES = 57;
    public static final Pattern LOG_PATTERN =
            Pattern.compile("^(\\d{2}/\\d{2}/\\d{2}\\s\\d{2}:\\d{2}:\\d{2})\\s(\\w+)\\s+(.+)$");
    public static final String DATABRICKS_OAUTH_REQUEST_URL = "oidc/v1/token";
    public static final String DATABRICKS_OAUTH_REQUEST_BODY = "grant_type=client_credentials&scope=all-apis";
}
