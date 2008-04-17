package com.envoisolutions.sxc.jaxb;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.JAXBException;
import javax.xml.bind.helpers.ValidationEventImpl;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;

import com.envoisolutions.sxc.util.XoXMLStreamReader;
import com.envoisolutions.sxc.util.Attribute;
import com.envoisolutions.sxc.util.XoXMLStreamWriter;

@SuppressWarnings({"StringEquality"})
public class RuntimeContext {
    private final Map<String, Object> properties = new HashMap<String, Object>();
    private final MarshallerImpl marshaller;
    private final UnmarshallerImpl unmarshaller;

    public RuntimeContext() {
        marshaller = null;
        unmarshaller = null;
    }

    public RuntimeContext(UnmarshallerImpl unmarshaller) {
        this.marshaller = null;
        this.unmarshaller = unmarshaller;
    }

    public RuntimeContext(MarshallerImpl marshaller) {
        this.marshaller = marshaller;
        this.unmarshaller = null;
    }

    public Object getProperty(String name) {
        return properties.get(name);
    }

    public Object setProperty(String name, Object value) {
        return properties.put(name, value);
    }

    public void unexpectedAttribute(Attribute attribute, QName... expectedAttributes) throws JAXBException {
        String message = "Unexpected attribute " + attribute.getName() + ", expected " + Arrays.toString(expectedAttributes);
        validationError(message, attribute.getReader().getLocation(), null);
    }

    public void unexpectedElement(XoXMLStreamReader reader, QName... expectedElements) throws JAXBException {
        String message = "Unexpected element " + reader.getName() + ", expected " + Arrays.toString(expectedElements);
        validationError(message, reader.getLocation(), null);
    }

    public <T> T unexpectedXsiType(XoXMLStreamReader reader, Class<T> expectedJavaType) throws JAXBException {
        if (unmarshaller != null) {
            //noinspection unchecked
            T value = (T) unmarshaller.read(reader, expectedJavaType, false, this);
            return value;
        }
        String message = "Unexpected xsi:type " + reader.getXsiType() + ", expected " + expectedJavaType;
        validationError(message, reader.getLocation(), null);
        return null;
    }

    public void unexpectedSubclass(XoXMLStreamWriter writer, Object bean, Class baseClass, Class... expectedSubclasses) throws JAXBException {
        if (marshaller != null) {
            marshaller.write(bean, writer, this, false, true);
            return;
        }

        String message = "Unknown subclass " + bean.getClass().getName() + " of base class " + baseClass.getName()+ ", expected [";
        for (int i = 0; i < expectedSubclasses.length; i++) {
            if (i != 0) message += ", ";
            Class expectedSubclass = expectedSubclasses[i];
            message += expectedSubclass.getName();
        }
        message += "]";
        validationError(message, new ValidationEventLocatorImpl(bean, null), null);
    }

    public void unexpectedNullValue(Object bean, String propertyName) throws JAXBException {
        // For compatability with the JaxB RI unexpected null values are not reported as an error when writing.
        //
        // The jaxb spec section B.4.2.5 says that if an element is anntoated @XmlElement(required=true, nillable = false)
        // the property or field can not be null.  This should be an validation error, but my guess is it is not
        // considering how common this error is.

        // String message = "Property " + bean.getClass().getName() + "." + propertyName + " cannot be null";
        // validationError(message, new ValidationEventLocatorImpl(bean, propertyName), null);
    }

    public void unexpectedElementType(XoXMLStreamWriter writer, Object bean, String propertyName, Object propertyValue, Class... expectedTypes) throws JAXBException {
        String message = "Property " + bean.getClass().getName() + "." + propertyName + " value is the unexpected type " + propertyName.getClass().getName() + ", expected [";
        for (int i = 0; i < expectedTypes.length; i++) {
            if (i != 0) message += ", ";
            Class expectedType = expectedTypes[i];
            message += expectedType.getName();
        }
        message += "]";
        validationError(message, new ValidationEventLocatorImpl(bean, propertyName), null);
    }

    public void unexpectedElementRef(XoXMLStreamWriter writer, Object bean, String propertyName, Object propertyValue, Class... expectedTypes) throws JAXBException {
        if (marshaller != null) {
            marshaller.write(bean, writer, this, true, false);
            return;
        }

        String message = "Property " + bean.getClass().getName() + "." + propertyName + " value is the unexpected type " + propertyName.getClass().getName() + ", expected [";
        for (int i = 0; i < expectedTypes.length; i++) {
            if (i != 0) message += ", ";
            Class expectedType = expectedTypes[i];
            message += expectedType.getName();
        }
        message += "]";
        validationError(message, new ValidationEventLocatorImpl(bean, propertyName), null);
    }

    public void unexpectedEnumValue(XoXMLStreamReader reader, Class enumType, String value, String... expectedValues) throws JAXBException {
        String message = "Unexpected XML value \"" + value + "\" for enum " + enumType.getName() + ", expected " + Arrays.toString(expectedValues);
        validationError(message, reader.getLocation(), null);
    }

    public <T> void unexpectedEnumConst(Object bean, String propertyName, T value, T... expectedValues) throws JAXBException {
        String message = "Unexpected constant \"" + value + "\" for enum " + value.getClass().getName() + ", expected " + Arrays.toString(expectedValues);
        validationError(message, new ValidationEventLocatorImpl(bean, propertyName), null);
    }

    public void xmlAdapterError(XoXMLStreamReader reader, Class adapterClass, Class sourceType, Class destinationType, Exception e) throws JAXBException {
        String message = "An error occured while converting an instance of " + sourceType.getName() + " to an instance of " + destinationType.getName() + " using XmlAdapter " + adapterClass.getName();
        validationError(message, reader.getLocation(), e);
    }

    public void xmlAdapterError(Object bean, String propertyName, Class adapterClass, Class sourceType, Class destinationType, Exception e) throws JAXBException {
        String message = "An error occured while converting an instance of " + sourceType.getName() + " to an instance of " + destinationType.getName() + " using XmlAdapter " + adapterClass.getName();
        validationError(message, new ValidationEventLocatorImpl(bean, propertyName), e);
    }

    public void uncreatableCollection(XoXMLStreamReader reader, Class beanType, String propertyName, Class collectionType) throws JAXBException {
        String message = "Collection property " + propertyName + " in class " + beanType.getName() + " is null and a new instance of " + collectionType.getName() + " can not be created";
        validationError(message, reader.getLocation(), null);
    }

    public void fieldGetError(XoXMLStreamReader reader, Class beanType, String fieldName, Exception e) throws JAXBException {
        String message = "An error occured while getting field value " + beanType.getName() + "." + fieldName;
        validationError(message, reader.getLocation(), e);
    }

    public void fieldGetError(Object bean, String propertyName, Class beanType, String fieldName, Exception e) throws JAXBException {
        String message = "An error occured while getting field value " + beanType.getName() + "." + fieldName;
        validationError(message, new ValidationEventLocatorImpl(bean, propertyName), e);
    }

    public void fieldSetError(XoXMLStreamReader reader, Class beanType, String fieldName, Exception e) throws JAXBException {
        String message = "An error occured while setting field value " + beanType.getName() + "." + fieldName;
        validationError(message, reader.getLocation(), e);
    }

    public void getterError(XoXMLStreamReader reader, Class beanType, String getterName, Exception e) throws JAXBException {
        String message = "An error occured while calling getter method " + beanType.getName() + "." + getterName + "()";
        validationError(message, reader.getLocation(), e);
    }

    public void getterError(Object bean, String propertyName, Class beanType, String getterName, Exception e) throws JAXBException {
        String message = "An error occured while calling getter method " + beanType.getName() + "." + getterName + "()";
        validationError(message, new ValidationEventLocatorImpl(bean, propertyName), e);
    }

    public void setterError(XoXMLStreamReader reader, Class beanType, String setterName, Class propertyType, Exception e) throws JAXBException {
        String message = "An error occured while calling setter method " + beanType.getName() + "." + setterName + "(" + propertyType.getName() + ")";
        validationError(message, reader.getLocation(), e);
    }

    private void validationError(String message, Location location, Throwable cause) throws JAXBException {
        validationError(message, new ValidationEventLocatorImpl(location), cause);
    }

    private void validationError(String message, ValidationEventLocatorImpl locator, Throwable cause) throws JAXBException {
        ValidationEventHandler validationEventHandler = null;
        if (marshaller != null) {
            validationEventHandler = marshaller.getEventHandler();
        } else if (unmarshaller != null) {
            validationEventHandler = unmarshaller.getEventHandler();
        }
        if (validationEventHandler == null || !validationEventHandler.handleEvent(new ValidationEventImpl(ValidationEvent.ERROR, message, locator, cause))) {
            throw new UnmarshalException(message, cause);
        }
    }
}
