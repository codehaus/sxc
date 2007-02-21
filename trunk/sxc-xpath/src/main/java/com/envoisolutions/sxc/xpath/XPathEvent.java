package com.envoisolutions.sxc.xpath;

import javax.xml.stream.XMLStreamReader;

public class XPathEvent {
    private String expression;
    private XMLStreamReader reader;
    public XPathEvent(String expression, XMLStreamReader reader) {
        super();
        this.expression = expression;
        this.reader = reader;
    }
    
    public String getExpression() {
        return expression;
    }
    
    public XMLStreamReader getReader() {
        return reader;
    }
}
