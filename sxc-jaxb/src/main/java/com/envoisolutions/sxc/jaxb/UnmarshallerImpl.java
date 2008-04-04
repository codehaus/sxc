package com.envoisolutions.sxc.jaxb;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
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

import com.envoisolutions.sxc.Context;
import com.envoisolutions.sxc.jaxb.model.Bean;
import com.envoisolutions.sxc.jaxb.model.Model;
import com.envoisolutions.sxc.util.XoXMLStreamReader;
import com.envoisolutions.sxc.util.XoXMLStreamReaderImpl;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

@SuppressWarnings({"unchecked"})
public class UnmarshallerImpl extends AbstractUnmarshallerImpl {
    private final Map<Class, QName> c2type;
    private final Context context;
    private final XMLInputFactory xif = XMLInputFactory.newInstance();
    private final DatatypeFactory dtFactory;

    private final Map<Class<?>, ? super XmlAdapter> adapters = new HashMap<Class<?>, XmlAdapter>();
    private Listener listener;
    private Schema schema;
    private AttachmentUnmarshaller attachmentUnmarshaller;

    public UnmarshallerImpl(Model model, Context context) throws JAXBException {
        this.context = context;

        c2type = new LinkedHashMap<Class, QName>();
        for (Bean bean : model.getBeans()) {
            if (bean.getSchemaTypeName() != null) {
                c2type.put(bean.getType(), bean.getSchemaTypeName());
            }
        }

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
        try {
            return context.createReader().read(reader);
        } catch (Exception e) {
            throw new JAXBException(e);
        }
    }

    public <T> JAXBElement<T> unmarshal(XMLStreamReader xsr, Class<T> cls) throws JAXBException {
        XoXMLStreamReader reader = new XoXMLStreamReaderImpl(xsr);
        try {
            int event = reader.getEventType();
            while (event != XMLStreamConstants.START_ELEMENT && reader.hasNext()) {
                event = reader.next();
            }

            if (event == XMLStreamConstants.START_ELEMENT) {
                QName name = reader.getName();

                Object o = null;
                if (reader.isXsiNil()) {
                    // its null
                } else if (cls.equals(String.class)) {
                    o = reader.getElementAsString();
                } else if (cls.equals(Boolean.class)) {
                    o = reader.getElementAsBoolean();
                } else if (cls.equals(Double.class)) {
                    o = reader.getElementAsDouble();
                } else if (cls.equals(Long.class)) {
                    o = reader.getElementAsLong();
                } else if (cls.equals(Float.class)) {
                    o = reader.getElementAsFloat();
                } else if (cls.equals(Short.class)) {
                    o = reader.getElementAsShort();
                } else if (cls.equals(QName.class)) {
                    o = reader.getElementAsQName();
                } else if (cls.equals(byte[].class)) {
                    o = BinaryUtils.decodeAsBytes(reader);
                } else if (cls.equals(XMLGregorianCalendar.class)) {
                    String s = reader.getElementAsString();
                    o = dtFactory.newXMLGregorianCalendar(s);
                } else if (cls.equals(Duration.class)) {
                    String s = reader.getElementAsString();
                    o = dtFactory.newDuration(s);
                } else {
                    QName type = c2type.get(cls);
                    if (type == null) {
                        o = context.createReader().read(reader, null);
                    } else {
                        return (JAXBElement<T>) context.createReader().read(reader, null, type);
                    }
                }
                return new JAXBElement<T>(name, cls, cls.cast(o));
            } else {
                // TODO: figure out what is appropriate per spec
                return null;
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
        throw new UnsupportedOperationException();
    }
}
