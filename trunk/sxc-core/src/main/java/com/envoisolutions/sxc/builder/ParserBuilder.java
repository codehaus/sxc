package com.envoisolutions.sxc.builder;

import java.util.List;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JVar;

public interface ParserBuilder {

    void setAllowUnkown(boolean allow);

    CodeBody getBody();

    JDefinedClass getReaderClass();
    
    JCodeModel getCodeModel();

    JVar as(Class<?> cls);
    
    /**
     * Get the XMLStreamReader variable.
     * @return
     */
    JVar getXSR();

    /** 
     * Assigns the return value of this ElementParserBuilder to the specified variable. 
     */
    void assignReturnTo(JVar nodeVar);
    
    JVar passParentVariable(JVar parentVar);

    boolean isRequired();
    
    void setRequired(boolean b);
    
    ParserBuilder newState();
    
    ParserBuilder newState(JBlock block);

}