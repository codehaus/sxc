package com.envoisolutions.sxc.jaxb.model;

import java.lang.reflect.Type;
import javax.xml.namespace.QName;

public class XmlMapping {
    private final Property property;

    private final QName xmlName;

    private boolean nillable;

    private Type componentType;

    private Bean targetBean;

    public XmlMapping(Property property, QName xmlName) {
        if (property == null) throw new NullPointerException("property is null");
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

    public Bean getTargetBean() {
        return targetBean;
    }

    public void setTargetBean(Bean targetBean) {
        this.targetBean = targetBean;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        XmlMapping mapping = (XmlMapping) o;

        return property.equals(mapping.property) &&
                !(xmlName != null ? !xmlName.equals(mapping.xmlName) : mapping.xmlName != null);
    }

    public int hashCode() {
        int result;
        result = property.hashCode();
        result = 31 * result + (xmlName != null ? xmlName.hashCode() : 0);
        return result;
    }

    public String toString() {
        if (xmlName != null) {
            return property.toString() + " - " + xmlName;
        } else {
            return property.toString() + " - [value]";
        }
    }
}
