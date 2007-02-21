package com.envoisolutions.sxc.xpath;

import javax.xml.stream.XMLStreamException;

import junit.framework.TestCase;

public class SimpleXPathTest extends TestCase {
    String custTag = null;
    String idTag = null;
    boolean match;
    boolean noMatch;
    String expr = null;
    
    public void testNonMatchedText() throws Exception {

        XPathEventHandler matchIdHandler = new XPathEventHandler() {

            public void onMatch(XPathEvent event) throws XMLStreamException {
                match = true;
                
            }
            
        };
        
        XPathBuilder builder = new XPathBuilder();
        builder.addPrefix("c", "urn:customer");
        builder.listen("//c:id[text()='2']", matchIdHandler);
        
        XPathEvaluator evaluator = builder.compile();
        
        evaluator.evaluate(getClass().getResourceAsStream("customer.xml"));
        
        assertFalse(match);
    }
    
    public void testText() throws Exception {

        XPathEventHandler idHandler = new XPathEventHandler() {

            public void onMatch(XPathEvent event) throws XMLStreamException {
                match = true;
                expr = event.getExpression();
            }
            
        };
        
        
        XPathBuilder builder = new XPathBuilder();
        builder.addPrefix("c", "urn:customer");
        builder.listen("//c:id[text()='1']", idHandler);
        
        XPathEvaluator evaluator = builder.compile();
        
        evaluator.evaluate(getClass().getResourceAsStream("customer.xml"));
        
        assertTrue(match);
        assertEquals("//c:id[text()='1']", expr);
    }
    
    public void testGlobalElement() throws Exception {

        System.setProperty("streax-xo.output.directory", "target/tmp-xpath");
        XPathEventHandler idHandler = new XPathEventHandler() {

            public void onMatch(XPathEvent event) throws XMLStreamException {
                idTag = event.getReader().getLocalName();
            }
            
        };
        
        XPathBuilder builder = new XPathBuilder();
        builder.addPrefix("c", "urn:customer");
        builder.listen("//c:id", idHandler);
        
        XPathEvaluator evaluator = builder.compile();
        
        evaluator.evaluate(getClass().getResourceAsStream("customer.xml"));
        
        assertEquals("id", idTag);
    }
    
    
    public void testSimpleNamespaces() throws Exception {

        XPathEventHandler idHandler = new XPathEventHandler() {

            public void onMatch(XPathEvent event) throws XMLStreamException {
                idTag = event.getReader().getLocalName();
            }
            
        };
        
        XPathBuilder builder = new XPathBuilder();
        builder.addPrefix("c", "urn:customer");
        builder.listen("/c:customer/c:id", idHandler);
        
        XPathEvaluator evaluator = builder.compile();
        
        evaluator.evaluate(getClass().getResourceAsStream("customer.xml"));
        
        assertEquals("id", idTag);
    }
    
    public void testSimpleExpression() throws Exception {
        
        XPathEventHandler customerHandler = new XPathEventHandler() {

            public void onMatch(XPathEvent event) throws XMLStreamException {
                custTag = event.getReader().getLocalName();
            }
            
        };
        
        XPathEventHandler idHandler = new XPathEventHandler() {

            public void onMatch(XPathEvent event) throws XMLStreamException {
                idTag = event.getReader().getLocalName();
            }
            
        };
        
        XPathBuilder builder = new XPathBuilder();
        builder.listen("/customer", customerHandler);
        builder.listen("/customer/id", idHandler);
        
        XPathEvaluator evaluator = builder.compile();
        
        evaluator.evaluate(getClass().getResourceAsStream("customer2.xml"));
        
        assertEquals("customer", custTag);
        assertEquals("id", idTag);
    }
}