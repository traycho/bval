/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.bval.constraints;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.bval.routines.EMailValidation;

/**
 * Description: pattern for validation taken from hibernate.<br/>
 * User: roman.stumm <br/>
 * Date: 14.10.2008 <br/>
 * Time: 12:38:37 <br/>
 * Copyright: Agimatec GmbH
 */
public class EmailValidator implements ConstraintValidator<Email, String> {
    protected final EMailValidation validation = new EMailValidation();

    public boolean isValid(String value, ConstraintValidatorContext context) {
        return validation.isValid(value);
    }

    public void initialize(Email parameters) {
        // do nothing (as long as Email has no properties)
    }
}
