package com.envoisolutions.sxc.util;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Element;

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

    Element getElementAsDomElement() throws XMLStreamException;

    QName getElementAsQName() throws XMLStreamException;
    QName getAsQName(String val) throws XMLStreamException;

    Iterable<String> getElementAsXmlList() throws XMLStreamException;

    int nextTagIgnoreAll() throws XMLStreamException;

    /**
     * Returns an iterator over the attributes of the current element.
     * </p>
     * <b>CAUTION:</b> For performance reasons, this iterator returns
     * the same attribute object for each iteration and only the
     * internal state of the attribute object is changed.  Therefore,
     * you must be careful to not retain the Attribute insance after
     * calling next() on the iterator.
     * @return an interator over the attributes on the current element
     */
    Iterable<Attribute> getAttributes();

    /**
     * Returns an iterator over the child elements of the current element.
     * </p>
     * <b>CAUTION:</b> For performance reasons, this iterator returns
     * this XoXMLStreamReader for each iteration and only the internal
     * state of the XoXMLStreamReader object is changed.  Therefore,
     * you must be careful to not retain the XoXMLStreamReader insance after
     * calling next() on the iterator.
     * <b>CAUTION:</b> The hasNext() method of this iterator advances
     * the stream reader to the next element, so once hasNext has been
     * called all operations on this XoXMLStreamReader will be performed
     * on the nexted child element.
     *
     * @return an interator over the attributes on the current element
     * @throws RuntimeXMLStreamException if a XMLStreamException occured
     * while advancing to the first child
     */
    Iterable<XoXMLStreamReader> getChildElements();

}
