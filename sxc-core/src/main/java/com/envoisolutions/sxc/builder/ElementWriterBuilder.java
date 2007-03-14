package com.envoisolutions.sxc.builder;

import javax.xml.namespace.QName;

import com.sun.codemodel.JExpression;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;

public interface ElementWriterBuilder extends WriterBuilder {
    
    ElementWriterBuilder newCondition(JExpression condition);
    
    ElementWriterBuilder newCondition(JExpression condition, JType castTo);
    
    ElementWriterBuilder writeElement(QName name);
    
    ElementWriterBuilder writeElement(QName qname, JType type, JExpression var);
    
    ElementWriterBuilder writeElement(QName name, JExpression condition, JType type, JExpression var);
    
    WriterBuilder writeAttribute(QName name);

    WriterBuilder writeAttribute(QName name, JType type, JExpression var);
    
    void writeNilIfNull();
    
    void writeProperty(QName name, Class cls, String propertyName, boolean nillable);
    
    void writeAsString();

    void writeAsInt();

    void writeAs(Class cls, boolean nillable);

    void setObject(JVar object);
}
