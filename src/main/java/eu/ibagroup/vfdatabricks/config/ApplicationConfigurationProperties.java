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

package eu.ibagroup.vfdatabricks.config;

import jakarta.validation.Valid;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Application Configuration (from yaml) class.
 * Represents properties from this configuration.
 */
@Data
@Component
@ConfigurationProperties
@Validated
public class ApplicationConfigurationProperties {

    @Valid
    private OauthSettings oauth;
    @Valid
    private NamespaceSettings namespace;
    @Valid
    private DatabricksSettings databricks;
    @Valid
    private JobStorage jobStorage;
    @Valid
    private HistoryService historyService;
    @Valid
    private Scheduler scheduler;
    @Valid
    private String jarFilePath;
    @Valid
    private String jarHash;
    @Valid
    private DBServiceSettings dbService;

    /**
     * Represents oauth and user management settings.
     */
    @Data
    public static class OauthSettings {
        private OauthUrlSettings url;
        private String provider;
    }

    /**
     * Represents settings, connected with oauth URL.
     */
    @Data
    public static class OauthUrlSettings {
        private String userInfo;
    }

    /**
     * Represents namespace settings.
     */
    @Data
    public static class NamespaceSettings {
        private String app;
        private String label;
        private String prefix;
    }

    /**
     * Represents Databricks settings.
     */
    @Data
    public static class DatabricksSettings {
        private DatabricksTransformationsSettings transformations;
        private DatabricksIsvSettings isv;
        private DatabricksRetrySettings retry;
    }

    /**
     * Represents settings, connected with Databricks transformations.
     */
    @Data
    public static class DatabricksTransformationsSettings {
        private String path;
    }

    /**
     * Represents settings, connected with Databricks isv.
     */
    @Data
    public static class DatabricksIsvSettings {
        private String name;
        private String version;
    }

    /**
     * Represents settings, connected with Databricks Retry logic.
     */
    @Data
    public static class DatabricksRetrySettings {
        private List<Integer> codes;
        private String intervals;
        private String upTo;
    }

    /**
     * Represents jobStorage info.
     */
    @Data
    public static class JobStorage {
        private String host;
    }

    /**
     * Represents historyService info.
     */
    @Data
    public static class HistoryService {
        private String host;
    }

    /**
     * Represents scheduler settings.
     */
    @Data
    public static class Scheduler {
        private String interval;
    }

    /**
     * Represents Databases connection settings.
     */
    @Data
    public static class DBServiceSettings {
        private String host;
    }

}
