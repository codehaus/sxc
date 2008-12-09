package com.envoisolutions.sxc.xpath;

import java.io.ByteArrayInputStream;

import javax.xml.stream.XMLStreamException;

import junit.framework.TestCase;

public class AttributeTest extends TestCase {
    String value = null;
    boolean found = false;
    
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

    public void testAttributeExpression1() throws Exception {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-xpath");
        
        XPathEventHandler idHandler = new XPathEventHandler() {

            public void onMatch(XPathEvent event) throws XMLStreamException {
                found = true;
            }
            
        };
        
        XPathBuilder builder = new XPathBuilder();
        builder.addPrefix("c", "urn:customer");
        builder.listen("/paymentService/submit/order[@orderCode='12345']", idHandler);
        
        XPathEvaluator evaluator = builder.compile();
        
        String nomatch = 
            "<paymentService version=\"1.4\" merchantCode=\"MYMERCHANT\">"
            + "<submit>"
            + "<order orderCode=\"xxxx\"/>"
            + "</submit>"
            + "</paymentService>";
        evaluator.evaluate(new ByteArrayInputStream(nomatch.getBytes()));
        
        assertFalse(found);

        String match = 
            "<paymentService version=\"1.4\" merchantCode=\"MYMERCHANT\">"
            + "<submit>"
            + "<order orderCode=\"12345\"/>"
            + "</submit>"
            + "</paymentService>";
        evaluator.evaluate(new ByteArrayInputStream(match.getBytes()));
        
        assertTrue(found);
    }
    
}
