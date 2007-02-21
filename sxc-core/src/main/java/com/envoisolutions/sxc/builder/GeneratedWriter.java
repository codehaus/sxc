package com.envoisolutions.sxc.builder;

import javax.xml.stream.XMLStreamException;

import com.envoisolutions.sxc.Context;
import com.envoisolutions.sxc.util.XoXMLStreamWriter;

public interface GeneratedWriter {
    public void write(XoXMLStreamWriter reader, Context context, Object o) throws XMLStreamException;
}
