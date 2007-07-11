package com.envoisolutions.sxc.jaxb.testsuite;

import com.envoisolutions.sxc.jaxb.JAXBContextImpl;
import com.envoisolutions.sxc.util.XoTestCase;
import com.sun.japex.testsuite.TestSuiteElement;

public class TestSuiteTest extends XoTestCase {
    
    @SuppressWarnings("unchecked")
    public void testJapexSchema() throws Exception {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");
        JAXBContextImpl ctx = new JAXBContextImpl(TestSuiteElement.class);
        
        TestSuiteElement test = (TestSuiteElement)
            ctx.createUnmarshaller().unmarshal(getClass().getResourceAsStream("suite.xml"));
        
        assertNotNull(test);
    }
}
