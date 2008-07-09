package com.envoisolutions.sxc.builder.impl;

import com.envoisolutions.sxc.builder.WriterBuilder;
import com.envoisolutions.sxc.util.XoXMLStreamWriter;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JVar;
import com.sun.codemodel.JType;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractWriterBuilder implements WriterBuilder {

    protected BuildContext buildContext;
    protected JCodeModel model;
    protected JDefinedClass writerClass;
    protected JMethod method;
    protected JVar xswVar;
    protected JVar rtContextVar;
    protected JVar objectVar;
    protected JBlock currentBlock;
    protected ElementWriterBuilderImpl parent;
    protected QName name;
    protected List<Class> exceptions = new ArrayList<Class>();

    protected final IdentityManager variableManager = new IdentityManager();

    public AbstractWriterBuilder() {
    }

    public void declareException(Class cls) {
		exceptions.add(cls);
		method._throws(cls);
	}

	protected JVar addBasicArgs(JMethod method, JType sourceObjectType, String sourceVariableName) {
        xswVar = method.param(XoXMLStreamWriter.class, variableManager.createId("writer"));
        String rtContextName = variableManager.createId("context");
        JVar var = method.param(sourceObjectType, variableManager.createId(sourceVariableName));
        rtContextVar = method.param(buildContext.getMarshalContextClass(), rtContextName);

        for (Class c : exceptions) {
        	method._throws(c);
        }

        return var;
    }

    public JCodeModel getCodeModel() {
        return model;
    }

    public void moveTo(WriterBuilder builder) {
        currentBlock.add(
            JExpr._this().invoke(((AbstractWriterBuilder)builder).method)
                .arg(xswVar).arg(JExpr.cast(objectVar.type(), objectVar)).arg(rtContextVar));
    }

    protected String getGetter(String name) {
        if (name.startsWith("_")) 
            name = name.substring(1);
        
        return "get" + name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public JVar getObject() {
        return objectVar;
    }

    public void setObject(JVar var) {
        this.objectVar = var;
    }

    public JVar getXSW() {
        return xswVar;
    }

    public JVar getContextVar() {
        return rtContextVar;
    }

    public JMethod getMethod() {
        return method;
    }

    public JDefinedClass getWriterClass() {
        return writerClass;
    }

    public JBlock getCurrentBlock() {
        return currentBlock;
    }

    public void setCurrentBlock(JBlock currentBlock) {
        this.currentBlock = currentBlock;
    }

    public QName getName() {
        return name;
    }

    public WriterBuilder getParent() {
        return parent;
    }

    public IdentityManager getVariableManager() {
        return variableManager;
    }
}
