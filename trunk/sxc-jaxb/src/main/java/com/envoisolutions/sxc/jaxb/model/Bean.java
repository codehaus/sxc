package com.envoisolutions.sxc.jaxb.model;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
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

    private final Set<Property> properties = new LinkedHashSet<Property>();

    private Bean baseClass;

    private final Map<Enum, String> enumMap = new LinkedHashMap<Enum, String>();

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

    public Map<Enum, String> getEnumMap() {
        return enumMap;
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
