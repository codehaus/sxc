package com.envoisolutions.sxc.jaxb.choice2;

import java.io.ByteArrayOutputStream;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;

import org.w3c.dom.Document;

import com.envoisolutions.sxc.choice2.Choice1;
import com.envoisolutions.sxc.choice2.Parent;
import com.envoisolutions.sxc.jaxb.JAXBContextImpl;
import com.envoisolutions.sxc.util.XoTestCase;
import com.sun.japex.testsuite.TestSuiteElement;

public class ChoiceTest extends XoTestCase {
    
    @SuppressWarnings("unchecked")
    public void testComplexChoices() throws Exception {
        System.setProperty("streax-xo.output.directory", "target/tmp-jaxb");
        JAXBContextImpl ctx = new JAXBContextImpl(Parent.class);
        
        Parent p = (Parent) 
            ctx.createUnmarshaller().unmarshal(getClass().getResourceAsStream("choice1.xml"));
        
        assertNotNull(p);
        
        List<Object> choiceGroup = p.getChoiceGroup();
        assertNotNull(choiceGroup);
        assertEquals(1, choiceGroup.size());
        
        Choice1 c1 = (Choice1) choiceGroup.get(0);
        assertEquals("bar", c1.getFoo());
    }
}
