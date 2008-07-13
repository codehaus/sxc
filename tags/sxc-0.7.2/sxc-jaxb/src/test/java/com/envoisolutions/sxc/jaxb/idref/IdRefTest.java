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
package com.envoisolutions.sxc.jaxb.idref;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Collections;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import com.envoisolutions.sxc.util.XoTestCase;
import org.w3c.dom.Document;

public class IdRefTest extends XoTestCase {
    private JAXBContext ctx;
    public void testAnyElement() throws Exception {
        IdRefGraph idRefGraph = (IdRefGraph) ctx.createUnmarshaller().unmarshal(getClass().getResourceAsStream("idref.xml"));

        assertNotNull("idRefGraph is null", idRefGraph);
        assertNotNull("idRefGraph.getNode() is null", idRefGraph.getNode());
        List<IdRefNode> nodes = idRefGraph.getNode();

        Map<String, IdRefNode> nodeMap = new TreeMap<String, IdRefNode>();
        for (IdRefNode node : nodes) {
            assertNotNull("node is null", node);
            assertNotNull("node.getId() is null", node.getId());
            assertTrue("Duplicate node id " + node.getId(), nodeMap.put(node.getId(), node) == null);
        }

        // verify all nodes loaded properly
        IdRefNode one = nodeMap.get("one");
        assertNotNull("one is null", one);
        IdRefNode two = nodeMap.get("two");
        assertNotNull("two is null", two);
        IdRefNode three = nodeMap.get("three");
        assertNotNull("three is null", three);

        // verify next ref
        assertSame(two, one.getNext());
        assertSame(three, two.getNext());
        assertSame(one, three.getNext());

        // verify self ref
        assertSame(one, one.getSelf());
        assertSame(two, two.getSelf());
        assertSame(three, three.getSelf());

        // verify all ref
        Set<IdRefNode> all = new HashSet<IdRefNode>(nodeMap.values());
        assertEquals(all, one.getAll());
        assertEquals(all, two.getAll());
        assertEquals(all, three.getAll());


        
        Marshaller marshaller = ctx.createMarshaller();
        assertNotNull(marshaller);
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(idRefGraph, bos);

        System.out.println(new String(bos.toByteArray()));
        Document d = readDocument(bos.toByteArray());
        assertValid("/idRefGraph", d);

        // verify id
        assertValid("/idRefGraph/node[@id='one']", d);
        assertValid("/idRefGraph/node[@id='two']", d);
        assertValid("/idRefGraph/node[@id='three']", d);

        // verify next
        assertValid("/idRefGraph/node[@id='one'][@next='two']", d);
        assertValid("/idRefGraph/node[@id='two'][@next='three']", d);
        assertValid("/idRefGraph/node[@id='three'][@next='one']", d);

        // verify self
        assertValid("/idRefGraph/node[@id='one'][@self='one']", d);
        assertValid("/idRefGraph/node[@id='two'][@self='two']", d);
        assertValid("/idRefGraph/node[@id='three'][@self='three']", d);

        // verify all
        for (String id : Arrays.asList("one", "two", "three")) {
            assertValid("/idRefGraph/node[@id='" + id + "']/all[text()='one']", d);
            assertValid("/idRefGraph/node[@id='" + id + "']/all[text()='two']", d);
            assertValid("/idRefGraph/node[@id='" + id + "']/all[text()='three']", d);

        }
    }

    public void testUnmarshalUnresolved() throws Exception {
        String xml =
                "<idRefGraph>\n" +
                "    <node self=\"one\" next=\"two\" id=\"one\">\n" +
                "        <all>one</all>\n" +
                "        <all>two</all>\n" +
                "        <all>three</all>\n" +
                "    </node>\n" +
                "</idRefGraph>";

        IdRefGraph idRefGraph = (IdRefGraph) ctx.createUnmarshaller().unmarshal(new ByteArrayInputStream(xml.getBytes()));


        assertNotNull("idRefGraph is null", idRefGraph);
        assertNotNull("idRefGraph.getNode() is null", idRefGraph.getNode());
        List<IdRefNode> nodes = idRefGraph.getNode();

        Map<String, IdRefNode> nodeMap = new TreeMap<String, IdRefNode>();
        for (IdRefNode node : nodes) {
            assertNotNull("node is null", node);
            assertNotNull("node.getId() is null", node.getId());
            assertTrue("Duplicate node id " + node.getId(), nodeMap.put(node.getId(), node) == null);
        }

        // verify all nodes loaded properly
        IdRefNode one = nodeMap.get("one");
        assertNotNull("one is null", one);

        // verify next ref
        assertNull(one.getNext());

        // verify self ref
        assertSame(one, one.getSelf());

        // verify all ref
        assertEquals(Collections.singleton(one), one.getAll());
    }

    public void testMarshalUnresolved() throws Exception {
        IdRefNode one = new IdRefNode();
        one.setId("one");

        IdRefNode two = new IdRefNode();
        two.setId("two");

        one.setNext(two);
        IdRefGraph graph = new IdRefGraph();
        graph.getNode().add(one);

        // NOTE: two is not added which means we have an unresolved ref

        // write graph which will work
        Marshaller marshaller = ctx.createMarshaller();
        assertNotNull(marshaller);
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(graph, bos);

        System.out.println(new String(bos.toByteArray()));
        Document d = readDocument(bos.toByteArray());
        assertValid("/idRefGraph", d);

        // verify id and next
        assertValid("/idRefGraph/node[@id='one']", d);
        assertValid("/idRefGraph/node[@id='one'][@next='two']", d);
    }

    public void setUp() throws Exception {
        super.setUp();
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");
        ctx = JAXBContext.newInstance(IdRefGraph.class);
    }
}