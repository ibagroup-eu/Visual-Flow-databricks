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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static eu.ibagroup.vfdatabricks.dto.Constants.CACHE_EXPIRE_MINUTES;

@Configuration
public class CacheConfig {

    @Bean
    public LoadingCache<String, byte[]> jarFileCache(ApplicationConfigurationProperties appProperties) {
        return CacheBuilder.newBuilder()
                .expireAfterAccess(CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
                .maximumSize(1)
                .build(new CacheLoader<>() {
                    @Override
                    public byte[] load(String key) throws IOException {
                        return Files.readAllBytes(Path.of(appProperties.getJarFilePath()));
                    }
                });
    }
}
