package com.envoisolutions.sxc.xpath;

import javax.xml.stream.XMLStreamException;

import junit.framework.TestCase;

public class SimpleXPathTest extends TestCase {
    String custTag = null;
    String tag = null;
    boolean match;
    boolean noMatch;
    String expr = null;
    String value;
    
    public void testNonMatchedText() throws Exception {

        System.setProperty("streax-xo.output.directory", "target/tmp-xpath");
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

        System.setProperty("streax-xo.output.directory", "target/tmp-xpath");
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
        

        evaluator.evaluate(getClass().getResourceAsStream("customer-whitespace.xml"));
        
        assertTrue(match);
        assertEquals("//c:id[text()='1']", expr);
    }
    
    public void testGlobalElement() throws Exception {

        System.setProperty("streax-xo.output.directory", "target/tmp-xpath");
        XPathEventHandler handler = new XPathEventHandler() {

            public void onMatch(XPathEvent event) throws XMLStreamException {
                tag = event.getReader().getLocalName();
            }
            
        };
        
        XPathBuilder builder = new XPathBuilder();
        builder.addPrefix("c", "urn:customer");
        builder.listen("//c:id", handler);
        
        XPathEvaluator evaluator = builder.compile();
        
        evaluator.evaluate(getClass().getResourceAsStream("customer.xml"));
        
        assertEquals("id", tag);
        
        builder = new XPathBuilder();
        builder.addPrefix("c", "urn:customer");
        builder.listen("//c:customer", handler);
        
        evaluator = builder.compile();
        
        evaluator.evaluate(getClass().getResourceAsStream("customer.xml"));
        
        assertEquals("customer", tag);
    }
    
    
    public void testGlobalElementLocalName() throws Exception {

        System.setProperty("streax-xo.output.directory", "target/tmp-xpath");
        XPathEventHandler idHandler = new XPathEventHandler() {

            public void onMatch(XPathEvent event) throws XMLStreamException {
                tag = event.getReader().getLocalName();
            }
            
        };
        
        XPathBuilder builder = new XPathBuilder();
        builder.addPrefix("c", "urn:customer");
        builder.listen("//*[local-name()='id']", idHandler);
        
        XPathEvaluator evaluator = builder.compile();
        
        evaluator.evaluate(getClass().getResourceAsStream("customer.xml"));
        
        assertEquals("id", tag);
    }
    
    public void testAnd() throws Exception {

        System.setProperty("streax-xo.output.directory", "target/tmp-xpath");
        XPathEventHandler idHandler = new XPathEventHandler() {

            public void onMatch(XPathEvent event) throws XMLStreamException {
                tag = event.getReader().getLocalName();
            }
            
        };
        
        XPathBuilder builder = new XPathBuilder();
        builder.addPrefix("c", "urn:customer");
        builder.listen("//*[local-name()='id' and namespace-uri()='urn:customer']", idHandler);
        
        XPathEvaluator evaluator = builder.compile();
        
        evaluator.evaluate(getClass().getResourceAsStream("customer.xml"));
        
        assertEquals("id", tag);
    }
    
    public void testOr() throws Exception {

        System.setProperty("streax-xo.output.directory", "target/tmp-xpath");
        XPathEventHandler idHandler = new XPathEventHandler() {

            public void onMatch(XPathEvent event) throws XMLStreamException {
                tag = event.getReader().getLocalName();
            }
            
        };
        
        XPathBuilder builder = new XPathBuilder();
        builder.addPrefix("c", "urn:customer");
        builder.listen("//*[local-name()='foo' or local-name()='id']", idHandler);
        
        XPathEvaluator evaluator = builder.compile();
        
        evaluator.evaluate(getClass().getResourceAsStream("customer.xml"));
        
        assertEquals("id", tag);
    }
    
    public void testElementNumber() throws Exception {

        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-xpath");
        XPathEventHandler idHandler = new XPathEventHandler() {

            public void onMatch(XPathEvent event) throws XMLStreamException {
                value = event.getReader().getAttributeValue("", "name");
            }
        };
        
        XPathBuilder builder = new XPathBuilder();
        builder.listen("//order[1]", idHandler);
        
        XPathEvaluator evaluator = builder.compile();
        
        evaluator.evaluate(getClass().getResourceAsStream("orders.xml"));
        
        assertEquals("1", value);
        
        // Try the second one
        builder = new XPathBuilder();
        builder.listen("//order[2]", idHandler);
        
        evaluator = builder.compile();
        
        evaluator.evaluate(getClass().getResourceAsStream("orders.xml"));
        
        assertEquals("2", value);
        
        evaluator.evaluate(getClass().getResourceAsStream("orders.xml"));
        
        assertEquals("2", value);
        
        // try a non global element
        builder = new XPathBuilder();
        builder.listen("/orders/order[2]", idHandler);
        
        evaluator = builder.compile();
        
        evaluator.evaluate(getClass().getResourceAsStream("orders.xml"));
        
        assertEquals("2", value);
    }
    
    public void testSimpleNamespaces() throws Exception {
        System.setProperty("streax-xo.output.directory", "target/tmp-xpath");
        
        XPathEventHandler idHandler = new XPathEventHandler() {

            public void onMatch(XPathEvent event) throws XMLStreamException {
                tag = event.getReader().getLocalName();
            }
            
        };
        
        XPathBuilder builder = new XPathBuilder();
        builder.addPrefix("c", "urn:customer");
        builder.listen("/c:customer/c:id", idHandler);
        
        XPathEvaluator evaluator = builder.compile();
        
        evaluator.evaluate(getClass().getResourceAsStream("customer.xml"));
        
        assertEquals("id", tag);
    }
    
    public void testSimpleExpression() throws Exception {
        
        XPathEventHandler customerHandler = new XPathEventHandler() {

            public void onMatch(XPathEvent event) throws XMLStreamException {
                custTag = event.getReader().getLocalName();
            }
            
        };
        
        XPathEventHandler idHandler = new XPathEventHandler() {

            public void onMatch(XPathEvent event) throws XMLStreamException {
                tag = event.getReader().getLocalName();
            }
            
        };
        
        XPathBuilder builder = new XPathBuilder();
        builder.listen("/customer", customerHandler);
        builder.listen("/customer/id", idHandler);
        
        XPathEvaluator evaluator = builder.compile();
        
        evaluator.evaluate(getClass().getResourceAsStream("customer2.xml"));
        
        assertEquals("customer", custTag);
        assertEquals("id", tag);
    }
}
