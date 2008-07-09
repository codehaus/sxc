package com.envoisolutions.sxc.performance;

import java.io.ByteArrayOutputStream;

import junit.framework.TestCase;

import com.envoisolutions.caserta.IntCollection;
import com.envoisolutions.caserta.SimpleTypes;
import com.envoisolutions.caserta.StringCollection;
import com.envoisolutions.sxc.jaxb.JAXBContextImpl;

public class WriteTest extends TestCase {
    public void testWrite() throws Exception {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");
        JAXBContextImpl impl = new JAXBContextImpl(IntCollection.class, StringCollection.class, SimpleTypes.class);
        
        IntCollection c = new IntCollection();
    
        c.getInt().add(new Integer(1));
        
        impl.createMarshaller().marshal(c, new ByteArrayOutputStream());
    }
}
