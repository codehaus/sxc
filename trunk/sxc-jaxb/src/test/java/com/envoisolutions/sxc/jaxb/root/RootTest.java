package com.envoisolutions.sxc.jaxb.root;

import java.io.ByteArrayOutputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.UnmarshalException;

import com.envoisolutions.node.NamedNode;
import com.envoisolutions.sxc.util.XoTestCase;
import org.w3c.dom.Document;

public class RootTest extends XoTestCase {
    public void setUp() throws Exception {
        super.setUp();
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");
    }

    /**
     * NoRoot has no element declaration so xsi:type must be used for JaxB to recognize the type.
     */
    public void testNoRoot() throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(NoRoot.class);

        JAXBElement<NoRoot> element = asJAXBElement(ctx.createUnmarshaller().unmarshal(getClass().getResource("noRoot.xml")));

        assertNotNull(element);
        NoRoot noRoot = element.getValue();
        assertEquals("no root", noRoot.getName());

        Marshaller marshaller = ctx.createMarshaller();
        assertNotNull(marshaller);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(element, bos);

        System.out.println(bos.toString());

        Document d = readDocument(bos.toByteArray());
        addNamespace("n", "http://envoisolutions.com/root");
        addNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        assertValid("/n:NoRoot", d);
        assertValid("/n:NoRoot[@xsi:type='NoRootType']", d);
        assertValid("/n:NoRoot/n:name", d);
        assertValid("/n:NoRoot/n:name[text()='no root']", d);
    }

    /**
     * AnnotatedRoot has @XmlRootElement annotation
     */
    public void testAnnotatedRoot() throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(AnnotatedRoot.class);

        AnnotatedRoot annotatedRoot = (AnnotatedRoot) ctx.createUnmarshaller().unmarshal(getClass().getResource("annotatedRoot.xml"));

        assertEquals("annotated root", annotatedRoot.getName());

        Marshaller marshaller = ctx.createMarshaller();
        assertNotNull(marshaller);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(annotatedRoot, bos);

        System.out.println(bos.toString());

        Document d = readDocument(bos.toByteArray());
        addNamespace("n", "http://envoisolutions.com/root");
        assertValid("/n:annotated-root", d);
        assertValid("/n:annotated-root/n:name", d);
        assertValid("/n:annotated-root/n:name[text()='annotated root']", d);
    }

    /**
     * ObjectFactoryRoot has @XmlElementDecl annotatated method in ObjectFactory.
     * Test is initalized with packakage name.
     */
    public void testObjectFactoryRootPackage() throws Exception {
        JAXBContext ctx = JAXBContext.newInstance("com.envoisolutions.sxc.jaxb.root");
        testObjectFactoryRootContext(ctx, "object-factory-root", "objectFactoryRoot.xml");
    }

    /**
     * ObjectFactoryRoot has @XmlElementDecl annotatated method in ObjectFactory.
     * Test is initalized with ObjectFactory class.
     */
    public void testObjectFactoryRootObjectFactory() throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(ObjectFactory.class);
        testObjectFactoryRootContext(ctx, "object-factory-root", "objectFactoryRoot.xml");
    }

    /**
     * ObjectFactoryRoot has @XmlElementDecl annotatated method in ObjectFactory.
     * Test is initalized with ObjectFactoryRoot class which does not have @XmlRootElement annotation.
     * Since the JAXBContext does not load the package ObjectFactory, the ObjectFactoryRoot class
     * has no root elements and test should throw an exception.
     */
    public void testObjectFactoryRootClass() throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(ObjectFactoryRoot.class);
        try {
            testObjectFactoryRootContext(ctx, "object-factory-root", "objectFactoryRoot.xml");
            fail("excpected UnmarshalException");
        } catch (UnmarshalException expected) {
            // expected
        }
    }

    /**
     * Alternate root element name.
     * Test is initalized with packakage name.
     */
    public void testAlternateRootNamePackage() throws Exception {
        JAXBContext ctx = JAXBContext.newInstance("com.envoisolutions.sxc.jaxb.root");
        testObjectFactoryRootContext(ctx, "alternate-root-name", "alternateRootName.xml");
    }

    /**
     * Alternate root element name
     * Test is initalized with ObjectFactory class.
     */
    public void testAlternateRootNameObjectFactory() throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(ObjectFactory.class);
        testObjectFactoryRootContext(ctx, "alternate-root-name", "alternateRootName.xml");
    }

    private void testObjectFactoryRootContext(JAXBContext ctx, String rootName, String fileName) throws Exception {
        JAXBElement<ObjectFactoryRoot> element = asJAXBElement(ctx.createUnmarshaller().unmarshal(getClass().getResource(fileName)));

        assertNotNull(element);
        ObjectFactoryRoot objectFactoryRoot = element.getValue();
        assertEquals(rootName.replace('-', ' '), objectFactoryRoot.getName());

        Marshaller marshaller = ctx.createMarshaller();
        assertNotNull(marshaller);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(element, bos);

        System.out.println(bos.toString());

        Document d = readDocument(bos.toByteArray());
        addNamespace("n", "http://envoisolutions.com/root");
        String rootPath = "/n:" + rootName;
        assertValid(rootPath, d);
        assertValid(rootPath + "/n:name", d);
        assertValid(rootPath +"/n:name[text()='" + rootName.replace('-', ' ') + "']", d);
    }

    /**
     * Root element from another package
     */
    public void testExternalRoot() throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(ObjectFactory.class);

        JAXBElement<NamedNode> element = asJAXBElement(ctx.createUnmarshaller().unmarshal(getClass().getResource("externalRoot.xml")));

        assertNotNull(element);
        NamedNode namedNode = element.getValue();
        assertEquals("external root", namedNode.getName());

        Marshaller marshaller = ctx.createMarshaller();
        assertNotNull(marshaller);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(element, bos);

        System.out.println(bos.toString());

        Document d = readDocument(bos.toByteArray());
        addNamespace("n", "http://envoisolutions.com/node");
        assertValid("/n:external-root", d);
        assertValid("/n:external-root/n:name", d);
        assertValid("/n:external-root/n:name[text()='external root']", d);
    }

    @SuppressWarnings("unchecked")
    private <T> JAXBElement<T> asJAXBElement(Object object) {
        assertTrue("object should be an instance of JAXBElement", object instanceof JAXBElement);
        return (JAXBElement<T>) object;
    }
}