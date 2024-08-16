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

package eu.ibagroup.vfdatabricks.validators;

import eu.ibagroup.vfdatabricks.dto.projects.DatabricksAuthentication;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DatabricksAuthenticationValidator implements
        ConstraintValidator<ValidDatabricksAuthentication, DatabricksAuthentication> {

    @Override
    public boolean isValid(DatabricksAuthentication authentication, ConstraintValidatorContext context) {
        if (authentication == null) {
            return false;
        }

        boolean isValid = true;

        if (DatabricksAuthentication.AuthenticationType.PAT == (authentication.getAuthenticationType())) {
            isValid = authentication.getToken() != null;
            if (!isValid) {
                context.buildConstraintViolationWithTemplate("Personal Access Token must not be null")
                        .addPropertyNode("personalAccessToken")
                        .addConstraintViolation();
            }
        } else if (DatabricksAuthentication.AuthenticationType.OAUTH == (authentication.getAuthenticationType())) {
            isValid = authentication.getClientId() != null && authentication.getSecret() != null;
            if (!isValid) {
                if (authentication.getClientId() == null) {
                    context.buildConstraintViolationWithTemplate("Client ID must not be null")
                            .addPropertyNode("clientId")
                            .addConstraintViolation();
                }
                if (authentication.getSecret() == null) {
                    context.buildConstraintViolationWithTemplate("Secret must not be null")
                            .addPropertyNode("secret")
                            .addConstraintViolation();
                }
            }
        } else {
            isValid = false;
            context.buildConstraintViolationWithTemplate("AuthenticationType is invalid")
                    .addPropertyNode("authenticationType")
                    .addConstraintViolation();
        }

        if (!isValid) {
            context.disableDefaultConstraintViolation();
        }

        return isValid;
    }
}

