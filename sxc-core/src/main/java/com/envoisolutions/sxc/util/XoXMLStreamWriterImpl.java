package com.envoisolutions.sxc.util;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class XoXMLStreamWriterImpl implements XoXMLStreamWriter {
    private final static String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";
    private XMLStreamWriter delegate;
    
    public XoXMLStreamWriterImpl(XMLStreamWriter writer) {
        super();
        this.delegate = writer;
    }

    public void writeXsiNil() throws XMLStreamException {
        String prefix = getUniquePrefix(XSI_NS, true);
        
        writeAttribute(prefix, XSI_NS, "nil", "true");
    }
    
    public void writeQName(QName q) throws XMLStreamException {
        String prefix = getUniquePrefix(q.getNamespaceURI(), true);
        if (prefix != "") {
            writeCharacters(prefix);
            writeCharacters(":");
        }
        writeCharacters(q.getLocalPart());
    }

    public String getQNameAsString(QName q) throws XMLStreamException {
        String prefix = getUniquePrefix(q.getNamespaceURI(), true);
        if (prefix != "") {
            return new StringBuilder(prefix).append(":").append(q.getLocalPart()).toString();
        } else {
            return q.getLocalPart();
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

    /**
     * Create a unique namespace uri/prefix combination.
     * 
     * @param nsUri
     * @return The namespace with the specified URI. If one doesn't exist, one
     *         is created.
     * @throws XMLStreamException
     */
    public String getUniquePrefix(String namespaceURI,
                                         boolean declare)
        throws XMLStreamException
    {
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

    public void writeInt(int i) throws XMLStreamException {
        writeCharacters(Integer.toString(i));
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

    public String getPrefix(String arg0) throws XMLStreamException {
        return delegate.getPrefix(arg0);
    }

    public Object getProperty(String arg0) throws IllegalArgumentException {
        return delegate.getProperty(arg0);
    }

    public void setDefaultNamespace(String arg0) throws XMLStreamException {
        delegate.setDefaultNamespace(arg0);
    }

    public void setNamespaceContext(NamespaceContext arg0) throws XMLStreamException {
        delegate.setNamespaceContext(arg0);
    }

    public void setPrefix(String arg0, String arg1) throws XMLStreamException {
        delegate.setPrefix(arg0, arg1);
    }

    public void writeAttribute(String arg0, String arg1, String arg2, String arg3) throws XMLStreamException {
        delegate.writeAttribute(arg0, arg1, arg2, arg3);
    }

    public void writeAttribute(String arg0, String arg1, String arg2) throws XMLStreamException {
        delegate.writeAttribute(arg0, arg1, arg2);
    }

    public void writeAttribute(String arg0, String arg1) throws XMLStreamException {
        delegate.writeAttribute(arg0, arg1);
    }

    public void writeCData(String arg0) throws XMLStreamException {
        delegate.writeCData(arg0);
    }

    public void writeCharacters(char[] arg0, int arg1, int arg2) throws XMLStreamException {
        delegate.writeCharacters(arg0, arg1, arg2);
    }

    public void writeCharacters(String arg0) throws XMLStreamException {
        delegate.writeCharacters(arg0);
    }

    public void writeComment(String arg0) throws XMLStreamException {
        delegate.writeComment(arg0);
    }

    public void writeDefaultNamespace(String arg0) throws XMLStreamException {
        delegate.writeDefaultNamespace(arg0);
    }

    public void writeDTD(String arg0) throws XMLStreamException {
        delegate.writeDTD(arg0);
    }

    public void writeEmptyElement(String arg0, String arg1, String arg2) throws XMLStreamException {
        delegate.writeEmptyElement(arg0, arg1, arg2);
    }

    public void writeEmptyElement(String arg0, String arg1) throws XMLStreamException {
        delegate.writeEmptyElement(arg0, arg1);
    }

    public void writeEmptyElement(String arg0) throws XMLStreamException {
        delegate.writeEmptyElement(arg0);
    }

    public void writeEndDocument() throws XMLStreamException {
        delegate.writeEndDocument();
    }

    public void writeEndElement() throws XMLStreamException {
        delegate.writeEndElement();
    }

    public void writeEntityRef(String arg0) throws XMLStreamException {
        delegate.writeEntityRef(arg0);
    }

    public void writeNamespace(String arg0, String arg1) throws XMLStreamException {
        delegate.writeNamespace(arg0, arg1);
    }

    public void writeProcessingInstruction(String arg0, String arg1) throws XMLStreamException {
        delegate.writeProcessingInstruction(arg0, arg1);
    }

    public void writeProcessingInstruction(String arg0) throws XMLStreamException {
        delegate.writeProcessingInstruction(arg0);
    }

    public void writeStartDocument() throws XMLStreamException {
        delegate.writeStartDocument();
    }

    public void writeStartDocument(String arg0, String arg1) throws XMLStreamException {
        delegate.writeStartDocument(arg0, arg1);
    }

    public void writeStartDocument(String arg0) throws XMLStreamException {
        delegate.writeStartDocument(arg0);
    }

    public void writeStartElement(String arg0, String arg1, String arg2) throws XMLStreamException {
        delegate.writeStartElement(arg0, arg1, arg2);
    }

    public void writeStartElement(String arg0, String arg1) throws XMLStreamException {
        delegate.writeStartElement(arg0, arg1);
    }

    public void writeStartElement(String arg0) throws XMLStreamException {
        delegate.writeStartElement(arg0);
    }
    
}
