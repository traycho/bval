/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.bval;

import org.apache.bval.model.ValidationContext;
import org.apache.bval.model.ValidationListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Description: Implements a contains to hold and transport validation results<br/>
 */
public class ValidationResults implements ValidationListener, Serializable {
    private static final long serialVersionUID = 1L;

    private Map<String, List<Error>> errorsByReason;
    private Map<Object, Map<String, List<Error>>> errorsByOwner;

    /**
     * API to add an error to the validation results.
     *
     * @param reason       - Features from {@link org.apache.bval.model.Features.Property}
     *                       or custom reason of validation error
     * @param context        - context information (bean, propertyName, value, ...)
     */
    @Override
    public <T extends ValidationListener> void addError(String reason, ValidationContext<T> context) {
        Error error = createError(reason, context.getBean(), context.getPropertyName());
        addError(error, context);
    }

    /**
     * API to add an error to the validation results.
     *
     * @param error       - holding the description of reason and object to describe
     *                     the validation error
     * @param context     - null or the context to provide additional information
     */
    @Override
    public <T extends ValidationListener> void addError(Error error, ValidationContext<T> context) {
        if (errorsByReason == null) {
            initialize();
        }
        addToReasonBucket(error);
        addToOwnerBucket(error);
    }

    /**
     * Old API to add an error to the validation results when no context is available.
     *
     * @param reason       - Features from {@link org.apache.bval.model.Features.Property} or custom validation reason
     * @param bean         - (optional) owner bean or null
     * @param propertyName - (optional) propertyName where valiation error occurred or null
     */
    public void addError(String reason, Object bean, String propertyName) {
        addError(createError(reason, bean, propertyName), null);
    }

    /**
     * Create an Error object.
     * @param reason
     * @param owner
     * @param propertyName
     * @return new {@link Error}
     */
    protected Error createError(String reason, Object owner, String propertyName) {
        return new Error(reason, owner, propertyName);
    }

    /**
     * initialize the error-buckets now when needed and
     * not on instance creation to save memory garbage.
     */
    protected void initialize() {
        errorsByReason = new LinkedHashMap<>();
        errorsByOwner = new LinkedHashMap<>();
    }

    /**
     * Add an Error to the set of Errors shared by a particular "reason."
     * @param error
     * @see {@link Error#getReason()}
     */
    protected void addToReasonBucket(Error error) {
        if (error.getReason() != null) {
            errorsByReason.computeIfAbsent(error.getReason(), k -> new ArrayList<>()).add(error);
        }
    }

    /**
     * Add an Error to the property-keyed map of Errors maintained for this Error's owner.
     * @param error
     * @see {@link Error#getOwner()}
     */
    protected void addToOwnerBucket(Error error) {
        if (error.getOwner() != null) {
            errorsByOwner.computeIfAbsent(error.getOwner(), k -> new HashMap<>())
                .computeIfAbsent(error.getPropertyName(), k -> new ArrayList<>()).add(error);
        }
    }

    /**
     * Get the map of Errors by reason; 
     * key = reason, value = list of errors for this reason
     * @return map
     */
    public Map<String, List<Error>> getErrorsByReason() {
        return errorsByReason == null ? Collections.emptyMap() : Collections.unmodifiableMap(errorsByReason);
    }

    /**
     * Get the map of Errors by owner;
     * key = owner, value = map with:<br>
     * &nbsp;&nbsp; key = propertyName, value = list of errors for this owner.propertyName
     * @return map
     */
    public Map<Object, Map<String, List<Error>>> getErrorsByOwner() {
        return errorsByOwner == null ? Collections.emptyMap() : Collections.unmodifiableMap(errorsByOwner);
    }

    /**
     * Learn whether these results are empty/error-free.
     * @return true when there are NO errors in this validation result
     */
    public boolean isEmpty() {
        if (errorsByReason == null || (errorsByReason.isEmpty() && errorsByOwner.isEmpty())) {
            return true;
        }
        return errorsByReason.values().stream().allMatch(Collection::isEmpty) && errorsByOwner.values().stream()
            .map(Map::values).flatMap(Collection::stream).allMatch(Collection::isEmpty);
    }

    /**
     * Learn whether there is an Error keyed to a specified reason description.
     * @param reason
     * @return boolean
     * @see {@link Error#getReason()}
     */
    public boolean hasErrorForReason(String reason) {
        if (errorsByReason == null) {
            return false;
        }
        List<Error> errors = errorsByReason.get(reason);
        return errors != null && !errors.isEmpty();
    }

    /**
     * Learn whether <code>bean</code> has any errors keyed to property <code>propertyName</code>.
     * @param bean
     * @param propertyName - may be null: any property is checked
     *                     OR the name of the property to check
     * @return boolean
     */
    public boolean hasError(Object bean, String propertyName) {
        if (errorsByOwner == null) {
            return false;
        }
        Map<String, List<Error>> errors = errorsByOwner.get(bean);
        if (errors == null) {
            return false;
        }
        if (propertyName == null) {
            return !errors.values().stream().allMatch(Collection::isEmpty);
        }
        List<Error> list = errors.get(propertyName);
        return list != null && !list.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "ValidationResults{" + errorsByOwner + "}";
    }
}
