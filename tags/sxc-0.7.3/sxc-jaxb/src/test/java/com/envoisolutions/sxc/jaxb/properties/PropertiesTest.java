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
package com.envoisolutions.sxc.jaxb.properties;

import java.io.ByteArrayOutputStream;
import javax.xml.bind.Marshaller;

import com.envoisolutions.sxc.jaxb.JAXBContextImpl;
import com.envoisolutions.sxc.util.XoTestCase;
import org.w3c.dom.Document;

public class PropertiesTest extends XoTestCase {
    public void testProperties() throws Exception {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");
        JAXBContextImpl ctx = new JAXBContextImpl(Properties.class);

        Properties properties = (Properties) ctx.createUnmarshaller().unmarshal(getClass().getResource("properties.xml"));

        assertNotNull(properties);
        properties.assertPublicProperty("-public-");
        properties.assertPackageProperty("-package-");
        properties.assertProtectedProperty("-protected-");
        properties.assertPrivateProperty("-private-");
        properties.assertBooleanProperty(true);
        properties.assertByteProperty((byte) 42);
        properties.assertShortProperty((short) 4242);
        properties.assertIntProperty(424242);
        properties.assertLongProperty(42424242);
        properties.assertFloatProperty((float) 0.42);
        properties.assertDoubleProperty(0.4242);

        Marshaller marshaller = ctx.createMarshaller();
        assertNotNull(marshaller);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(properties, bos);

        Document d = readDocument(bos.toByteArray());
        assertValid("/properties/public-property[text()='-public-']", d);
        assertValid("/properties/package-property[text()='-package-']", d);
        assertValid("/properties/protected-property[text()='-protected-']", d);
        assertValid("/properties/private-property[text()='-private-']", d);
        assertValid("/properties/booleanProperty[text()='true']", d);
        assertValid("/properties/byteProperty[text()='42']", d);
        assertValid("/properties/shortProperty[text()='4242']", d);
        assertValid("/properties/intProperty[text()='424242']", d);
        assertValid("/properties/longProperty[text()='42424242']", d);
        assertValid("/properties/floatProperty[text()='0.42']", d);
        assertValid("/properties/doubleProperty[text()='0.4242']", d);
    }
}