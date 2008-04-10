package com.envoisolutions.sxc.jaxb;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.lang.reflect.Field;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

public class JAXBIntrospectorImpl extends JAXBIntrospector {
    private final Map<Class, JAXBMarshaller> marshallerByClass = new LinkedHashMap<Class, JAXBMarshaller>();
    private final Map<QName, JAXBMarshaller> marshallerByElementName = new LinkedHashMap<QName, JAXBMarshaller>();
    private final Map<QName, JAXBMarshaller> marshallerBySchemaType = new LinkedHashMap<QName, JAXBMarshaller>();
    private boolean fullyResolved = false;

    public JAXBIntrospectorImpl() {
    }

    public JAXBIntrospectorImpl(JAXBMarshaller... marshallers) {
        for (JAXBMarshaller marshaller : marshallers) {
            addMarshaller(marshaller);
        }
    }

    public void addMarshaller(JAXBMarshaller marshaller) {
        if (marshallerByClass.containsKey(marshaller.getType())) return;

        marshallerByClass.put(marshaller.getType(), marshaller);
        if (marshaller.getXmlRootElement() != null) {
            marshallerByElementName.put(marshaller.getXmlRootElement(), marshaller);
        }
        if (marshaller.getXmlType() != null) {
            marshallerBySchemaType.put(marshaller.getXmlType(), marshaller);
        }

        if (fullyResolved) {
            resolveDependencies(marshaller);
        }
    }

    @SuppressWarnings({"unchecked"})
    public <T> JAXBMarshaller<T> getJaxbMarshaller(Class<T> type) {
        if (type == null) return null;

        JAXBMarshaller marshaller = marshallerByClass.get(type);
        if (marshaller == null) {
            setFullyResolved(true);
            marshaller = marshallerByClass.get(type);
        }
        return marshaller;
    }

    public JAXBMarshaller getJaxbMarshallerByElementName(QName elementName) {
        if (elementName == null) return null;

        JAXBMarshaller marshaller = marshallerByElementName.get(elementName);
        if (marshaller == null) {
            setFullyResolved(true);
            marshaller = marshallerByElementName.get(elementName);
        }
        return marshaller;
    }

    public JAXBMarshaller getJaxbMarshallerBySchemaType(QName schemaType) {
        if (schemaType == null) return null;

        JAXBMarshaller marshaller = marshallerBySchemaType.get(schemaType);
        if (marshaller == null) {
            setFullyResolved(true);
            marshaller = marshallerBySchemaType.get(schemaType);
        }
        return marshaller;
    }

    public boolean isElement(Object jaxbElement) {
        return getElementName(jaxbElement) != null;
    }

    public QName getElementName(Object jaxbElement) {
        if (jaxbElement instanceof JAXBElement) {
            JAXBElement element = (JAXBElement) jaxbElement;
            return element.getName();
        }
        JAXBMarshaller marshaller = getJaxbMarshaller(jaxbElement.getClass());
        if (marshaller == null) return null;
        return marshaller.getXmlRootElement();
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
            for (JAXBMarshaller marshaller : new ArrayList<JAXBMarshaller>(marshallerByClass.values())) {
                resolveDependencies(marshaller);
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    private void resolveDependencies(JAXBMarshaller marshaller) {
        Collection<Class<? extends JAXBMarshaller>> dependencies = marshaller.getDependencies();
        for (Class<? extends JAXBMarshaller> marshallerClass : dependencies) {
            JAXBMarshaller depencency = createMarshallerInstance(marshallerClass);
            addMarshaller(depencency);
        }
    }

    public static JAXBMarshaller loadJAXBMarshaller(Class type, ClassLoader classLoader) {
        if (classLoader == null)  classLoader = type.getClassLoader();
        if (classLoader == null)  classLoader = ClassLoader.getSystemClassLoader();

        Class<?> readerClass;
        try {
            readerClass = classLoader.loadClass("generated.sxc." + type.getName() + "JaxB");
        } catch (ClassNotFoundException e) {
            return null;
        }

        return createMarshallerInstance(readerClass.asSubclass(JAXBMarshaller.class));
    }

    public static JAXBMarshaller createMarshallerInstance(Class<? extends JAXBMarshaller> readerClass) {
        JAXBMarshaller marshaller = null;
        try {
            Field instanceField = readerClass.getField("INSTANCE");
            marshaller = (JAXBMarshaller) instanceField.get(null);
        } catch (Exception e) {
        }
        if (marshaller == null) {
            try {
                marshaller = readerClass.newInstance();
            } catch (Exception e) {
            }
        }
        return marshaller;
    }
}
