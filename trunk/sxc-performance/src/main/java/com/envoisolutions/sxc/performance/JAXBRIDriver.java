package com.envoisolutions.sxc.performance;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import com.sun.japex.Constants;
import com.sun.japex.JapexDriver;
import com.sun.japex.JapexDriverBase;
import com.sun.japex.TestCase;
import com.sun.xml.bind.v2.ContextFactory;

import org.apache.commons.io.IOUtils;

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
