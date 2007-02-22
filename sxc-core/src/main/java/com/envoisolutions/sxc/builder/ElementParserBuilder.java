package com.envoisolutions.sxc.builder;

import javax.xml.namespace.QName;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;

public interface ElementParserBuilder extends ParserBuilder {

    //    
    //    void beginSequence();
    //    void endSequence();
    //    
    
    ParserBuilder expectAttribute(QName qname);
    
    ElementParserBuilder expectElement(QName qname);

    ElementParserBuilder expectAnyElement();
    
    ElementParserBuilder onXsiType(QName qname);
    
    ElementParserBuilder globalElement(QName qname);

    JVar as(Class<?> cls, boolean nillable);
    
    QName getName();

    void expectElement(QName name, ElementParserBuilder childNodeBuilder);

    ElementParserBuilder newState(JType type, String name);

}
