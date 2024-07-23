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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DatabricksEmailNotifications {
    @JsonProperty("on_start")
    private List<String> onStart;
    @JsonProperty("on_success")
    private List<String> onSuccess;
    @JsonProperty("on_failure")
    private List<String> onFailure;
    @JsonProperty("on_duration_warning_threshold_exceeded")
    private List<String> onDurationWarningThresholdExceeded;
    @JsonProperty("no_alert_for_skipped_runs")
    private boolean noAlertForSkippedRuns;

}
