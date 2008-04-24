package com.envoisolutions.sxc.jaxb.model;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.xml.namespace.QName;

public class EnumInfo {
    /**
     * The model that owns this enum.
     */
    private final Model model;

    /**
     * The enum class.
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
     * Map from enum constant to xml representation
     */
    private final Map<Enum, String> enumMap = new LinkedHashMap<Enum, String>();

    public EnumInfo(Model model, Class<?> type) {
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

    public Map<Enum, String> getEnumMap() {
        return enumMap;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EnumInfo enumInfo = (EnumInfo) o;

        return type.equals(enumInfo.type);
    }

    public int hashCode() {
        return type.hashCode();
    }

    public String toString() {
        return type.getName();
    }
}