package com.envoisolutions.sxc.jaxb;

import java.util.Collections;
import java.util.Arrays;
import java.util.Collection;

public abstract class JAXBClass<T> {
    /**
     * The ObjectFactory class.
     */
    protected Class<T> type;

    /**
     * Dependencies
     */
    protected Collection<Class<? extends JAXBClass>> dependencies;

    protected JAXBClass(Class<T> type, Class<? extends JAXBClass>... dependencies) {
        this.type = type;
        this.dependencies = Collections.unmodifiableCollection(Arrays.asList(dependencies));
    }

    public Class<T> getType() {
        return type;
    }

    public Collection<Class<? extends JAXBClass>> getDependencies() {
        return dependencies;
    }
}