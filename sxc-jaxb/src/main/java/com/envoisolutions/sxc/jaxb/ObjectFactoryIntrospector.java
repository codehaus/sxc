package com.envoisolutions.sxc.jaxb;

import java.util.Map;
import javax.xml.namespace.QName;

import com.envoisolutions.sxc.jaxb.model.Bean;
import com.envoisolutions.sxc.jaxb.model.Model;
import com.envoisolutions.sxc.jaxb.model.ObjectFactory;

public class ObjectFactoryIntrospector {
    public ObjectFactoryIntrospector(BuilderContext builderContext, Model model) {
        for (ObjectFactory objectFactory : model.getObjectFactories()) {
            JAXBObjectFactoryBuilder objectFactoryBuilder = builderContext.createJAXBObjectFactoryBuilder(objectFactory.getType());

            // add dependencies as args to super call
            for (Bean bean : objectFactory.getDependencies()) {
                objectFactoryBuilder.addDependency(bean.getType());
            }

            // add root elements
            for (Map.Entry<QName, Class> rootElement : objectFactory.getRootElements().entrySet()) {
                QName qname = rootElement.getKey();
                Class<?> type = rootElement.getValue();
                objectFactoryBuilder.addRootElement(qname, type);
                objectFactoryBuilder.addDependency(type);
            }
        }
    }
}