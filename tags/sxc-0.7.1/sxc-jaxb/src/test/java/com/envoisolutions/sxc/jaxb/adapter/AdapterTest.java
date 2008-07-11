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
package com.envoisolutions.sxc.jaxb.adapter;

import java.io.ByteArrayOutputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import com.envoisolutions.sxc.util.XoTestCase;
import org.w3c.dom.Document;

public class AdapterTest extends XoTestCase {
    public void testElementAdapter() throws Exception {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");

        JAXBContext ctx = JAXBContext.newInstance(Holder.class);

        Holder holder = (Holder) ctx.createUnmarshaller().unmarshal(getClass().getResourceAsStream("elementAdapter.xml"));

        assertNotNull("holder is null", holder);
        assertNotNull("holder.boundType is null", holder.boundType);
        BoundType boundType = holder.boundType;
        assertEquals("zip-tie", boundType.name);

        Marshaller marshaller = ctx.createMarshaller();
        assertNotNull(marshaller);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(holder, bos);

        Document d = readDocument(bos.toByteArray());
        assertValid("/holder", d);
        assertValid("/holder/boundType", d);
        assertValid("/holder/boundType/name", d);
        assertValid("/holder/boundType/name[text()='zip-tie']", d);
    }

    public void testAttributeAndValueAdapter() throws Exception {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");

        JAXBContext ctx = JAXBContext.newInstance(StringHolder.class);

        StringHolder stringHolder = (StringHolder) ctx.createUnmarshaller().unmarshal(getClass().getResourceAsStream("attributeAndValueAdapter.xml"));

        assertNotNull("stringHolder is null", stringHolder);
        assertNotNull("stringHolder.attribute is null", stringHolder.attribute);
        assertNotNull("stringHolder.value is null", stringHolder.value);
        assertEquals("duct tape", stringHolder.attribute.name);
        assertEquals("zip-tie", stringHolder.value.name);

        Marshaller marshaller = ctx.createMarshaller();
        assertNotNull(marshaller);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(stringHolder, bos);

        Document d = readDocument(bos.toByteArray());
        assertValid("/stringHolder", d);
        assertValid("/stringHolder[@attribute='duct tape']", d);
        assertValid("/stringHolder[text()='zip-tie']", d);
    }
}
