package com.envoisolutions.sxc.builder.impl;

import javax.xml.namespace.QName;

import com.envoisolutions.sxc.builder.WriterBuilder;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JVar;

public class AttributeWriterBuilder extends AbstractWriterBuilder implements WriterBuilder {

    public AttributeWriterBuilder(ElementWriterBuilderImpl parent, 
                                  QName name, JMethod method, JVar objectVar) {
        this.buildContext = parent.buildContext;
        this.method = method;
        this.objectVar = objectVar;
        this.parent = parent;
        this.name = name;
        this.xswVar = parent.xswVar;
        this.rtContextVar = parent.rtContextVar;
        this.writerClass = parent.writerClass;
        this.model = parent.model;
        currentBlock = method.body();
    }
    
    public void writeAs(Class cls) {
        JBlock block = currentBlock._if(objectVar.ne(JExpr._null()))._then();
        
        if (cls.equals(String.class)) {
            writeAs(block, objectVar);
        } else if (cls.equals(int.class) || cls.equals(Integer.class)) {
            JClass jc = (JClass) model._ref(Integer.class);
            writeAs(block, jc.staticInvoke("toString").arg(objectVar));
        } else if (cls.equals(boolean.class) || cls.equals(Boolean.class)) {
            JClass jc = (JClass) model._ref(Boolean.class);
            writeAs(block, jc.staticInvoke("toString").arg(objectVar));
        } else if (cls.equals(short.class) || cls.equals(Short.class)) {
            JClass jc = (JClass) model._ref(Short.class);
            writeAs(block, jc.staticInvoke("toString").arg(objectVar));
        } else if (cls.equals(double.class) || cls.equals(Double.class)) {
            JClass jc = (JClass) model._ref(Double.class);
            writeAs(block, jc.staticInvoke("toString").arg(objectVar));
        } else if (cls.equals(long.class) || cls.equals(Long.class)) {
            JClass jc = (JClass) model._ref(Long.class);
            writeAs(block, jc.staticInvoke("toString").arg(objectVar));
        } else if (cls.equals(float.class) || cls.equals(Float.class)) {
            JClass jc = (JClass) model._ref(Float.class);
            writeAs(block, jc.staticInvoke("toString").arg(objectVar));
        } else if (cls.equals(byte.class) || cls.equals(Byte.class)) {
            JClass jc = (JClass) model._ref(Byte.class);
            writeAs(block, jc.staticInvoke("toString").arg(objectVar));
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private void writeAs(JBlock block, JExpression exp) {
        block.add(xswVar.invoke("writeAttribute")
                  .arg(JExpr.lit(name.getPrefix()))
                  .arg(JExpr.lit(name.getNamespaceURI()))
                  .arg(JExpr.lit(name.getLocalPart()))
                  .arg(exp));
    }
}
