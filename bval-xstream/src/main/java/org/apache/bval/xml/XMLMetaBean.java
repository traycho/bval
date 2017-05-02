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

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Description: <br/>
 */
@XStreamAlias("bean")
public class XMLMetaBean extends XMLFeaturesCapable {
    /** Serialization version */
    private static final long serialVersionUID = 1L;

    @XStreamAsAttribute()
    private String id;
    @XStreamAsAttribute()
    private String name;
    @XStreamAsAttribute()
    private String impl;
    @XStreamImplicit
    private List<XMLMetaProperty> properties;
    @XStreamImplicit
    private List<XMLMetaBeanReference> beanRelations;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImpl() {
        return impl;
    }

    public void setImpl(String impl) {
        this.impl = impl;
    }

    public List<XMLMetaProperty> getProperties() {
        return properties;
    }

    public void setProperties(List<XMLMetaProperty> properties) {
        this.properties = properties;
    }

    public void addProperty(XMLMetaProperty property) {
        if (properties == null) {
            properties = new ArrayList<>();
        }
        properties.add(property);
    }

    public void putProperty(XMLMetaProperty property) {
        if (property.getName() != null) {
            Optional<XMLMetaProperty> prop = findProperty(property.getName());
            if (prop.isPresent()) {
                properties.remove(prop.get());
            }
        }
        addProperty(property);
    }

    public XMLMetaProperty removeProperty(String name) {
        Optional<XMLMetaProperty> prop = findProperty(name);
        if (prop.isPresent()) {
            properties.remove(prop.get());
        }
        return prop.orElse(null);
    }

    public XMLMetaProperty getProperty(String name) {
        return findProperty(name).orElse(null);
    }

    private Optional<XMLMetaProperty> findProperty(String name) {
        return properties == null ? Optional.empty()
            : properties.stream().filter(prop -> name.equals(prop.getName())).findFirst();
    }

    public List<XMLMetaBeanReference> getBeanRefs() {
        return beanRelations;
    }

    public void setBeanRefs(List<XMLMetaBeanReference> beanRelations) {
        this.beanRelations = beanRelations;
    }

    public void addBeanRef(XMLMetaBeanReference beanRelation) {
        if (beanRelations == null) {
            beanRelations = new ArrayList<>();
        }
        beanRelations.add(beanRelation);
    }

    public void putBeanRef(XMLMetaBeanReference beanRelation) {
        if (beanRelation.getName() != null) {
            Optional<XMLMetaBeanReference> relation = findBeanRef(beanRelation.getName());
            if (relation.isPresent()) {
                beanRelations.remove(relation.get());
            }
        }
        addBeanRef(beanRelation);
    }

    public XMLMetaBeanReference removeBeanRef(String name) {
        Optional<XMLMetaBeanReference> relation = findBeanRef(name);
        if (relation.isPresent()) {
            beanRelations.remove(relation.get());
        }
        return relation.orElse(null);
    }

    public XMLMetaBeanReference getBeanRef(String name) {
        return findBeanRef(name).orElse(null);
    }

    private Optional<XMLMetaBeanReference> findBeanRef(String name) {
        if (beanRelations == null) {
            return Optional.empty();
        }
        return beanRelations.stream().filter(relation -> name.equals(relation.getName())).findFirst();
    }

}
