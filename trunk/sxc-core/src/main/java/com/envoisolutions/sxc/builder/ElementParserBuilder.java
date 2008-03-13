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

    /**
     * Optional code to be generated right before the return statement,
     * after the main loop.
     *
     * <p>
     * This can be used to perform the post-construction to be executed
     * when all the child elements are read and processed.
     */
    JBlock getTailBlock();

    /**
     * Spedified the block used to read an element.
     *
     * <p>
     * This can be used to perform operations on nested elements such
     * as placing them in a collection.
     *
     * @param name the qname of the expected element to enhance
     * @param var the variable that will be assigned the item
     * @param block the code block
     */
    void setReadBlock(QName name, JVar var, JBlock block);
}
