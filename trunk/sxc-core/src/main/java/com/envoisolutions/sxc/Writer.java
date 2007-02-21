package com.envoisolutions.sxc;

import java.io.OutputStream;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import com.ctc.wstx.stax.WstxOutputFactory;
import com.envoisolutions.sxc.util.XoXMLStreamWriter;
import com.envoisolutions.sxc.util.XoXMLStreamWriterImpl;

public abstract class Writer {
    Context context;
    XMLOutputFactory xof = new WstxOutputFactory();
    
    public Writer(Context context) {
        this.context = context;
    }

    public void write(OutputStream is, Object o) throws Exception {
        XMLStreamWriter w = xof.createXMLStreamWriter(is);
        write(w, o);
        w.close();
    }
    
    public void write(XMLStreamWriter xsr, Object o) throws Exception {
        write(new XoXMLStreamWriterImpl(xsr), o);
    }
    
    public abstract void write(XoXMLStreamWriter xw, Object o) throws Exception;
}
