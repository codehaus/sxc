package com.envoisolutions.sxc.performance;

import com.envoisolutions.sxc.jaxb.JAXBContextImpl;
import com.sun.japex.TestCase;

public class SXCDriver extends AbstractDriver {

   
    @Override
    protected void createContext(TestCase tc) {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");
        
        try {
            context = JAXBContextImpl.createContext(tc.getParam("jaxbPackage"), 
                                                    Thread.currentThread().getContextClassLoader(), null);
        } catch (Exception e1) {
            System.err.println(e1.getMessage());
            e1.printStackTrace();
        } 
        
    }
}
