package com.envoisolutions.sxc.builder.impl;

import static java.beans.Introspector.decapitalize;
import javax.xml.namespace.QName;

import com.envoisolutions.sxc.Context;
import com.envoisolutions.sxc.Writer;
import com.envoisolutions.sxc.builder.BuildException;
import com.envoisolutions.sxc.builder.ElementWriterBuilder;
import com.envoisolutions.sxc.builder.WriterBuilder;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;

public class ElementWriterBuilderImpl extends AbstractWriterBuilder implements ElementWriterBuilder {

    private final JIfElseBlock conditions = new JIfElseBlock();
    private JBlock attributeBlock;

    public ElementWriterBuilderImpl(BuildContext buildContext,String className) {
        this.buildContext = buildContext;
        this.model = buildContext.getCodeModel();
        
        try {
            writerClass = model._class(className);
            writerClass._extends(Writer.class);
        } catch (JClassAlreadyExistsException e) {
            throw new BuildException(e);
        }

        JMethod ctr = writerClass.constructor(JMod.PUBLIC);
        ctr.body().invoke("super").arg(ctr.param(Context.class,"context"));

        method = writerClass.method(JMod.PUBLIC | JMod.FINAL, void.class, "write");
        objectVar = addBasicArgs(method, model.ref(Object.class), "o");
        currentBlock = method.body();
        currentBlock.add(conditions);

        variableManager.addId(objectVar.name());
        variableManager.addId(getXSW().name());
        variableManager.addId(getContextVar().name());
    }
    
    public ElementWriterBuilderImpl(ElementWriterBuilderImpl parent, QName name, JMethod method, JVar objectVar) {
        this.buildContext = parent.buildContext;
        this.method = method;
        this.objectVar = objectVar;
        this.parent = parent;
        this.name = name;
        this.xswVar = parent.xswVar;
        this.rtContextVar = parent.rtContextVar;
        this.writerClass = parent.writerClass;
        this.model = parent.model;
        exceptions.addAll(parent.exceptions);
        
        currentBlock = method.body();
        currentBlock.add(conditions);
        attributeBlock = method.body().block();

        variableManager.addId(objectVar.name());
        variableManager.addId(getXSW().name());
        variableManager.addId(getContextVar().name());
    }

    public ElementWriterBuilder newCondition(JExpression condition) {
        return newCondition(condition, objectVar.type());
    }
    
    public ElementWriterBuilder newCondition(JExpression condition, JType type) {
        JBlock block = conditions.addCondition(condition);
        
        JMethod m = buildContext.createMethod(writerClass, "write" + capitalize(type.name()));
        JVar newObjectVar = addBasicArgs(m, type, decapitalize(type.name()));

        block.invoke(m).arg(xswVar).arg(JExpr.cast(type, objectVar)).arg(rtContextVar);
        
        return new ElementWriterBuilderImpl(this, name, m, newObjectVar);
    }

    public JBlock newBlock(JExpression condition) {
        return conditions.addCondition(condition);
    }

    public ElementWriterBuilder writeElement(QName name) {
        return writeElement(name, objectVar.type(), objectVar);
    }

    public ElementWriterBuilder writeElement(QName name, JExpression condition, JType type, JExpression var) {
        JConditional conditional = currentBlock._if(condition);
        JBlock block = conditional._then();
        
        return writeElement(name, type, var, block);
    }

    private ElementWriterBuilder writeElement(QName name, JType type, JExpression var, JBlock block) {
        block.add(xswVar.invoke("writeStartElement").arg(name.getPrefix()).arg(name.getLocalPart()).arg(name.getNamespaceURI()));

        if (getParent() == null || getName() == null ||  
            !getName().getNamespaceURI().equals(name.getNamespaceURI())) {
            block.add(xswVar.invoke("writeAndDeclareIfUndeclared").arg(JExpr.lit("")).arg(name.getNamespaceURI()));
        }
        
        JMethod m = buildContext.createMethod(writerClass, "write" + capitalize(type.name()));
        JVar newObjectVar = addBasicArgs(m, type, decapitalize(type.name()));
        
        block.invoke(m).arg(xswVar).arg(JExpr.cast(type, var)).arg(rtContextVar);

        block.add(xswVar.invoke("writeEndElement"));
        
        return new ElementWriterBuilderImpl(this, name, m, newObjectVar);
    }

    public ElementWriterBuilder writeElement(QName qname, JType type, JExpression var) {
        return writeElement(qname, type, var, currentBlock);
    }
    
    public void writeProperty(QName name, Class cls, String propertyName, boolean nillable) {
        ElementWriterBuilder b = writeElement(name, model._ref(cls), getObject().invoke(getGetter(propertyName)));
        
        b.writeAs(cls, nillable);
    }
    
    public WriterBuilder writeAttribute(QName name) {
        return writeAttribute(name, objectVar.type(), objectVar);
    }

    public WriterBuilder writeAttribute(QName name, JType type, JExpression var) {
        JMethod m = buildContext.createMethod(writerClass, "write" + capitalize(type.name()));
        JVar newObjectVar = addBasicArgs(m, type, "_obj");

        JBlock block = attributeBlock;
        block.invoke(m).arg(xswVar).arg(JExpr.cast(type, var)).arg(rtContextVar);

        return new AttributeWriterBuilder(this, name, m, newObjectVar);
    }

    public void writeNilIfNull() {
        JBlock ifBlock = currentBlock;
        JConditional cond2 = ifBlock._if(getObject().eq(JExpr._null()));
        JBlock nullBlock2 = cond2._then();
        nullBlock2.add(xswVar.invoke("writeXsiNil"));
        nullBlock2._return();
    }

    public void writeAs(Class cls, boolean nillable) {
        if (!cls.isPrimitive()) {
            JBlock block = currentBlock;
            JConditional cond = block._if(getObject().ne(JExpr._null()));
            JBlock newBlock = cond._then();
            setCurrentBlock(newBlock);
            
            writeAs(cls);
            
            if (nillable) {
                newBlock = cond._else();
                newBlock.add(xswVar.invoke("writeXsiNil"));
                setCurrentBlock(newBlock);
            }
            setCurrentBlock(block);
        } else {
            writeAs(cls);
        }
    }
    
    public void writeAs(Class cls) {
        if (cls.equals(String.class)) {
            writeAs("writeString");
        } else if (cls.equals(int.class) || cls.equals(Integer.class)) {
            writeAs("writeInt");
        } else if (cls.equals(boolean.class) || cls.equals(Boolean.class)) {
            writeAs("writeBoolean");
        } else if (cls.equals(short.class) || cls.equals(Short.class)) {
            writeAs("writeShort");
        } else if (cls.equals(double.class) || cls.equals(Double.class)) {
            writeAs("writeDouble");
        } else if (cls.equals(long.class) || cls.equals(Long.class)) {
            writeAs("writeLong");
        } else if (cls.equals(float.class) || cls.equals(Float.class)) {
            writeAs("writeFloat");
        } else if (cls.equals(byte.class) || cls.equals(Byte.class)) {
            writeAs("writeByte");
        } else {
            throw new UnsupportedOperationException("Unsupported type " + cls.getName());
        }
    }

    private void writeAs(String methodName) {
        currentBlock.add(xswVar.invoke(methodName).arg(getObject()));
    }

    public void writeAsString() {
        writeAs(String.class);
    }

    
    public void writeAsInt() {
        writeAs(int.class);
    }

    public void write() {
    }

    private static String capitalize(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        if (Character.isUpperCase(name.charAt(0))) {
            return name;
        }
        char chars[] = name.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
    }
}
