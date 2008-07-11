package com.envoisolutions.sxc.builder.impl;

import java.lang.reflect.Constructor;

import com.envoisolutions.sxc.Context;
import com.envoisolutions.sxc.Reader;
import com.envoisolutions.sxc.Writer;
import com.envoisolutions.sxc.builder.BuildException;

public class CompiledContext extends Context {

    private Constructor<?> readerConstructor;
    private Constructor<?> writerConstructor;
    
    public CompiledContext(ClassLoader cl, String readerClsName, String writerClsName) {   
        try {
            if(readerClsName!=null) {
                Class<?> readerCls = cl.loadClass(readerClsName);
                readerConstructor = readerCls.getConstructor(Context.class);
            }
            if(writerClsName!=null) {
                Class<?> writerCls = cl.loadClass(writerClsName);
                writerConstructor = writerCls.getConstructor(Context.class);
            } 
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    @Override
    public Reader createReader() {
        try {
            return (Reader) readerConstructor.newInstance(this);
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    @Override
    public Writer createWriter() {
        try {
            return (Writer) writerConstructor.newInstance(this);
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    

}
