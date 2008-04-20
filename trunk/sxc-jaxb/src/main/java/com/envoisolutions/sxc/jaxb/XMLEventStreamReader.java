package com.envoisolutions.sxc.jaxb;

import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Iterator;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.Location;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.DTD;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

public class XMLEventStreamReader implements XMLStreamReader {
    private final XMLEventReader reader;
    private XMLEvent current;
    private List<Attribute> attributes;
    private List<Namespace> namespaces;
    private LinkedList<NamespaceContext> namespaceContextStack = new LinkedList<NamespaceContext>();

    public XMLEventStreamReader(XMLEventReader reader) {
        this.reader = reader;
    }

    //
    // General
    //

    public boolean hasNext() {
        return reader.hasNext();
    }

    public int next() throws XMLStreamException {
        if (current != null && current.isEndElement()) {
            namespaceContextStack.removeFirst();
        }

        current = reader.nextEvent();
        attributes = null;
        namespaces = null;

        if (current.isStartElement()) {
            namespaceContextStack.addFirst(current.asStartElement().getNamespaceContext());
        }

        return current.getEventType();
    }

    public int nextTag() throws XMLStreamException {
        if (current.isEndElement()) {
            namespaceContextStack.removeFirst();
        }

        current = reader.nextTag();
        attributes = null;
        namespaces = null;

        if (current.isStartElement()) {
            namespaceContextStack.addFirst(current.asStartElement().getNamespaceContext());
        }

        return current.getEventType();
    }

    public int getEventType() {
        if (current == null) return -1;
        return current.getEventType();
    }

    public Location getLocation() {
        return current.getLocation();
    }

    public Object getProperty(String name) throws IllegalArgumentException {
        return reader.getProperty(name);
    }

    public void close() throws XMLStreamException {
        reader.close();
    }

    public void require(int type, String namespaceURI, String localName) throws XMLStreamException {
        if (type != getEventType()) {
            throw new XMLStreamException("Expected event type " + type + ", but was " + getEventType(), getLocation());
        }
        if (namespaceURI != null && !namespaceURI.equals(getNamespaceURI())) {
            throw new XMLStreamException("Expected namespaceURI " + namespaceURI + ", but was " + getNamespaceURI(), getLocation());
        }
        if (localName != null && !localName.equals(getLocalName())) {
            throw new XMLStreamException("Expected localName " + localName + ", but was " + getLocalName(), getLocation());
        }
    }

    //
    // Element
    //

    public QName getName() {
        requiredEventType(START_ELEMENT, END_ELEMENT);
        if (current instanceof StartElement) {
            return current.asStartElement().getName();
        } else {
            return current.asEndElement().getName();
        }
    }

    public String getPrefix() {
        return getName().getPrefix();
    }

    public String getNamespaceURI() {
        return getName().getNamespaceURI();
    }

    public String getLocalName() {
        return getName().getLocalPart();
    }

    public boolean isEndElement() {
        return current.isEndElement();
    }

    public boolean isStartElement() {
        return current.isStartElement();
    }

    public boolean hasName() {
        int event = getEventType();
        return event == START_ELEMENT || event == END_ELEMENT;
    }

    //
    // Attribute
    //

    public List<Attribute> getAttributes() {
        requiredEventType(START_ELEMENT);

        if (attributes == null) {
            attributes = new ArrayList<Attribute>();
            Iterator iter = current.asStartElement().getAttributes();
            while (iter.hasNext()) {
                attributes.add((Attribute) iter.next());
            }
        }
        return attributes;
    }

    public int getAttributeCount() {
        return getAttributes().size();
    }

    public QName getAttributeName(int index) {
        return getAttributes().get(index).getName();
    }

    public String getAttributePrefix(int index) {
        return getAttributeName(index).getPrefix();
    }

    public String getAttributeNamespace(int index) {
        return getAttributeName(index).getNamespaceURI();
    }

    public String getAttributeLocalName(int index) {
        return getAttributeName(index).getLocalPart();
    }

    public String getAttributeType(int index) {
        return getAttributes().get(index).getName().getPrefix();
    }

    public String getAttributeValue(int index) {
        return getAttributes().get(index).getValue();
    }

    public boolean isAttributeSpecified(int index) {
        return getAttributes().get(index).isSpecified();
    }

    public String getAttributeValue(String namespaceURI, String localName) {
        for (Attribute attribute : getAttributes()) {
            if (attribute.getName().getNamespaceURI().equals(namespaceURI) && attribute.getName().getLocalPart().equals(localName)) {
                return attribute.getValue();
            }
        }
        return null;
    }

    //
    // Characters
    //

    public boolean hasText() {
        int event = getEventType();
        return event == CHARACTERS || event == DTD || event == ENTITY_REFERENCE || event == COMMENT || event == SPACE;
    }

    public String getText() {
        requiredEventType(CHARACTERS, DTD, ENTITY_REFERENCE, COMMENT, SPACE);

        if (current.isCharacters()) {
            return current.asCharacters().getData();
        } else if (current.isEntityReference()) {
            return ((EntityReference)current).getDeclaration().getReplacementText();
        } else if (current instanceof DTD) {
            // no idea what to do here
            return "";
        } else if (current instanceof Comment) {
            return ((Comment) current).getText();
        } else {
            // should never get here
            throw new IllegalStateException("Unexpected element type " + current);
        }
    }

    public char[] getTextCharacters() {
        requiredEventType(CHARACTERS, CDATA, SPACE);
        return getText().toCharArray();
    }

    public int getTextStart() {
        requiredEventType(CHARACTERS, CDATA, SPACE);
        return 0;
    }

    public int getTextLength() {
        requiredEventType(CHARACTERS, CDATA, SPACE);
        return getText().length();
    }

    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int targetLength) throws XMLStreamException {
        requiredEventType(CHARACTERS, CDATA, SPACE);
        String source = getText();
        int length = Math.min(targetLength, source.length() - sourceStart);
        source.getChars(sourceStart, sourceStart + length, target, targetStart);
        return length;
    }

    public boolean isCharacters() {
        return current.isCharacters();
    }

    public boolean isWhiteSpace() {
        return current.isCharacters() && current.asCharacters().isIgnorableWhiteSpace();
    }

    public String getElementText() throws XMLStreamException {
        if (getEventType() != START_ELEMENT) {
            throw new XMLStreamException();
        }

        StringBuilder text = new StringBuilder();
        for (int eventType = next(); eventType != END_ELEMENT; eventType = next()) {
            if (eventType == START_ELEMENT) {
                throw new XMLStreamException("element text content may not contain START_ELEMENT", getLocation());
            }
            if (eventType == END_DOCUMENT) {
                throw new XMLStreamException("unexpected end of document when reading element text content", getLocation());
            }

            if (eventType == CDATA || eventType == CHARACTERS || eventType == ENTITY_REFERENCE || eventType == SPACE) {
                text.append(getText());
            } else if (eventType == COMMENT || eventType == PROCESSING_INSTRUCTION) {
                // ignore
            } else {
                throw new XMLStreamException("Unexpected event type " + eventType, getLocation());
            }
        }
        return text.toString();
    }

    //
    // StartDocument
    //

    public String getCharacterEncodingScheme() {
        requiredEventType(START_DOCUMENT);

        return ((StartDocument) current).getCharacterEncodingScheme();
    }

    public String getEncoding() {
        return null;
    }

    public String getVersion() {
        requiredEventType(START_DOCUMENT);

        return ((StartDocument) current).getVersion();
    }

    public boolean isStandalone() {
        requiredEventType(START_DOCUMENT);

        return ((StartDocument) current).isStandalone();
    }

    public boolean standaloneSet() {
        requiredEventType(START_DOCUMENT);

        return ((StartDocument) current).standaloneSet();
    }

    //
    // Namespace
    //

    public NamespaceContext getNamespaceContext() {
        return namespaceContextStack.getFirst();
    }

    public List<Namespace> getNamespaces() {
        requiredEventType(START_ELEMENT, END_ELEMENT);

        if (namespaces == null) {
            namespaces = new ArrayList<Namespace>();
            Iterator iter;
            if (current.isStartElement()) {
                iter = current.asStartElement().getNamespaces();
            } else {
                iter = current.asEndElement().getNamespaces();
            }
            while (iter.hasNext()) {
                attributes.add((Attribute) iter.next());
            }
        }
        return namespaces;
    }

    public int getNamespaceCount() {
        return getNamespaces().size();
    }

    public String getNamespacePrefix(int index) {
        return getNamespaces().get(index).getPrefix();
    }

    public String getNamespaceURI(int index) {
        return getNamespaces().get(index).getNamespaceURI();
    }

    public String getNamespaceURI(String prefix) {
        return getNamespaceContext().getNamespaceURI(prefix);
    }

    //
    // ProcessingInstruction
    //

    public String getPIData() {
        requiredEventType(PROCESSING_INSTRUCTION);

        ProcessingInstruction pi = (ProcessingInstruction) current;
        return pi.getData();
    }

    public String getPITarget() {
        requiredEventType(PROCESSING_INSTRUCTION);

        ProcessingInstruction pi = (ProcessingInstruction) current;
        return pi.getTarget();
    }

    private static String[] EVENT_NAMES = new String[] {
            "START_ELEMENT",
            "END_ELEMENT",
            "PROCESSING_INSTRUCTION",
            "CHARACTERS",
            "COMMENT",
            "SPACE",
            "START_DOCUMENT",
            "END_DOCUMENT",
            "ENTITY_REFERENCE",
            "ATTRIBUTE",
            "DTD",
            "CDATA",
            "NAMESPACE",
            "NOTATION_DECLARATION",
            "ENTITY_DECLARATION"
    };

    private void requiredEventType(int... eventTypes)  {
        int event = getEventType();
        for (int eventType : eventTypes) {
            if (eventType == event) {
                return;
            }
        }
        throw new IllegalStateException("Expected event type " + getEventNames("or", eventTypes) + ", but was " + EVENT_NAMES[event]);
    }

    private String getEventNames(String junction, int... eventTypes) {
        if (eventTypes.length == 0) {
            return "";
        }
        if (eventTypes.length == 1) {
            return EVENT_NAMES[eventTypes[0]];
        }

        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < eventTypes.length; i++) {
            int event = eventTypes[i];
            if (i == 0) {
                buffer.append(EVENT_NAMES[event]);
            } else if (i == eventTypes.length -1) {
                buffer.append(" ").append(junction).append(" ").append(EVENT_NAMES[event]);
            } else {
                buffer.append(", ").append(EVENT_NAMES[event]);
            }
        }
        return buffer.toString();
    }
}
