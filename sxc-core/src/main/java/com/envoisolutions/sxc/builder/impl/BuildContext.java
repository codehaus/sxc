package com.envoisolutions.sxc.builder.impl;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;

public class BuildContext {
    private JCodeModel model = new JCodeModel();
    private Map<QName, ElementParserBuilderImpl> globalElements = new HashMap<QName, ElementParserBuilderImpl>();
    
    private int readCount;
    private int writeCount;
    
    public JCodeModel getCodeModel() {
        return model;
    }

    JMethod getNextReadMethod(JDefinedClass contextClass) {
        readCount++;
        return contextClass.method(JMod.PUBLIC | JMod.FINAL | JMod.STATIC, void.class, "read" + readCount);
    }


    JMethod getNextWriteMethod(JDefinedClass contextClass) {
        writeCount++;
        return contextClass.method(JMod.PUBLIC | JMod.FINAL, void.class, "write" + writeCount);
    }
    
    public Map<QName, ElementParserBuilderImpl> getGlobalElements() {
        return globalElements;
    }

}
