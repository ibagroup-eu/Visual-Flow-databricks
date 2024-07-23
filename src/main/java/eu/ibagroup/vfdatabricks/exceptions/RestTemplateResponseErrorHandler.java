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

package eu.ibagroup.vfdatabricks.exceptions;

import lombok.AllArgsConstructor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.util.List;

@AllArgsConstructor
public class RestTemplateResponseErrorHandler implements ResponseErrorHandler {

    private List<Integer> codesForRetry;

    @Override
    public boolean hasError(ClientHttpResponse httpResponse) throws IOException {
        return httpResponse.getStatusCode().isError();
    }

    @Override
    public void handleError(ClientHttpResponse httpResponse) throws IOException {
        if (codesForRetry.contains(httpResponse.getStatusCode().value())) {
            String responseBody = new String(httpResponse.getBody().readAllBytes());
            throw new ForRetryRestTemplateException(responseBody);
        } else {
            throw new RestTemplateException(new String(httpResponse.getBody().readAllBytes()));
        }
    }
}