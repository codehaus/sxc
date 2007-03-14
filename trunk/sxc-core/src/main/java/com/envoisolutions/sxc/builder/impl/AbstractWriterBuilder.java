package com.envoisolutions.sxc.builder.impl;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import com.envoisolutions.sxc.Context;
import com.envoisolutions.sxc.builder.WriterBuilder;
import com.envoisolutions.sxc.util.XoXMLStreamWriter;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JVar;

public class AbstractWriterBuilder {

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

    public AbstractWriterBuilder() {
        super();
    }

    protected String getContextClassName() {
        return "streax.generated.Writer";
    }

    protected void addBasicArgs(JMethod method) {
        xswVar = method.param(XoXMLStreamWriter.class, "writer");
        rtContextVar = method.param(Context.class, "context");
    
        method._throws(XMLStreamException.class);
    }

    public JCodeModel getCodeModel() {
        return model;
    }

    public void moveTo(WriterBuilder builder) {
        currentBlock.add(
            JExpr._this().invoke(((AbstractWriterBuilder)builder).method)
                .arg(xswVar).arg(rtContextVar).arg(JExpr.cast(objectVar.type(), objectVar)));
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

}
