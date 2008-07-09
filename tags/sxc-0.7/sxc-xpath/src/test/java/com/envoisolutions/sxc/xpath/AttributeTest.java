package com.envoisolutions.sxc.xpath;

import javax.xml.stream.XMLStreamException;

import junit.framework.TestCase;

public class AttributeTest extends TestCase {
    String value = null;
    
    public void testSimpleNamespaces() throws Exception {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-xpath");
        
        XPathEventHandler idHandler = new XPathEventHandler() {

            public void onMatch(XPathEvent event) throws XMLStreamException {
                value = (String) event.getReader().getAttributeValue("", "country");
            }
            
        };
        
        XPathBuilder builder = new XPathBuilder();
        builder.addPrefix("c", "urn:customer");
        builder.listen("/order/address[@country]", idHandler);
        
        XPathEvaluator evaluator = builder.compile();
        
        evaluator.evaluate(getClass().getResourceAsStream("test.xml"));
        
        assertEquals("US", value);
    }
    
}
