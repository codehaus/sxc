package com.envoisolutions.sxc.builder.impl;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

import com.envoisolutions.sxc.builder.impl.ElementParserBuilderImpl.ExpectedElement;

public class BuildContext {
    private JCodeModel model = new JCodeModel();
    private Map<QName, ExpectedElement> globalElements = new HashMap<QName, ExpectedElement>();
    private final IdentityManager methodManager = new IdentityManager();

    private JClass marshalContextClass = model.ref(Map.class).narrow(String.class,Object.class);
    private JClass unmarshalContextClass = model.ref(Map.class).narrow(String.class,Object.class);

    public BuildContext() {
        methodManager.addId("read");
    }

    public JCodeModel getCodeModel() {
        return model;
    }

    public JClass getMarshalContextClass() {
        return marshalContextClass;
    }

    public void setMarshalContextClass(JClass marshalContextClass) {
        this.marshalContextClass = marshalContextClass;
    }

    public JClass getUnmarshalContextClass() {
        return unmarshalContextClass;
    }

    public void setUnmarshalContextClass(JClass unmarshalContextClass) {
        this.unmarshalContextClass = unmarshalContextClass;
    }

    JMethod getNextReadMethod(JDefinedClass contextClass) {
        return createMethod(contextClass, "read");
    }

    JMethod getNextWriteMethod(JDefinedClass contextClass) {
        return createMethod(contextClass, "write");
    }

    JMethod createMethod(JDefinedClass contextClass, String name) {
        return contextClass.method(JMod.PUBLIC | JMod.FINAL, void.class, methodManager.createId(name));
    }

    public Map<QName, ExpectedElement> getGlobalElements() {
        return globalElements;
    }

}
