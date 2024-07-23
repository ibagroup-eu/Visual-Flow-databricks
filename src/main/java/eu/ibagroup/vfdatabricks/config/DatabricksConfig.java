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

import eu.ibagroup.vfdatabricks.exceptions.ConfigurationException;
import eu.ibagroup.vfdatabricks.exceptions.RestTemplateResponseErrorHandler;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import static org.springframework.http.HttpHeaders.USER_AGENT;

@RequiredArgsConstructor
@Configuration
public class DatabricksConfig {

    private final ApplicationConfigurationProperties appProperties;

    @Bean
    @Primary
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }

    @Bean("databricksRestTemplate")
    public RestTemplate getDatabricksRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new RestTemplateResponseErrorHandler(appProperties.getDatabricks()
                .getRetry().getCodes()));
        restTemplate.getInterceptors().add((HttpRequest request, byte[] body,
                                            ClientHttpRequestExecution execution) -> {
            request.getHeaders().set(
                    USER_AGENT,
                    String.format("%s/%s", appProperties.getDatabricks().getIsv().getName(),
                    appProperties.getDatabricks().getIsv().getVersion()));
            return execution.execute(request, body);
        });
        return restTemplate;
    }

    @Bean("authRestTemplate")
    public RestTemplate getAuthRestTemplate() {
        try {
            TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
            SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy)
                    .build();
            SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);
            HttpClientConnectionManager connectionManager =
                    PoolingHttpClientConnectionManagerBuilder.create().setSSLSocketFactory(csf).build();
            CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(connectionManager).build();


            HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
            requestFactory.setHttpClient(httpClient);
            return new RestTemplate(requestFactory);
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            throw new ConfigurationException("Unable to build unsecured rest template", e);
        }
    }
}
