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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

public class Property {
    public enum XmlStyle {
        // the major difference between an element and an element ref is how type substitution is handled
        // with element an xsi:type is written when a subclass detected and with an element ref a different
        // tag name is used
        ATTRIBUTE, ELEMENT, ELEMENT_REF, VALUE
    }

    private final Bean bean;

    private final String name;

    private QName xmlName;

    private XmlStyle xmlStyle;

    private final List<ElementMapping> elementMappings = new ArrayList<ElementMapping>();

    private Type type;
    private Type componentType;

    private boolean id;
    private boolean idref;

    private boolean required;
    private boolean nillable;
    private boolean collection;
    private boolean xmlList;

    private boolean xmlAny;
    private boolean mixed;
    private boolean lax;

    private Field field;
    private Method getter;
    private Method setter;

    private Class adapterType;
    private Class componentAdaptedType;

    public Property(Bean bean, String name) {
        if (bean == null) throw new NullPointerException("bean is null");
        if (name == null) throw new NullPointerException("name is null");
        this.bean = bean;
        this.name = name;
    }

    public Bean getBean() {
        return bean;
    }

    public String getName() {
        return name;
    }

    public QName getXmlName() {
        return xmlName;
    }

    public void setXmlName(QName xmlName) {
        this.xmlName = xmlName;
    }

    public XmlStyle getXmlStyle() {
        return xmlStyle;
    }

    public void setXmlStyle(XmlStyle xmlStyle) {
        this.xmlStyle = xmlStyle;
    }

    public List<ElementMapping> getElementMappings() {
        return elementMappings;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Type getComponentType() {
        return componentType;
    }

    public void setComponentType(Type componentType) {
        this.componentType = componentType;
    }

    public boolean isId() {
        return id;
    }

    public void setId(boolean id) {
        this.id = id;
    }

    public boolean isIdref() {
        return idref;
    }

    public void setIdref(boolean idref) {
        this.idref = idref;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isNillable() {
        return nillable;
    }

    public void setNillable(boolean nillable) {
        this.nillable = nillable;
    }

    public boolean isCollection() {
        return collection;
    }

    public void setCollection(boolean collection) {
        this.collection = collection;
    }

    public boolean isXmlList() {
        return xmlList;
    }

    public void setXmlList(boolean xmlList) {
        this.xmlList = xmlList;
    }

    public boolean isXmlAny() {
        return xmlAny;
    }

    public void setXmlAny(boolean xmlAny) {
        this.xmlAny = xmlAny;
    }

    public boolean isMixed() {
        return mixed;
    }

    public void setMixed(boolean mixed) {
        this.mixed = mixed;
    }

    public boolean isLax() {
        return lax;
    }

    public void setLax(boolean lax) {
        this.lax = lax;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public Method getGetter() {
        return getter;
    }

    public void setGetter(Method getter) {
        this.getter = getter;
    }

    public Method getSetter() {
        return setter;
    }

    public void setSetter(Method setter) {
        this.setter = setter;
    }

    public Class getAdapterType() {
        return adapterType;
    }

    public void setAdapterType(Class adapterType) {
        this.adapterType = adapterType;
    }

    public Class getComponentAdaptedType() {
        return componentAdaptedType;
    }

    public void setComponentAdaptedType(Class componentAdaptedType) {
        this.componentAdaptedType = componentAdaptedType;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Property property = (Property) o;

        return name.equals(property.name);

    }

    public int hashCode() {
        return name.hashCode();
    }

    public String toString() {
        return bean.getType().getName() + "." + name;
    }
}
