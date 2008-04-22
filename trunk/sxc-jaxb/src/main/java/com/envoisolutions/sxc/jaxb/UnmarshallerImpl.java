package com.envoisolutions.sxc.jaxb;

import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.MarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.PropertyException;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.attachment.AttachmentUnmarshaller;
import javax.xml.bind.helpers.ValidationEventImpl;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
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
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;

import com.envoisolutions.sxc.util.XoXMLStreamReader;
import com.envoisolutions.sxc.util.XoXMLStreamReaderImpl;
import com.envoisolutions.sxc.util.RuntimeXMLStreamException;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

@SuppressWarnings({"unchecked"})
public class UnmarshallerImpl implements Unmarshaller {
    private final JAXBIntrospectorImpl introspector;

    private final XMLInputFactory xif = XMLInputFactory.newInstance();
    private final DatatypeFactory dtFactory;

    private final Map<Class<?>, ? super XmlAdapter> adapters = new HashMap<Class<?>, XmlAdapter>();
    private Listener listener;
    private Schema schema;
    private AttachmentUnmarshaller attachmentUnmarshaller;
    private ValidationEventHandler handler;

    public UnmarshallerImpl(JAXBIntrospectorImpl introspector) throws JAXBException {
        this.introspector = introspector;
        try {
            dtFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new JAXBException("Could not create datatype factory.", e);
        }
    }

    private XMLStreamReader createXMLStreamReader(InputSource source) throws UnmarshalException {
        StreamSource streamSource = new StreamSource();
        streamSource.setSystemId(source.getSystemId());
        streamSource.setPublicId(source.getPublicId());
        streamSource.setInputStream(source.getByteStream());
        streamSource.setReader(source.getCharacterStream());
        XMLStreamReader streamReader = createXMLStreamReader(streamSource);
        return streamReader;
    }

    private XMLStreamReader createXMLStreamReader(Source source) throws UnmarshalException {
        if (source == null) throw new IllegalArgumentException("source is null");

        XMLStreamReader streamReader;
        try {
            streamReader = xif.createXMLStreamReader(source);
        } catch (XMLStreamException e) {
            throw new UnmarshalException(e);
        }
        return streamReader;
    }

    public Object unmarshal(File file) throws JAXBException {
        if (file == null) throw new IllegalArgumentException("file is null");

        XMLStreamReader streamReader = createXMLStreamReader(new StreamSource(file));
        return unmarshal(streamReader);
    }

    public Object unmarshal(InputStream inputStream) throws JAXBException {
        if (inputStream == null) throw new IllegalArgumentException("inputStream is null");

        XMLStreamReader streamReader = createXMLStreamReader(new StreamSource(inputStream));
        return unmarshal(streamReader);
    }

    public Object unmarshal(Reader reader) throws JAXBException {
        if (reader == null) throw new IllegalArgumentException("reader is null");

        XMLStreamReader streamReader = createXMLStreamReader(new StreamSource(reader));
        return unmarshal(streamReader);
    }

    public Object unmarshal(URL url) throws JAXBException {
        if (url == null) throw new IllegalArgumentException("url is null");

        XMLStreamReader streamReader = createXMLStreamReader(new StreamSource(url.toExternalForm()));
        return unmarshal(streamReader);
    }

    public <T> JAXBElement<T>  unmarshal(InputSource inputSource, Class<T> declaredType) throws JAXBException {
        if (inputSource == null) throw new IllegalArgumentException("inputSource is null");

        XMLStreamReader streamReader = createXMLStreamReader(inputSource);
        return unmarshal(streamReader, declaredType);
    }

    public Object unmarshal(InputSource inputSource) throws JAXBException {
        if (inputSource == null) throw new IllegalArgumentException("inputSource is null");

        XMLStreamReader streamReader = createXMLStreamReader(inputSource);
        return unmarshal(streamReader);
    }

    public <T> JAXBElement<T> unmarshal(Node node, Class<T> declaredType) throws JAXBException {
        if (node == null) throw new IllegalArgumentException("node is null");
        if (declaredType == null) throw new IllegalArgumentException("declaredType is null");

        XMLStreamReader streamReader = createXMLStreamReader(new DOMSource(node));
        return unmarshal(streamReader, declaredType);
    }

    public Object unmarshal(Node node) throws JAXBException {
        if (node == null) throw new IllegalArgumentException("node is null");

        XMLStreamReader streamReader = createXMLStreamReader(new DOMSource(node));
        return unmarshal(streamReader);
    }

    public <T> JAXBElement<T> unmarshal(Source source, Class<T> declaredType) throws JAXBException {
        if (source == null) throw new IllegalArgumentException("source is null");
        if (declaredType == null) throw new IllegalArgumentException("declaredType is null");

        if (source instanceof SAXSource) {
            SAXSource saxSource = (SAXSource) source;
            return (JAXBElement<T>) unmarshal(saxSource, declaredType);
        } else {
            XMLStreamReader streamReader = createXMLStreamReader(source);
            return unmarshal(streamReader, declaredType);
        }
    }

    public Object unmarshal(Source source) throws JAXBException {
        if (source == null) throw new IllegalArgumentException("source is null");

        if (source instanceof SAXSource) {
            SAXSource saxSource = (SAXSource) source;
            return unmarshal(saxSource, null);
        } else {
            XMLStreamReader streamReader = createXMLStreamReader(source);
            return unmarshal(streamReader);
        }
    }

    private Object unmarshal(SAXSource saxSource, Class<?> declaredType) throws JAXBException {
        if (saxSource == null) throw new IllegalArgumentException("saxSource is null");

        InputSource inputSource = saxSource.getInputSource();
        if (inputSource == null) {
            throw new UnmarshalException("source.getInputSource() is null");
        }

        XMLReader xmlReader = saxSource.getXMLReader();
        if (xmlReader == null) {
            // no Sax parser specified so we can just use Stax
            if (declaredType != null) {
                return unmarshal(inputSource, declaredType);
            } else {
                return unmarshal(inputSource);
            }
        }

        UnmarshallerHandlerImpl unmarshallerHandler = getUnmarshallerHandler();
        unmarshallerHandler.setType(declaredType);
        xmlReader.setContentHandler(unmarshallerHandler);
        try {
            xmlReader.parse(inputSource);
        } catch (Exception e) {
            throw new JAXBException("Error reading XML stream.", e);
        }
        return unmarshallerHandler.getResult();
    }

    public <T> JAXBElement<T> unmarshal(XMLEventReader xmlEventReader, Class<T> declaredType) throws JAXBException {
        if (xmlEventReader == null) throw new IllegalArgumentException("xmlEventReader is null");
        if (declaredType == null) throw new IllegalArgumentException("declaredType is null");

        XMLEventStreamReader streamReader = new XMLEventStreamReader(xmlEventReader);
        return unmarshal(streamReader, declaredType);
    }

    public Object unmarshal(XMLEventReader xmlEventReader) throws JAXBException {
        if (xmlEventReader == null) throw new IllegalArgumentException("xmlEventReader is null");

        XMLEventStreamReader streamReader = new XMLEventStreamReader(xmlEventReader);
        return unmarshal(streamReader);
    }

    public <T> JAXBElement<T> unmarshal(XMLStreamReader xmlStreamReader, Class<T> declaredType) throws JAXBException {
        if (xmlStreamReader == null) throw new IllegalArgumentException("xmlStreamReader is null");
        if (declaredType == null) throw new IllegalArgumentException("declaredType is null");

        return (JAXBElement<T>) read(xmlStreamReader, declaredType, true, new RuntimeContext(this));
    }

    public Object unmarshal(XMLStreamReader xmlStreamReader) throws JAXBException {
        if (xmlStreamReader == null) throw new IllegalArgumentException("xmlStreamReader is null");

        return read(xmlStreamReader, null, null, new RuntimeContext(this));
    }

    public Object read(XMLStreamReader xmlStreamReader, Class<?> declaredType, Boolean jaxbElementWrap, RuntimeContext runtimeContext) throws JAXBException {
        if (xmlStreamReader == null) throw new IllegalArgumentException("xmlStreamReader is null");
        if (runtimeContext == null) throw new IllegalArgumentException("runtimeContext is null");

        XoXMLStreamReader reader = new XoXMLStreamReaderImpl(xmlStreamReader);
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
                    if (declaredType == null) declaredType = Object.class;
                    if (jaxbElementWrap == null)  jaxbElementWrap =  instance.getXmlRootElement() == null;

                    // check assignment is possible
                    if (declaredType.isAssignableFrom(instance.getType())) {
                        // read the object
                        o = instance.read(reader, runtimeContext);
                    } else {
                        String message = "Expected instance of " + declaredType.getName() + ", but found xsi:type " + reader.getXsiType() + " which is mapped to " + instance.getType().getName();
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
            } else if (declaredType != null && !Object.class.equals(declaredType)) {
                // check built in types first
                if (String.class.equals(declaredType)) {
                    o = reader.getElementAsString();
                } else if (Boolean.class.equals(declaredType)) {
                    o = reader.getElementAsBoolean();
                } else if (Double.class.equals(declaredType)) {
                    o = reader.getElementAsDouble();
                } else if (Long.class.equals(declaredType)) {
                    o = reader.getElementAsLong();
                } else if (Float.class.equals(declaredType)) {
                    o = reader.getElementAsFloat();
                } else if (Short.class.equals(declaredType)) {
                    o = reader.getElementAsShort();
                } else if (QName.class.equals(declaredType)) {
                    o = reader.getElementAsQName();
                } else if (byte[].class.equals(declaredType)) {
                    o = BinaryUtils.decodeAsBytes(reader);
                } else if (XMLGregorianCalendar.class.equals(declaredType)) {
                    String s = reader.getElementAsString();
                    o = dtFactory.newXMLGregorianCalendar(s);
                } else if (Duration.class.equals(declaredType)) {
                    String s = reader.getElementAsString();
                    o = dtFactory.newDuration(s);
                } else if (Node.class.equals(declaredType)) {
                    Element element = reader.getElementAsDomElement();
                    o = element;
                } else {
                    // find marshaller by expected type
                    JAXBMarshaller instance = introspector.getJaxbMarshaller(declaredType);
                    if (instance != null) {
                        if (declaredType == null) {
                            declaredType = Object.class;
                        }
                        if (jaxbElementWrap == null)  jaxbElementWrap =  instance.getXmlRootElement() == null;

                        // read the object
                        o = instance.read(reader, runtimeContext);
                    } else {
                        String message = declaredType.getName() + " is not a JAXB object";
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
                    declaredType = Object.class;

                    // read the object
                    o = instance.read(reader, runtimeContext);
                } else if (Object.class.equals(declaredType)) {
                    // @XmlAnyType(lax = true)
                    Element element = reader.getElementAsDomElement();
                    o = element;
                } else {
                    String message = "No JAXB object mapped to root element " + name + "; known root elemnts are " + introspector.getElementNames();
                    if (getEventHandler() == null || !getEventHandler().handleEvent(new ValidationEventImpl(ValidationEvent.ERROR, message, new ValidationEventLocatorImpl(reader.getLocation())))) {
                        throw new MarshalException(message);
                    }
                    jaxbElementWrap = false;
                }
            }

            // wrap if necessary
            if (jaxbElementWrap != null && jaxbElementWrap) {
                return new JAXBElement(name, declaredType, o);
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

    public Listener getListener() {
        return listener;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public ValidationEventHandler getEventHandler() throws JAXBException {
        return handler;
    }

    public void setEventHandler(ValidationEventHandler handler) throws JAXBException {
        if (handler == null) {
            handler = new DefaultValidationEventHandler();
        }
        this.handler = handler;
    }

    @SuppressWarnings({"unchecked"})
    public <A extends XmlAdapter> A getAdapter(Class<A> type) {
        return (A) adapters.get(type);
    }

    public void setAdapter(XmlAdapter adapter) {
        if (adapter == null) throw new IllegalArgumentException("adapter is null");
        setAdapter((Class<XmlAdapter>) adapter.getClass(), adapter);
    }

    public <A extends XmlAdapter> void setAdapter(Class<A> type, A adapter) {
        if (type == null) throw new IllegalArgumentException("type is null");
        if (adapter != null) {
            adapters.put(type, adapter);
        } else {
            adapters.remove(type);
        }
    }

    public AttachmentUnmarshaller getAttachmentUnmarshaller() {
        return attachmentUnmarshaller;
    }

    public void setAttachmentUnmarshaller(AttachmentUnmarshaller attachmentUnmarshaller) {
        this.attachmentUnmarshaller = attachmentUnmarshaller;
    }

    public Schema getSchema() {
        return schema;
    }

    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    public UnmarshallerHandlerImpl getUnmarshallerHandler() {
        return new UnmarshallerHandlerImpl(this);
    }

    //
    // Unused methods
    //

    public Object getProperty(String name) throws PropertyException {
        if (name == null) throw new IllegalArgumentException("name is null");
        throw new PropertyException(name);
    }

    public void setProperty(String name, Object value) throws PropertyException {
        if (name == null) throw new IllegalArgumentException("name is null");
        throw new PropertyException(name, value);
    }

    public boolean isValidating() throws JAXBException {
        return false;
    }

    public void setValidating(boolean validating) throws JAXBException {
    }

}
