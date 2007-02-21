package com.envoisolutions.sxc.builder;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import com.envoisolutions.sxc.Context;
import com.envoisolutions.sxc.util.XoXMLStreamReader;

public interface GeneratedReader {
    public Object read(XoXMLStreamReader reader, 
                       Context context) throws XMLStreamException;
    
    public Object read(XoXMLStreamReader reader, 
                       Context context,
                       QName type) throws XMLStreamException;
}
