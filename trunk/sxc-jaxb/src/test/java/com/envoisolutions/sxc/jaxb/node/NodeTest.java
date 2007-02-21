package com.envoisolutions.sxc.jaxb.node;

import java.io.ByteArrayOutputStream;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;

import org.w3c.dom.Document;

import com.envoisolutions.node.NamedNode;
import com.envoisolutions.node.Node;
import com.envoisolutions.sxc.jaxb.JAXBContextImpl;
import com.envoisolutions.sxc.util.XoTestCase;

public class NodeTest extends XoTestCase {
    
    @SuppressWarnings("unchecked")
    public void xtestNode1() throws Exception {
        System.setProperty("streax-xo.output.directory", "target/tmp-jaxb");
        JAXBContextImpl ctx = new JAXBContextImpl(Node.class);
        
        JAXBElement<Node> jn = (JAXBElement<Node>) 
            ctx.createUnmarshaller().unmarshal(getClass().getResourceAsStream("node.xml"));
        
        assertNotNull(jn);
        
        Node n = jn.getValue();
        assertTrue(n instanceof NamedNode);
        NamedNode root = (NamedNode) n;
        assertEquals("root", root.getName());
        
        assertEquals(2, n.getNode().size());
        
        Marshaller marshaller = ctx.createMarshaller();
        assertNotNull(marshaller);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(jn, bos);
        System.out.println(bos.toString());
        
        Document d = readDocument(bos.toByteArray());
        addNamespace("n", "http://envoisolutions.com/node");
        addNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        assertValid("/n:Node", d);
        assertValid("/n:Node[@xsi:type='ns1:NamedNode']", d);
        assertValid("/n:Node/n:node[@xsi:type='ns1:NamedNode']", d);
        assertValid("/n:Node/n:node[@xsi:type='ns1:NamedNode']/n:name[text()='child']", d);
        
    }
    
    
    @SuppressWarnings("unchecked")
    public void testNestedNodes() throws Exception {
        System.setProperty("streax-xo.output.directory", "target/tmp-jaxb");
        JAXBContextImpl ctx = new JAXBContextImpl(Node.class);
        
        JAXBElement<Node> jn = (JAXBElement<Node>) 
            ctx.createUnmarshaller().unmarshal(getClass().getResourceAsStream("node2.xml"));
        
        assertNotNull(jn);
        
        Node object = jn.getValue();
        assertTrue(object instanceof NamedNode);
        
        NamedNode n = (NamedNode) object;
        assertEquals("root", n.getName());
        assertEquals(2, n.getNode().size());
        
        Node child = n.getNode().get(0);
        assertEquals(1, child.getNode().size());
        
        child = child.getNode().get(0);
        assertEquals(1, child.getNode().size());
        
        child = child.getNode().get(0);
        assertEquals(1, child.getNode().size());

        child = child.getNode().get(0);
        assertEquals(1, child.getNode().size());

        child = child.getNode().get(0);
        assertEquals(1, child.getNode().size());

        child = child.getNode().get(0);
        assertEquals(1, child.getNode().size());

        child = child.getNode().get(0);
        assertNotNull(child);
        
        // Check to see if a nested xsi:type read worked
        Node nnChild = n.getNode().get(1);
        assertNotNull(nnChild);
        assertTrue(nnChild instanceof NamedNode);
        assertEquals("child", ((NamedNode)nnChild).getName());
        
        Marshaller marshaller = ctx.createMarshaller();
        assertNotNull(marshaller);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(jn, bos);
        
        Document d = readDocument(bos.toByteArray());
        addNamespace("n", "http://envoisolutions.com/node");
        addNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        assertValid("/n:Node", d);
        assertValid("/n:Node[@xsi:type='ns1:NamedNode']", d);
        assertValid("/n:Node/n:node/n:node/n:node/n:node/n:node/n:node/n:node", d);
        assertValid("/n:Node/n:node[@xsi:type='ns1:NamedNode']", d);
        assertValid("/n:Node/n:node[@xsi:type='ns1:NamedNode']/n:name[text()='child']", d);
    }
}
