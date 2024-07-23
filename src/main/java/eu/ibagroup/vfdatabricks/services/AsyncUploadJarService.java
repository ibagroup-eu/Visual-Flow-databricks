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

package eu.ibagroup.vfdatabricks.services;

import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static eu.ibagroup.vfdatabricks.dto.Constants.JAR_FILE_NAME;

@Slf4j
@Service
public class AsyncUploadJarService {
    private final DatabricksJobService databricksApiService;
    private final LoadingCache<String, byte[]> jarFileCache;

    public AsyncUploadJarService(DatabricksJobService databricksApiService,
                                 LoadingCache<String, byte[]> jarFileCache) {
        this.databricksApiService = databricksApiService;
        this.jarFileCache = jarFileCache;
    }

    @Async
    public CompletableFuture<Object> uploadJarFileToDatabricks(String projectId, String path) {
        try {
            databricksApiService.uploadFile(projectId, path, jarFileCache.get("jarFile"), JAR_FILE_NAME);
        } catch (ExecutionException e) {
            LOGGER.info("Error with cache", e);
        }
        return CompletableFuture.completedFuture(null);
    }

}
