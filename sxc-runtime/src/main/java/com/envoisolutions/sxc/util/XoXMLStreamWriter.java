package com.envoisolutions.sxc.util;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public interface XoXMLStreamWriter extends XMLStreamWriter {
    void writeString(String s) throws XMLStreamException;
    void writeInt(int i) throws XMLStreamException;
    void writeBoolean(boolean b) throws XMLStreamException;
    void writeLong(long b) throws XMLStreamException;
    void writeFloat(float b) throws XMLStreamException;
    void writeShort(short b) throws XMLStreamException;
    void writeDouble(double b) throws XMLStreamException;
    void writeByte(byte b) throws XMLStreamException;
    void writeQName(QName q) throws XMLStreamException;
    String getQNameAsString(QName q) throws XMLStreamException;
    
    void writeXsiNil() throws XMLStreamException; 
    void writeXsiType(String namespace, String local) throws XMLStreamException;
    
    void writeAndDeclareIfUndeclared(String prefix, String namespace) throws XMLStreamException ;

    String getUniquePrefix(String namespaceURI) throws XMLStreamException;
}
