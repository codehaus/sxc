package com.envoisolutions.sxc.jaxb.testsuite;

import java.io.ByteArrayOutputStream;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;

import org.w3c.dom.Document;

import com.envoisolutions.node.NamedNode;
import com.envoisolutions.node.Node;
import com.envoisolutions.sxc.choice.Parent;
import com.envoisolutions.sxc.jaxb.JAXBContextImpl;
import com.envoisolutions.sxc.util.XoTestCase;
import com.sun.japex.testsuite.TestSuiteElement;

public class TestSuiteTest extends XoTestCase {
    
    @SuppressWarnings("unchecked")
    public void testJapexSchema() throws Exception {
        System.setProperty("streax-xo.output.directory", "target/tmp-jaxb");
        JAXBContextImpl ctx = new JAXBContextImpl(TestSuiteElement.class);
        
        TestSuiteElement test = (TestSuiteElement)
            ctx.createUnmarshaller().unmarshal(getClass().getResourceAsStream("suite.xml"));
        
        assertNotNull(test);
    }
}
