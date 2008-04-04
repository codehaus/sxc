package com.envoisolutions.sxc.jaxb;

import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.MarshalException;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.attachment.AttachmentMarshaller;
import javax.xml.bind.helpers.AbstractMarshallerImpl;
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

import com.envoisolutions.sxc.Context;
import com.envoisolutions.sxc.jaxb.model.Bean;
import com.envoisolutions.sxc.jaxb.model.Model;
import com.envoisolutions.sxc.util.PrettyPrintXMLStreamWriter;
import com.envoisolutions.sxc.util.W3CDOMStreamWriter;
import com.envoisolutions.sxc.util.XoXMLStreamWriter;
import com.envoisolutions.sxc.util.XoXMLStreamWriterImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class MarshallerImpl extends AbstractMarshallerImpl {
	public static final String MARSHALLER = "sxc.marshaller";

    private final Model model;
    private final Context context;
    private final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();

    private final Map<Class<?>, ? super XmlAdapter> adapters = new HashMap<Class<?>, XmlAdapter>();
    private AttachmentMarshaller attachmentMarshaller;
    private ValidationEventHandler eventHandler;
    private Listener listener;
    private Schema schema;

    private final JAXBIntrospector introspector;

    public MarshallerImpl(JAXBContext jaxbContext, Model model, Context context) {
        super();
        this.model = model;
        this.context = context;
        this.introspector = jaxbContext.createJAXBIntrospector();
        context.put(MARSHALLER, this);
    }

    public void marshal(Object jaxbElement, Result result) throws JAXBException {
        if (jaxbElement == null) throw new IllegalArgumentException("jaxbElement is null");
        if (result == null) throw new IllegalArgumentException("result is null");
        XMLStreamWriter writer = null;
        try {
            if (result instanceof DOMResult) {
                Node node = ((DOMResult) result).getNode();

                if (node instanceof Document) {
                    writer = new W3CDOMStreamWriter((Document) node);
                } else if (node instanceof Element) {
                    writer = new W3CDOMStreamWriter((Element) node);
                } else {
                    throw new UnsupportedOperationException("Node type not supported.");
                }
            } else {
                writer = xmlOutputFactory.createXMLStreamWriter(result);
            }
            marshal(jaxbElement, writer);
        } catch (XMLStreamException e) {
            throw new JAXBException("Could not close XMLStreamWriter.", e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (XMLStreamException ignored) {
                }
            }
        }
    }

    public void marshal(Object jaxbElement, XMLEventWriter writer) throws JAXBException {
        // todo how do we convert XMLEventWriter into a XMLStreamWriter 
        throw new UnsupportedOperationException();
    }

    public void marshal(Object jaxbElement, XMLStreamWriter writer) throws JAXBException {
        if (jaxbElement == null) throw new IllegalArgumentException("o is null");
        if (writer == null) throw new IllegalArgumentException("xsw is null");
        try {
            if (!introspector.isElement(jaxbElement)) {
                throw new MarshalException("Object must be annotated with @XmlRootElement or be a JAXBElement!");
            }

            // if formatted output is set, use the pretty print wrapper
            if (isFormattedOutput()) {
                writer = new PrettyPrintXMLStreamWriter(writer);
            }

            XoXMLStreamWriter w = new XoXMLStreamWriterImpl(writer);

            // if the is not a fragment, write the document header
            if (!isFragment()) {
                w.writeStartDocument(getEncoding(), null);
            }
                                    
            QName name;
            QName xsiType = null;
            boolean writeXsiNil = false;
            if (jaxbElement instanceof JAXBElement) {
                JAXBElement element = (JAXBElement) jaxbElement;
                jaxbElement = element.getValue();

                name = element.getName();
                writeXsiNil = element.isNil();

                Bean bean = model.getBean(jaxbElement.getClass());
                if (bean != null && bean.getRootElementName() == null) {
                    xsiType = bean.getSchemaTypeName();
                }
            } else {
                Bean bean = model.getBean(jaxbElement.getClass());
                if (bean == null || bean.getRootElementName() == null) {
                    throw new MarshalException("Object must be annotated with @XmlRootElement or be a JAXBElement!");
                }
                name = bean.getRootElementName();
            }

            // write root element
            w.writeStartElement("",  name.getLocalPart(), name.getNamespaceURI());
            w.writeAndDeclareIfUndeclared("ns1", name.getNamespaceURI());
            w.writeDefaultNamespace(name.getNamespaceURI());

            if (xsiType != null) {
                w.writeXsiType(xsiType.getNamespaceURI(), xsiType.getLocalPart());
            }

            if (writeXsiNil) {
                w.writeXsiNil();
            } else if (jaxbElement != null) {
                Class c = jaxbElement.getClass();
                if (c == String.class) {
                    w.writeCharacters((String) jaxbElement);
                } else if (c == Boolean.class) {
                    w.writeBoolean((Boolean) jaxbElement);
                } else if (c == Byte.class) {
                    w.writeByte((Byte) jaxbElement);
                } else if (c == Double.class) {
                    w.writeDouble((Double) jaxbElement);
                } else if (c == Float.class) {
                    w.writeFloat((Float) jaxbElement);
                } else if (c == Long.class) {
                    w.writeLong((Long) jaxbElement);
                } else if (c == Integer.class) {
                    w.writeInt((Integer) jaxbElement);
                } else if (c == Short.class) {
                    w.writeShort((Short) jaxbElement);
                } else if (c == Duration.class) {
                    w.writeCharacters(jaxbElement.toString());
                } else if (c == XMLGregorianCalendar.class) {
                    w.writeCharacters(((XMLGregorianCalendar) jaxbElement).toXMLFormat());
                } else if (c == byte[].class) {
                    BinaryUtils.encodeBytes(w, (byte[]) jaxbElement);
                } else {
                    context.createWriter().write(w, jaxbElement);
                }
            }

            // close root element
            w.writeEndElement();

            if (!isFragment()) {
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

    @SuppressWarnings({"unchecked"})
    public <A extends XmlAdapter> A getAdapter(Class<A> type) {
        return (A) adapters.get(type);
    }

    public <A extends XmlAdapter> void setAdapter(Class<A> type, A adapter) {
        adapters.put(type, adapter);
    }

    public AttachmentMarshaller getAttachmentMarshaller() {
        return attachmentMarshaller;
    }

    public void setAttachmentMarshaller(AttachmentMarshaller attachmentMarshaller) {
        this.attachmentMarshaller = attachmentMarshaller;
    }

    public ValidationEventHandler getEventHandler() throws JAXBException {
        return eventHandler;
    }

    public void setEventHandler(ValidationEventHandler eventHandler) throws JAXBException {
        this.eventHandler = eventHandler;
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
}
