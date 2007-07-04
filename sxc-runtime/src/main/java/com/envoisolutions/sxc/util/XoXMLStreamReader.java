package com.envoisolutions.sxc.util;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public interface XoXMLStreamReader extends XMLStreamReader {
    QName getXsiType();
    boolean isXsiNil();
    
    int getDepth();
    
    int getElementAsInt() throws XMLStreamException;
    double getElementAsDouble() throws XMLStreamException;
    short getElementAsShort() throws XMLStreamException;
    float getElementAsFloat() throws XMLStreamException;
    long getElementAsLong() throws XMLStreamException;
    boolean getElementAsBoolean() throws XMLStreamException;
    byte getElementAsByte() throws XMLStreamException;
    String getElementAsString() throws XMLStreamException;
    
    QName getElementAsQName() throws XMLStreamException;
    QName getAsQName(String val) throws XMLStreamException;
    
    int nextTagIgnoreAll() throws XMLStreamException;
}
