package com.envoisolutions.sxc.jaxb.model;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.LinkedHashSet;
import javax.xml.namespace.QName;

public class ObjectFactory {
    /**
     * The model that owns this bean.
     */
    private final Model model;

    /**
     * The ObjectFactory class.
     */
    private final Class<?> type;

    /**
     * Root element declarations by element name.
     */
    private final Map<QName, Class> rootElements = new LinkedHashMap<QName, Class>();

    /**
     * Dependencies
     */
    private final Set<Bean> dependencies = new LinkedHashSet<Bean>();

    public ObjectFactory(Model model, Class<?> type) {
        this.model = model;
        this.type = type;
    }

    public Model getModel() {
        return model;
    }

    public Class<?> getType() {
        return type;
    }

    public Map<QName, Class> getRootElements() {
        return rootElements;
    }

    public Set<Bean> getDependencies() {
        return dependencies;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ObjectFactory that = (ObjectFactory) o;

        return type.equals(that.type);
    }

    public int hashCode() {
        return type.hashCode();
    }

    public String toString() {
        return type.getName();
    }
}
