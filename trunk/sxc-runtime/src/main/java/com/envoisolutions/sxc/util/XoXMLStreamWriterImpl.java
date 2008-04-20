package com.envoisolutions.sxc.util;

import java.util.Iterator;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Attr;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Entity;
import org.w3c.dom.Text;
import org.w3c.dom.Document;
import org.w3c.dom.EntityReference;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Notation;
import org.w3c.dom.Comment;
import org.w3c.dom.DocumentType;

public class XoXMLStreamWriterImpl implements XoXMLStreamWriter {
    private final static String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";
    private XMLStreamWriter delegate;
    private DefaultNamespace defaultNamespace = new DefaultNamespace();

    public XoXMLStreamWriterImpl(XMLStreamWriter writer) {
        super();
        this.delegate = writer;
        try {
            delegate.setPrefix("xml", "http://www.w3.org/XML/1998/namespace");
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeXsiNil() throws XMLStreamException {
        String prefix = getUniquePrefix(XSI_NS, true);
        
        writeAttribute(prefix, XSI_NS, "nil", "true");
    }
    
    public void writeAndDeclareIfUndeclared(String prefix, String namespace) throws XMLStreamException {
        if (!namespace.equals(defaultNamespace.getNamespaceURI()) && getPrefix(namespace) == null) {
            writeNamespace(prefix, namespace);
            setPrefix(prefix, namespace);
        }
    }

    private void writeAndDeclareIfUndeclared(String prefix, String namespace, boolean useExactPrefix) throws XMLStreamException {
        if (useExactPrefix) {
            // prefix must match exactally
            Iterator prefixes = getNamespaceContext().getPrefixes(namespace);
            while (prefixes.hasNext()) {
                if (prefix.equals(prefixes.next())) {
                    break;
                }
            }
        } else if (!namespace.equals(defaultNamespace.getNamespaceURI()) && getPrefix(namespace) == null) {
            // any prefix will do
            return;
        }
        writeNamespace(prefix, namespace);
        setPrefix(prefix, namespace);
    }

    public void writeQName(QName q) throws XMLStreamException {
        String prefix = q.getPrefix();
        if (prefix.length() > 0) {
            writeAndDeclareIfUndeclared(prefix, q.getNamespaceURI(), true);
        } else {
            if (!q.getNamespaceURI().equals(defaultNamespace.getNamespaceURI())) {
                prefix = getUniquePrefix(q.getNamespaceURI(), true);
            }
        }

        if (prefix.length() > 0) {
            writeCharacters(prefix);
            writeCharacters(":");
        }
        writeCharacters(q.getLocalPart());
    }

    public String getQNameAsString(QName q) throws XMLStreamException {
        String prefix = q.getPrefix();
        if (prefix.length() > 0) {
            setPrefix(prefix, q.getNamespaceURI());
        } else {
            prefix = getUniquePrefix(q.getNamespaceURI(), true);
        }

        if (prefix.length() > 0) {
            return new StringBuilder(prefix).append(":").append(q.getLocalPart()).toString();
        } else {
            return q.getLocalPart();
        }
    }

    public void writeDomElement(Element element) throws XMLStreamException {
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attribute = (Attr) attributes.item(i);
            String prefix = "";
            String attributeNamespace = attribute.getNamespaceURI();
            if (attributeNamespace != null) {
                if ("http://www.w3.org/XML/1998/namespace".equals(attributeNamespace)) {
                    prefix = "xml";
                } else {
                    prefix = getUniquePrefix(attributeNamespace);
                }
            }
            writeAttribute(prefix, attribute.getNamespaceURI(), attribute.getLocalName(), attribute.getValue());
        }

        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (child instanceof Element) {
                Element childElement = (Element) child;
                writeStartElementWithAutoPrefix(childElement.getNamespaceURI(), childElement.getLocalName());
                writeDomElement(childElement);
                writeEndElement();
            } else if (child instanceof Text) {
                Text text = (Text) child;
                writeString(text.getData());
            } else if (child instanceof CDATASection) {
                CDATASection cdataSection = (CDATASection) child;
                writeCData(cdataSection.getData());
            } else if (child instanceof Comment) {
                Comment comment = (Comment) child;
                writeComment(comment.getData());
            } else if (child instanceof Attr) {
            } else if (child instanceof Document) {
            } else if (child instanceof DocumentFragment) {
            } else if (child instanceof DocumentType) {
            } else if (child instanceof Entity) {
            } else if (child instanceof EntityReference) {
            } else if (child instanceof Notation) {
            } else if (child instanceof ProcessingInstruction) {
            }
        }
    }

    public void writeXsiType(String namespace, String local) throws XMLStreamException {
        String prefix = getUniquePrefix(namespace, true);
        String value;
        if (prefix.equals("")) {
            value = local;
        } else {
            value = new StringBuilder(prefix).append(":").append(local).toString();
        }
        
        String xsiP = getPrefix(XSI_NS);
        if (xsiP == null) {
            xsiP = "xsi";
            writeNamespace("xsi", XSI_NS);
        }
        writeAttribute(xsiP, XSI_NS, "type", value);
    }

    public String getUniquePrefix(String namespaceURI) throws XMLStreamException {
        return getUniquePrefix(namespaceURI, true);
    }
    
    public String getUniquePrefix(String namespaceURI, boolean declare) throws XMLStreamException {
        if (namespaceURI.equals(defaultNamespace.getNamespaceURI())) {
            return "";
        }

        String prefix = getNamespaceContext().getPrefix(namespaceURI);
        if (prefix == null)
        {
            prefix = getUniquePrefix();

            if (declare) 
            {
                setPrefix(prefix, namespaceURI);
                writeNamespace(prefix, namespaceURI);
            }
        }
        
        return prefix;
    }

    public String getUniquePrefix()
    {
        int n = 1;

        while (true)
        {
            String nsPrefix = "ns" + n;

            if (getNamespaceContext().getNamespaceURI(nsPrefix) == null)
            {
                return nsPrefix;
            }

            n++;
        }
    }
    
    public void writeString(String s) throws XMLStreamException {
        if (s == null) {
            return;
        }
        writeCharacters(s);
    }
    
    public void writeBoolean(boolean b) throws XMLStreamException {
        writeCharacters(Boolean.toString(b));
    }

    public void writeDouble(double b) throws XMLStreamException {
        writeCharacters(Double.toString(b));
    }

    public void writeFloat(float b) throws XMLStreamException {
        writeCharacters(Float.toString(b));
    }

    public void writeLong(long b) throws XMLStreamException {
        writeCharacters(Long.toString(b));
    }

    public void writeShort(short b) throws XMLStreamException {
        writeCharacters(Short.toString(b));
    }
    public void writeByte(byte b) throws XMLStreamException {
        writeCharacters(Byte.toString(b));
    }
    
    public void writeInt(int i) throws XMLStreamException {
        writeCharacters(Integer.toString(i));
    }

    public void writeStartElementWithAutoPrefix(String namespaceURI, String localName) throws XMLStreamException {
        // check if there is an existing prefix for this namespace
        String prefix;
        if (namespaceURI.equals(defaultNamespace.getNamespaceURI())) {
            prefix = "";
        } else {
            prefix = getNamespaceContext().getPrefix(namespaceURI);
        }

        // if there isn't an existing prefix, generate a new one which we'll declare below
        boolean declareNs = false;
        if (prefix == null) {
            prefix = "";
            declareNs = true;
        }

        // start the element
        writeStartElement(prefix, localName, namespaceURI);

        // declare the namespace in the new element
        if (declareNs) {
            writeDefaultNamespace(namespaceURI);
        }
    }

    public void close() throws XMLStreamException {
        delegate.close();
    }

    public void flush() throws XMLStreamException {
        delegate.flush();
    }

    public NamespaceContext getNamespaceContext() {
        return delegate.getNamespaceContext();
    }

    public String getPrefix(String uri) throws XMLStreamException {
        return delegate.getPrefix(uri);
    }

    public Object getProperty(String name) throws IllegalArgumentException {
        return delegate.getProperty(name);
    }

    public void setDefaultNamespace(String uri) throws XMLStreamException {
        delegate.setDefaultNamespace(uri);
    }

    public void setNamespaceContext(NamespaceContext arg0) throws XMLStreamException {
        delegate.setNamespaceContext(arg0);
    }

    public void setPrefix(String prefix, String uri) throws XMLStreamException {
        delegate.setPrefix(prefix, uri);
    }

    public void writeAttribute(String prefix, String namespaceURI, String localName, String value) throws XMLStreamException {
        delegate.writeAttribute(prefix, namespaceURI, localName, value);
    }

    public void writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {
        delegate.writeAttribute(namespaceURI, localName, value);
    }

    public void writeAttribute(String localName, String value) throws XMLStreamException {
        delegate.writeAttribute(localName, value);
    }

    public void writeCData(String data) throws XMLStreamException {
        delegate.writeCData(data);
    }

    public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
        delegate.writeCharacters(text, start, len);
    }

    public void writeCharacters(String text) throws XMLStreamException {
        delegate.writeCharacters(text);
    }

    public void writeComment(String data) throws XMLStreamException {
        delegate.writeComment(data);
    }

    public void writeDefaultNamespace(String uri) throws XMLStreamException {
        defaultNamespace.setNamespaceURI(uri);
        delegate.writeDefaultNamespace(uri);
    }

    public void writeDTD(String dtd) throws XMLStreamException {
        delegate.writeDTD(dtd);
    }

    public void writeEmptyElement(String prefix, String namespaceURI, String localName) throws XMLStreamException {
        delegate.writeEmptyElement(prefix, namespaceURI, localName);
    }

    public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
        delegate.writeEmptyElement(namespaceURI, localName);
    }

    public void writeEmptyElement(String localName) throws XMLStreamException {
        delegate.writeEmptyElement(localName);
    }

    public void writeEndDocument() throws XMLStreamException {
        delegate.writeEndDocument();
    }

    public void writeEndElement() throws XMLStreamException {
        delegate.writeEndElement();
        defaultNamespace = defaultNamespace.getParent();
    }

    public void writeEntityRef(String name) throws XMLStreamException {
        delegate.writeEntityRef(name);
    }

    public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
        delegate.writeNamespace(prefix, namespaceURI);
    }

    public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
        delegate.writeProcessingInstruction(target, data);
    }

    public void writeProcessingInstruction(String target) throws XMLStreamException {
        delegate.writeProcessingInstruction(target);
    }

    public void writeStartDocument() throws XMLStreamException {
        delegate.writeStartDocument();
    }

    public void writeStartDocument(String encoding, String version) throws XMLStreamException {
        delegate.writeStartDocument(encoding, version);
    }

    public void writeStartDocument(String version) throws XMLStreamException {
        delegate.writeStartDocument(version);
    }

    public void writeStartElement(String prefix, String namespaceURI, String localName) throws XMLStreamException {
        defaultNamespace = new DefaultNamespace(defaultNamespace);
        delegate.writeStartElement(prefix, namespaceURI, localName);
    }

    public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
        defaultNamespace = new DefaultNamespace(defaultNamespace);
        delegate.writeStartElement(namespaceURI, localName);
    }

    public void writeStartElement(String localName) throws XMLStreamException {
        defaultNamespace = new DefaultNamespace(defaultNamespace);
        delegate.writeStartElement(localName);
    }

    private static class DefaultNamespace {
        private final DefaultNamespace parent;
        private String namespaceURI;

        private DefaultNamespace() {
            parent = null;
        }

        private DefaultNamespace(DefaultNamespace parent) {
            if (parent == null) throw new NullPointerException("parent is null");
            this.parent = parent;
        }

        public DefaultNamespace getParent() {
            if (parent == null) {
                throw new IllegalAccessError("parent is null");
            }
            return parent;
        }

        public String getNamespaceURI() {
            if (namespaceURI != null) {
                return namespaceURI;
            }
            if (parent == null) {
                return null;
            }
            return parent.getNamespaceURI();
        }

        public void setNamespaceURI(String namespaceURI) {
            if (this.namespaceURI != null) {
                throw new IllegalStateException("Default namespace is alreayd set to " + this.namespaceURI);
            }
            this.namespaceURI = namespaceURI;
        }
    }
}
