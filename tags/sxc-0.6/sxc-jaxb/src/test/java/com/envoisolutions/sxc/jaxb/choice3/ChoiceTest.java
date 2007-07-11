package com.envoisolutions.sxc.jaxb.choice3;

import java.util.List;

import com.envoisolutions.sxc.choice3.Parent;
import com.envoisolutions.sxc.jaxb.JAXBContextImpl;
import com.envoisolutions.sxc.util.XoTestCase;

public class ChoiceTest extends XoTestCase {
    
    @SuppressWarnings("unchecked")
    public void testCircular() throws Exception {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");
        JAXBContextImpl ctx = new JAXBContextImpl(Parent.class);
        
        Parent p = (Parent) 
            ctx.createUnmarshaller().unmarshal(getClass().getResourceAsStream("choice1.xml"));
        
        assertNotNull(p);
        
        List<Object> choiceGroup = p.getChoiceGroup();
        assertNotNull(choiceGroup);
        assertEquals(1, choiceGroup.size());
        
        p = (Parent) choiceGroup.get(0);
        
        choiceGroup = p.getChoiceGroup();
        assertNotNull(choiceGroup);
        assertEquals(1, choiceGroup.size());
    }
}
