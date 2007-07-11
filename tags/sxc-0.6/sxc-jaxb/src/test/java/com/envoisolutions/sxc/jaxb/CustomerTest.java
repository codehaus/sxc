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
