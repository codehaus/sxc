package com.envoisolutions.sxc.builder.impl;

import com.envoisolutions.sxc.builder.CodeBody;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JStatement;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;

public class CodeBodyImpl implements CodeBody {

    private AbstractParserBuilder builder;

    public CodeBodyImpl(AbstractParserBuilder builder) {
        this.builder = builder;
    }

    public void add(JStatement stmt) {
        builder.codeBlock.add(stmt);
    }

    public JVar field(int flags, JType jc, String name, JExpression expr) {
        return builder.getReaderClass().field(flags, jc, name, expr);
    }

    public JVar decl(JType jc, String name, JExpression expr) {
        JVar var = builder.codeBlock.decl(jc, name, expr);
        // builder.getVariables().add(var);
        return var;
    }

    public void _return(JVar var) {
        _return(var.type(), var);
    }

    public void _return(JType type, JExpression ex) {
        builder.setReturnVar(type, ex);
    }

    public JBlock getBlock() {
        return builder.codeBlock;
    }

}
