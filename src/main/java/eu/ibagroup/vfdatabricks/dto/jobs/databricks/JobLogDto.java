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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.regex.Matcher;

/**
 * Log DTO class.
 */
@EqualsAndHashCode
@Builder
@Getter
@ToString
@Schema(description = "DTO that represents a single log entry")
public class JobLogDto {
    public static final int TIMESTAMP_GROUP_INDEX = 1;
    public static final int LEVEL_GROUP_INDEX = 2;
    public static final int MESSAGE_GROUP_INDEX = 3;
    private final String timestamp;
    private final String level;
    private String message;

    /**
     * Getting LogDto object from Matcher.
     *
     * @param matcher matcher
     * @return LogDto object
     */
    public static JobLogDto fromMatcher(Matcher matcher) {
        if (matcher.matches()) {
            return JobLogDto
                    .builder()
                    .timestamp(matcher.group(JobLogDto.TIMESTAMP_GROUP_INDEX))
                    .level(matcher.group(JobLogDto.LEVEL_GROUP_INDEX))
                    .message(matcher.group(JobLogDto.MESSAGE_GROUP_INDEX))
                    .build();
        }
        return null;
    }

    /**
     * Setter for message.
     *
     * @param message message
     * @return this
     */
    public JobLogDto withMessage(String message) {
        this.message = message;
        return this;
    }
}
