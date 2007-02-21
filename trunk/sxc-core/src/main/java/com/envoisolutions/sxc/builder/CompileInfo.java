package com.envoisolutions.sxc.builder;

import java.lang.reflect.Method;

public class CompileInfo {
    private Class reader;
    private Class writer;
    private Method readMethod;
    private Method writeMethod;
    
    public Class getReader() {
        return reader;
    }
    
    public void setReader(Class reader) {
        this.reader = reader;
    }
    
    public Method getReadMethod() {
        return readMethod;
    }
    
    public void setReadMethod(Method readMethod) {
        this.readMethod = readMethod;
    }
    
    public Method getWriteMethod() {
        return writeMethod;
    }
    
    public void setWriteMethod(Method writeMethod) {
        this.writeMethod = writeMethod;
    }
    
    public Class getWriter() {
        return writer;
    }
    
    public void setWriter(Class writer) {
        this.writer = writer;
    }
}
