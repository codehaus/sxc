package com.envoisolutions.sxc.jaxb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.PropertyException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshallerHandler;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.attachment.AttachmentUnmarshaller;
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
import javax.xml.validation.Schema;

import org.w3c.dom.Node;

import org.xml.sax.InputSource;

import com.envoisolutions.sxc.Context;
import com.envoisolutions.sxc.util.XoXMLStreamReader;
import com.envoisolutions.sxc.util.XoXMLStreamReaderImpl;

public class UnmarshallerImpl implements Unmarshaller {

    private XMLInputFactory xif = XMLInputFactory.newInstance();
    private JAXBContextImpl jaxbCtx;
    private Context context;
    private com.envoisolutions.sxc.Reader unmarshaller;
    private Listener listener;
    private Schema schema;
    private boolean validating;
    private AttachmentUnmarshaller au;
    private ValidationEventHandler eventHandler;
    private UnmarshallerHandler unmarshallerHandler;
    private Map<Class, QName> c2type;
    private DatatypeFactory dtFactory;
    
    public UnmarshallerImpl(JAXBContextImpl jaxbCtx, Map<Class, QName> c2type, Context context) 
        throws JAXBException {
        this.jaxbCtx = jaxbCtx;
        this.context = context;
        this.c2type = c2type;
        this.unmarshaller = context.createReader();
        try {
            dtFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new JAXBException("Could not create datatype factory.", e);
        }
    }

    public <A extends XmlAdapter> A getAdapter(Class<A> arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    public AttachmentUnmarshaller getAttachmentUnmarshaller() {
        return au;
    }

    public ValidationEventHandler getEventHandler() throws JAXBException {
        return eventHandler;
    }

    public Listener getListener() {
        return listener;
    }

    public Object getProperty(String key) throws PropertyException {
        return context.get(key);
    }

    public Schema getSchema() {
        return schema;
    }

    public UnmarshallerHandler getUnmarshallerHandler() {
        return unmarshallerHandler;
    }

    public boolean isValidating() throws JAXBException {
        // TODO Auto-generated method stub
        return validating;
    }

    public <A extends XmlAdapter> void setAdapter(Class<A> arg0, A arg1) {
        // TODO Auto-generated method stub
        
    }

    public void setAdapter(XmlAdapter arg0) {
        // TODO Auto-generated method stub
        
    }

    public void setAttachmentUnmarshaller(AttachmentUnmarshaller arg0) {
        // TODO Auto-generated method stub
        
    }

    public void setEventHandler(ValidationEventHandler eventHandler) throws JAXBException {
        this.eventHandler = eventHandler;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setProperty(String key, Object value) throws PropertyException {
        context.put(key, value);
    }

    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    public void setValidating(boolean validating) throws JAXBException {
        this.validating = validating;
    }

    public Object unmarshal(File arg0) throws JAXBException {
        try {
            return unmarshal(new FileInputStream(arg0));
        } catch (FileNotFoundException e) {
            throw new JAXBException("Could not open file: " + arg0.getAbsolutePath(), e);
        }
    }

    public Object unmarshal(InputSource is) throws JAXBException {
        throw new UnsupportedOperationException();
    }

    public Object unmarshal(InputStream is) throws JAXBException {
        try {
            return unmarshaller.read(is);
        } catch (Exception e) {
            throw new JAXBException(e);
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

    public Object unmarshal(Reader reader) throws JAXBException {
        try {
            XMLStreamReader xsr = xif.createXMLStreamReader(reader);
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

    public Object unmarshal(URL url) throws JAXBException {
        try {
            return unmarshal(url.openStream());
        } catch (IOException e) {
            throw new JAXBException("Could not open URL stream.", e);
        }
    }

    public <T> JAXBElement<T> unmarshal(XMLEventReader arg0, Class<T> arg1) throws JAXBException {
        throw new UnsupportedOperationException();
    }

    public Object unmarshal(XMLEventReader arg0) throws JAXBException {
        throw new UnsupportedOperationException();
    }

    public <T> JAXBElement<T> unmarshal(XMLStreamReader xsr, 
                                        Class<T> cls) throws JAXBException {
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
                        o = unmarshaller.read(reader, null);
                    } else {
                        return (JAXBElement<T>) unmarshaller.read(reader, null, type);
                    }
                }
                return new JAXBElement<T>(name, cls, cast(o, cls));
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

    @SuppressWarnings("unchecked")
    protected static <T> T cast(Object p, Class<T> cls) {
        return (T)p;
    }
    
    public Object unmarshal(XMLStreamReader reader) throws JAXBException {
        try {
            return unmarshaller.read(reader);
        } catch (Exception e) {
            throw new JAXBException(e);
        }
    }

}
