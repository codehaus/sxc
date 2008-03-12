package com.envoisolutions.sxc.jaxb.fields;

import java.io.ByteArrayOutputStream;
import javax.xml.bind.Marshaller;

import com.envoisolutions.sxc.jaxb.JAXBContextImpl;
import com.envoisolutions.sxc.util.XoTestCase;
import org.w3c.dom.Document;

public class FieldsTest extends XoTestCase {
    public void testFields() throws Exception {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");
        JAXBContextImpl ctx = new JAXBContextImpl(Fields.class);

        Fields fields = (Fields) ctx.createUnmarshaller().unmarshal(getClass().getResourceAsStream("fields.xml"));

        assertNotNull(fields);
        fields.assertPublicField("-public-");
        fields.assertPackageField("-package-");
        fields.assertProtectedField("-protected-");
        fields.assertPrivateField("-private-");
        fields.assertBooleanField(true);
        fields.assertByteField((byte) 42);
        fields.assertShortField((short) 4242);
        fields.assertIntField(424242);
        fields.assertLongField(42424242);
        fields.assertFloatField((float) 0.42);
        fields.assertDoubleField(0.4242);

        Marshaller marshaller = ctx.createMarshaller();
        assertNotNull(marshaller);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(fields, bos);

        Document d = readDocument(bos.toByteArray());
        assertValid("/fields/publicField[text()='-public-']", d);
        assertValid("/fields/packageField[text()='-package-']", d);
        assertValid("/fields/protectedField[text()='-protected-']", d);
        assertValid("/fields/privateField[text()='-private-']", d);
    }
}
