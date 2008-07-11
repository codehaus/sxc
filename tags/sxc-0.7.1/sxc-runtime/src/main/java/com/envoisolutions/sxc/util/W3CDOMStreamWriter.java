/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.envoisolutions.sxc.util;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.Collections;
import java.util.Map;
import java.util.Stack;

public class W3CDOMStreamWriter implements XMLStreamWriter {
    static final String XML_NS = "http://www.w3.org/2000/xmlns/";
    private Stack<Element> stack = new Stack<Element>();
    private Document document;
    private Element currentNode;
    private NamespaceContext context;
    private Map properties = Collections.EMPTY_MAP;

    public W3CDOMStreamWriter() throws ParserConfigurationException {
        this(DocumentBuilderFactory.newInstance().newDocumentBuilder());
    }

    public W3CDOMStreamWriter(DocumentBuilder builder) {
        document = builder.newDocument();
    }

    public W3CDOMStreamWriter(Document document) {
        this.document = document;
    }

    public W3CDOMStreamWriter(Element e) {
        this.document = e.getOwnerDocument();
        
        currentNode = e;

        W3CNamespaceContext newContext = new W3CNamespaceContext();
        newContext.setElement(currentNode);
        this.context = newContext;
    }

    public void setProperties(Map properties) {
        this.properties = properties;
    }

    public Document getDocument() {
        return document;
    }

    public void writeStartElement(String local) throws XMLStreamException {
        newChild(document.createElement(local));
    }

    private void newChild(Element element) {
        if (currentNode != null) {
            stack.push(currentNode);
            currentNode.appendChild(element);
        } else {
            document.appendChild(element);
        }

        W3CNamespaceContext newContext = new W3CNamespaceContext();
        newContext.setElement(element);
        this.context = newContext;

        currentNode = element;
    }

    public void writeStartElement(String namespace, String local) throws XMLStreamException {
        newChild(document.createElementNS(namespace, local));
    }

    public void writeStartElement(String prefix, String local, String namespace) throws XMLStreamException {
        if (prefix == null || prefix.equals("")) {
            writeStartElement(namespace, local);
        } else {
            newChild(document.createElementNS(namespace, prefix + ":" + local));
        }
    }

    public void writeEmptyElement(String namespace, String local) throws XMLStreamException {
        writeStartElement(namespace, local);
    }

    public void writeEmptyElement(String prefix, String namespace, String local) throws XMLStreamException {
        writeStartElement(prefix, namespace, local);
    }

    public void writeEmptyElement(String local) throws XMLStreamException {
        writeStartElement(local);
    }

    public void writeEndElement() throws XMLStreamException {
        if (stack.size() > 0) {
            currentNode = (Element)stack.pop();
        } else {
            currentNode = null;
        }
        ((W3CNamespaceContext)context).setElement(currentNode);
    }

    public void writeEndDocument() throws XMLStreamException {
    }

    public void writeAttribute(String local, String value) throws XMLStreamException {
        Attr a = document.createAttribute(local);
        a.setValue(value);
        currentNode.setAttributeNode(a);
    }

    public void writeAttribute(String prefix, String namespace, String local, String value)
        throws XMLStreamException {
        if (prefix.length() > 0) {
            local = prefix + ":" + local;
        }
        
        Attr a = document.createAttributeNS(namespace, local);
        a.setValue(value);
        currentNode.setAttributeNodeNS(a);
    }

    public void writeAttribute(String namespace, String local, String value) throws XMLStreamException {
        Attr a = document.createAttributeNS(namespace, local);
        a.setValue(value);
        currentNode.setAttributeNodeNS(a);
    }

    public void writeNamespace(String prefix, String namespace) throws XMLStreamException {
        if (prefix.length() == 0) {
            writeDefaultNamespace(namespace);
        } else {
            currentNode.setAttributeNS(XML_NS, "xmlns:" + prefix, namespace);
        }
    }

    public void writeDefaultNamespace(String namespace) throws XMLStreamException {
        currentNode.setAttributeNS(XML_NS, "xmlns", namespace);
    }

    public void writeComment(String value) throws XMLStreamException {
        currentNode.appendChild(document.createComment(value));
    }

    public void writeProcessingInstruction(String target) throws XMLStreamException {
        currentNode.appendChild(document.createProcessingInstruction(target, null));
    }

    public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
        currentNode.appendChild(document.createProcessingInstruction(target, data));
    }

    public void writeCData(String data) throws XMLStreamException {
        currentNode.appendChild(document.createCDATASection(data));
    }

    public void writeDTD(String arg0) throws XMLStreamException {
        throw new UnsupportedOperationException();
    }

    public void writeEntityRef(String ref) throws XMLStreamException {
        currentNode.appendChild(document.createEntityReference(ref));
    }

    public void writeStartDocument() throws XMLStreamException {
    }

    public void writeStartDocument(String version) throws XMLStreamException {
        writeStartDocument();
    }

    public void writeStartDocument(String encoding, String version) throws XMLStreamException {
        writeStartDocument();
    }

    public void writeCharacters(String text) throws XMLStreamException {
        currentNode.appendChild(document.createTextNode(text));
    }

    public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
        writeCharacters(new String(text, start, len));
    }

    public String getPrefix(String uri) throws XMLStreamException {
        return context.getPrefix(uri);
    }

    public void setPrefix(String arg0, String arg1) throws XMLStreamException {
    }

    public void setDefaultNamespace(String arg0) throws XMLStreamException {
    }

    public void setNamespaceContext(NamespaceContext ctx) throws XMLStreamException {
        this.context = ctx;
    }

    public NamespaceContext getNamespaceContext() {
        return context;
    }

    public Object getProperty(String prop) throws IllegalArgumentException {
        return properties.get(prop);
    }

    public void close() throws XMLStreamException {
    }

    public void flush() throws XMLStreamException {
    }
    
}
