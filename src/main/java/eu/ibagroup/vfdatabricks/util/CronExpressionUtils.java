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

package eu.ibagroup.vfdatabricks.util;

import com.cronutils.mapper.CronMapper;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CronExpressionUtils {
    private static final CronParser UNIX_PARSER = getCronParser(CronType.UNIX);
    private static final CronParser QUARTZ_PARSER = getCronParser(CronType.QUARTZ);

    public static String unixToQuartz(String expression) {
        Cron cron = UNIX_PARSER.parse(expression);
        return CronMapper.fromUnixToQuartz()
                .map(cron)
                .asString();
    }

    public static String quartzToUnix(String expression) {
        Cron cron = QUARTZ_PARSER.parse(expression);
        return CronMapper.fromQuartzToUnix()
                .map(cron)
                .asString();
    }

    private static CronParser getCronParser(CronType cronType) {
        CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(cronType);
        return new CronParser(cronDefinition);
    }
}
