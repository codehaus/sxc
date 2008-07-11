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

import javax.xml.bind.Marshaller;

import org.w3c.dom.Document;

import com.envoisolutions.sxc.util.XoTestCase;

import customer.Customer;

public class CustomerTest extends XoTestCase {
    
    public void testJAXBContextUnmarshal() throws Exception {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");
        JAXBContextImpl ctx = new JAXBContextImpl(Customer.class);
        
        Customer c = (Customer) ctx.createUnmarshaller().unmarshal(getClass().getResourceAsStream("customer.xml"));
        
        assertNotNull(c);
        assertEquals(1, c.getId());
        assertEquals("Dan Diephouse", c.getName());
        
        Marshaller marshaller = ctx.createMarshaller();
        assertNotNull(marshaller);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(c, bos);
        
        Document d = readDocument(bos.toByteArray());
        addNamespace("c", "urn:customer");
        assertValid("/c:customer/c:name[text()='Dan Diephouse']", d);
        assertValid("/c:customer/c:id[text()='1']", d);
    }
}
