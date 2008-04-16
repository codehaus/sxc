package com.envoisolutions.sxc.jaxb;

import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.MarshalException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.attachment.AttachmentMarshaller;
import javax.xml.bind.helpers.AbstractMarshallerImpl;
import javax.xml.bind.helpers.ValidationEventImpl;
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

import com.envoisolutions.sxc.util.PrettyPrintXMLStreamWriter;
import com.envoisolutions.sxc.util.RuntimeXMLStreamException;
import com.envoisolutions.sxc.util.W3CDOMStreamWriter;
import com.envoisolutions.sxc.util.XoXMLStreamWriter;
import com.envoisolutions.sxc.util.XoXMLStreamWriterImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class MarshallerImpl extends AbstractMarshallerImpl {
	public static final String MARSHALLER = "sxc.marshaller";

    private final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
    private final JAXBIntrospectorImpl introspector;

    private final Map<Class<?>, ? super XmlAdapter> adapters = new HashMap<Class<?>, XmlAdapter>();
    private AttachmentMarshaller attachmentMarshaller;
    private Listener listener;
    private Schema schema;

    public MarshallerImpl(JAXBIntrospectorImpl introspector) {
        this.introspector = introspector;
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
        if (jaxbElement == null) throw new IllegalArgumentException("jaxbElement is null");
        if (writer == null) throw new IllegalArgumentException("xsw is null");

        if (!introspector.isElement(jaxbElement)) {
            throw new MarshalException("Object must be annotated with @XmlRootElement or be a JAXBElement!");
        }

        // if formatted output is set, use the pretty print wrapper
        if (isFormattedOutput()) {
            writer = new PrettyPrintXMLStreamWriter(writer);
        }

        // writer with out custom extension
        XoXMLStreamWriter w = new XoXMLStreamWriterImpl(writer);

        try {
            // if the is not a fragment, write the document header
            if (!isFragment()) {
                w.writeStartDocument(getEncoding(), null);
            }

            write(jaxbElement, w, new RuntimeContext(this), true);

            if (!isFragment()) {
                w.writeEndDocument();
            }
        } catch (XMLStreamException e) {
            throw new UnmarshalException(e);
        }
    }

    public void write(Object jaxbElement, XoXMLStreamWriter w, RuntimeContext context, boolean writeTag) throws JAXBException {
        QName name;
        QName xsiType = null;
        boolean writeXsiNil = false;
        if (jaxbElement instanceof JAXBElement) {
            JAXBElement element = (JAXBElement) jaxbElement;
            jaxbElement = element.getValue();

            name = element.getName();
            writeXsiNil = element.isNil();

            JAXBMarshaller marshaller = introspector.getJaxbMarshaller(jaxbElement.getClass());
            if (marshaller != null && marshaller.getXmlRootElement() == null) {
                xsiType = marshaller.getXmlType();
            }
        } else {
            JAXBMarshaller marshaller = introspector.getJaxbMarshaller(jaxbElement.getClass());
            if (marshaller == null || marshaller.getXmlRootElement() == null) {
                throw new MarshalException("Object must be annotated with @XmlRootElement or be a JAXBElement!");
            }
            name = marshaller.getXmlRootElement();
        }

        try {
            if (writeTag) {
                // open element
                w.writeStartElement("",  name.getLocalPart(), name.getNamespaceURI());

                // declare namespace (this can only be done when writing the tag)
                if (name.getNamespaceURI().length() > 0) {
                    w.writeDefaultNamespace(name.getNamespaceURI());
                }
            }

            if (xsiType != null) {
                w.writeXsiType(xsiType.getNamespaceURI(), xsiType.getLocalPart());
            }

            if (writeXsiNil) {
                w.writeXsiNil();
            } else if (jaxbElement != null) {
                Class<?> c = jaxbElement.getClass();
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
                    JAXBMarshaller jaxbMarshaller = introspector.getJaxbMarshaller(c);
                    if (jaxbMarshaller != null) {
                        //noinspection unchecked
                        jaxbMarshaller.write(w, jaxbElement, context);
                    } else {
                        String message = "No marshaller for " + c.getName();
                        if (getEventHandler() == null || !getEventHandler().handleEvent(new ValidationEventImpl(ValidationEvent.ERROR, message, new ValidationEventLocatorImpl(jaxbElement, null)))) {
                            throw new MarshalException(message);
                        }
                    }
                }
            }

            if (writeTag) {
                // close element
                w.writeEndElement();
            }
        } catch (Exception e) {
            if (e instanceof JAXBException) {
                // assume event handler has already been notified
                throw (JAXBException) e;
            }
            if (e instanceof RuntimeXMLStreamException) {
                // simply unwrap and handle below
                e = ((RuntimeXMLStreamException) e).getCause();
            }

            // report fatal error
            if (getEventHandler() != null) {
                getEventHandler().handleEvent(new ValidationEventImpl(ValidationEvent.FATAL_ERROR, "Fatal error", new ValidationEventLocatorImpl(), e));
            }

            if (e instanceof XMLStreamException) {
                Throwable cause = e.getCause();
                if (cause instanceof JAXBException) {
                    throw (JAXBException) e;
                }
                throw new MarshalException(cause == null ? e : cause);
            }
            throw new MarshalException(e);

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
