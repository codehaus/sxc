package com.envoisolutions.sxc.util;

import java.util.Arrays;
import java.util.LinkedList;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class PrettyPrintXMLStreamWriter implements XMLStreamWriter {
    public static final int DEFAULT_INDENT_SIZE = 4;
    public static final String DEFAULT_NEW_LINE = "\n";

    private final XMLStreamWriter writer;
    private final String indentString;
    private final String newLine;

    private final LinkedList<Element> elements = new LinkedList<Element>();
    private int indentLevel;

    public PrettyPrintXMLStreamWriter(XMLStreamWriter writer) {
        this(writer, DEFAULT_INDENT_SIZE, DEFAULT_NEW_LINE);
    }

    public PrettyPrintXMLStreamWriter(XMLStreamWriter writer, int indentSize) {
        this(writer, indentSize, DEFAULT_NEW_LINE);
    }

    public PrettyPrintXMLStreamWriter(XMLStreamWriter writer, int indentSize, String newLine) {
        this.writer = writer;

        char[] chars = new char[indentSize];
        Arrays.fill(chars, ' ');
        this.indentString = new String(chars);

        this.newLine = newLine;
    }

    public int getIndentSize() {
        return indentString.length();
    }

    private String getNewLine() {
        return newLine;
    }

    public void writeStartElement(String localName) throws XMLStreamException {
        beforeStartElement(localName, null);
        writeStartElement(null, null, localName);
        afterStartElement(localName, null);
    }

    public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
        beforeStartElement(localName, namespaceURI);
        writeStartElement(null, namespaceURI, localName);
        afterStartElement(localName, namespaceURI);
    }

    public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        beforeStartElement(localName, namespaceURI);
        writer.writeStartElement(prefix, localName, namespaceURI);
        afterStartElement(localName, namespaceURI);
    }

    public void writeEmptyElement(String localName) throws XMLStreamException {
        beforeStartElement(localName, null);
        writer.writeEmptyElement(localName);
        afterEndElement();
    }

    public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
        beforeStartElement(localName, null);
        writeEmptyElement(null, namespaceURI, localName);
        afterEndElement();
    }

    public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        beforeStartElement(localName, null);
        writer.writeEmptyElement(prefix, localName, namespaceURI);
        afterEndElement();
    }

    public void writeEndElement() throws XMLStreamException {
        beforeEndElement();
        writer.writeEndElement();
        afterEndElement();
    }

    public void writeEndDocument() throws XMLStreamException {
        beforeEndDocument();
        writer.writeEndDocument();
        afterEndDocument();
    }

    public void close() throws XMLStreamException {
        writer.close();
    }

    public void flush() throws XMLStreamException {
        writer.flush();
    }

    public void writeAttribute(String localName, String value) throws XMLStreamException {
        writeAttribute(null, localName, value);
    }

    public void writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {
        writeAttribute(null, namespaceURI, localName, value);
    }

    public void writeAttribute(String prefix, String namespaceURI, String localName, String value) throws XMLStreamException {
        writer.writeAttribute(prefix, namespaceURI, localName, value);
    }

    public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
        writer.writeNamespace(prefix, namespaceURI);
    }

    public void writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
        writer.writeDefaultNamespace(namespaceURI);
    }

    public void writeComment(String data) throws XMLStreamException {
        writer.writeComment(data);
    }

    public void writeProcessingInstruction(String target) throws XMLStreamException {
        beforeProcessingInstruction();
        writer.writeProcessingInstruction(target);
        afterProcessingInstruction();
    }

    public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
        beforeProcessingInstruction();
        writer.writeProcessingInstruction(target, data);
        afterProcessingInstruction();
    }

    public void writeCData(String data) throws XMLStreamException {
        writer.writeCData(data);
    }

    public void writeDTD(String dtd) throws XMLStreamException {
        beforeProcessingInstruction();
        writer.writeDTD(dtd);
        afterProcessingInstruction();
    }

    public void writeEntityRef(String name) throws XMLStreamException {
        writer.writeEntityRef(name);
    }

    public void writeStartDocument() throws XMLStreamException {
        beforeStartDocument();
        writer.writeStartDocument();
        afterStartDocument();
    }

    public void writeStartDocument(String version) throws XMLStreamException {
        beforeStartDocument();
        writer.writeStartDocument(version);
        afterStartDocument();
    }

    public void writeStartDocument(String encoding, String version) throws XMLStreamException {
        beforeStartDocument();
        writer.writeStartDocument(encoding, version);
        afterStartDocument();
    }

    public void writeCharacters(String text) throws XMLStreamException {
        writer.writeCharacters(text);
    }

    public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
        writer.writeCharacters(text, start, len);
    }

    public String getPrefix(String uri) throws XMLStreamException {
        return writer.getPrefix(uri);
    }

    public void setPrefix(String prefix, String uri) throws XMLStreamException {
        writer.setPrefix(prefix, uri);
    }

    public void setDefaultNamespace(String uri) throws XMLStreamException {
        writer.setDefaultNamespace(uri);
    }

    public void setNamespaceContext(NamespaceContext namespaceContext) throws XMLStreamException {
        writer.setNamespaceContext(namespaceContext);
    }

    public NamespaceContext getNamespaceContext() {
        return writer.getNamespaceContext();
    }

    public Object getProperty(String name) throws IllegalArgumentException {
        return writer.getProperty(name);
    }

    protected void beforeStartDocument() throws XMLStreamException {
    }

    protected void afterStartDocument() throws XMLStreamException {
        writeNewLine();
    }

    protected void beforeEndDocument() throws XMLStreamException {
    }

    protected void afterEndDocument() throws XMLStreamException {
    }

    private void beforeProcessingInstruction() throws XMLStreamException {
        if (elements.isEmpty()) {
            writeAndIncreaseIndent();
        } else {
            writer.writeCharacters("");
            writeNewLine();
            writeAndIncreaseIndent();
            Element element = elements.peek();
            element.setChildElements(true);
        }
    }

    private void afterProcessingInstruction() throws XMLStreamException {
        if (elements.isEmpty()) {
            writeNewLine();
        }
    }

    protected void beforeStartElement(String localName, String namespaceURI) throws XMLStreamException {
        if (elements.isEmpty()) {
            writeAndIncreaseIndent();
        } else {
            // nested element, write newline, indent and increase indent level for further nested elements
            writer.writeCharacters("");
            writeNewLine();
            writeAndIncreaseIndent();

            // parent element now has a child
            Element element = elements.peek();
            element.setChildElements(true);
        }
        elements.addFirst(new Element(localName));
    }

    protected void afterStartElement(String localName, String namespaceURI) throws XMLStreamException {
    }

    protected void beforeEndElement() throws XMLStreamException {
        Element element = elements.remove();
        unindent();
        if (element.hasChildElements()) {
            writeNewLine();
            writeIndent();
        }
    }

    protected void afterEndElement() throws XMLStreamException {
        if (elements.isEmpty()) {
            writeNewLine();
        }
    }

    private void writeNewLine() throws XMLStreamException {
        writer.writeCharacters(getNewLine());
    }

    protected void writeIndent() throws XMLStreamException {
        for (int i = 0; i < indentLevel; i++) {
            writer.writeCharacters(indentString);
        }
    }

    protected void writeAndIncreaseIndent() throws XMLStreamException {
        writeIndent();
        indent();
    }

    protected void indent() {
        indentLevel++;
    }

    protected void unindent() {
        indentLevel--;
    }

    private static class Element {
        private final String localName;
        private boolean hasChildElements;

        // remember the local name of the element for debugging purposes only
        Element(String localName) {
            this.localName = localName;
        }

        public boolean hasChildElements() {
            return hasChildElements;
        }

        public void setChildElements(boolean childElements) {
            hasChildElements = childElements;
        }

        public String toString() {
            return localName;
        }
    }
}