package com.envoisolutions.sxc.jaxb.attachment;

import com.envoisolutions.sxc.jaxb.JAXBContextImpl;
import com.envoisolutions.sxc.picture.BinaryData;
import com.envoisolutions.sxc.util.XoTestCase;

public class AttachmentTest extends XoTestCase {
    
    @SuppressWarnings("unchecked")
    public void testChoices() throws Exception {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");
        JAXBContextImpl ctx = new JAXBContextImpl(BinaryData.class);
        
        BinaryData b = (BinaryData) 
            ctx.createUnmarshaller().unmarshal(getClass().getResourceAsStream("xop.xml"));
        
//        assertNotNull(b);
        
//
//        DataHandler octetStream = b.getOctetStream();
//        assertNotNull(octetStream);
    }
}
