package com.envoisolutions.sxc.jaxb;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;

import com.envoisolutions.sxc.util.Base64;
import com.envoisolutions.sxc.util.XoTestCase;
import com.everything.AttributesType;

public class EverythingAttributeTest extends XoTestCase {
    
    @SuppressWarnings("unchecked")
    public void testJAXBContextUnmarshal() throws Exception {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");
        JAXBContextImpl ctx = JAXBContextImpl.createContext("com.everything", getClass().getClassLoader(), null);

        JAXBElement<AttributesType> a = (JAXBElement<AttributesType>) 
            ctx.createUnmarshaller().unmarshal(getClass().getResourceAsStream("everything-attribute.xml"));
        assertNotNull(a);
        
        AttributesType st = a.getValue();
        assertNotNull(st);
        assertEquals("urn:test", st.getAnyURI());
        assertTrue(Arrays.equals(Base64.decode("0123456789ABCDEF"), st.getBase64Binary()));
        assertTrue(st.isBoolean());
        assertEquals(new BigDecimal("123.123"), st.getDecimal());
        assertEquals(123.123, st.getDouble());
        assertEquals(1.2e-2, st.getFloat(), 1.0e-4);
        assertEquals(123, st.getInt().intValue());
        assertEquals(new BigInteger("123"), st.getInteger());
        assertEquals("en", st.getLanguage());
        assertEquals(new QName("http://everything.com", "FooBar"), st.getQName());
        assertEquals(new QName("http://everything.com", "FooBar"), st.getQNameNoPrefix());
        
        Marshaller marshaller = ctx.createMarshaller();
        assertNotNull(marshaller);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(a, bos);
        
        Document d = readDocument(bos.toByteArray());
        addNamespace("e", "http://everything.com");
        addNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        assertValid("/e:attributes", d);
        assertValid("/e:attributes[@base64Binary='0123456789ABCDEF']", d);
        assertValid("/e:attributes[@decimal='123.123']", d);
        assertValid("/e:attributes[@duration='P1347Y']", d);
        assertValid("/e:attributes[@double='123.123']", d);
        assertValid("/e:attributes[@float='0.012']", d);
        assertValid("/e:attributes[@int='123']", d);
        assertValid("/e:attributes[@integer='123']", d);
        assertValid("/e:attributes[@string='string']", d);
    }
}
