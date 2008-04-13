package com.envoisolutions.sxc.util;

import javax.xml.stream.XMLStreamException;

public class RuntimeXMLStreamException extends RuntimeException {
    public RuntimeXMLStreamException(XMLStreamException cause) {
        super(cause);
        if (cause == null) throw new NullPointerException("cause is null");
    }

    public XMLStreamException getCause() {
        return (XMLStreamException) super.getCause();
    }
}
