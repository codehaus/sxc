package com.envoisolutions.sxc.jaxb.enums;

import java.io.ByteArrayOutputStream;
import javax.xml.bind.Marshaller;

import com.envoisolutions.sxc.jaxb.JAXBContextImpl;
import com.envoisolutions.sxc.util.XoTestCase;
import org.w3c.dom.Document;

public class EnumTest extends XoTestCase {
    public void testEnum() throws Exception {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");
        JAXBContextImpl ctx = new JAXBContextImpl(Enums.class);

        Enums enums = (Enums) ctx.createUnmarshaller().unmarshal(getClass().getResourceAsStream("enum.xml"));

        assertNotNull(enums);

        assertEquals(AnnotatedEnum.TWO, enums.getAnnotatedEnumAttribute());
        assertEquals(NotAnnotatedEnum.TWO, enums.getNotAnnotatedEnumAttribute());
        assertEquals(GeneratedEnum.SILVER, enums.getGeneratedEnumAttribute());

        assertEquals(AnnotatedEnum.TWO, enums.getAnnotatedEnum());
        assertEquals(NotAnnotatedEnum.TWO, enums.getNotAnnotatedEnum());
        assertEquals(GeneratedEnum.SILVER, enums.getGeneratedEnum());

        Marshaller marshaller = ctx.createMarshaller();
        assertNotNull(marshaller);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(enums, bos);

        Document d = readDocument(bos.toByteArray());
        assertValid("/enums[@annotatedEnumAttribute='dos']", d);
        assertValid("/enums[@notAnnotatedEnumAttribute='TWO']", d);
        assertValid("/enums[@generatedEnumAttribute='Silver']", d);

        assertValid("/enums/annotatedEnum[text()='dos']", d);
        assertValid("/enums/notAnnotatedEnum[text()='TWO']", d);
        assertValid("/enums/generatedEnum[text()='Silver']", d);
    }
}
