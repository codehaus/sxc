package com.envoisolutions.sxc.builder;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JStatement;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;

public interface CodeBody {
    JVar decl(JType jc, String name, JExpression expr);
    
    JVar field(int flags, JType jc, String name, JExpression expr);
    
    void add(JStatement stmt);
    
    void _return(JType type, JExpression ex);
    
    void _return(JVar var);
    
    JBlock getBlock();
}
