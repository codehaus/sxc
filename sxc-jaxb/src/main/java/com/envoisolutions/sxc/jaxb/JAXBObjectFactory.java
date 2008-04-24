package com.envoisolutions.sxc.jaxb;

import java.util.Map;
import javax.xml.namespace.QName;

public abstract class JAXBObjectFactory<T> extends JAXBClass<T> {
    public JAXBObjectFactory(Class<T> type, Class<? extends JAXBClass>... dependencies) {
        super(type, dependencies);
    }

    public abstract Map<QName, Class<? extends JAXBObject>> getRootElements();
}
