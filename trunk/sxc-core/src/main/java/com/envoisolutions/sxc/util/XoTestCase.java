package com.envoisolutions.sxc.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import junit.framework.TestCase;

public abstract class XoTestCase extends TestCase {
    
    private static String basedirPath;
    
    /**
     * Namespaces for the XPath expressions.
     */
    private Map<String, String> namespaces = new HashMap<String, String>();

    public void setUp() throws Exception {
        addNamespace("s", "http://schemas.xmlsoap.org/soap/envelope/");
        addNamespace("xsd", "http://www.w3.org/2001/XMLSchema");
        addNamespace("wsdl", "http://schemas.xmlsoap.org/wsdl/");
        addNamespace("wsdlsoap", "http://schemas.xmlsoap.org/wsdl/soap/");
        addNamespace("soap", "http://schemas.xmlsoap.org/soap/");
        addNamespace("soap12env", "http://www.w3.org/2003/05/soap-envelope");        
        addNamespace("xml", "http://www.w3.org/XML/1998/namespace");
    }

    protected XMLStreamReader getXSR(String string) throws XMLStreamException {
        ByteArrayInputStream bis = new ByteArrayInputStream(string.getBytes());
        
        return XMLInputFactory.newInstance().createXMLStreamReader(bis);
    }
    

    protected XMLStreamReader getXSR(InputStream is) throws XMLStreamException {
        return XMLInputFactory.newInstance().createXMLStreamReader(is);
    }
    
    public Document readDocument(byte[] b) throws SAXException, IOException, ParserConfigurationException {
        return readXml(new ByteArrayInputStream(b));
    }
    
    /**
     * Read XML as DOM.
     */
    public static Document readXml(InputStream is) throws SAXException, IOException,
        ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        dbf.setValidating(false);
        dbf.setIgnoringComments(false);
        dbf.setIgnoringElementContentWhitespace(true);
        dbf.setNamespaceAware(true);
        // dbf.setCoalescing(true);
        // dbf.setExpandEntityReferences(true);

        DocumentBuilder db = null;
        db = dbf.newDocumentBuilder();
        db.setEntityResolver(new NullResolver());

        // db.setErrorHandler( new MyErrorHandler());

        return db.parse(is);
    }
    
    public static class NullResolver implements EntityResolver {
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            return new InputSource(new StringReader(""));
        }
    }

    /**
     * Assert that the following XPath query selects one or more nodes.
     * 
     * @param xpath
     * @throws Exception 
     */
    public NodeList assertValid(String xpath, Node node) throws Exception {
        return XPathAssert.assertValid(xpath, node, namespaces);
    }

    /**
     * Assert that the following XPath query selects no nodes.
     * 
     * @param xpath
     */
    public NodeList assertInvalid(String xpath, Node node) throws Exception {
        return XPathAssert.assertInvalid(xpath, node, namespaces);
    }

    /**
     * Asser that the text of the xpath node retrieved is equal to the value
     * specified.
     * 
     * @param xpath
     * @param value
     * @param node
     */
    public void assertXPathEquals(String xpath, String value, Node node) throws Exception {
        XPathAssert.assertXPathEquals(xpath, value, node, namespaces);
    }

    public void assertNoFault(Node node) throws Exception {
        XPathAssert.assertNoFault(node);
    }
    
    /**
     * Add a namespace that will be used for XPath expressions.
     * 
     * @param ns Namespace name.
     * @param uri The namespace uri.
     */
    public void addNamespace(String ns, String uri) {
        namespaces.put(ns, uri);
    }

    protected InputStream getResourceAsStream(String resource) {
        return getClass().getResourceAsStream(resource);
    }

    protected Reader getResourceAsReader(String resource) {
        return new InputStreamReader(getResourceAsStream(resource));
    }

    public File getTestFile(String relativePath) {
        return new File(getBasedir(), relativePath);
    }

    public static String getBasedir() {
        if (basedirPath != null) {
            return basedirPath;
        }

        basedirPath = System.getProperty("basedir");

        if (basedirPath == null) {
            basedirPath = new File("").getAbsolutePath();
        }

        return basedirPath;
    }
    
}
