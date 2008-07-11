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
package com.envoisolutions.sxc.jaxb;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;

import com.envoisolutions.sxc.util.Base64;
import com.envoisolutions.sxc.util.XoTestCase;
import com.everything.Everything;
import com.everything.OccursAndNillable;
import com.everything.SimpleTypes;

public class EverythingTest extends XoTestCase {
    
    public void testJAXBContextUnmarshal() throws Exception {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");
        JAXBContextImpl ctx = new JAXBContextImpl(Everything.class);
        
        Everything e = (Everything) ctx.createUnmarshaller().unmarshal(getClass().getResourceAsStream("everything.xml"));
        assertNotNull(e);
        
        SimpleTypes st = e.getSimpleTypes();
        assertNotNull(st);
        assertEquals("urn:test", st.getAnyURI());
        assertTrue(Arrays.equals(Base64.decode("0123456789ABCDEF"), st.getBase64Binary()));
        assertTrue(st.isBoolean());
        assertEquals(new BigDecimal("123.123"), st.getDecimal());
        assertEquals(123.123, st.getDouble());
        assertEquals(1.2e-2, st.getFloat(), 1.0e-4);
        assertEquals(123, st.getInt());
        assertEquals(new BigInteger("123"), st.getInteger());
        assertEquals("en", st.getLanguage());
        assertEquals(new QName("http://everything.com", "FooBar"), st.getQName());
        assertEquals(new QName("http://everything.com", "FooBar"), st.getQNameNoPrefix());
        
        assertNotNull(e);
        OccursAndNillable o = e.getOccursAndNillable();
        assertEquals("test", o.getMax0Min1());
        assertEquals("test", o.getMax1Min1());
        assertNull(o.getMax1Min1Nillable());
        assertNull(o.getMax1Min1NillableComplex());
        assertNull(o.getMax1Min1NillableEnum());

        assertEquals(2, o.getMaxUnbounded().size());
        assertEquals("string1", o.getMaxUnbounded().get(0));
        assertEquals("string2", o.getMaxUnbounded().get(1));
        assertEquals(2, o.getMaxUnboundedNillable().size());
        assertEquals("string1", o.getMaxUnboundedNillable().get(0));
        assertNull(o.getMaxUnboundedNillable().get(1));
        
        Marshaller marshaller = ctx.createMarshaller();
        assertNotNull(marshaller);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(e, bos);
        
        Document d = readDocument(bos.toByteArray());
        addNamespace("e", "http://everything.com");
        addNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        assertValid("/e:Everything", d);
        assertValid("//e:simpleTypes/e:base64Binary[text()='0123456789ABCDEF']", d);
        assertValid("//e:simpleTypes/e:decimal[text()='123.123']", d);
        assertValid("//e:simpleTypes/e:integer[text()='123']", d);
        assertValid("/e:Everything/e:occursAndNillable/e:max0min1[text()='test']", d);
        assertValid("/e:Everything/e:occursAndNillable/e:max1min1[text()='test']", d);
        assertValid("/e:Everything/e:occursAndNillable/e:max1min1Nillable[@xsi:nil='true']", d);
        assertValid("/e:Everything/e:occursAndNillable/e:max1min1NillableComplex[@xsi:nil='true']", d);
        assertValid("/e:Everything/e:occursAndNillable/e:max1min1NillableEnum[@xsi:nil='true']", d);
        assertValid("/e:Everything/e:occursAndNillable/e:maxUnboundedNillable[1][text()='string1']", d);
        assertValid("/e:Everything/e:occursAndNillable/e:maxUnboundedNillable[2][@xsi:nil='true']", d);
    }
    
    public void testJAXBContextUnmarshalWithSpecifiedClass() throws Exception {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");
        JAXBContextImpl ctx = new JAXBContextImpl(Everything.class);
        
        XMLStreamReader xsr = getXSR(getClass().getResourceAsStream("everything.xml"));
        xsr.nextTag();
        JAXBElement<Everything> je = 
            (JAXBElement<Everything>) ctx.createUnmarshaller().unmarshal(xsr, Everything.class);
        assertNotNull(je);
        
        Everything e = je.getValue();
        assertNotNull(e);
        SimpleTypes st = e.getSimpleTypes();
        assertNotNull(st);
        assertEquals("urn:test", st.getAnyURI());
        assertTrue(Arrays.equals(Base64.decode("0123456789ABCDEF"), st.getBase64Binary()));
        assertTrue(st.isBoolean());
        assertEquals(new BigDecimal("123.123"), st.getDecimal());
        assertEquals(123.123, st.getDouble());
        assertEquals(1.2e-2, st.getFloat(), 1.0e-4);
        assertEquals(123, st.getInt());
        assertEquals(new BigInteger("123"), st.getInteger());
        assertEquals("en", st.getLanguage());
        assertEquals(new QName("http://everything.com", "FooBar"), st.getQName());
        assertEquals(new QName("http://everything.com", "FooBar"), st.getQNameNoPrefix());
        
        assertNotNull(e);
        OccursAndNillable o = e.getOccursAndNillable();
        assertEquals("test", o.getMax0Min1());
        assertEquals("test", o.getMax1Min1());
        assertNull(o.getMax1Min1Nillable());
        assertNull(o.getMax1Min1NillableComplex());
        assertNull(o.getMax1Min1NillableEnum());

        assertEquals(2, o.getMaxUnbounded().size());
        assertEquals("string1", o.getMaxUnbounded().get(0));
        assertEquals("string2", o.getMaxUnbounded().get(1));
        assertEquals(2, o.getMaxUnboundedNillable().size());
        assertEquals("string1", o.getMaxUnboundedNillable().get(0));
        assertNull(o.getMaxUnboundedNillable().get(1));
        
        Marshaller marshaller = ctx.createMarshaller();
        assertNotNull(marshaller);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(e, bos);
        
        Document d = readDocument(bos.toByteArray());
        addNamespace("e", "http://everything.com");
        addNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        assertValid("/e:Everything", d);
        assertValid("//e:simpleTypes/e:base64Binary[text()='0123456789ABCDEF']", d);
        assertValid("//e:simpleTypes/e:decimal[text()='123.123']", d);
        assertValid("//e:simpleTypes/e:integer[text()='123']", d);
        assertValid("/e:Everything/e:occursAndNillable/e:max0min1[text()='test']", d);
        assertValid("/e:Everything/e:occursAndNillable/e:max1min1[text()='test']", d);
        assertValid("/e:Everything/e:occursAndNillable/e:max1min1Nillable[@xsi:nil='true']", d);
        assertValid("/e:Everything/e:occursAndNillable/e:max1min1NillableComplex[@xsi:nil='true']", d);
        assertValid("/e:Everything/e:occursAndNillable/e:max1min1NillableEnum[@xsi:nil='true']", d);
        assertValid("/e:Everything/e:occursAndNillable/e:maxUnboundedNillable[1][text()='string1']", d);
        assertValid("/e:Everything/e:occursAndNillable/e:maxUnboundedNillable[2][@xsi:nil='true']", d);
    }
}
