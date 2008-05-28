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
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.lang.reflect.Field;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

public class JAXBIntrospectorImpl extends JAXBIntrospector {
    private final Map<Class, JAXBObject> jaxbObjectByClass = new LinkedHashMap<Class, JAXBObject>();
    private final Map<QName, JAXBObject> jaxbObjectByElementName = new LinkedHashMap<QName, JAXBObject>();
    private final Map<QName, JAXBObject> jaxbObjectBySchemaType = new LinkedHashMap<QName, JAXBObject>();
    private boolean fullyResolved = false;

    public JAXBIntrospectorImpl() {
    }

    public JAXBIntrospectorImpl(JAXBObject... jaxbObjects) {
        for (JAXBObject jaxbObject : jaxbObjects) {
            addJAXBClass(jaxbObject);
        }
    }

    @SuppressWarnings({"unchecked"})
    public void addJAXBClass(JAXBClass jaxbClass) {
        if (jaxbObjectByClass.containsKey(jaxbClass.getType())) return;

        // index JAXBObject
        if (jaxbClass instanceof JAXBObject) {
            JAXBObject jaxbObject = (JAXBObject) jaxbClass;

            jaxbObjectByClass.put(jaxbObject.getType(), jaxbObject);
            if (jaxbObject.getXmlRootElement() != null) {
                jaxbObjectByElementName.put(jaxbObject.getXmlRootElement(), jaxbObject);
            }
            if (jaxbObject.getXmlType() != null) {
                jaxbObjectBySchemaType.put(jaxbObject.getXmlType(), jaxbObject);
            }
        }

        // index root element declarations in JAXBObjectFactory
        if (jaxbClass instanceof JAXBObjectFactory) {
            JAXBObjectFactory jaxbObjectFactory = (JAXBObjectFactory) jaxbClass;

            Map<QName, Class<? extends JAXBObject>> map = jaxbObjectFactory.getRootElements();
            for (Map.Entry<QName, Class<? extends JAXBObject>> rootElements : map.entrySet()) {
                // create an instance of the jaxb class
                // todo get existing instance from the jaxbObjectByClass map
                Class<? extends JAXBObject> jaxbObjectClass = rootElements.getValue();
                JAXBObject jaxbObject = (JAXBObject) createJAXBClassInstance(jaxbObjectClass);

                // add root element declaration
                jaxbObjectByElementName.put(rootElements.getKey(), jaxbObject);
            }
        }

        if (fullyResolved) {
            resolveDependencies(jaxbClass);
        }
    }

    @SuppressWarnings({"unchecked"})
    public <T> JAXBObject<T> getJaxbMarshaller(Class<T> type) {
        if (type == null) return null;

        JAXBObject jaxbObject = jaxbObjectByClass.get(type);
        if (jaxbObject == null) {
            jaxbObject = StandardJAXBObjects.jaxbObjectByClass.get(type);
        }
        if (jaxbObject == null) {
            setFullyResolved(true);
            jaxbObject = jaxbObjectByClass.get(type);
        }
        return jaxbObject;
    }

    public JAXBObject getJaxbMarshallerByElementName(QName elementName) {
        if (elementName == null) return null;

        JAXBObject jaxbObject = jaxbObjectByElementName.get(elementName);
        if (jaxbObject == null) {
            setFullyResolved(true);
            jaxbObject = jaxbObjectByElementName.get(elementName);
        }
        return jaxbObject;
    }

    public JAXBObject getJaxbMarshallerBySchemaType(QName schemaType) {
        if (schemaType == null) return null;

        JAXBObject jaxbObject = jaxbObjectBySchemaType.get(schemaType);
        if (jaxbObject == null) {
            jaxbObject = StandardJAXBObjects.jaxbObjectBySchemaType.get(schemaType);
        }
        if (jaxbObject == null) {
            setFullyResolved(true);
            jaxbObject = jaxbObjectBySchemaType.get(schemaType);
        }
        return jaxbObject;
    }

    public boolean isElement(Object jaxbElement) {
        return getElementName(jaxbElement) != null;
    }

    public QName getElementName(Object jaxbElement) {
        if (jaxbElement instanceof JAXBElement) {
            JAXBElement element = (JAXBElement) jaxbElement;
            return element.getName();
        }
        JAXBObject jaxbObject = getJaxbMarshaller(jaxbElement.getClass());
        if (jaxbObject == null) return null;
        return jaxbObject.getXmlRootElement();
    }

    public Set<QName> getElementNames() {
        return jaxbObjectByElementName.keySet();
    }

    public boolean isFullyResolved() {
        return fullyResolved;
    }

    public void setFullyResolved(boolean fullyResolved) {
        if (this.fullyResolved == fullyResolved) return;

        // changing value
        this.fullyResolved = fullyResolved;
        if (fullyResolved) {
            // state changed to fully resolved, so resolve all existing marshallers
            for (JAXBObject jaxbObject : new ArrayList<JAXBObject>(jaxbObjectByClass.values())) {
                resolveDependencies(jaxbObject);
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    private void resolveDependencies(JAXBClass jaxbClass) {
        Collection<Class<? extends JAXBObject>> dependencies = jaxbClass.getDependencies();
        for (Class<? extends JAXBObject> marshallerClass : dependencies) {
            JAXBClass depencency = createJAXBClassInstance(marshallerClass);
            addJAXBClass(depencency);
        }
    }

    public static JAXBClass loadJAXBClass(Class type, ClassLoader classLoader) {
        if (classLoader == null)  classLoader = type.getClassLoader();
        if (classLoader == null)  classLoader = ClassLoader.getSystemClassLoader();

        Class<?> readerClass;
        try {
            readerClass = classLoader.loadClass("sxc." + type.getName() + "JAXB");
        } catch (ClassNotFoundException e) {
            return null;
        }

        return createJAXBClassInstance(readerClass.asSubclass(JAXBClass.class));
    }

    public static JAXBClass createJAXBClassInstance(Class<? extends JAXBClass> readerClass) {
        JAXBClass jaxbClass = null;
        try {
            Field instanceField = readerClass.getField("INSTANCE");
            jaxbClass = (JAXBObject) instanceField.get(null);
        } catch (Exception e) {
        }
        if (jaxbClass == null) {
            try {
                jaxbClass = readerClass.newInstance();
            } catch (Exception e) {
            }
        }
        return jaxbClass;
    }
}
