package com.envoisolutions.sxc.jaxb;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.UnmarshallerHandler;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.attachment.AttachmentUnmarshaller;
import javax.xml.bind.helpers.AbstractUnmarshallerImpl;
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
        return read(reader, null, false);
    }

    public <T> JAXBElement<T> unmarshal(XMLStreamReader xsr, Class<T> cls) throws JAXBException {
        return (JAXBElement<T>) read(xsr, cls, true);
    }

    private Object read(XMLStreamReader xsr, Class<?> cls, boolean jaxbElementWrap) throws JAXBException {
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

            QName name = reader.getName();

            Object o = null;
            if (reader.isXsiNil()) {
                // its null
            } else if (String.class.equals(cls)) {
                o = reader.getElementAsString();
            } else if (Boolean.class.equals(cls)) {
                o = reader.getElementAsBoolean();
            } else if (Double.class.equals(cls)) {
                o = reader.getElementAsDouble();
            } else if (Long.class.equals(cls)) {
                o = reader.getElementAsLong();
            } else if (Float.class.equals(cls)) {
                o = reader.getElementAsFloat();
            } else if (Short.class.equals(cls)) {
                o = reader.getElementAsShort();
            } else if (QName.class.equals(cls)) {
                o = reader.getElementAsQName();
            } else if (byte[].class.equals(cls)) {
                o = BinaryUtils.decodeAsBytes(reader);
            } else if (XMLGregorianCalendar.class.equals(cls)) {
                String s = reader.getElementAsString();
                o = dtFactory.newXMLGregorianCalendar(s);
            } else if (Duration.class.equals(cls)) {
                String s = reader.getElementAsString();
                o = dtFactory.newDuration(s);
            } else {
                JAXBMarshaller instance = introspector.getJaxbMarshaller(cls);
                if (instance == null) {
                    instance = introspector.getJaxbMarshallerBySchemaType(reader.getXsiType());
                }
                if (instance == null) {
                    instance = introspector.getJaxbMarshallerByElementName(name);
                }
                if (instance != null) {
                    o = instance.read(reader, new TreeMap<String, Object>());
                    if (cls == null) {
                        cls = instance.getType();
                    }
                    jaxbElementWrap = jaxbElementWrap || instance.getXmlRootElement() == null;
                }
            }
            if (jaxbElementWrap) {
                return new JAXBElement(name, cls, o);
            } else {
                return o;
            }
        } catch (Exception e) {
            if (e instanceof JAXBException) {
                throw (JAXBException) e;
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
