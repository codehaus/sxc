package com.envoisolutions.sxc.builder.impl;

import com.envoisolutions.sxc.Context;
import com.envoisolutions.sxc.Reader;
import com.envoisolutions.sxc.Writer;
import com.envoisolutions.sxc.builder.BuildException;

public class CompiledContext extends Context {

    private Reader reader;
    private Writer writer;
    
    public CompiledContext(ClassLoader cl, String readerClsName, String writerClsName) {   
        try {
            if(readerClsName!=null) {
                Class<?> readerCls = cl.loadClass(readerClsName);
                reader = (Reader) readerCls.getConstructor(Context.class).newInstance(this);
            } else {
                reader = null;
            }
            if(writerClsName!=null) {
                Class<?> writerCls = cl.loadClass(writerClsName);
                writer = (Writer) writerCls.getConstructor(Context.class).newInstance(this);
            } else {
                writer = null;
            }
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
