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

import java.lang.reflect.Type;
import javax.xml.namespace.QName;

public class ElementMapping {
    private final Property property;

    private final QName xmlName;

    private boolean nillable;

    private Type componentType;

    public ElementMapping(Property property, QName xmlName) {
        if (property == null) throw new NullPointerException("property is null");
        if (xmlName == null) throw new NullPointerException("xmlName is null");
        this.property = property;
        this.xmlName = xmlName;
    }

    public Property getProperty() {
        return property;
    }

    public QName getXmlName() {
        return xmlName;
    }

    public boolean isNillable() {
        return nillable;
    }

    public void setNillable(boolean nillable) {
        this.nillable = nillable;
    }

    public Type getComponentType() {
        return componentType;
    }

    public void setComponentType(Type componentType) {
        this.componentType = componentType;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ElementMapping mapping = (ElementMapping) o;

        return property.equals(mapping.property) &&
               xmlName.equals(mapping.xmlName);
    }

    public int hashCode() {
        int result;
        result = property.hashCode();
        result = 31 * result + xmlName.hashCode();
        return result;
    }

    public String toString() {
        return property.toString() + " - " + xmlName;
    }
}
