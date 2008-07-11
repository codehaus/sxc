package com.envoisolutions.sxc.builder.impl;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static java.beans.Introspector.decapitalize;

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
    JBlock codeBlock = new JBlock(false, false);

    protected final IdentityManager variableManager = new IdentityManager();

    static class Prop {
        Class type;
        boolean nillable;
    }

    public AbstractParserBuilder() throws BuildException {
    }

    public JVar getXSR() {
        return xsrVar;
    }

    public JVar getContextVar() {
        return rtContextVar;
    }

    public void setAllowUnkown(boolean allow) {
        allowUnknown = allow;
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
        variableManager.addId("reader");
        rtContextVar = method.param(buildContext.getUnmarshalContextClass(), "context");
        variableManager.addId("context");
    }
    
    
    public JVar passParentVariable(JVar parentVar) {
        variables.add(parentVar);
        String name = variableManager.createId(decapitalize(parentVar.type().name()));
        return method.param(parentVar.type(), name);
    }

    public List<JVar> getVariables() {
        return variables;
    }

    public JMethod getMethod() {
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

    public IdentityManager getVariableManager() {
        return variableManager;
    }
}
