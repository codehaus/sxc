package com.envoisolutions.sxc.jaxb;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.MarshalException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.attachment.AttachmentMarshaller;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;
import javax.xml.validation.Schema;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;

import com.envoisolutions.sxc.Context;
import com.envoisolutions.sxc.util.W3CDOMStreamWriter;
import com.envoisolutions.sxc.util.XoXMLStreamWriter;
import com.envoisolutions.sxc.util.XoXMLStreamWriterImpl;

public class MarshallerImpl implements Marshaller {

	public static final String MARSHALLER = "sxc.marshaller"; 
    JAXBContext jaxbContext;
    Context context;
    XMLOutputFactory xof = XMLOutputFactory.newInstance();
    Map<String, Object> properties = new HashMap<String, Object>();
    Listener listener;
    private AttachmentMarshaller am;
    private ValidationEventHandler eventHandler;
    private Schema schema;
    private JAXBIntrospector introspector;
    private boolean writeStartAndEnd = true;
    
    public MarshallerImpl(JAXBContext jaxbContext, Context context) {
        super();
        this.jaxbContext = jaxbContext;
        this.context = context;
        this.introspector = jaxbContext.createJAXBIntrospector();
        context.put(MARSHALLER, this);
    }

    public <A extends XmlAdapter> A getAdapter(Class<A> arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    public AttachmentMarshaller getAttachmentMarshaller() {
        return am;
    }

    public ValidationEventHandler getEventHandler() throws JAXBException {
        return eventHandler;
    }

    public Listener getListener() {
        return listener;
    }

    public Node getNode(Object arg0) throws JAXBException {
        throw new UnsupportedOperationException();
    }

    public Object getProperty(String key) throws PropertyException {
        return properties.get(key);
    }

    public Schema getSchema() {
        return schema;
    }

    public void marshal(Object arg0, ContentHandler arg1) throws JAXBException {
        throw new UnsupportedOperationException();
    }

    public void marshal(Object obj, Node node) throws JAXBException {
        if (obj == null) throw new IllegalArgumentException("obj is null");
        if (node == null) throw new IllegalArgumentException("out is node");
        marshal(obj, new DOMResult(node));
    }

    public void marshal(Object obj, File file) throws JAXBException {
        if (obj == null) throw new IllegalArgumentException("obj is null");
        if (file == null) throw new IllegalArgumentException("file is null");
        try {
            OutputStream stream = new FileOutputStream(file);
            marshal(obj, stream);
            stream.close();
        } catch (IOException e) {
            throw new JAXBException(e);
        }
    }

    public void marshal(Object obj, OutputStream out) throws JAXBException {
        if (obj == null) throw new IllegalArgumentException("obj is null");
        if (out == null) throw new IllegalArgumentException("out is null");
        try {
            XMLStreamWriter writer = xof.createXMLStreamWriter(out);
            marshalAndClose(obj, writer);
        } catch (XMLStreamException e) {
            throw new JAXBException("Could not close XMLStreamWriter.", e);
        }
    }

    private void marshalAndClose(Object obj, XMLStreamWriter writer) throws JAXBException, XMLStreamException {
        marshal(obj, writer);
        writer.close();
    }

    public void marshal(Object obj, Result r) throws JAXBException {
        if (obj == null) throw new IllegalArgumentException("obj is null");
        if (r == null) throw new IllegalArgumentException("r is null");
        try {
            XMLStreamWriter writer;
            if (r instanceof DOMResult) {
                Node node = ((DOMResult) r).getNode();
                
                if (node instanceof Document) {
                    writer = new W3CDOMStreamWriter((Document) node);
                } else if (node instanceof Element) {
                    writer = new W3CDOMStreamWriter((Element) node);
                } else {
                    throw new UnsupportedOperationException("Node type not supported.");
                }
            } else {
                writer = xof.createXMLStreamWriter(r);
            }
            marshalAndClose(obj, writer);
        } catch (XMLStreamException e) {
            throw new JAXBException("Could not close XMLStreamWriter.", e);
        }
    }

    public void marshal(Object obj, Writer writer) throws JAXBException {
        if (obj == null) throw new IllegalArgumentException("obj is null");
        if (writer == null) throw new IllegalArgumentException("writer is null");
        try {
            XMLStreamWriter xsw = xof.createXMLStreamWriter(writer);
            marshalAndClose(obj, xsw);
        } catch (XMLStreamException e) {
            throw new JAXBException("Could not close XMLStreamWriter.", e);
        }
    }

    public void marshal(Object arg0, XMLEventWriter arg1) throws JAXBException {
        throw new UnsupportedOperationException();
    }

    public void marshal(Object o, XMLStreamWriter xsw) throws JAXBException {
        if (o == null) throw new IllegalArgumentException("o is null");
        if (xsw == null) throw new IllegalArgumentException("xsw is null");
        try {
            if (!introspector.isElement(o)) {
                throw new MarshalException("Object must be annotated with @XmlRootElement or be a JAXBElement!");
            }
            
            XoXMLStreamWriter w = new XoXMLStreamWriterImpl(xsw);
            if (writeStartAndEnd) {
                w.writeStartDocument();
            }
            
            JAXBElement jaxbElement = null;
            if (o instanceof JAXBElement) {
                jaxbElement = (JAXBElement) o;
            }
            
            if (jaxbElement != null) {
                QName n = jaxbElement.getName();
                w.writeStartElement("",  n.getLocalPart(), n.getNamespaceURI());
                // TODO: we should check to see if a NS is already written here
                w.writeDefaultNamespace(n.getNamespaceURI());
                
                o = jaxbElement.getValue();
            }
            
            if (o == null) {
                w.writeXsiNil();
            } else {
                Class c = o.getClass();
                if (c == String.class) {
                    w.writeCharacters((String) o);
                } else if (c == Boolean.class) {
                    w.writeBoolean((Boolean) o);
                } else if (c == Byte.class) {
                    w.writeByte((Byte) o);
                } else if (c == Double.class) {
                    w.writeDouble((Double) o);
                } else if (c == Float.class) {
                    w.writeFloat((Float) o);
                } else if (c == Long.class) {
                    w.writeLong((Long) o);
                } else if (c == Integer.class) {
                    w.writeInt((Integer) o);
                } else if (c == Short.class) {
                    w.writeShort((Short) o);
                } else if (c == Duration.class) {
                    w.writeCharacters(((Duration) o).toString());
                } else if (c == XMLGregorianCalendar.class) {
                    w.writeCharacters(((XMLGregorianCalendar) o).toXMLFormat());
                } else if (c == byte[].class) {
                    BinaryUtils.encodeBytes(w, (byte[]) o);
                } else {
                    context.createWriter().write(w, o);
                }
            }
            
            if (jaxbElement != null) {
                w.writeEndElement();
            }
            
            if (writeStartAndEnd) {
                w.writeEndDocument();
            }
        } catch (Exception e) {
            if (e instanceof JAXBException) {
                throw (JAXBException) e;
            } else if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new JAXBException("Could not marshal object.", e);
            }
        }
    }

    public <A extends XmlAdapter> void setAdapter(Class<A> arg0, A arg1) {
        // TODO Auto-generated method stub
        
    }

    public void setAdapter(XmlAdapter arg0) {
        // TODO Auto-generated method stub
        
    }

    public void setAttachmentMarshaller(AttachmentMarshaller am) {
        this.am = am;
    }

    public void setEventHandler(ValidationEventHandler eventHandler) throws JAXBException {
        this.eventHandler = eventHandler;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setProperty(String key, Object value) throws PropertyException {
        if (key.equals(Marshaller.JAXB_FRAGMENT)) {
            writeStartAndEnd = !((Boolean) value).booleanValue();
        }
        
        properties.put(key, value);
        
    }

    public void setSchema(Schema schema) {
        this.schema = schema;
    }

}
