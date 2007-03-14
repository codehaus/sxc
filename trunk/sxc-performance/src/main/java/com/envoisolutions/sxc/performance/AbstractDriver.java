package com.envoisolutions.sxc.performance;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

import com.sun.japex.JapexDriverBase;
import com.sun.japex.TestCase;

public abstract class AbstractDriver extends JapexDriverBase {

    byte[] bytes;
    double size;
    String pckg;
    JAXBContext context;
    XMLInputFactory xif = XMLInputFactory.newInstance();
    XMLOutputFactory xof = XMLOutputFactory.newInstance();
    
    boolean write = true;
    private Object object;
    private Unmarshaller unmarshaller;
    private Marshaller marshaller;
//    
//    public void finish(TestCase testCase) {
//        testCase.setParam(Constants.RESULT_UNIT, "KBs");
//        testCase.setDoubleParam(Constants.RESULT_VALUE, 
//                                (size / 1024.0));
//    }
    
    public void prepare(TestCase tc) {
        pckg = tc.getParam("package");
        write = tc.getBooleanParam("write");
        createContext(tc);
        
        String xifName = getParam("xmlInputFactory");
        if (xifName != null) {
            try {
                xif = (XMLInputFactory)getClass().getClassLoader().loadClass(xifName).newInstance();
            } catch (Exception e) {
                System.err.println("Could not load XMLInputFactory: " + e.getClass().getName());
            }
        }
        
        String xofName = getParam("xmlOutputFactory");
        if (xofName != null) {
            try {
                xof = (XMLOutputFactory)getClass().getClassLoader().loadClass(xofName).newInstance();
            } catch (Exception e) {
                System.err.println("Could not load XMLOutputFactory: " + e.getClass().getName());
            }
        }
        
        String fn = tc.getParam("inputFile");
        
        try {
            File f = new File(fn);
            if (!f.exists()) {
                throw new RuntimeException("file does not exist!");
            }
            size = f.length();
            
            FileInputStream stream = new FileInputStream(f);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            org.jfree.io.IOUtils.getInstance().copyStreams(stream, bos);
            stream.close();
            bos.close();
            
            bytes = bos.toByteArray();
            
            
        } catch (IOException e) {
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }
        
        try {
            if (write) {
                    object = context.createUnmarshaller().unmarshal(new ByteArrayInputStream(bytes));
            }
            
            unmarshaller = context.createUnmarshaller();
            marshaller = context.createMarshaller();
            
            String schema = tc.getParam("schema");
            if (schema != null) {
                SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                Schema s = factory.newSchema(new File(schema));
                unmarshaller.setSchema(s);
            }
        } catch (JAXBException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        } catch (SAXException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    protected abstract void createContext(TestCase tc);

    public void run(TestCase tc) {
        try {
            if (!write) {
                XMLStreamReader xsr = xif.createXMLStreamReader(new ByteArrayInputStream(bytes));
                unmarshaller.unmarshal(xsr);
                xsr.close();
            } else {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                XMLStreamWriter writer = xof.createXMLStreamWriter(new BufferedOutputStream(bos));
                marshaller.marshal(object, writer);
                writer.close();
            }
        } catch (JAXBException e) {
            System.err.println("Could not unmarhsal!");
            e.printStackTrace();
        } catch (XMLStreamException e) {
            System.err.println("Could not unmarhsal!");
            e.printStackTrace();
        }
    }


}
