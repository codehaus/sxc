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
