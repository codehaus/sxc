package com.envoisolutions.sxc.jaxb;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationEventLocator;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.JAXBException;
import javax.xml.bind.helpers.ValidationEventImpl;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;

import com.envoisolutions.sxc.util.XoXMLStreamReader;
import com.envoisolutions.sxc.util.Attribute;
import org.w3c.dom.Node;

@SuppressWarnings({"StringEquality"})
public class ReadContext {
    private final Map<String, Object> properties = new HashMap<String, Object>();
    private final UnmarshallerImpl unmarshaller;

    public ReadContext() {
        unmarshaller = null;
    }

    public ReadContext(UnmarshallerImpl unmarshaller) {
        this.unmarshaller = unmarshaller;
    }

    public Object getProperty(String name) {
        return properties.get(name);
    }

    public Object setProperty(String name, Object value) {
        return properties.put(name, value);
    }

    public void unexpectedAttribute(Attribute attribute, QName... expectedAttributes) throws JAXBException {
        String message = "Unexpected attribute " + attribute.getName() + ", expected " + Arrays.toString(expectedAttributes);
        validationError(attribute.getReader().getLocation(), message, null);
    }

    public void unexpectedElement(XoXMLStreamReader reader, QName... expectedElements) throws JAXBException {
        String message = "Unexpected element " + reader.getName() + ", expected " + Arrays.toString(expectedElements);
        validationError(reader.getLocation(), message, null);
    }

    public <T> T unexpectedXsiType(XoXMLStreamReader reader, Class<T> expectedJavaType) throws JAXBException {
        if (unmarshaller != null) {
            //noinspection unchecked
            T value = (T) unmarshaller.read(reader, expectedJavaType, false, this);
            return value;
        }
        String message = "Unexpected xsi:type " + reader.getXsiType() + ", expected " + expectedJavaType;
        validationError(reader.getLocation(), message, null);
        return null;
    }

    private void validationError(Location location, String message, Throwable cause) throws JAXBException {
        ValidationEventHandler validationEventHandler = unmarshaller == null ? null : unmarshaller.getEventHandler();
        if (validationEventHandler == null || !validationEventHandler.handleEvent(new ValidationEventImpl(ValidationEvent.ERROR, message, new ReadValidationEventLocator(location), cause))) {
            throw new UnmarshalException(message, cause);
        }
    }

    private static class ReadValidationEventLocator implements ValidationEventLocator {
        private Location location;

        public ReadValidationEventLocator(Location location) {
            this.location = location;
        }

        public URL getURL() {
            try {
                return new URL(location.getSystemId());
            } catch (MalformedURLException e) {
                return null;
            }
        }

        public int getOffset() {
            return -1;
        }

        public int getLineNumber() {
            return location.getLineNumber();
        }

        public int getColumnNumber() {
            return location.getColumnNumber();
        }

        public Object getObject() {
            return null;
        }

        public Node getNode() {
            return null;
        }
    }

}
