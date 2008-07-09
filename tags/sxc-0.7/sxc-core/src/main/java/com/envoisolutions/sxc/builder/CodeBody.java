package com.envoisolutions.sxc.builder;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JStatement;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;

public interface CodeBody {
    JVar decl(JType jc, String name, JExpression expr);
    
    JVar field(int flags, JType jc, String name, JExpression expr);

    /**
     * Short-cut for {@code getBlock().add(...)}.
     */
    void add(JStatement stmt);

    /**
     * Gets the code block that will be executed when the attribute/element
     * matches.
     */
    JBlock getBlock();

    /**
     * When the match completes, return the given expression (of the given type)
     * to the parent {@link ParserBuilder}.
     */
    void _return(JType type, JExpression ex);

    /**
     * Short-cut for {@code _return(var.type(),var)} so that returning
     * a variable is easier.
     */
    void _return(JVar var);
}
