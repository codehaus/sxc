package com.envoisolutions.sxc;

import java.io.InputStream;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import com.ctc.wstx.stax.WstxInputFactory;
import com.envoisolutions.sxc.util.XoXMLStreamReader;
import com.envoisolutions.sxc.util.XoXMLStreamReaderImpl;

public abstract class Reader {
    protected Context context;
    XMLInputFactory xif = new WstxInputFactory();

    protected Reader(Context context) {
        this.context = context;
    }

    public Object read(InputStream is, Map<String,Object> properties) throws Exception {
        XMLStreamReader r = xif.createXMLStreamReader(is);
        try {
            return read(r, properties);
        } finally {
            r.close();
        }
    }

    public Object read(InputStream is) throws Exception {
        return read(is,null);
    }
    
    public Object read(XMLStreamReader xsr) throws Exception {
        return read(xsr, null);
    }
    
    public Object read(XMLStreamReader xsr, 
                       Map<String,Object> properties) throws Exception {
        return read(new XoXMLStreamReaderImpl(xsr), properties);
    }
    
    public Object read(XMLStreamReader xsr, 
                       Map<String,Object> properties,
                       QName type) throws Exception {
        return read(new XoXMLStreamReaderImpl(xsr), properties, type);
    }
    
    public abstract Object read(XoXMLStreamReader xsr, 
                                Map<String,Object> properties) throws Exception;
    
    public abstract Object read(XoXMLStreamReader xsr, 
                                Map<String,Object> properties,
                                QName type) throws Exception;
}
