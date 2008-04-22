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