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
        assertValid("/fields/public-field[text()='-public-']", d);
        assertValid("/fields/package-field[text()='-package-']", d);
        assertValid("/fields/protected-field[text()='-protected-']", d);
        assertValid("/fields/private-field[text()='-private-']", d);
        assertValid("/fields/booleanField[text()='true']", d);
        assertValid("/fields/byteField[text()='42']", d);
        assertValid("/fields/shortField[text()='4242']", d);
        assertValid("/fields/intField[text()='424242']", d);
        assertValid("/fields/longField[text()='42424242']", d);
        assertValid("/fields/floatField[text()='0.42']", d);
        assertValid("/fields/doubleField[text()='0.4242']", d);
    }
}
