package com.envoisolutions.sxc.performance;

import javax.xml.bind.JAXBException;

import com.sun.japex.TestCase;
import com.sun.xml.bind.v2.ContextFactory;

public class JAXBRIDriver extends AbstractDriver {

    @Override
    protected void createContext(TestCase tc) {
        try {
            context = ContextFactory.createContext(tc.getParam("jaxbPackage"), 
                                                   getClass().getClassLoader(), null);
        } catch (JAXBException e1) {
            throw new RuntimeException(e1);
        }
    }

}
