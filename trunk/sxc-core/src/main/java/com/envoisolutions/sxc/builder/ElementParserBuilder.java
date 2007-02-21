package com.envoisolutions.sxc.builder;

import javax.xml.namespace.QName;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JExpression;
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

    ParserBuilder newState(JBlock block);

    void expectElement(QName name, ElementParserBuilder childNodeBuilder);

}
