package com.envoisolutions.sxc.jaxb.choice;

import java.util.List;

import javax.xml.bind.JAXBElement;

import com.envoisolutions.sxc.choice.Parent;
import com.envoisolutions.sxc.jaxb.JAXBContextImpl;
import com.envoisolutions.sxc.util.XoTestCase;

public class ChoiceTest extends XoTestCase {
    
    @SuppressWarnings("unchecked")
    public void testChoices() throws Exception {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");
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
