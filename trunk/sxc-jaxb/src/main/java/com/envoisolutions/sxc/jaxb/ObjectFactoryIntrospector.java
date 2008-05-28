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