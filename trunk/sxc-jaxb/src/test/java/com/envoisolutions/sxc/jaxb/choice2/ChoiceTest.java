package com.envoisolutions.sxc.jaxb.choice2;

import java.util.List;

import com.envoisolutions.sxc.choice2.Choice1;
import com.envoisolutions.sxc.choice2.Parent;
import com.envoisolutions.sxc.jaxb.JAXBContextImpl;
import com.envoisolutions.sxc.util.XoTestCase;

public class ChoiceTest extends XoTestCase {
    
    @SuppressWarnings("unchecked")
    public void testComplexChoices() throws Exception {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");
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
