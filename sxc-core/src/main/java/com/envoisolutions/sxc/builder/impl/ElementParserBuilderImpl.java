package com.envoisolutions.sxc.builder.impl;

import com.envoisolutions.sxc.Reader;
import com.envoisolutions.sxc.Context;
import com.envoisolutions.sxc.builder.BuildException;
import com.envoisolutions.sxc.builder.CodeBody;
import com.envoisolutions.sxc.builder.ElementParserBuilder;
import com.envoisolutions.sxc.builder.ParserBuilder;
import com.envoisolutions.sxc.util.XoXMLStreamReader;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JForLoop;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import com.sun.codemodel.JClass;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElementParserBuilderImpl extends AbstractParserBuilder implements ElementParserBuilder {

    Map<QName, ElementParserBuilderImpl> elements = new HashMap<QName, ElementParserBuilderImpl>();
    Map<QName, ElementCall> elementCalls = new HashMap<QName, ElementCall>();
    Map<QName, AttributeParserBuilderImpl> attributes = new HashMap<QName, AttributeParserBuilderImpl>();
    Map<QName, ElementParserBuilderImpl> xsiTypes = new HashMap<QName, ElementParserBuilderImpl>();
    List<ElementParserBuilderImpl> states = new ArrayList<ElementParserBuilderImpl>();
    int varCount = 0;
    
    ElementParserBuilderImpl anyElement;
    boolean root;
    int depth = 1;
    QName name;
    private boolean valueType;
    private boolean checkXsiTypes = true;
    private JInvocation methodInvocation;
    JMethod constructor;

    /**
     * Base class to extend from. Only used when root.
     */
    JClass baseClass;

    public ElementParserBuilderImpl(BuildContext buildContext, String className) throws BuildException {
        this.buildContext = buildContext;
        model = buildContext.getCodeModel();
        try {
            readerClass = model._class(className);
            baseClass = model.ref(Reader.class);
        } catch (JClassAlreadyExistsException e) {
            throw new BuildException(e);
        }

        constructor = readerClass.constructor(JMod.PUBLIC);
        constructor.body().invoke("super").arg(constructor.param(Context.class,"context"));

        method = readerClass.method(JMod.PUBLIC | JMod.FINAL, Object.class, "read");
        
        addBasicArgs(method);
        
        root = true;
    }

    public ElementParserBuilderImpl(ElementParserBuilderImpl parent, boolean increaseDepth, QName name) {
        this.model = parent.getCodeModel();
        this.buildContext = parent.getBuildContext();
        this.readerClass = parent.getReaderClass();
        // this.variables.addAll(parent.getVariables());
        this.name = name;

        method = buildContext.getNextReadMethod(readerClass);
        addBasicArgs(method);

        if (increaseDepth) {
            depth = parent.depth + 1;
        } else {
            depth = parent.depth;
        }
    }
    
    public ElementParserBuilderImpl(ElementParserBuilderImpl parent, QName name) {
        this(parent, true, name);
    }

    public ParserBuilder expectAttribute(QName qname) {
        if (qname == null) {
            throw new NullPointerException("Attribute name cannot be null!");
        }
        
        AttributeParserBuilderImpl b = attributes.get(qname);
        if (b == null) {
            b = new AttributeParserBuilderImpl(this);
            attributes.put(qname, b);
        }
        return b;
    }

    public ElementParserBuilder expectElement(QName qname) {
        if (qname == null) {
            throw new NullPointerException("Element name cannot be null!");
        }
        
        ElementParserBuilderImpl b = elements.get(qname);
        if (b == null) {
            ElementCall call = elementCalls.get(qname);
            if (call != null) {
                b = call.getElement();
            } else {
                b = new ElementParserBuilderImpl(this, qname);
                elements.put(qname, b);
            }
        }
        return b;
    }

    public void expectElement(QName name, ElementParserBuilder elementBuilder, JExpression... vars) {
        if (name == null) {
            throw new NullPointerException("Element name cannot be null!");
        }
        if (elementBuilder == null) {
            throw new NullPointerException("ElementParserBuilder cannot be null!");
        }
        elements.remove(name);
        elementCalls.put(name, new ElementCall((ElementParserBuilderImpl) elementBuilder, vars));
    }

    public ElementParserBuilder expectXsiType(QName qname) {
        if (qname == null) {
            throw new NullPointerException("Element name cannot be null!");
        }
        
        ElementParserBuilderImpl b = getXsiTypes().get(qname);
        if (b == null) {
            b = new ElementParserBuilderImpl(this, qname);
            b.checkXsiTypes  = false;
            getXsiTypes().put(qname, b);
        }
        
        return b;
    }
    
    public ElementParserBuilder expectAnyElement() {
        if (anyElement == null) {
            anyElement = new ElementParserBuilderImpl(this, null);
        }
        return anyElement;
    }

    public ElementParserBuilder expectGlobalElement(QName qname) {
        if (qname == null) {
            throw new NullPointerException("Element name cannot be null!");
        }
        
        ElementParserBuilderImpl b = buildContext.getGlobalElements().get(qname);
        if (b == null) {
            b = new ElementParserBuilderImpl(this, qname);
            buildContext.getGlobalElements().put(qname, b);
        }
        return b;
    }
    
    
    public JVar as(Class<?> cls) {
        return as(cls, false);
    }
    
    public JVar as(Class<?> cls, boolean nillable) {
        this.valueType = true;
        
        if (cls.equals(String.class)) {
            return createVar("getElementAsString", String.class, nillable);
        } else if (cls.equals(int.class) || cls.equals(Integer.class)) {
            return createVar("getElementAsInt", Integer.class, nillable);
        } else if (cls.equals(double.class) || cls.equals(Double.class)) {
            return createVar("getElementAsDouble", Double.class, nillable);
        } else if (cls.equals(float.class) || cls.equals(Float.class)) {
            return createVar("getElementAsFloat", Float.class, nillable);
        } else if (cls.equals(long.class) || cls.equals(Long.class)) {
            return createVar("getElementAsLong", Long.class, nillable);
        } else if (cls.equals(short.class) || cls.equals(Short.class)) {
            return createVar("getElementAsShort", Short.class, nillable);
        } else if (cls.equals(boolean.class) || cls.equals(Boolean.class)) {
            return createVar("getElementAsBoolean", Boolean.class, nillable);
        }  else if (cls.equals(byte.class) || cls.equals(Byte.class)) {
            return createVar("getElementAsByte", Byte.class, nillable);
        } 
        throw new UnsupportedOperationException("Invalid type " + cls);
    }

    private JVar createVar(String value, Class<?> cls, boolean nillable) {
        JVar var;
        if (nillable) {
            var = method.body().decl(model._ref(cls), "value" + varCount++, JExpr._null());
            JConditional cond = method.body()._if(xsrVar.invoke("isXsiNil").not());
            
            JInvocation invocation = xsrVar.invoke(value);
            cond._then().assign(var, invocation);
        } else {
            var = method.body().decl(model._ref(cls), "value" + varCount++, xsrVar.invoke(value));
        }
        
        return var;
    }

    public JVar asString() {
        return as(String.class, false);
    }
    
    public ParserBuilder newState() {
        return newState(codeBlock);
    }

    public ParserBuilder newState(JBlock block) {
        ElementParserBuilderImpl b = new ElementParserBuilderImpl(this, false, name);
        states.add(b);
        
        JMethod nextMethod = b.getMethod();
        
        JInvocation invocation = JExpr.invoke(nextMethod).arg(xsrVar).arg(rtContextVar);
        b.methodInvocation = invocation;
        
        block.add(invocation);
        
        return b;
    }

    @Override
    public JVar passParentVariable(JVar parentVar) {
        if (methodInvocation != null) {
            methodInvocation.arg(parentVar);
        }
        
        return super.passParentVariable(parentVar);
    }

    public JVar call(JType type, String varName, ElementParserBuilder builder) {
        JBlock block = codeBlock;
        
        ElementParserBuilderImpl b = (ElementParserBuilderImpl) builder;
        JMethod nextMethod = b.getMethod();
        
        JInvocation invocation = JExpr.invoke(nextMethod).arg(xsrVar).arg(rtContextVar);
        for (JVar v : b.variables) {
            invocation.arg(v);
        }
        
        return block.decl(type, varName, invocation);
    }

    public CodeBody getBody() {
        return new CodeBodyImpl(this);
    }
    
    protected void write() {
        if (written) return;
        
        written = true;
        
        JBlock b = method.body();
        
        if (!valueType &&
            (elements.size() > 0 
            || elementCalls.size() > 0 
            || buildContext.getGlobalElements().size() > 0 
            || getXsiTypes().size() > 0 
            || attributes.size() > 0 
            || anyElement != null)){
            writeMainLoop();
        } else {
            b.add(codeBlock);
        }
        
        for (ElementParserBuilderImpl e : states) {
            e.write();
        }
        
        // Add return statement to the end of the block
        if (returnType != null) {
            setReturnType(returnType);
            codeBlock._return(_return);
        } 
        
        if (root) {
            b._return(JExpr._null());
        }

        if (root) {
            writeReadAsType();
            readerClass._extends(baseClass);
        }
    }
    
    protected void writeReadAsType() {
        JMethod m = getReaderClass().method(JMod.PUBLIC, Object.class, "read");
        m.param(XoXMLStreamReader.class, "reader");
        m.param(buildContext.getStringToObjectMap(), "properties");
        JVar typeVar = m.param(QName.class, "type");
        m._throws(XMLStreamException.class);
        
        JBlock block = m.body();
        writeXsiChecks(block, typeVar);
        
        block._return(JExpr._null());
    }
    
    public Map<QName, ElementParserBuilderImpl> getXsiTypes() {
        return xsiTypes;
    }

    /**
     * Write out a loop which will read in a sequence of elements.
     *
     */
    private void writeMainLoop() {
        JBlock b = method.body();
        
        // Add XSI checks
        if (!valueType && xsiTypes.size() > 0 && depth > 1) {
            writeXsiChecks(b);
        }
        
        // Add the user constructed codeblock and continue from there
        b.add(codeBlock);
        b = codeBlock;
        
        if (attributes.size() > 0) {
            writeAttributeReader(b);
        }
        
        JVar depthVar = b.decl(model._ref(int.class), "depth", xsrVar.invoke("getDepth"));
        JVar targetDepthVar;
        JVar event;
        if (depth == 1) {
            targetDepthVar = b.decl(model._ref(int.class), "targetDepth", JExpr.lit(depth));
            event = b.decl(model._ref(int.class), "event", xsrVar.invoke("getEventType"));
        } else {
            targetDepthVar = b.decl(model._ref(int.class), "targetDepth", depthVar.plus(JExpr.lit(1)));
            event = b.decl(model._ref(int.class), "event", xsrVar.invoke("nextTagIgnoreAll"));
        }
        
        
        
        b.assign(depthVar, xsrVar.invoke("getDepth"));
        
//        JClass sysType = (JClass) model._ref(System.class);
//        if (depth != 1)
//            b.add(sysType.staticRef("out").invoke("println").arg(JExpr.lit("TD ").plus(targetDepthVar)
//                     .plus(JExpr.lit(" Depth: ")).plus(depthVar)
//                     .plus(JExpr.lit(" Name: " + name).plus(JExpr.lit(" Current: "))
//                           .plus(xsrVar.invoke("getName")))));

        JBlock loop = b._while(depthVar.gte(targetDepthVar.minus(JExpr.lit(1)))).body();
        
        b = loop._if(event.eq(JExpr.lit(XMLStreamConstants.START_ELEMENT)))._then();

        
        JConditional ifDepth = b._if(depthVar.eq(targetDepthVar));
        
        writeElementReader(elements, ifDepth._then(), false);
        
        if (allowUnknown) {
            writeElementReader(buildContext.getGlobalElements(), ifDepth._else(), true);
        }
        
        JConditional ifHasNext = loop._if(xsrVar.invoke("hasNext"));
        ifHasNext._then().assign(event, xsrVar.invoke("next"));
        ifHasNext._then().assign(depthVar, xsrVar.invoke("getDepth"));
        ifHasNext._else()._break();
    }

    /**
     * Write out a loop which will read in a sequence of elements.
     *
     */
    private void writeAttributeReader(JBlock b) {
        JForLoop loop = b._for();
        JVar var = loop.init(model._ref(int.class), "i", JExpr.lit(0));
        loop.test(var.lt(xsrVar.invoke("getAttributeCount")));
        loop.update(var.assignPlus(JExpr.lit(1)));
        
        b = loop.body();
        JVar attName = b.decl(model._ref(String.class), "attName", xsrVar.invoke("getAttributeLocalName").arg(var));
        JVar attNs = b.decl(model._ref(String.class), "attNs", xsrVar.invoke("getAttributeNamespace").arg(var));
        JVar attValue = b.decl(model._ref(String.class), "attValue", xsrVar.invoke("getAttributeValue").arg(var));
        
        JConditional cond = null;
        
        for (Map.Entry<QName, AttributeParserBuilderImpl> e : attributes.entrySet()) {
            QName name = e.getKey();
            AttributeParserBuilderImpl builder = e.getValue();
            
            JExpression localInv = attName.eq(JExpr.lit(name.getLocalPart()));
            String ns = name.getNamespaceURI();
            JExpression nsInv = JExpr.lit(name.getNamespaceURI()).eq(attNs);
            
            if (ns.equals("")) {
                nsInv = nsInv.cor(attNs.eq(JExpr._null()));
            }
            
            JExpression qnameCompare = localInv.cand(nsInv);

            if (cond == null) {
                cond = b._if(qnameCompare); 
            } else {
                cond = cond._else()._if(qnameCompare);
            }
            
            JMethod nextMethod = builder.getMethod();
            
            JInvocation invocation = JExpr.invoke(nextMethod).arg(xsrVar).arg(rtContextVar);
            for (JVar v : builder.variables) {
                invocation.arg(v);
            }
            
            nextMethod.param(model._ref(String.class), "_attValue");
            invocation.arg(attValue);
            
            nextMethod.body().add(builder.codeBlock);
            
            if (root && builder.returnType != null) {
                cond._then()._return(invocation);
            } else {
                cond._then().add(invocation);
            }
            
            // TODO: throw exception if unknown elements are encountered and allowUnknown == false
            builder.write();
        }
    }

    private void writeXsiChecks(JBlock b) {
        JVar xsiType = b.decl(model._ref(QName.class), "xsiType", xsrVar.invoke("getXsiType"));
        JConditional cond = b._if(xsiType.ne(JExpr._null())); 
        
        writeXsiChecks(cond._then(), xsiType);
    }

    private void writeXsiChecks(JBlock b, JVar xsiType) {
        JConditional xsiCond = null;
        
        for (Map.Entry<QName, ElementParserBuilderImpl> e : getXsiTypes().entrySet()) {
            QName name = e.getKey();
            
            ElementParserBuilderImpl builder = e.getValue();
            
            JExpression localInv = xsiType.invoke("getLocalPart").eq(JExpr.lit(name.getLocalPart()));
            String ns = name.getNamespaceURI();
            JExpression nsInv = JExpr.lit(name.getNamespaceURI()).eq(xsiType.invoke("getNamespaceURI"));
            if (ns.equals("")) {
                nsInv = nsInv.cor(xsiType.invoke("getNamespaceURI").eq(JExpr._null()));
            }
            
            JExpression qnameCompare = localInv.cand(nsInv);

            if (xsiCond == null) {
                xsiCond = b._if(qnameCompare); 
            } else {
                xsiCond = xsiCond._else()._if(qnameCompare);
            }
            
            JBlock block = xsiCond._then();
            
            writeElementReader(builder, block, false);
            
            if (builder.returnType == null) {
                block._return();
            }
        }
    }

    
    private void writeElementReader(Map<QName, ElementParserBuilderImpl> els, JBlock b, boolean global) {
        JConditional cond = null;
        
        if (depth == 1 && !global && checkXsiTypes) {
            writeXsiChecks(b);
        }
        
        for (Map.Entry<QName, ElementParserBuilderImpl> e : els.entrySet()) {
            QName name = e.getKey();
            ElementParserBuilderImpl builder = e.getValue();
            
            JExpression localInv = xsrVar.invoke("getLocalName").eq(JExpr.lit(name.getLocalPart()));
            String ns = name.getNamespaceURI();
            JExpression nsInv = JExpr.lit(name.getNamespaceURI()).eq(xsrVar.invoke("getNamespaceURI"));
            if (ns.equals("")) {
                nsInv = nsInv.cor(xsrVar.invoke("getNamespaceURI").eq(JExpr._null()));
            }
            
            JExpression qnameCompare = localInv.cand(nsInv);

            if (cond == null) {
                cond = b._if(qnameCompare); 
            } else {
                cond = cond._else()._if(qnameCompare);
            }
            
            JBlock block = cond._then();
            
            writeElementReader(builder, block, global);
        }
        
        for (Map.Entry<QName, ElementCall> e : elementCalls.entrySet()) {
            QName name = e.getKey();
            ElementCall call = e.getValue();
            ElementParserBuilderImpl builder = call.getElement();
            
            JExpression localInv = xsrVar.invoke("getLocalName").eq(JExpr.lit(name.getLocalPart()));
            String ns = name.getNamespaceURI();
            JExpression nsInv = JExpr.lit(name.getNamespaceURI()).eq(xsrVar.invoke("getNamespaceURI"));
            if (ns.equals("")) {
                nsInv = nsInv.cor(xsrVar.invoke("getNamespaceURI").eq(JExpr._null()));
            }
            
            JExpression qnameCompare = localInv.cand(nsInv);

            if (cond == null) {
                cond = b._if(qnameCompare); 
            } else {
                cond = cond._else()._if(qnameCompare);
            }
            
            JBlock block = cond._then();
            
            JMethod nextMethod = builder.getMethod();
            JInvocation invocation = JExpr.invoke(nextMethod).arg(xsrVar).arg(rtContextVar);
            for (JExpression var : call.getVars()) {
                invocation.arg(var);
            }
            block.add(invocation);
            if (builder != this)
                builder.write();
        }
        
        if (anyElement != null) {
            JBlock anyBlock = b;
            if (cond != null) {
                anyBlock = cond._else().block();
            }
            
            writeElementReader(anyElement, anyBlock, false);
        }
    }

    private void writeElementReader(ElementParserBuilderImpl builder, 
                                    JBlock block, 
                                    boolean global) {
        JMethod nextMethod = builder.getMethod();
        
        JInvocation invocation = JExpr.invoke(nextMethod).arg(xsrVar).arg(rtContextVar);

        if (!global) {
            for (JVar v : builder.variables) {
                invocation.arg(v);
            }
        } 
        
        if (root && builder.returnType != null) {
            block._return(invocation);
        } else {
            block.add(invocation);
        }
        
        // TODO: throw exception if unknown elements are encountered and allowUnknown == false
        if (builder != this)
            builder.write();
    }

    private void setReturnType(JType type) {
        try {
            Field field = JMethod.class.getDeclaredField("type");
            boolean accessibility = field.isAccessible();
            field.setAccessible(true);
            field.set(method, type);
            field.setAccessible(accessibility);
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    public QName getName() {
        return name;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }    
    
    static class ElementCall {
        private JExpression[] vars;
        private ElementParserBuilderImpl element;
        
        public ElementCall(ElementParserBuilderImpl element, JExpression[] vars) {
            super();
            this.vars = vars;
            this.element = element;
        }
        public ElementParserBuilderImpl getElement() {
            return element;
        }
        public JExpression[] getVars() {
            return vars;
        }
        
    }
}
