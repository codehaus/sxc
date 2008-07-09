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
package com.envoisolutions.sxc.jaxb.array;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import javax.xml.bind.Marshaller;

import com.envoisolutions.sxc.jaxb.JAXBContextImpl;
import com.envoisolutions.sxc.util.XoTestCase;
import org.w3c.dom.Document;

public class ArraysTest extends XoTestCase {
    protected JAXBContextImpl ctx;

    public void testFields() throws Exception {
        // load collections object
        Arrays arrays = (Arrays) ctx.createUnmarshaller().unmarshal(getClass().getResourceAsStream("arrays.xml"));
        assertNotNull(arrays);

        // verify all fields loaded correctly
        assertValues(arrays.booleanField);
        assertValues(arrays.shortField);
        assertValues(arrays.intField);
        assertValues(arrays.longField);
        assertValues(arrays.floatField);
        assertValues(arrays.doubleField);
        assertValues(arrays.stringField, "stringField");
        assertValues(arrays.finalField, "finalField");

        assertValues(arrays.getBooleanProperty());
        assertValues(arrays.getShortProperty());
        assertValues(arrays.getIntProperty());
        assertValues(arrays.getLongProperty());
        assertValues(arrays.getFloatProperty());
        assertValues(arrays.getDoubleProperty());
        assertValues(arrays.getStringProperty(), "stringProperty");

        Marshaller marshaller = ctx.createMarshaller();
        assertNotNull(marshaller);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(arrays, bos);

        Document d = readDocument(bos.toByteArray());
        assertValid("/arrays/booleanField[text()='true']", d);
        assertValid("/arrays/booleanField[text()='false']", d);
        assertValues(d, "shortField", "1");
        assertValues(d, "intField", "11");
        assertValues(d, "longField", "111");
        assertValues(d, "floatField", "1.1");
        assertValues(d, "doubleField", "1.11");
        assertValues(d, "stringField");
        assertValues(d, "finalField");

        assertValid("/arrays/booleanProperty[text()='true']", d);
        assertValid("/arrays/booleanProperty[text()='false']", d);
        assertValues(d, "shortProperty", "1");
        assertValues(d, "intProperty", "11");
        assertValues(d, "longProperty", "111");
        assertValues(d, "floatProperty", "1.1");
        assertValues(d, "doubleProperty", "1.11");
        assertValues(d, "stringProperty");
    }

    private void assertValues(boolean[] array) {
        for (int i = 0; i < 5; i++) {
            assertEquals(i % 2 == 0, array[i]);
        }
    }

    private void assertValues(short[] array) {
        for (int i = 0; i < 5; i++) {
            assertEquals(i, array[i]);
        }
    }

    private void assertValues(int[] array) {
        for (int i = 0; i < 5; i++) {
            assertEquals(11 * i, array[i]);
        }
    }

    private void assertValues(long[] array) {
        for (int i = 0; i < 5; i++) {
            assertEquals(111 * i, array[i]);
        }
    }

    private void assertValues(float[] array) {
        for (int i = 0; i < 5; i++) {
            assertEquals((float) (1.1 * i), array[i]);
        }
    }

    private void assertValues(double[] array) {
        for (int i = 0; i < 5; i++) {
            assertEquals(1.11 * i, array[i]);
        }
    }

    private void assertValues(String[] array, String name) {
        for (int i =0; i < 5; i++) {
            assertEquals(name + i, array[i]);
        }
    }

    private void assertValues(Document d, String name) throws Exception {
        for (int i =0; i < 5; i++) {
            assertValid("/arrays/" + name + "[text()='" + name + i + "']", d);
        }
    }

    private void assertValues(Document d, String name, String baseValue) throws Exception {
        for (int i =1; i < 5; i++) {
            assertValid("/arrays/" + name + "[text()='" + new BigDecimal(baseValue).multiply(new BigDecimal(i)).toPlainString() + "']", d);
        }
    }

    public void setUp() throws Exception {
        super.setUp();
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");
        ctx = new JAXBContextImpl(Arrays.class);
    }
}