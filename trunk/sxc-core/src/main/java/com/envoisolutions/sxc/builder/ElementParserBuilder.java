package com.envoisolutions.sxc.builder;

import javax.xml.namespace.QName;

import com.sun.codemodel.JExpression;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import com.sun.codemodel.JBlock;

public interface ElementParserBuilder extends ParserBuilder {

    //    
    //    void beginSequence();
    //    void endSequence();
    //    
    
    ParserBuilder expectAttribute(QName qname);
    void setAttributeBlock(QName name, JVar readVar, JBlock readBlock);

    ElementParserBuilder expectElement(QName qname);
    ElementParserBuilder expectElement(QName qname, String methodNameHint);
    void setElementBlock(QName name, JVar readVar, JBlock readBlock);

    ElementParserBuilder expectAnyElement();
    ElementParserBuilder expectAnyElement(String methodNameHint);

    ElementParserBuilder expectGlobalElement(QName qname);
    ElementParserBuilder expectGlobalElement(QName qname, String methodNameHint);

    ElementParserBuilder expectXsiType(QName qname);
    ElementParserBuilder expectXsiType(QName qname, String methodNameHint);
    void setXsiTypeBlock(QName name, JVar readVar, JBlock readBlock);

    ElementParserBuilder newState();
    ElementParserBuilder newState(JBlock block);
    ElementParserBuilder newState(JBlock block, String methodNameHint);

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

    /**
     * Optional code to be generated right before the return statement,
     * after the main loop.
     *
     * <p>
     * This can be used to perform the post-construction to be executed
     * when all the child elements are read and processed.
     */
    JBlock getTailBlock();
}
