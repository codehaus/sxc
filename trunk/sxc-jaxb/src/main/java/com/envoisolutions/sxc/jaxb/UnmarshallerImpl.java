package com.envoisolutions.sxc.jaxb;

import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.UnmarshallerHandler;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.MarshalException;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.attachment.AttachmentUnmarshaller;
import javax.xml.bind.helpers.AbstractUnmarshallerImpl;
import javax.xml.bind.helpers.ValidationEventImpl;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;

import com.envoisolutions.sxc.util.XoXMLStreamReader;
import com.envoisolutions.sxc.util.XoXMLStreamReaderImpl;
import com.envoisolutions.sxc.util.RuntimeXMLStreamException;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

@SuppressWarnings({"unchecked"})
public class UnmarshallerImpl extends AbstractUnmarshallerImpl {
    private final JAXBIntrospectorImpl introspector;

    private final XMLInputFactory xif = XMLInputFactory.newInstance();
    private final DatatypeFactory dtFactory;

    private final Map<Class<?>, ? super XmlAdapter> adapters = new HashMap<Class<?>, XmlAdapter>();
    private Listener listener;
    private Schema schema;
    private AttachmentUnmarshaller attachmentUnmarshaller;

    public UnmarshallerImpl(JAXBIntrospectorImpl introspector) throws JAXBException {
        this.introspector = introspector;
        try {
            dtFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new JAXBException("Could not create datatype factory.", e);
        }
    }

    public <T> JAXBElement<T> unmarshal(Node node, Class<T> cls) throws JAXBException {
        try {
            XMLStreamReader reader = xif.createXMLStreamReader(new DOMSource(node));
            JAXBElement<T> o = unmarshal(reader, cls);
            reader.close();
            return o;
        } catch (XMLStreamException e) {
            throw new JAXBException("Error reading XML stream.", e);
        }
    }

    public Object unmarshal(Node node) throws JAXBException {
        try {
            XMLStreamReader xsr = xif.createXMLStreamReader(new DOMSource(node));
            Object o = unmarshal(xsr);
            xsr.close();
            return o;
        } catch (XMLStreamException e) {
            throw new JAXBException("Error reading XML stream.", e);
        }
    }

    public <T> JAXBElement<T> unmarshal(Source source, Class<T> cls) throws JAXBException {
        try {
            XMLStreamReader reader = xif.createXMLStreamReader(source);
            JAXBElement<T> o = unmarshal(reader, cls);
            reader.close();
            return o;
        } catch (XMLStreamException e) {
            throw new JAXBException("Error reading XML stream.", e);
        }
    }

    public Object unmarshal(Source source) throws JAXBException {
        try {
            XMLStreamReader reader = xif.createXMLStreamReader(source);
            Object o = unmarshal(reader);
            reader.close();
            return o;
        } catch (XMLStreamException e) {
            throw new JAXBException("Error reading XML stream.", e);
        }
    }

    protected Object unmarshal(XMLReader reader, InputSource source) throws JAXBException {
        return unmarshal(new SAXSource(reader, source));
    }

    public Object unmarshal(XMLEventReader reader) throws JAXBException {
        throw new UnsupportedOperationException();
    }

    public <T> JAXBElement<T> unmarshal(XMLEventReader reader, Class<T> expectedType) throws JAXBException {
        throw new UnsupportedOperationException();
    }

    public Object unmarshal(XMLStreamReader reader) throws JAXBException {
        return read(reader, null, null, new RuntimeContext(this));
    }

    public <T> JAXBElement<T> unmarshal(XMLStreamReader xsr, Class<T> cls) throws JAXBException {
        return (JAXBElement<T>) read(xsr, cls, true, new RuntimeContext(this));
    }

    public Object read(XMLStreamReader xsr, Class<?> expectedType, Boolean jaxbElementWrap, RuntimeContext runtimeContext) throws JAXBException {
        XoXMLStreamReader reader = new XoXMLStreamReaderImpl(xsr);
        try {
            int event = reader.getEventType();
            while (event != XMLStreamConstants.START_ELEMENT && reader.hasNext()) {
                event = reader.next();
            }

            if (event != XMLStreamConstants.START_ELEMENT) {
                // TODO: empty document - figure out what is appropriate per spec
                return null;
            }

            // read and save element name before stream advances
            QName name = reader.getName();

            Object o = null;
            if (reader.isXsiNil()) {
                // was xsi:nil
                return null;
            } else if (reader.getXsiType() != null) {
                // find the marshaller by xsi:type
                JAXBMarshaller instance = introspector.getJaxbMarshallerBySchemaType(reader.getXsiType());
                if (instance != null) {
                    if (expectedType == null) expectedType = instance.getType();
                    if (jaxbElementWrap == null)  jaxbElementWrap =  instance.getXmlRootElement() == null;

                    // check assignment is possible
                    if (expectedType.isAssignableFrom(instance.getType())) {
                        // read the object
                        o = instance.read(reader, runtimeContext);
                    } else {
                        String message = "Expected instance of " + expectedType.getName() + ", but found xsi:type " + reader.getXsiType() + " which is mapped to " + instance.getType().getName();
                        if (getEventHandler() == null || !getEventHandler().handleEvent(new ValidationEventImpl(ValidationEvent.ERROR, message, new ValidationEventLocatorImpl(reader.getLocation())))) {
                            throw new MarshalException(message);
                        }
                        jaxbElementWrap = false;
                    }
                } else {
                    String message = "No JAXB object for XML type " + reader.getXsiType();
                    if (getEventHandler() == null || !getEventHandler().handleEvent(new ValidationEventImpl(ValidationEvent.ERROR, message, new ValidationEventLocatorImpl(reader.getLocation())))) {
                        throw new MarshalException(message);
                    }
                    jaxbElementWrap = false;
                }
            } else if (expectedType != null) {
                // check built in types first
                if (String.class.equals(expectedType)) {
                    o = reader.getElementAsString();
                } else if (Boolean.class.equals(expectedType)) {
                    o = reader.getElementAsBoolean();
                } else if (Double.class.equals(expectedType)) {
                    o = reader.getElementAsDouble();
                } else if (Long.class.equals(expectedType)) {
                    o = reader.getElementAsLong();
                } else if (Float.class.equals(expectedType)) {
                    o = reader.getElementAsFloat();
                } else if (Short.class.equals(expectedType)) {
                    o = reader.getElementAsShort();
                } else if (QName.class.equals(expectedType)) {
                    o = reader.getElementAsQName();
                } else if (byte[].class.equals(expectedType)) {
                    o = BinaryUtils.decodeAsBytes(reader);
                } else if (XMLGregorianCalendar.class.equals(expectedType)) {
                    String s = reader.getElementAsString();
                    o = dtFactory.newXMLGregorianCalendar(s);
                } else if (Duration.class.equals(expectedType)) {
                    String s = reader.getElementAsString();
                    o = dtFactory.newDuration(s);
                } else {
                    // find marshaller by expected type
                    JAXBMarshaller instance = introspector.getJaxbMarshaller(expectedType);
                    if (instance != null) {
                        if (expectedType == null) {
                            expectedType = instance.getType();
                        }
                        if (jaxbElementWrap == null)  jaxbElementWrap =  instance.getXmlRootElement() == null;

                        // read the object
                        o = instance.read(reader, runtimeContext);
                    } else {
                        String message = expectedType.getName() + " is not a JAXB object";
                        if (getEventHandler() == null || !getEventHandler().handleEvent(new ValidationEventImpl(ValidationEvent.ERROR, message, new ValidationEventLocatorImpl(reader.getLocation())))) {
                            throw new MarshalException(message);
                        }
                        jaxbElementWrap = false;
                    }
                }
            } else {
                // find the marshaller by root element name
                JAXBMarshaller instance = introspector.getJaxbMarshallerByElementName(name);
                if (instance != null) {
                    expectedType = instance.getType();

                    // read the object
                    o = instance.read(reader, runtimeContext);
                } else {
                    String message = "No JAXB object mapped to root element " + name;
                    if (getEventHandler() == null || !getEventHandler().handleEvent(new ValidationEventImpl(ValidationEvent.ERROR, message, new ValidationEventLocatorImpl(reader.getLocation())))) {
                        throw new MarshalException(message);
                    }
                    jaxbElementWrap = false;
                }
            }

            // wrap if necessary
            if (jaxbElementWrap != null && jaxbElementWrap) {
                return new JAXBElement(name, expectedType, o);
            } else {
                return o;
            }
        } catch (Exception e) {
            if (e instanceof RuntimeXMLStreamException) {
                e = ((RuntimeXMLStreamException) e).getCause();
            }
            if (e instanceof XMLStreamException) {
                Throwable cause = e.getCause();
                if (cause instanceof JAXBException) {
                    throw (JAXBException) e;
                }
                throw new UnmarshalException(cause == null ? e : cause);
            }
            if (e instanceof JAXBException) {
                throw (JAXBException) e;
            }

            // report fatal error
            if (getEventHandler() != null) {
                getEventHandler().handleEvent(new ValidationEventImpl(ValidationEvent.FATAL_ERROR, "Fatal error", new ValidationEventLocatorImpl(reader.getLocation()), e));
            }
            throw new UnmarshalException(e);
        }
    }

    @SuppressWarnings({"unchecked"})
    public <A extends XmlAdapter> A getAdapter(Class<A> type) {
        return (A) adapters.get(type);
    }

    public <A extends XmlAdapter> void setAdapter(Class<A> type, A adapter) {
        adapters.put(type, adapter);
    }

    public AttachmentUnmarshaller getAttachmentUnmarshaller() {
        return attachmentUnmarshaller;
    }

    public void setAttachmentUnmarshaller(AttachmentUnmarshaller attachmentUnmarshaller) {
        this.attachmentUnmarshaller = attachmentUnmarshaller;
    }

    public Listener getListener() {
        return listener;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public Schema getSchema() {
        return schema;
    }

    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    public UnmarshallerHandler getUnmarshallerHandler() {
        return null;
    }
}
