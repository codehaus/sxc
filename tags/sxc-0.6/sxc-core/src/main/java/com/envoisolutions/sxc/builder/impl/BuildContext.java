package com.envoisolutions.sxc.builder.impl;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

public class BuildContext {
    private JCodeModel model = new JCodeModel();
    private Map<QName, ElementParserBuilderImpl> globalElements = new HashMap<QName, ElementParserBuilderImpl>();
    
    private int readCount;
    private int writeCount;

    /**
     * {@code Map<String,Object>} type.
     */
    private JClass stringToObjectMap = model.ref(Map.class).narrow(String.class,Object.class);
    
    public JCodeModel getCodeModel() {
        return model;
    }

    JClass getStringToObjectMap() {
        return stringToObjectMap;
    }

    JMethod getNextReadMethod(JDefinedClass contextClass) {
        readCount++;
        return contextClass.method(JMod.PUBLIC | JMod.FINAL, void.class, "read" + readCount);
    }


    JMethod getNextWriteMethod(JDefinedClass contextClass) {
        writeCount++;
        return contextClass.method(JMod.PUBLIC | JMod.FINAL, void.class, "write" + writeCount);
    }
    
    public Map<QName, ElementParserBuilderImpl> getGlobalElements() {
        return globalElements;
    }

}
