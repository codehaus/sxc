package com.envoisolutions.sxc.jaxb.any;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.namespace.QName;
import javax.xml.XMLConstants;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import com.envoisolutions.sxc.util.XoTestCase;
import com.envoisolutions.sxc.jaxb.JAXBContextImpl;

public class AnyTest extends XoTestCase {
    public void testAnyElement() throws Exception {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");

        JAXBContext ctx = JAXBContextImpl.newInstance(AnyElement.class);

        AnyElement anyElement = (AnyElement) ctx.createUnmarshaller().unmarshal(getClass().getResourceAsStream("anyElement.xml"));

        assertNotNull("anyElement is null", anyElement);
        assertNotNull("anyElement.getElement() is null", anyElement.getElement());
        Element element = anyElement.getElement();
        assertEquals("three", element.getLocalName());
        assertTrue(element.getChildNodes().getLength() > 0);
        assertEquals("tres", getChildElement(element).getLocalName());
        

        Marshaller marshaller = ctx.createMarshaller();
        assertNotNull(marshaller);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(anyElement, bos);

        Document d = readDocument(bos.toByteArray());
        assertValid("/anyElement", d);
        assertValid("/anyElement/three", d);
        assertValid("/anyElement/three/tres", d);
        assertValid("/anyElement/three/tres[text()='trios']", d);
    }

    private Element getChildElement(Element element) {
        for(int i = 0; i < element.getChildNodes().getLength(); i++) {
            Node node = element.getChildNodes().item(i);
            if (node instanceof Element) {
                return (Element) node;
            }
        }
        return null;
    }

    public void testAnyElementArray() throws Exception {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");

        JAXBContext ctx = JAXBContextImpl.newInstance(AnyElementArray.class);

        StreamSource source = new StreamSource(getClass().getResourceAsStream("anyElement.xml"));
        JAXBElement<AnyElementArray> jaxbAnyElementArray = ctx.createUnmarshaller().unmarshal(source, AnyElementArray.class);
        assertNotNull("jaxbAnyElementArray is null", jaxbAnyElementArray);
        assertNotNull("jaxbAnyElementArray.getValue() is null", jaxbAnyElementArray.getValue());

        AnyElementArray anyElementArray = jaxbAnyElementArray.getValue();
        assertNotNull("anyElementArray is null", anyElementArray);
        assertNotNull("anyElementArray.getElements() is null", anyElementArray.getElements());
        Element[] elements = anyElementArray.getElements();
        assertEquals(3, elements.length);

        assertEquals("one", elements[0].getLocalName());
        assertTrue(elements[0].getChildNodes().getLength() > 0);
        assertEquals("uno", getChildElement(elements[0]).getLocalName());
        assertEquals("two", elements[1].getLocalName());
        assertTrue(elements[1].getChildNodes().getLength() > 0);
        assertEquals("dos", getChildElement(elements[1]).getLocalName());
        assertEquals("three", elements[2].getLocalName());
        assertTrue(elements[2].getChildNodes().getLength() > 0);
        assertEquals("tres", getChildElement(elements[2]).getLocalName());


        Marshaller marshaller = ctx.createMarshaller();
        assertNotNull(marshaller);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(anyElementArray, bos);

        Document d = readDocument(bos.toByteArray());
        assertValid("/anyElementArray", d);
        assertValid("/anyElementArray/one", d);
        assertValid("/anyElementArray/one/uno", d);
        assertValid("/anyElementArray/one/uno[text()='un']", d);
        assertValid("/anyElementArray/two", d);
        assertValid("/anyElementArray/two/dos", d);
        assertValid("/anyElementArray/two/dos[text()='deux']", d);
        assertValid("/anyElementArray/three", d);
        assertValid("/anyElementArray/three/tres", d);
        assertValid("/anyElementArray/three/tres[text()='trios']", d);
    }

    public void testAnyElementList() throws Exception {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");

        JAXBContext ctx = JAXBContextImpl.newInstance(AnyElementList.class);

        StreamSource source = new StreamSource(getClass().getResourceAsStream("anyElement.xml"));
        JAXBElement<AnyElementList> jaxbAnyElementList = ctx.createUnmarshaller().unmarshal(source, AnyElementList.class);
        assertNotNull("jaxbAnyElementList is null", jaxbAnyElementList);
        assertNotNull("jaxbAnyElementList.getValue() is null", jaxbAnyElementList.getValue());

        AnyElementList anyElementList = jaxbAnyElementList.getValue();
        assertNotNull("anyElementList is null", anyElementList);
        assertNotNull("anyElementList.getElements() is null", anyElementList.getElements());
        List<Element> elements = anyElementList.getElements();
        assertEquals(3, elements.size());

        assertEquals("one", elements.get(0).getLocalName());
        assertTrue(elements.get(0).getChildNodes().getLength() > 0);
        assertEquals("uno", getChildElement(elements.get(0)).getLocalName());
        assertEquals("two", elements.get(1).getLocalName());
        assertTrue(elements.get(1).getChildNodes().getLength() > 0);
        assertEquals("dos", getChildElement(elements.get(1)).getLocalName());
        assertEquals("three", elements.get(2).getLocalName());
        assertTrue(elements.get(2).getChildNodes().getLength() > 0);
        assertEquals("tres", getChildElement(elements.get(2)).getLocalName());


        Marshaller marshaller = ctx.createMarshaller();
        assertNotNull(marshaller);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(anyElementList, bos);

        Document d = readDocument(bos.toByteArray());
        assertValid("/anyElementList", d);
        assertValid("/anyElementList/one", d);
        assertValid("/anyElementList/one/uno", d);
        assertValid("/anyElementList/one/uno[text()='un']", d);
        assertValid("/anyElementList/two", d);
        assertValid("/anyElementList/two/dos", d);
        assertValid("/anyElementList/two/dos[text()='deux']", d);
        assertValid("/anyElementList/three", d);
        assertValid("/anyElementList/three/tres", d);
        assertValid("/anyElementList/three/tres[text()='trios']", d);
    }

    public void testAnyObject() throws Exception {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");

        JAXBContext ctx = JAXBContextImpl.newInstance(AnyObject.class, Three.class);

        StreamSource source = new StreamSource(getClass().getResourceAsStream("anyElement.xml"));
        JAXBElement<AnyObject> jaxbAnyObject = ctx.createUnmarshaller().unmarshal(source, AnyObject.class);
        assertNotNull("jaxbAnyObject is null", jaxbAnyObject);
        assertNotNull("jaxbAnyObject.getValue() is null", jaxbAnyObject.getValue());

        AnyObject anyObject = jaxbAnyObject.getValue();
        assertNotNull("anyObject is null", anyObject);
        assertNotNull("anyObject.getElement() is null", anyObject.getObject());
        assertTrue("anyObject.getObject() should be an instance of Three", anyObject.getObject() instanceof Three);

        Three three = (Three)anyObject.getObject();
        assertEquals("trios", three.getTres());

        Marshaller marshaller = ctx.createMarshaller();
        assertNotNull(marshaller);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(anyObject, bos);

        Document d = readDocument(bos.toByteArray());
        assertValid("/anyObject", d);
        assertValid("/anyObject/three", d);
        assertValid("/anyObject/three/tres", d);
        assertValid("/anyObject/three/tres[text()='trios']", d);
    }

    public void testAnyObjecttArray() throws Exception {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");

        JAXBContext ctx = JAXBContextImpl.newInstance(AnyObjectArray.class, Three.class);

        StreamSource source = new StreamSource(getClass().getResourceAsStream("anyElement.xml"));
        JAXBElement<AnyObjectArray> jaxbAnyObjectArray = ctx.createUnmarshaller().unmarshal(source, AnyObjectArray.class);
        assertNotNull("jaxbAnyObjectArray is null", jaxbAnyObjectArray);
        assertNotNull("jaxbAnyObjectArray.getValue() is null", jaxbAnyObjectArray.getValue());

        AnyObjectArray anyObjectArray = jaxbAnyObjectArray.getValue();
        assertNotNull("anyObjectArray is null", anyObjectArray);
        assertNotNull("anyObjectArray.getObjects() is null", anyObjectArray.getObjects());
        Object[] objects = anyObjectArray.getObjects();
        assertEquals(3, objects.length);

        assertTrue("objects[0] should be an instance of Element", objects[0] instanceof Element);
        Element one = (Element)objects[0];
        assertEquals("one", one.getLocalName());
        assertTrue("objects[1] should be an instance of Element", objects[1] instanceof Element);
        Element two = (Element)objects[1];
        assertEquals("two", two.getLocalName());
        assertTrue(two.getChildNodes().getLength() > 0);
        assertTrue("objects[2] should be an instance of Three", objects[2] instanceof Three);
        Three three = (Three)objects[2];
        assertEquals("trios", three.getTres());

        Marshaller marshaller = ctx.createMarshaller();
        assertNotNull(marshaller);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(anyObjectArray, bos);

        Document d = readDocument(bos.toByteArray());
        assertValid("/anyObjectArray", d);
        assertValid("/anyObjectArray/one", d);
        assertValid("/anyObjectArray/one/uno", d);
        assertValid("/anyObjectArray/one/uno[text()='un']", d);
        assertValid("/anyObjectArray/two", d);
        assertValid("/anyObjectArray/two/dos", d);
        assertValid("/anyObjectArray/two/dos[text()='deux']", d);
        assertValid("/anyObjectArray/three", d);
        assertValid("/anyObjectArray/three/tres", d);
        assertValid("/anyObjectArray/three/tres[text()='trios']", d);
    }

    public void testAnyObjectList() throws Exception {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");

        JAXBContext ctx = JAXBContextImpl.newInstance(AnyObjectList.class, Three.class);

        StreamSource source = new StreamSource(getClass().getResourceAsStream("anyElement.xml"));
        JAXBElement<AnyObjectList> jaxbAnyObjectList = ctx.createUnmarshaller().unmarshal(source, AnyObjectList.class);
        assertNotNull("jaxbAnyObjectList is null", jaxbAnyObjectList);
        assertNotNull("jaxbAnyObjectList.getValue() is null", jaxbAnyObjectList.getValue());

        AnyObjectList anyObjectList = jaxbAnyObjectList.getValue();
        assertNotNull("anyObjectList is null", anyObjectList);
        assertNotNull("anyObjectList.getObjects() is null", anyObjectList.getObjects());
        List<Object> objects = anyObjectList.getObjects();
        assertEquals(3, objects.size());

        assertTrue("objects.get(0) should be an instance of Element", objects.get(0) instanceof Element);
        Element one = (Element)objects.get(0);
        assertEquals("one", one.getLocalName());
        assertTrue("objects.get(1) should be an instance of Element", objects.get(1) instanceof Element);
        Element two = (Element)objects.get(1);
        assertEquals("two", two.getLocalName());
        assertTrue(two.getChildNodes().getLength() > 0);
        assertTrue("objects.get(2) should be an instance of Three", objects.get(2) instanceof Three);
        Three three = (Three)objects.get(2);
        assertEquals("trios", three.getTres());

        Marshaller marshaller = ctx.createMarshaller();
        assertNotNull(marshaller);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(anyObjectList, bos);

        Document d = readDocument(bos.toByteArray());
        assertValid("/anyObjectList", d);
        assertValid("/anyObjectList/one", d);
        assertValid("/anyObjectList/one/uno", d);
        assertValid("/anyObjectList/one/uno[text()='un']", d);
        assertValid("/anyObjectList/two", d);
        assertValid("/anyObjectList/two/dos", d);
        assertValid("/anyObjectList/two/dos[text()='deux']", d);
        assertValid("/anyObjectList/three", d);
        assertValid("/anyObjectList/three/tres", d);
        assertValid("/anyObjectList/three/tres[text()='trios']", d);
    }

    public void testAnyAttribute() throws Exception {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");

        JAXBContext ctx = JAXBContextImpl.newInstance(AnyAttribute.class);

        StreamSource source = new StreamSource(getClass().getResourceAsStream("anyAttribute.xml"));
        JAXBElement<AnyAttribute> jaxbAnyAttribute = ctx.createUnmarshaller().unmarshal(source, AnyAttribute.class);
        assertNotNull("jaxbAnyAttribute is null", jaxbAnyAttribute);
        assertNotNull("jaxbAnyAttribute.getValue() is null", jaxbAnyAttribute.getValue());

        AnyAttribute anyAttribute = jaxbAnyAttribute.getValue();
        assertNotNull("anyAttribute is null", anyAttribute);

        assertEquals("uno", anyAttribute.getOne());
        assertEquals("dos", anyAttribute.getTwo());
        assertEquals("tres", anyAttribute.getThree());

        assertNotNull("anyAttribute.getAttributes() is null", anyAttribute.getAttributes());
        Map<QName,String> attributes = anyAttribute.getAttributes();
        assertEquals("cuatro", attributes.get(new QName("", "four")));
        assertEquals("un", attributes.get(new QName("urn:french", "one")));
        assertEquals("deux", attributes.get(new QName("urn:french", "two")));
        assertEquals("trios", attributes.get(new QName("urn:french", "three")));
        assertEquals("spanish", attributes.get(new QName(XMLConstants.XML_NS_URI, "lang")));

        Marshaller marshaller = ctx.createMarshaller();
        assertNotNull(marshaller);
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(anyAttribute, bos);

        System.out.println(new String(bos.toByteArray()));

        Document d = readDocument(bos.toByteArray());
        addNamespace("f", "urn:french");
        addNamespace("xml", XMLConstants.XML_NS_URI);
        assertValid("/anyAttribute", d);
        assertValid("/anyAttribute[@one='uno']", d);
        assertValid("/anyAttribute[@two='dos']", d);
        assertValid("/anyAttribute[@three='tres']", d);
        assertValid("/anyAttribute[@four='cuatro']", d);
        assertValid("/anyAttribute[@f:one='un']", d);
        assertValid("/anyAttribute[@f:two='deux']", d);
        assertValid("/anyAttribute[@f:three='trios']", d);
        assertValid("/anyAttribute[@xml:lang='spanish']", d);
    }
}