package com.envoisolutions.sxc.util;

import javax.xml.namespace.QName;

/**
 * Attribute provides access to the current attribute of the getAttributes
 * iterator on of XoXMLStreamReader.
 * </p>
 * <b>CAUTION:</b> For performance reasons, the the same attribute
 * object instance is used for all iterations with only the internal
 * state of the attribute object changed for each iteration.  Therefore,
 * you must be careful to not retain the Attribute insance after
 * calling next() on the iterator.
 */
public interface Attribute {
    /**
     * Returns the QName of the current attribute.
     * @return the QName of the current attribute
     * @throws IllegalStateException if the XoXMLStreamReader
     *  is not in START_ELEMENT or ATTRIBUTE state
     */
    QName getName();

    /**
     * Returns the local name of the current attribute.
     * @return the local name of the current attribute
     * @throws IllegalStateException if the XoXMLStreamReader
     *  is not in START_ELEMENT or ATTRIBUTE state
     */
    String getLocalName();

    /**
     * Returns the namespace of the current attribute.
     * @return the namespace of the current attribute
     * @throws IllegalStateException if the XoXMLStreamReader
     *  is not in START_ELEMENT or ATTRIBUTE state
     */
    String getNamespace();

    /**
     * Returns the namespace prefix of the current attribute.
     * @return the namespace prefix of the current attribute
     * @throws IllegalStateException if the XoXMLStreamReader
     *  is not in START_ELEMENT or ATTRIBUTE state
     */
    String getPrefix();

    /**
     * Returns the type of the current attribute.
     * @return the type of the current attribute
     * @throws IllegalStateException if the XoXMLStreamReader
     *  is not in START_ELEMENT or ATTRIBUTE state
     */
    String getType();

    /**
     * Returns the value of the current attribute.
     * @return the value of the current attribute
     * @throws IllegalStateException if the XoXMLStreamReader
     *  is not in START_ELEMENT or ATTRIBUTE state
     */
    String getValue();

    boolean getBooleanValue();
    byte getByteValue();
    short getShortValue();
    int getIntValue();
    long getLongValue();
    float getFloatValue();
    double getDoubleValue();
    Iterable<String> getXmlListValue();

    /**
     * Returns the index of the current attribute in the XoXMLStreamREader.
     * @return the index of the current attribute
     */
    int getIndex();

    /**
     * Returns the XoXMLStreamReader;
     * @return the XoXMLStreamReader
     */
    XoXMLStreamReader getReader();

}
