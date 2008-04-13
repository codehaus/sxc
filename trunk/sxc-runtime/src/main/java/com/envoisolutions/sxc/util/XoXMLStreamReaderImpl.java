package com.envoisolutions.sxc.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class XoXMLStreamReaderImpl implements XoXMLStreamReader {
    XMLStreamReader reader;
    String text;

    private int depth = 0;
    private final static String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";
    
    public XoXMLStreamReaderImpl(XMLStreamReader reader) {
        this.reader = reader;
        
        if (reader.getEventType() == START_ELEMENT)
            depth++;
    }

    public QName getXsiType() {
        String val = getAttributeValue(XSI_NS, "type");
        if (val != null) {
            return getAsQName(val);
        }
        return null;
    }

    public QName getAsQName(String val) {
        int i = val.indexOf(":");
        if (i == -1) {
            String ns = getNamespaceURI("");
            if (ns == null) ns = "";
            
            return new QName(ns, val.intern());
        } else {
            String prefix = val.substring(0, i);
            String ns = getNamespaceURI(prefix);
            if (ns == null) ns = "";
            
            return new QName(ns, val.substring(i+1).intern(), prefix);
        }
    }
    
    public boolean isXsiNil() {
        String val = getAttributeValue(XSI_NS, "nil");
        return (val != null && (val.equals("1") || val.equals("true")));
    }

    public int getDepth() {
        return depth;
    }

    public QName getElementAsQName() throws XMLStreamException {
        String val = getElementAsString(); 
        if (val != null) {
            return getAsQName(val);
        }
        return null;
    }

    public int getElementAsInt() throws XMLStreamException, NumberFormatException {
        return Integer.parseInt(getElementText().trim());
    }
    
    public double getElementAsDouble() throws XMLStreamException {
        return Double.parseDouble(getElementAsString());
    }

    public float getElementAsFloat() throws XMLStreamException {
        return Float.parseFloat(getElementAsString());
    }

    public long getElementAsLong() throws XMLStreamException {
        return Long.parseLong(getElementAsString());
    }

    public short getElementAsShort() throws XMLStreamException {
        return Short.parseShort(getElementAsString());
    }

    public byte getElementAsByte() throws XMLStreamException {
        return Byte.parseByte(getElementAsString());
    }
    
    public String getElementAsString() throws XMLStreamException {
        return getElementText().trim();
    }

    public boolean getElementAsBoolean() throws XMLStreamException {
        String s = getElementAsString();
        return s.equals("true") || s.equals("1");
    }

    public void close() throws XMLStreamException {
        reader.close();
    }

    public int getAttributeCount() {
        return reader.getAttributeCount();
    }

    public String getAttributeLocalName(int arg0) {
        return reader.getAttributeLocalName(arg0);
    }

    public QName getAttributeName(int arg0) {
        return reader.getAttributeName(arg0);
    }

    public String getAttributeNamespace(int arg0) {
        return reader.getAttributeNamespace(arg0);
    }

    public String getAttributePrefix(int arg0) {
        return reader.getAttributePrefix(arg0);
    }

    public String getAttributeType(int arg0) {
        return reader.getAttributeType(arg0);
    }

    public String getAttributeValue(int arg0) {
        return reader.getAttributeValue(arg0);
    }

    public String getAttributeValue(String arg0, String arg1) {
        return reader.getAttributeValue(arg0, arg1);
    }

    public String getCharacterEncodingScheme() {
        return reader.getCharacterEncodingScheme();
    }

    public String getElementText() throws XMLStreamException {
        if (text == null) {
            depth--;
            text = reader.getElementText();
        }
        return text;
    }

    public String getEncoding() {
        return reader.getEncoding();
    }

    public int getEventType() {
        return reader.getEventType();
    }

    public String getLocalName() {
        return reader.getLocalName();
    }

    public Location getLocation() {
        return reader.getLocation();
    }

    public QName getName() {
        return reader.getName();
    }

    public NamespaceContext getNamespaceContext() {
        return reader.getNamespaceContext();
    }

    public int getNamespaceCount() {
        return reader.getNamespaceCount();
    }

    public String getNamespacePrefix(int arg0) {
        return reader.getNamespacePrefix(arg0);
    }

    public String getNamespaceURI() {
        return reader.getNamespaceURI();
    }

    public String getNamespaceURI(int arg0) {

        return reader.getNamespaceURI(arg0);
    }

    public String getNamespaceURI(String arg0) {
        return reader.getNamespaceURI(arg0);
    }

    public String getPIData() {
        return reader.getPIData();
    }

    public String getPITarget() {
        return reader.getPITarget();
    }

    public String getPrefix() {
        return reader.getPrefix();
    }

    public Object getProperty(String arg0) throws IllegalArgumentException {

        return reader.getProperty(arg0);
    }

    public String getText() {

        return reader.getText();
    }

    public char[] getTextCharacters() {
        return reader.getTextCharacters();
    }

    public int getTextCharacters(int arg0, char[] arg1, int arg2, int arg3) throws XMLStreamException {
        return reader.getTextCharacters(arg0, arg1, arg2, arg3);
    }

    public int getTextLength() {
        return reader.getTextLength();
    }

    public int getTextStart() {

        return reader.getTextStart();
    }

    public String getVersion() {
        return reader.getVersion();
    }

    public boolean hasName() {

        return reader.hasName();
    }

    public boolean hasNext() throws XMLStreamException {
        return reader.hasNext();
    }

    public boolean hasText() {
        return reader.hasText();
    }

    public boolean isAttributeSpecified(int arg0) {
        return reader.isAttributeSpecified(arg0);
    }

    public boolean isCharacters() {

        return reader.isCharacters();
    }

    public boolean isEndElement() {

        return reader.isEndElement();
    }

    public boolean isStandalone() {

        return reader.isStandalone();
    }

    public boolean isStartElement() {

        return reader.isStartElement();
    }

    public boolean isWhiteSpace() {

        return reader.isWhiteSpace();
    }

    public int next() throws XMLStreamException {
        text = null;
        int next = reader.next();

        if (next == START_ELEMENT) {
            depth++;
        } else if (next == END_ELEMENT) {
            depth--;
        }

        return next;
    }

    public int nextTag() throws XMLStreamException {
        int eventType = reader.nextTag();
        if (eventType == START_ELEMENT) {
            depth++;
        } else if (eventType == END_ELEMENT) {
            depth--;
        }
        return eventType;
    }

    public int nextTagIgnoreAll() throws XMLStreamException {
        int event = next();
        while (event != START_DOCUMENT && event != START_ELEMENT && event != END_ELEMENT && event != END_DOCUMENT) {
            event = next();
        }
        
        return event;
    }

    public void require(int arg0, String arg1, String arg2) throws XMLStreamException {
        reader.require(arg0, arg1, arg2);
    }

    public boolean standaloneSet() {
        return reader.standaloneSet();
    }

    public Iterable<XoXMLStreamReader> getChildElements() {
        return new Iterable<XoXMLStreamReader>() {
            public Iterator<XoXMLStreamReader> iterator() {
                return new ChildElementsIterator();
            }
        };
    }

    private class ChildElementsIterator implements Iterator<XoXMLStreamReader> {
        private final int targetDepth;
        private boolean moveToNext;
        private boolean hasMoreEvents;

        public ChildElementsIterator() {
            targetDepth = depth + 1;
//            try {
//                event = nextTagIgnoreAll();
//            } catch (XMLStreamException e) {
//                throw new RuntimeXMLStreamException(e);
//            }
            moveToNext = true;
        }

        public boolean hasNext() {
            if (moveToNext) {
                advanceToNext();
            }
            return hasMoreEvents;
        }

        public XoXMLStreamReader next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            moveToNext = true;
            return XoXMLStreamReaderImpl.this;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        private void advanceToNext() {
            moveToNext = false;

            do {
                // advance one event
                int event;
                try {
                    // are we are at the end of the stream?
                    if (!XoXMLStreamReaderImpl.this.hasNext()) {
                        hasMoreEvents = false;
                        return;
                    }
                    event = XoXMLStreamReaderImpl.this.next();
                } catch (XMLStreamException e) {
                    throw new RuntimeXMLStreamException(e);
                }

                // if we are at a start element event and the proper depth,
                // then we are at the next child element
                if (event == START_ELEMENT && depth == targetDepth) {
                    hasMoreEvents = true;
                    return;
                }
            } while(depth >= targetDepth - 1);
//
//            // Advance stream until we hit a START_ELEMENT event at our target depth, or
//            // we either run out of elements or step out our our element (depth is less
//            // then our target depth)
//            for(int depth = getDepth(); depth >= targetDepth - 1; depth = getDepth()) {
//                // if are at a start element event and the proper depth,
//                // then we are at the next element
//                if (event == START_ELEMENT && depth == targetDepth) {
//                    hasMoreEvents = true;
//                    return;
//                }
//
//                // advance one event
//                try {
//                    if (!XoXMLStreamReaderImpl.this.hasNext()) {
//                        hasMoreEvents = false;
//                        return;
//                    }
//                    event = XoXMLStreamReaderImpl.this.next();
//                } catch (XMLStreamException e) {
//                    throw new RuntimeXMLStreamException(e);
//                }
//            }

            // we stepped out of our element so there will be no more child elements
            hasMoreEvents = false;
        }
    }

    public Iterable<Attribute> getAttributes() {
        return new Iterable<Attribute>() {
            public Iterator<Attribute> iterator() {
                return new AttributesIterator();
            }
        };
    }

    private final class AttributesIterator implements Iterator<Attribute> {
        private final Attribute attribute = new AttributeImpl(this);
        private int index = -1;

        public boolean hasNext() {
            return index + 1 < getAttributeCount();
        }

        public Attribute next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            index++;
            return attribute;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    public final class AttributeImpl implements Attribute {
        private final AttributesIterator attributesIterator;

        public AttributeImpl(AttributesIterator attributesIterator) {
            this.attributesIterator = attributesIterator;
        }

        public QName getName() {
            return XoXMLStreamReaderImpl.this.getAttributeName(attributesIterator.index);
        }

        public String getLocalName() {
            return XoXMLStreamReaderImpl.this.getAttributeLocalName(attributesIterator.index);
        }

        public String getNamespace() {
            return XoXMLStreamReaderImpl.this.getAttributeNamespace(attributesIterator.index);
        }

        public String getPrefix() {
            return XoXMLStreamReaderImpl.this.getAttributePrefix(attributesIterator.index);
        }

        public String getType() {
            return XoXMLStreamReaderImpl.this.getAttributeType(attributesIterator.index);
        }

        public String getValue() {
            return XoXMLStreamReaderImpl.this.getAttributeValue(attributesIterator.index);
        }

        public int getIndex() {
            return attributesIterator.index;
        }

        public XoXMLStreamReader getReader() {
            return XoXMLStreamReaderImpl.this;
        }
    }

    public int hashCode() {
        return reader.hashCode();
    }

    @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
    public boolean equals(Object arg0) {
        return reader.equals(arg0);
    }

    public String toString() {
        return reader.toString();
    }
}
