package com.envoisolutions.sxc.builder.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import com.envoisolutions.sxc.Context;
import com.envoisolutions.sxc.builder.BuildException;
import com.envoisolutions.sxc.builder.CodeBody;
import com.envoisolutions.sxc.builder.ParserBuilder;
import com.envoisolutions.sxc.util.XoXMLStreamReader;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;

public abstract class AbstractParserBuilder implements ParserBuilder {

    Map<String, Prop> properties = new HashMap<String, Prop>();
    
    boolean allowUnknown = true;
    BuildContext buildContext;
    List<JVar> variables = new ArrayList<JVar>();
    JDefinedClass readerClass;
    JMethod method;
    JVar rtContextVar;
    JVar xsrVar;
    JCodeModel model;
    JExpression _return;
    boolean written;
    boolean required;
    JType returnType;
    JBlock codeBlock = new JBlock();
    
    static class Prop {
        Class type;
        boolean nillable;
    }

    public AbstractParserBuilder() throws BuildException {
    }

    public JVar getXSR() {
        return xsrVar;
    }


    public void setAllowUnkown(boolean allow) {
        allow = true;
    }

    public JVar asInteger(int min, int max) {
        return null;
    }
    
    public void mapAsProperty(String name, Class type, boolean nillable) {
        Prop prop = new Prop();
        prop.type = type;
        prop.nillable = nillable;
        
        properties.put(name, prop);
    }

    public CodeBody getBody() {
        return new CodeBodyImpl(this);
    }

    protected abstract void write();
    
    public JDefinedClass getReaderClass() {
        return readerClass;
    }

    protected BuildContext getBuildContext() {
        return buildContext;
    }

    public JCodeModel getCodeModel() {
        return model;
    }

    protected void addBasicArgs(JMethod method) {
        xsrVar = method.param(XoXMLStreamReader.class, "reader");
        rtContextVar = method.param(Context.class, "context");

        method._throws(XMLStreamException.class);
    }
    
    
    public JVar passParentVariable(JVar parentVar) {
        variables.add(parentVar);
        String name = "parent_" + parentVar.name();
//        if (!name.startsWith("_p_")) {
//            name = "_p_" + name;
//        }
        return method.param(parentVar.type(), name);
    }

    public List<JVar> getVariables() {
        return variables;
    }

    JMethod getMethod() {
        return method;
    }

    void setReturnVar(JVar var) {
        this._return = var;
    }

    public void setReturnVar(JType type, JExpression ex) {
        this._return = ex;
        this.returnType = type;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }
    
}
