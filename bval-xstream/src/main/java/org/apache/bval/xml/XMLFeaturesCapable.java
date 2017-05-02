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
package org.apache.bval.xml;

import com.thoughtworks.xstream.annotations.XStreamImplicit;
import org.apache.bval.model.FeaturesCapable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Description: <br/>
 */
public class XMLFeaturesCapable implements Serializable {
    /** Serialization version */
    private static final long serialVersionUID = 1L;

    @XStreamImplicit
    private List<XMLMetaFeature> features;
    @XStreamImplicit(itemFieldName = "validator")
    private List<XMLMetaValidatorReference> validators;

    public List<XMLMetaFeature> getFeatures() {
        return features;
    }

    public void setFeatures(List<XMLMetaFeature> features) {
        this.features = features;
    }

    public void putFeature(String key, Object value) {
        if (features == null) {
            features = new ArrayList<>();
        }
        Optional<XMLMetaFeature> anno = findFeature(key);
        if (anno.isPresent()) {
            anno.get().setValue(value);
        } else {
            features.add(new XMLMetaFeature(key, value));
        }
    }

    public void removeFeature(String key) {
        Optional<XMLMetaFeature> anno = findFeature(key);
        if (anno.isPresent()) {
            getFeatures().remove(anno.get());
        }
    }

    public Object getFeature(String key) {
        return findFeature(key).map(XMLMetaFeature::getValue).orElse(null);
    }

    private Optional<XMLMetaFeature> findFeature(String key) {
        return features == null ? Optional.empty()
            : features.stream().filter(anno -> key.equals(anno.getKey())).findFirst();
    }

    public List<XMLMetaValidatorReference> getValidators() {
        return validators;
    }

    public void setValidators(List<XMLMetaValidatorReference> validators) {
        this.validators = validators;
    }

    public void addValidator(String validatorId) {
        if (validators == null) {
            validators = new ArrayList<>();
        }
        validators.add(new XMLMetaValidatorReference(validatorId));
    }

    public void mergeFeaturesInto(FeaturesCapable fc) {
        if (getFeatures() != null) {
            for (XMLMetaFeature each : getFeatures()) {
                fc.putFeature(each.getKey(), each.getValue());
            }
        }
    }
}
