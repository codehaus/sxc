/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.envoisolutions.sxc.jaxb.node;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.Iterator;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.JAXBContext;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.namespace.NamespaceContext;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.envoisolutions.node.NamedNode;
import com.envoisolutions.node.Node;
import com.envoisolutions.sxc.jaxb.JAXBContextImpl;
import com.envoisolutions.sxc.util.XoTestCase;

public class NodeTest extends XoTestCase {
    
    @SuppressWarnings("unchecked")
    public void xtestNode1() throws Exception {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");
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
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");
        JAXBContext ctx = JAXBContextImpl.newInstance(Node.class, NamedNode.class);

        StreamSource source = new StreamSource(getClass().getResourceAsStream("node2.xml"));
        JAXBElement<?> jn = (JAXBElement<?>) ctx.createUnmarshaller().unmarshal(source);

        assertNotNull(jn);
        assertTrue(jn.isTypeSubstituted());
        assertEquals(Object.class, jn.getDeclaredType());

        Node object = (Node) jn.getValue(); 
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
        // NOTE: xsi:type contains a hardcoded namespace prefix wich will break if prefix selection algorithm is changed
        assertValid("/n:Node[@xsi:type='NamedNode']", d);
        assertValid("/n:Node/n:node/n:node/n:node/n:node/n:node/n:node/n:node", d);
        assertValid("/n:Node/n:node[@xsi:type='NamedNode']", d);
        assertValid("/n:Node/n:node[@xsi:type='NamedNode']/n:name[text()='child']", d);
    }
}
