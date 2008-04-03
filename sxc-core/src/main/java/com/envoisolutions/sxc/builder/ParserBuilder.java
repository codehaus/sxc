package com.envoisolutions.sxc.builder;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JVar;
import com.envoisolutions.sxc.builder.impl.IdentityManager;

public interface ParserBuilder {

    void setAllowUnkown(boolean allow);

    CodeBody getBody();

    JDefinedClass getReaderClass();
    
    JCodeModel getCodeModel();

    /**
     * Reads the value of the attribute/element into a variable of the given type.
     */
    JVar as(Class<?> cls);
    
    /**
     * Get the XMLStreamReader variable.
     * @return
     */
    JVar getXSR();

    /**
     * Pass a variable in the parent {@link ParserBuilder} to this parser builder.
     *
     * @param parentVar
     *      Variable used in the parent's {@link ParserBuilder} scope.
     *
     * @return
     *      Variable that represents the local variable in the current {@link ParserBuilder}'s scope.
     */
    JVar passParentVariable(JVar parentVar);

    boolean isRequired();
    
    void setRequired(boolean b);
    
    ParserBuilder newState();
    ParserBuilder newState(JBlock block);

    IdentityManager getFieldManager();
    IdentityManager getVariableManager();
}