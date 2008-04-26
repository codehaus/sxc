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

    private boolean xmlAny;
    private boolean lax;

    private Field field;
    private Method getter;
    private Method setter;

    private Class adapterType;

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

    public boolean isXmlAny() {
        return xmlAny;
    }

    public void setXmlAny(boolean xmlAny) {
        this.xmlAny = xmlAny;
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
