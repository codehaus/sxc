package com.envoisolutions.sxc.jaxb.array;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.util.Collection;
import javax.xml.bind.Marshaller;
import javax.xml.bind.JAXBException;

import com.envoisolutions.sxc.jaxb.JAXBContextImpl;
import com.envoisolutions.sxc.util.XoTestCase;
import org.w3c.dom.Document;

public class ArraysTest extends XoTestCase {
    protected JAXBContextImpl ctx;

    public void testFields() throws Exception {
        // load collections object
        Arrays arrays = (Arrays) ctx.createUnmarshaller().unmarshal(getClass().getResourceAsStream("arrays.xml"));
        assertNotNull(arrays);

//        // verify all fields loaded correctly
//        assertValues(arrays.collectionField, "collectionField");
//        assertValues(arrays.listField, "listField");
//        assertValues(arrays.setField, "setField");
//        assertValues(arrays.sortedSetField, "sortedSetField");
//        assertValues(arrays.queueField, "queueField");
//        assertValues(arrays.linkedHashSetField, "linkedHashSetField");
//        assertValues(arrays.linkedListField, "linkedListField");
//        assertValues(arrays.customCollectionField, "customCollectionField");
//        assertValues(arrays.initializedField, "initializedField");
//        assertValues(arrays.finalField, "finalField");
//
//        // verify all properties loaded correctly
//        assertValues(arrays.getCollectionProperty(), "collectionProperty");
//        assertValues(arrays.getListProperty(), "listProperty");
//        assertValues(arrays.getSetProperty(), "setProperty");
//        assertValues(arrays.getSortedSetProperty(), "sortedSetProperty");
//        assertValues(arrays.getQueueProperty(), "queueProperty");
//        assertValues(arrays.getLinkedHashSetProperty(), "linkedHashSetProperty");
//        assertValues(arrays.getLinkedListProperty(), "linkedListProperty");
//        assertValues(arrays.getCustomCollectionProperty(), "customCollectionProperty");
//        assertValues(arrays.getInitializedProperty(), "initializedProperty");
//        assertValues(arrays.getFinalProperty(), "finalProperty");
//
//        // verify initialized instances didn't change
//        assertSame(arrays.initializedField, Collections.INITIALIZED_FIELD);
//        assertSame(arrays.getInitializedProperty(), Collections.INITIALIZED_PROPERTY);
//        assertSame(arrays.getFinalProperty(), Collections.FINAL_PROPERTY);
//
//        // Fill the unknown and uncreatable collections
//        arrays.uncreatableCollectionField = new Collections.UncreatableCollection<String>(42);
//        addValues(arrays.uncreatableCollectionField, "uncreatableCollectionField");
//        arrays.unknownCollectionField = new Collections.UnknownCollectionImpl<String>();
//        addValues(arrays.unknownCollectionField, "unknownCollectionField");
//        arrays.setUncreatableCollectionProperty(new Collections.UncreatableCollection<String>(42));
//        addValues(arrays.getUncreatableCollectionProperty(), "uncreatableCollectionProperty");
//        arrays.setUnknownCollectionProperty(new Collections.UnknownCollectionImpl<String>());
//        addValues(arrays.getUnknownCollectionProperty(), "unknownCollectionProperty");

        Marshaller marshaller = ctx.createMarshaller();
        assertNotNull(marshaller);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(arrays, bos);

        Document d = readDocument(bos.toByteArray());
//        assertValues(d, "collectionField");
//        assertValues(d, "listField");
//        assertValues(d, "setField");
//        assertValues(d, "sortedSetField");
//        assertValues(d, "queueField");
//        assertValues(d, "linkedHashSetField");
//        assertValues(d, "linkedListField");
//        assertValues(d, "customCollectionField");
//        assertValues(d, "initializedField");
//        assertValues(d, "finalField");
//        assertValues(d, "uncreatableCollectionField");
//        assertValues(d, "unknownCollectionField");
//
//        assertValues(d, "collectionProperty");
//        assertValues(d, "listProperty");
//        assertValues(d, "setProperty");
//        assertValues(d, "sortedSetProperty");
//        assertValues(d, "queueProperty");
//        assertValues(d, "linkedHashSetProperty");
//        assertValues(d, "linkedListProperty");
//        assertValues(d, "customCollectionProperty");
//        assertValues(d, "initializedProperty");
//        assertValues(d, "finalProperty");
//        assertValues(d, "uncreatableCollectionProperty");
//        assertValues(d, "unknownCollectionProperty");

    }

//    public void testUncreatableCollection() throws Exception {
//        assertLoad("collectionField", true); // just verify the code works
//        assertLoad("uncreatableCollectionField", false);
//        assertLoad("uncreatableCollectionProperty", false);
//    }
//
//    public void testUnknownCollection() throws Exception {
//        assertLoad("collectionField", true); // just verify the code works
//        assertLoad("unknownCollectionField", false);
//        assertLoad("unknownCollectionProperty", false);
//    }

    private void assertValues(Collection<String> collection, String name) {
        for (int i =0; i < 5; i++) {
            assertTrue("Expected collection " + name + " to contain value " + name + i, collection.contains(name + i));
        }
    }

    private void assertValues(Document d, String name) throws Exception {
        for (int i =0; i < 5; i++) {
            assertValid("/collections/" + name + "[text()='" + name + i + "']", d);
        }
    }

    private void addValues(Collection<String> c, String name) throws Exception {
        for (int i =0; i < 5; i++) {
            c.add(name + i);
        }
    }

    public void setUp() throws Exception {
        super.setUp();
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");
        ctx = new JAXBContextImpl(Arrays.class);
    }

    private void assertLoad(String name, boolean shouldLoad) {
        try {
            String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<collections>" +
                    "    <" + name + ">value</" + name + ">" +
                    "</collections>";

            ctx.createUnmarshaller().unmarshal(new ByteArrayInputStream(xml.getBytes()));
            if (!shouldLoad) fail("Expected to NOT be able to load xml containing a " + name + " element");
        } catch (JAXBException e) {
            if (shouldLoad) fail("Expected to be able to load xml containing a " + name + " element");
        }
    }
}