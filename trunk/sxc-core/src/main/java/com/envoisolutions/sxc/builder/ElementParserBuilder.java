package com.envoisolutions.sxc.builder;

import javax.xml.namespace.QName;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JExpression;
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

    ElementParserBuilder expectGlobalElement(QName qname);

    ElementParserBuilder expectXsiType(QName qname);
    
    JVar as(Class<?> cls, boolean nillable);
    
    QName getName();

    void expectElement(QName name, ElementParserBuilder childNodeBuilder, JExpression... vars);

    /**
     * Call out to a parser that has already been build and create a variable
     * which the result is stored in.
     * @param type
     * @param varName
     * @param builder
     * @return
     */
    JVar call(JType type, String varName, ElementParserBuilder builder);
}
