package com.envoisolutions.sxc.jaxb.simple;

import java.io.ByteArrayOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;

import com.envoisolutions.sxc.jaxb.JAXBContextImpl;
import com.envoisolutions.sxc.util.XoTestCase;

public class SimpleReadWriteTest extends XoTestCase {
    
    public void testJAXBContextUnmarshal() throws Exception {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");
        JAXBContext ctx = new JAXBContextImpl();
        
        XMLStreamReader reader = getXSR("<name>Dan</name>");
        JAXBElement<?> c = (JAXBElement<?>) ctx.createUnmarshaller().unmarshal(reader, 
                                                                               String.class);
        
        assertNotNull(c);
        assertEquals("Dan", c.getValue());
        
        Marshaller marshaller = ctx.createMarshaller();
        assertNotNull(marshaller);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(c, bos);
        
        Document d = readDocument(bos.toByteArray());
        assertValid("/name[text()='Dan']", d);
    }
}
