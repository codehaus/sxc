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
package com.envoisolutions.sxc.jaxb.simple;

import java.io.ByteArrayOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;

import com.envoisolutions.sxc.jaxb.JAXBContextImpl;
import com.envoisolutions.sxc.util.XoTestCase;

public class SimpleGlobalTest extends XoTestCase {
    
    public void testJAXBContextUnmarshal() throws Exception {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");
        JAXBContext ctx = JAXBContextImpl.newInstance("com.everything", getClass().getClassLoader(), null);
        
        XMLStreamReader reader = getXSR("<string xmlns=\"http://everything.com\">Dan</string>");
        JAXBElement<?> c = (JAXBElement<?>) ctx.createUnmarshaller().unmarshal(reader);
        
        assertNotNull(c);
        assertEquals("Dan", c.getValue());
        assertEquals(new QName("http://everything.com", "string"), c.getName());
        
        Marshaller marshaller = ctx.createMarshaller();
        assertNotNull(marshaller);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(c, bos);
        
        Document d = readDocument(bos.toByteArray());
        addNamespace("e", "http://everything.com");
        assertValid("/e:string[text()='Dan']", d);
    }

}
