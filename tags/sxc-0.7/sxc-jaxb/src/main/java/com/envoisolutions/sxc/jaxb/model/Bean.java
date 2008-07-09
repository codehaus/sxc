/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.envoisolutions.sxc.jaxb.model;

import java.util.LinkedHashSet;
import java.util.Set;
import javax.xml.namespace.QName;

public class Bean {
    /**
     * The model that owns this bean.
     */
    private final Model model;

    /**
     * The bean class.
     */
    private final Class<?> type;

    /**
     * Used for xsi:type checks.
     */
    private QName schemaTypeName;

    /**
     * If this bean can be a root element, this is the name.
     */
    private QName rootElementName;

    /**
     * Properties of this bean.
     */
    private final Set<Property> properties = new LinkedHashSet<Property>();

    /**
     * Parent JAXB class info.
     */
    private Bean baseClass;

    public Bean(Model model, Class<?> type) {
        if (model == null) throw new NullPointerException("model is null");
        if (type == null) throw new NullPointerException("type is null");
        this.model = model;
        this.type = type;
    }

    public Model getModel() {
        return model;
    }

    public Class<?> getType() {
        return type;
    }

    public QName getSchemaTypeName() {
        return schemaTypeName;
    }

    public void setSchemaTypeName(QName schemaTypeName) {
        this.schemaTypeName = schemaTypeName;
    }

    public QName getRootElementName() {
        return rootElementName;
    }

    public void setRootElementName(QName rootElementName) {
        this.rootElementName = rootElementName;
    }

    public Set<Property> getProperties() {
        return properties;
    }

    public void addProperty(Property property) {
        properties.add(property);
    }

    public Bean getBaseClass() {
        return baseClass;
    }

    public void setBaseClass(Bean baseClass) {
        this.baseClass = baseClass;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Bean bean = (Bean) o;

        return type.equals(bean.type);
    }

    public int hashCode() {
        return type.hashCode();
    }

    public String toString() {
        return type.getName();
    }
}
