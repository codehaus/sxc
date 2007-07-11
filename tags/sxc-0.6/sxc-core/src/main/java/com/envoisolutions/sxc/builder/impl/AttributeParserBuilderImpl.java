package com.envoisolutions.sxc.builder.impl;

import com.envoisolutions.sxc.builder.ParserBuilder;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JVar;


public class AttributeParserBuilderImpl extends AbstractParserBuilder {

    private ElementParserBuilderImpl parent;

    public AttributeParserBuilderImpl(ElementParserBuilderImpl parent) {
        this.model = parent.getCodeModel();
        this.buildContext = parent.getBuildContext();
        this.readerClass = parent.getReaderClass();
        this.parent = parent;
        
        method = buildContext.getNextReadMethod(readerClass);
        addBasicArgs(method);
    }

    public ParserBuilder newState() {
        return newState(method.body());
    }

    public ParserBuilder newState(JBlock block) {
        AttributeParserBuilderImpl b = 
            new AttributeParserBuilderImpl(parent);
        // states.add(b);
        
        JMethod nextMethod = b.getMethod();
        
        JInvocation invocation = JExpr.invoke(nextMethod).arg(xsrVar).arg(rtContextVar);
        for (JVar v : b.variables) {
            invocation.arg(v);
        }
        
        block.add(invocation);
        
        return b;
    }


    public JVar asString() {
        JVar var = method.body().decl(model._ref(String.class), "value", JExpr.direct("_attValue"));
        return var;
    }

    public JVar as(Class<?> cls) {
        if (cls.equals(String.class)) {
            return asString();
        } else if (cls.equals(int.class) || cls.equals(Integer.class)) {
            return createVar(int.class, Integer.class);
        } else if (cls.equals(double.class) || cls.equals(Double.class)) {
            return createVar(double.class, Double.class);
        } else if (cls.equals(float.class) || cls.equals(Float.class)) {
            return createVar(float.class, Float.class);
        } else if (cls.equals(long.class) || cls.equals(Long.class)) {
            return createVar(long.class, Long.class);
        } else if (cls.equals(short.class) || cls.equals(Short.class)) {
            return createVar(short.class, Short.class);
        } else if (cls.equals(byte.class) || cls.equals(Byte.class)) {
            return createVar(byte.class, Byte.class);
        } else if (cls.equals(boolean.class) || cls.equals(Boolean.class)) {
            JExpression var = JExpr.direct("_attValue");
            JBlock b = method.body();
            JVar retVar = method.body().decl(model._ref(boolean.class), "value");
            
            JConditional cond = b._if(JExpr.lit("1").invoke("equals").arg(var).cor(JExpr.lit("true").invoke("equals").arg(var)));
            JClass boolClass = (JClass) model._ref(Boolean.class);
            
            cond._then().assign(retVar, boolClass.staticRef("TRUE"));
            cond._else().assign(retVar, boolClass.staticRef("FALSE"));
            
            return retVar;
        } 
        throw new UnsupportedOperationException();
    }

    private JVar createVar( Class<?> cls, Class parser) {
        JClass jcls = (JClass) model._ref(parser);
        
        return method.body().decl(model._ref(cls), "value", 
                                  jcls.staticInvoke("valueOf").arg(JExpr.direct("_attValue")));
    }
    
    @Override
    protected void write() {

    }
}
