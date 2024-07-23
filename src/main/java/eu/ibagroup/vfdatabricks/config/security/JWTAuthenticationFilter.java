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

package eu.ibagroup.vfdatabricks.config.security;

import eu.ibagroup.vfdatabricks.config.SuperusersConfig;
import eu.ibagroup.vfdatabricks.exceptions.BadRequestException;
import eu.ibagroup.vfdatabricks.model.auth.UserInfo;
import eu.ibagroup.vfdatabricks.services.auth.AuthenticationService;
import eu.ibagroup.vfdatabricks.services.auth.OAuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Filter for extracting token from every request
 * and check with OAuth service.
 */
@Slf4j
@Component
public class JWTAuthenticationFilter extends OncePerRequestFilter {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final OAuthService oauthService;
    private final AuthenticationService authenticationService;
    private final Set<String> superusers;

    @Autowired
    public JWTAuthenticationFilter(
        SuperusersConfig superusersConfig,
        OAuthService oauthService,
        AuthenticationService authenticationService
    ) {
        this.oauthService = oauthService;
        this.superusers = superusersConfig.getSet();
        this.authenticationService = authenticationService;
    }

    /**
     * Gets token and validates it.
     *
     * @param httpServletRequest  request.
     * @param httpServletResponse response.
     * @param filterChain         chain.
     * @throws ServletException when doFilter.
     * @throws IOException      when doFilter.
     */
    @Override
    protected void doFilterInternal(
        HttpServletRequest httpServletRequest,
        @NonNull HttpServletResponse httpServletResponse,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        LOGGER.debug("Starting authentication filter");
        try {
            String token = httpServletRequest.getHeader(AUTHORIZATION_HEADER);
            if (token == null) {
                throw new AuthenticationServiceException("Empty token");
            } else {
                token = token.replace(BEARER_PREFIX, "");
                UserInfo userInfo = oauthService.getUserInfoByToken(token);
                userInfo.setSuperuser(superusers.contains(userInfo.getUsername()));
                userInfo.setToken(token);
                if (!userInfo.hasAllInformation()) {
                    throw new BadRequestException("User information doesn't contain all necessary data");
                }
                authenticationService.setUserInfo(userInfo);

                LOGGER.info(
                    "{} has been successfully authenticated",
                    AuthenticationService.getFormattedUserInfo(userInfo)
                );

                filterChain.doFilter(httpServletRequest, httpServletResponse);
            }
        } catch (AuthenticationException e) {
            LOGGER.error("Authentication exception", e);
            httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } catch (BadRequestException e) {
            LOGGER.error("Cannot authenticate user", e);
            httpServletResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }
}
