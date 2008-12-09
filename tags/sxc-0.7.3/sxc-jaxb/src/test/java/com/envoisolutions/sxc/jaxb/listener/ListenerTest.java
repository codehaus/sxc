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
package com.envoisolutions.sxc.jaxb.listener;

import java.io.ByteArrayOutputStream;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.envoisolutions.sxc.jaxb.JAXBContextImpl;
import com.envoisolutions.sxc.util.XoTestCase;
import org.w3c.dom.Document;

public class ListenerTest extends XoTestCase {
    public void testFields() throws Exception {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");
        JAXBContextImpl ctx = new JAXBContextImpl(Listener.class);

        Unmarshaller unmarshaller = ctx.createUnmarshaller();
        Listener listener = (Listener) unmarshaller.unmarshal(getClass().getResourceAsStream("listener.xml"));

        assertNotNull("listener is null", listener);
        assertEquals("root", listener.getName());
        assertNotNull("listener.getListener() is null", listener.getListener());
        assertEquals("child", listener.getListener().getName());

        listener.assertUnmarhsalCallbacks(unmarshaller, null);
        listener.getListener().assertUnmarhsalCallbacks(unmarshaller, listener);

        listener.resetCallbackData();
        listener.getListener().resetCallbackData();

        Marshaller marshaller = ctx.createMarshaller();
        assertNotNull(marshaller);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(listener, bos);

        Document d = readDocument(bos.toByteArray());
        assertValid("/listener", d);
        assertValid("/listener/name[text()='root']", d);
        assertValid("/listener/listener", d);
        assertValid("/listener/listener/name[text()='child']", d);

        listener.assertMarhsalCallbacks(marshaller);
        listener.getListener().assertMarhsalCallbacks(marshaller);
    }
}