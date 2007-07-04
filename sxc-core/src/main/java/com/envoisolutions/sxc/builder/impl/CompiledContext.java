package com.envoisolutions.sxc.builder.impl;

import java.util.Map;

import javax.xml.namespace.QName;

import com.envoisolutions.sxc.Context;
import com.envoisolutions.sxc.Reader;
import com.envoisolutions.sxc.Writer;
import com.envoisolutions.sxc.builder.BuildException;
import com.envoisolutions.sxc.builder.GeneratedReader;
import com.envoisolutions.sxc.builder.GeneratedWriter;
import com.envoisolutions.sxc.util.XoXMLStreamReader;
import com.envoisolutions.sxc.util.XoXMLStreamWriter;

public class CompiledContext extends Context {

    private Reader reader;
    private Writer writer;
    
    public CompiledContext(ClassLoader cl, String readerClsName, String writerClsName) {   
        try {
            final GeneratedReader gr;
            final GeneratedWriter gw;

            if(readerClsName!=null) {
                Class<?> readerCls = cl.loadClass(readerClsName);
                gr = (GeneratedReader) readerCls.newInstance();
            } else {
                gr = null;
            }
            if(writerClsName!=null) {
                Class<?> writerCls = cl.loadClass(writerClsName);
                gw = (GeneratedWriter) writerCls.newInstance();
            } else {
                gw = null;
            }

            final CompiledContext ctx = this;
            this.reader = new Reader(this) {
                @Override
                public Object read(XoXMLStreamReader xsr, Map<String, Object> properties) 
                    throws Exception {
                    return gr.read(xsr, ctx);
                }
                
                public Object read(XoXMLStreamReader xsr,
                                   Map<String, Object> properties,
                                   QName type) 
                    throws Exception {
                    return gr.read(xsr, ctx, type);
                }
            };
            
            this.writer = new Writer(this) {
                @Override
                public void write(XoXMLStreamWriter xw, Object o, Map<String, Object> properties) throws Exception {
                    gw.write(xw, ctx, o);
                }
                
            };
            
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    @Override
    public Reader createReader() {
        return reader;
    }

    @Override
    public Writer createWriter() {
        return writer;
    }

    

}
