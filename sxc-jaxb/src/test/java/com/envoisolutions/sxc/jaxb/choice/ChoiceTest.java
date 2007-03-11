package com.envoisolutions.sxc.jaxb.choice;

import java.io.ByteArrayOutputStream;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;

import org.w3c.dom.Document;

import com.envoisolutions.node.NamedNode;
import com.envoisolutions.node.Node;
import com.envoisolutions.sxc.choice.Parent;
import com.envoisolutions.sxc.jaxb.JAXBContextImpl;
import com.envoisolutions.sxc.util.XoTestCase;
import com.sun.japex.testsuite.TestSuiteElement;

public class ChoiceTest extends XoTestCase {
    
    @SuppressWarnings("unchecked")
    public void testChoices() throws Exception {
        System.setProperty("streax-xo.output.directory", "target/tmp-jaxb");
        JAXBContextImpl ctx = new JAXBContextImpl(Parent.class);
        
        Parent p = (Parent) 
            ctx.createUnmarshaller().unmarshal(getClass().getResourceAsStream("choice1.xml"));
        
        assertNotNull(p);
        
        List<JAXBElement<String>> choiceGroup = p.getChoiceGroup();
        assertNotNull(choiceGroup);
        assertEquals(1, choiceGroup.size());
        
        JAXBElement<String> name = choiceGroup.get(0);
        assertEquals("choice1", name.getName().getLocalPart());
        assertEquals("foo", name.getValue());
    }
}
