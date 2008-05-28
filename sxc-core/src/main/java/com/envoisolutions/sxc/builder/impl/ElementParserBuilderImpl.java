package com.envoisolutions.sxc.builder.impl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.XMLConstants;

import com.envoisolutions.sxc.Context;
import com.envoisolutions.sxc.Reader;
import com.envoisolutions.sxc.builder.BuildException;
import com.envoisolutions.sxc.builder.CodeBody;
import com.envoisolutions.sxc.builder.ElementParserBuilder;
import com.envoisolutions.sxc.builder.ParserBuilder;
import static com.envoisolutions.sxc.builder.impl.IdentityManager.capitalize;
import com.envoisolutions.sxc.util.XoXMLStreamReader;
import com.envoisolutions.sxc.util.Attribute;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
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
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JForEach;

public class ElementParserBuilderImpl extends AbstractParserBuilder implements ElementParserBuilder {

    private final Map<QName, ExpectedAttribute> attributes = new LinkedHashMap<QName, ExpectedAttribute>();
    private ExpectedAttribute anyAttribute;
    private final Map<QName, ExpectedElement> elements = new LinkedHashMap<QName, ExpectedElement>();
    private ExpectedElement anyElement;
    private ExpectedElement mixedElement;
    private final Map<QName, ExpectedXsiType> xsiTypes = new LinkedHashMap<QName, ExpectedXsiType>();
    private ExpectedXsiType unexpectedXsiType;
    private final List<ElementParserBuilderImpl> states = new ArrayList<ElementParserBuilderImpl>();

    private boolean root;
    private int depth = 1;
    private QName name;
    private QName xmlType;
    private JMethod constructor;
    private JInvocation methodInvocation;

    private boolean valueType;
    private boolean checkXsiTypes = true;
    private boolean addReadAsType = true;

    private JForEach forEachAttribute;
    private JForEach forEachChildElement;

    private final JBlock preElementBlock = new JBlock(false, false);

    /**
     * Base class to extend from. Only used when root.
     */
    private JClass baseClass;
    private final JBlock tailBlock = new JBlock(false, false);

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
        method._throws(XMLStreamException.class);

        root = true;

        reserveVariables();
    }

    public ElementParserBuilderImpl(BuildContext buildContext, JDefinedClass readerClass, Class returnType, boolean mixed) throws BuildException {
        this(buildContext, readerClass, returnType, mixed, 1, null);
    }

    public ElementParserBuilderImpl(BuildContext buildContext, JDefinedClass readerClass, Class returnType, boolean mixed, int depth, String methodNameHint) throws BuildException {
        if (buildContext == null) throw new NullPointerException("buildContext is null");
        if (readerClass == null) throw new NullPointerException("readerClass is null");

        this.buildContext = buildContext;
        this.readerClass = readerClass;
        model = buildContext.getCodeModel();

        if (returnType != null) {
            this.returnType = model._ref(returnType);
        }

        if (methodNameHint == null) methodNameHint = "";
        method = readerClass.method(JMod.PUBLIC | JMod.FINAL, returnType == null ? void.class : returnType, "read" + capitalize(methodNameHint));

        addBasicArgs(method);

        this.depth = depth;

        if (depth == 1) {
            root = true;
        }
        addReadAsType = false;

        forEachAttribute = new JForEach(model.ref(Attribute.class), variableManager.createId("attribute"), getXSR().invoke("getAttributes"));
        if (!mixed) {
            forEachChildElement = new JForEach(model.ref(XoXMLStreamReader.class), variableManager.createId("elementReader"), getXSR().invoke("getChildElements"));
        } else {
            forEachChildElement = new JForEach(model.ref(XoXMLStreamReader.class), variableManager.createId("elementReader"), getXSR().invoke("getMixedChildElements"));
        }

        reserveVariables();
    }

    public ElementParserBuilderImpl(ElementParserBuilderImpl parent, QName name) {
        this(parent, true, name, null);
    }

    public ElementParserBuilderImpl(ElementParserBuilderImpl parent, boolean increaseDepth, QName name) {
        this(parent, increaseDepth, name, null);
    }

    public ElementParserBuilderImpl(ElementParserBuilderImpl parent, boolean increaseDepth, QName name, String methodNameHint) {
        this.model = parent.getCodeModel();
        this.buildContext = parent.getBuildContext();
        this.readerClass = parent.getReaderClass();
        // this.variables.addAll(parent.getVariables());
        this.name = name;

        if (methodNameHint == null) methodNameHint = "";
        method = buildContext.createMethod(readerClass, "read" + capitalize(methodNameHint));
        addBasicArgs(method);
        method._throws(XMLStreamException.class);

        if (increaseDepth) {
            depth = parent.depth + 1;
        } else {
            depth = parent.depth;
        }

        reserveVariables();
    }

    private void reserveVariables() {
        variableManager.addId("depth");
        variableManager.addId("targetDepth");
        variableManager.addId("event");
        variableManager.addId("attName");
        variableManager.addId("attNs");
        variableManager.addId("attValue");
        variableManager.addId("xsiType");
    }

    public QName getName() {
        return name;
    }

    public QName getXmlType() {
        return xmlType;
    }

    public void setXmlType(QName xmlType) {
        this.xmlType = xmlType;
    }

    public JMethod getConstructor() {
        return constructor;
    }

    public JClass getBaseClass() {
        return baseClass;
    }

    public void setBaseClass(JClass baseClass) {
        this.baseClass = baseClass;
    }

    public boolean isAddReadAsType() {
        return addReadAsType;
    }

    public void setAddReadAsType(boolean addReadAsType) {
        this.addReadAsType = addReadAsType;
    }

    public JVar getAttributeVar() {
        if (forEachAttribute == null) {
            return null;
        }
        return forEachAttribute.var();
    }

    public JVar getChildElementVar() {
        if (forEachChildElement == null) {
            return null;
        }
        return forEachChildElement.var();
    }

    public ParserBuilder expectAttribute(QName name) {
        if (name == null) {
            throw new NullPointerException("Attribute name cannot be null!");
        }

        ExpectedAttribute expectedAttribute = attributes.get(name);
        if (expectedAttribute == null) {
            expectedAttribute = new ExpectedAttribute();
            attributes.put(name, expectedAttribute);
        }
        if (expectedAttribute.getParserBuilder() == null) {
            expectedAttribute.setParserBuilder(new AttributeParserBuilderImpl(this));
        }
        return expectedAttribute.getParserBuilder();
    }

    public void setAttributeBlock(QName name, JVar readVar, JBlock readBlock) {
        if (name == null) {
            throw new NullPointerException("Attribute name cannot be null!");
        }

        ExpectedAttribute expectedAttribute = attributes.get(name);
        if (expectedAttribute == null) {
            expectedAttribute = new ExpectedAttribute();
            attributes.put(name, expectedAttribute);
        }
        expectedAttribute.setReadVar(readVar);
        expectedAttribute.setReadBlock(readBlock);
    }

    public ParserBuilder expectAnyAttribute() {
        if (anyAttribute == null) {
            anyAttribute = new ExpectedAttribute();
        }
        if (anyAttribute.getParserBuilder() == null) {
            anyAttribute.setParserBuilder(new AttributeParserBuilderImpl(this));
        }
        return anyAttribute.getParserBuilder();
    }

    public void setAnyAttributeBlock(JVar readVar, JBlock readBlock) {
        if (anyAttribute == null) {
            anyAttribute = new ExpectedAttribute();
        }
        anyAttribute.setReadVar(readVar);
        anyAttribute.setReadBlock(readBlock);
    }

    public ElementParserBuilder expectElement(QName name) {
        return expectElement(name, null);
    }

    public ElementParserBuilder expectElement(QName name, String methodNameHint) {
        if (name == null) {
            throw new NullPointerException("Element name cannot be null!");
        }
        
        ExpectedElement expectedElement = elements.get(name);
        if (expectedElement == null) {
            expectedElement = new ExpectedElement();
            elements.put(name, expectedElement);
        }
        if (expectedElement.getParserBuilder() == null) {
            expectedElement.setParserBuilder(new ElementParserBuilderImpl(this, true, name, methodNameHint));
        }
        return expectedElement.getParserBuilder();
    }

    public void expectElement(QName name, ElementParserBuilder elementBuilder, JExpression... vars) {
        if (name == null) {
            throw new NullPointerException("Element name cannot be null!");
        }

        ExpectedElement expectedElement = elements.get(name);
        if (expectedElement == null) {
            expectedElement = new ExpectedElement();
            elements.put(name, expectedElement);
        }
        expectedElement.setParserBuilder((ElementParserBuilderImpl) elementBuilder);
        expectedElement.setVars(vars);
    }

    public void setElementBlock(QName name, JVar readVar, JBlock readBlock) {
        if (name == null) {
            throw new NullPointerException("Element name cannot be null!");
        }

        ExpectedElement expectedElement = elements.get(name);
        if (expectedElement == null) {
            expectedElement = new ExpectedElement();
            elements.put(name, expectedElement);
        }
        expectedElement.setReadVar(readVar);
        expectedElement.setReadBlock(readBlock);
    }

    public ElementParserBuilder expectAnyElement() {
        return expectAnyElement(null);
    }

    public ElementParserBuilder expectAnyElement(String methodNameHint) {
        if (anyElement == null) {
            anyElement = new ExpectedElement();
        }
        if (anyElement.getParserBuilder() == null) {
            anyElement.setParserBuilder(new ElementParserBuilderImpl(this, true, null, methodNameHint));
        }
        return anyElement.getParserBuilder();
    }

    public void setAnyElementBlock(JVar readVar, JBlock readBlock) {
        if (anyElement == null) {
            anyElement = new ExpectedElement();
        }
        anyElement.setReadVar(readVar);
        anyElement.setReadBlock(readBlock);
    }

    public ElementParserBuilder expectMixedElement() {
        return expectMixedElement(null);
    }

    public ElementParserBuilder expectMixedElement(String methodNameHint) {
        if (mixedElement == null) {
            mixedElement = new ExpectedElement();
        }
        if (mixedElement.getParserBuilder() == null) {
            mixedElement.setParserBuilder(new ElementParserBuilderImpl(this, true, null, methodNameHint));
        }
        return mixedElement.getParserBuilder();
    }

    public void setMixedElementBlock(JVar readVar, JBlock readBlock) {
        if (mixedElement == null) {
            mixedElement = new ExpectedElement();
        }
        mixedElement.setReadVar(readVar);
        mixedElement.setReadBlock(readBlock);
    }

    public ElementParserBuilder expectXsiType(QName name) {
        return expectXsiType(name, null);
    }

    public ElementParserBuilder expectXsiType(QName name, String methodNameHint) {
        if (name == null) {
            throw new NullPointerException("XsiType name cannot be null!");
        }

        ExpectedXsiType expectedXsiType = xsiTypes.get(name);
        if (expectedXsiType == null) {
            expectedXsiType = new ExpectedXsiType();
            xsiTypes.put(name, expectedXsiType);
        }
        if (expectedXsiType.getParserBuilder() == null) {
            ElementParserBuilderImpl xsiType = new ElementParserBuilderImpl(this, true, name, methodNameHint);
            xsiType.checkXsiTypes = false;
            expectedXsiType.setParserBuilder(xsiType);
        }
        return expectedXsiType.getParserBuilder();
    }

    public void setXsiTypeBlock(QName name, JVar readVar, JBlock readBlock) {
        if (name == null) {
            throw new NullPointerException("XsiType name cannot be null!");
        }

        ExpectedXsiType expectedXsiType = xsiTypes.get(name);
        if (expectedXsiType == null) {
            expectedXsiType = new ExpectedXsiType();
            xsiTypes.put(name, expectedXsiType);
        }
        expectedXsiType.setReadVar(readVar);
        expectedXsiType.setReadBlock(readBlock);
    }

    public ElementParserBuilder expectUnexpectedXsiType() {
        return expectUnexpectedXsiType(null);
    }

    public ElementParserBuilder expectUnexpectedXsiType(String methodNameHint) {
        if (unexpectedXsiType == null) {
            unexpectedXsiType = new ExpectedXsiType();
        }
        if (unexpectedXsiType.getParserBuilder() == null) {
            unexpectedXsiType.setParserBuilder(new ElementParserBuilderImpl(this, true, null, methodNameHint));
        }
        return unexpectedXsiType.getParserBuilder();
    }

    public void setUnexpectedXsiTypeBlock(JVar readVar, JBlock readBlock) {
        if (unexpectedXsiType == null) {
            unexpectedXsiType = new ExpectedXsiType();
        }
        unexpectedXsiType.setReadVar(readVar);
        unexpectedXsiType.setReadBlock(readBlock);
    }

    public ElementParserBuilder expectGlobalElement(QName qname) {
        return expectGlobalElement(qname, null);
    }

    public ElementParserBuilder expectGlobalElement(QName qname, String methodNameHint) {
        if (qname == null) {
            throw new NullPointerException("Element name cannot be null!");
        }

        ExpectedElement expectedElement = buildContext.getGlobalElements().get(qname);
        if (expectedElement == null) {
            expectedElement = new ExpectedElement();
            buildContext.getGlobalElements().put(qname, expectedElement);
        }
        if (expectedElement.getParserBuilder() == null) {
            expectedElement.setParserBuilder(new ElementParserBuilderImpl(this, true, qname, methodNameHint));
        }
        return expectedElement.getParserBuilder();
    }

    public JVar as(Class<?> cls) {
        return as(cls, false);
    }
    
    public JVar as(Class<?> cls, boolean nillable) {
        this.valueType = true;
        
        if (cls.equals(String.class)) {
            return createVar("getElementAsString", String.class, nillable);
        } else if (cls.equals(int.class) || cls.equals(Integer.class)) {
            return createVar("getElementAsInt", cls, nillable);
        } else if (cls.equals(double.class) || cls.equals(Double.class)) {
            return createVar("getElementAsDouble", cls, nillable);
        } else if (cls.equals(float.class) || cls.equals(Float.class)) {
            return createVar("getElementAsFloat", cls, nillable);
        } else if (cls.equals(long.class) || cls.equals(Long.class)) {
            return createVar("getElementAsLong", cls, nillable);
        } else if (cls.equals(short.class) || cls.equals(Short.class)) {
            return createVar("getElementAsShort", cls, nillable);
        } else if (cls.equals(boolean.class) || cls.equals(Boolean.class)) {
            return createVar("getElementAsBoolean", cls, nillable);
        }  else if (cls.equals(byte.class) || cls.equals(Byte.class)) {
            return createVar("getElementAsByte", cls, nillable);
        } 
        throw new UnsupportedOperationException("Invalid type " + cls);
    }

    private JVar createVar(String method, Class<?> cls, boolean nillable) {
        String name = variableManager.createId("value");

        JVar var;
        if (!cls.isPrimitive() && nillable) {
            var = this.method.body().decl(model._ref(cls), name, JExpr._null());
            JConditional cond = this.method.body()._if(xsrVar.invoke("isXsiNil").not());
            
            JInvocation invocation = xsrVar.invoke(method);
            cond._then().assign(var, invocation);
        } else {
            var = this.method.body().decl(model._ref(cls), name, xsrVar.invoke(method));
        }
        
        return var;
    }

    public JVar asString() {
        return as(String.class, false);
    }
    
    public ElementParserBuilder newState() {
        return newState(preElementBlock);
    }

    public ElementParserBuilder newState(JBlock block) {
        return newState(block, null);
    }

    public ElementParserBuilder newState(JBlock block, String methodNameHint) {
        ElementParserBuilderImpl b = new ElementParserBuilderImpl(this, false, name, methodNameHint);
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
        JBlock block = preElementBlock;
        
        ElementParserBuilderImpl b = (ElementParserBuilderImpl) builder;
        JMethod nextMethod = b.getMethod();
        
        JInvocation invocation = JExpr.invoke(nextMethod).arg(xsrVar).arg(rtContextVar);
        for (JVar v : b.variables) {
            invocation.arg(v);
        }

        varName = variableManager.createId(varName);
        return block.decl(type, varName, invocation);
    }

    public CodeBody getBody() {
        return new CodeBodyImpl(this) {
            public JBlock getBlock() {
                return preElementBlock;
            }
        };
    }

    public JBlock getTailBlock() {
        return tailBlock;
    }

    public void write() {
        if (written) return;
        
        written = true;
        
        JBlock b = method.body();

        if (!valueType &&
            (elements.size() > 0
            || buildContext.getGlobalElements().size() > 0 
            || xsiTypes.size() > 0
            || attributes.size() > 0
            || unexpectedXsiType != null
            || anyAttribute != null
            || anyElement != null
            || mixedElement != null)) {
            writeMainLoop();
        } else {
            b.add(removeBraces(codeBlock));
            codeBlock.add(removeBraces(preElementBlock));
        }
        
        for (ElementParserBuilderImpl e : states) {
            e.write();
        }
        
        if(!tailBlock.getContents().isEmpty()) {
            preElementBlock.add(removeBraces(tailBlock));
        }

        // Add return statement to the end of the block
        if (returnType != null && _return != null) {
            preElementBlock.add(new JBlankLine());
            setReturnType(returnType);
            preElementBlock._return(_return);
        }
        
        if (root && returnType == null) {
            b.add(new JBlankLine());
            b._return(JExpr._null());
        }

        if (root) {
            writeReadAsType();
            if (baseClass != null) {
                readerClass._extends(baseClass);
            }
        }
    }
    
    protected void writeReadAsType() {
        if (!addReadAsType) return;
        JMethod m = getReaderClass().method(JMod.PUBLIC, Object.class, "read");
        m.param(XoXMLStreamReader.class, "reader");
        m.param(buildContext.getMarshalContextClass(), "context");
        JVar typeVar = m.param(QName.class, "type");
        m._throws(XMLStreamException.class);
        
        JBlock block = m.body();
        writeXsiChecks(block, typeVar);
        
        block.add(new JBlankLine());
        block._return(JExpr._null());
    }

    /**
     * Write out a loop which will read in a sequence of elements.
     *
     */
    private void writeMainLoop() {
        JBlock b = method.body();
        
        // Add XSI checks
        if (returnType != null || !valueType && depth > 1) {
            writeXsiChecks(b);
        }
        
        // Add the user constructed codeblock and continue from there
        b.add(codeBlock);
        b = codeBlock;

        writeAttributeReader(b);

        // Add the user constructed codeblock and continue from there
        b.add(preElementBlock);
        b = preElementBlock;

        if (!elements.isEmpty() || !xsiTypes.isEmpty() || anyElement != null || mixedElement != null || allowUnknown ) {
            b.add(new JBlankLine());
            b.add(new JLineComment("Read elements"));

            if (forEachChildElement != null && !allowUnknown) {
                b.add(forEachChildElement);
                writeElementReader(elements, forEachChildElement.body(), forEachChildElement.var(), false);
            } else {
                // declare variables used during element reading
                JVar targetDepthVar = b.decl(model._ref(int.class), "targetDepth", xsrVar.invoke("getDepth").plus(JExpr.lit(1)));
                JVar event = b.decl(model._ref(int.class), "event", xsrVar.invoke("nextTagIgnoreAll"));
                JVar depthVar = b.decl(model._ref(int.class), "depth", xsrVar.invoke("getDepth"));

//            JClass sysType = (JClass) model._ref(System.class);
//            if (depth != 1)
//                b.add(sysType.staticRef("out").invoke("println").arg(JExpr.lit("TD ").plus(targetDepthVar)
//                         .plus(JExpr.lit(" Depth: ")).plus(depthVar)
//                         .plus(JExpr.lit(" Name: " + name).plus(JExpr.lit(" Current: "))
//                               .plus(xsrVar.invoke("getName")))));

                JBlock loop = b._while(depthVar.gte(targetDepthVar.minus(JExpr.lit(1)))).body();

                b = loop._if(event.eq(JExpr.lit(XMLStreamConstants.START_ELEMENT)))._then();


                JConditional ifDepth = b._if(depthVar.eq(targetDepthVar));

                writeElementReader(elements, ifDepth._then(), xsrVar, false);

                if (allowUnknown) {
                    writeElementReader(buildContext.getGlobalElements(), ifDepth._else(), xsrVar, true);
                }

                JConditional ifHasNext = loop._if(xsrVar.invoke("hasNext"));
                ifHasNext._then().assign(event, xsrVar.invoke("next"));
                ifHasNext._then().assign(depthVar, xsrVar.invoke("getDepth"));
                ifHasNext._else()._break();
            }
        }
        removeBraces(preElementBlock);
    }

    private void writeAttributeReader(JBlock b) {
        if (attributes.isEmpty() && anyAttribute == null) {
            return;
        }

        b.add(new JBlankLine());
        b.add(new JLineComment("Read attributes"));

        JExpression attName;
        JExpression attNs;
        JExpression attValue;
        if (forEachAttribute != null) {
            b.add(forEachAttribute);
            b = forEachAttribute.body();
            JVar attributeVar = forEachAttribute.var();
            attName = attributeVar.invoke("getLocalName");
            attNs = attributeVar.invoke("getNamespace");
            attValue = attributeVar.invoke("getValue");
        } else {
            JForLoop loop = b._for();
            JVar var = loop.init(model._ref(int.class), "i", JExpr.lit(0));
            loop.test(var.lt(xsrVar.invoke("getAttributeCount")));
            loop.update(var.assignPlus(JExpr.lit(1)));

            b = loop.body();
            attName = b.decl(model._ref(String.class), "attName", xsrVar.invoke("getAttributeLocalName").arg(var));
            attNs = b.decl(model._ref(String.class), "attNs", xsrVar.invoke("getAttributeNamespace").arg(var));
            attValue = b.decl(model._ref(String.class), "attValue", xsrVar.invoke("getAttributeValue").arg(var));
        }


        JIfElseBlock attributesBlock = new JIfElseBlock();
        b.add(attributesBlock);
        for (Map.Entry<QName, ExpectedAttribute> e : attributes.entrySet()) {
            QName name = e.getKey();
            ExpectedAttribute expectedAttribute = e.getValue();

            JExpression qnameCompare = buildQNameCompare(name, attName, attNs);
            JBlock block = attributesBlock.addCondition(qnameCompare);

            List<JExpression> vars = null;
            if (expectedAttribute.getParserBuilder() != null) {
                AttributeParserBuilderImpl builder = expectedAttribute.getParserBuilder();

                // if we have a builder we need to pass the attribute value
                builder.getMethod().param(model._ref(String.class), "_attValue");
                vars = new ArrayList<JExpression>(builder.variables);
                vars.add(attValue);

                // todo why?
                builder.getMethod().body().add(builder.codeBlock);
            }

            writeReader(block, xsrVar, expectedAttribute, vars);
        }

        if (anyAttribute != null) {
            JBlock block = attributesBlock.addCondition(model.ref(XMLConstants.class).staticRef("W3C_XML_SCHEMA_INSTANCE_NS_URI").ne(attNs));
            writeReader(block, xsrVar, anyAttribute, null);
        }
    }

    private void writeXsiChecks(JBlock b) {
        if(xsiTypes.isEmpty() && unexpectedXsiType == null) {
            return; // no @xsi:type to look for.
        }

        b.add(new JBlankLine());
        b.add(new JLineComment("Check xsi:type"));
        JVar xsiType = b.decl(model._ref(QName.class), "xsiType", xsrVar.invoke("getXsiType"));
        JConditional cond = b._if(xsiType.ne(JExpr._null())); 
        
        writeXsiChecks(cond._then(), xsiType);
    }

    private void writeXsiChecks(JBlock block, JVar xsiType) {
        JIfElseBlock xsiTypesBlock = new JIfElseBlock();
        block.add(xsiTypesBlock);
        for (Map.Entry<QName, ExpectedXsiType> entry : xsiTypes.entrySet()) {
            QName name = entry.getKey();
            ExpectedXsiType expectedXsiType = entry.getValue();

            JExpression qnameCompare = buildQNameCompare(name, xsiType.invoke("getLocalPart"), xsiType.invoke("getNamespaceURI"));
            JBlock xsiTypeBlock = xsiTypesBlock.addCondition(qnameCompare);

            // xsi:type elements are immediately returned to the caller
            writeReader(xsiTypeBlock, xsrVar, expectedXsiType, null);

            ElementParserBuilderImpl parserBuilder = expectedXsiType.getParserBuilder();
            if (parserBuilder != null && expectedXsiType.getParserBuilder().returnType == null) {
                xsiTypeBlock._return();
            }
        }

        if (unexpectedXsiType != null) {
            JBlock anyBlock = block;
            if (xmlType != null) {
                JExpression localInv = JExpr.lit(xmlType.getLocalPart()).ne(xsiType.invoke("getLocalPart"));
                JExpression nsInv = JExpr.lit(xmlType.getNamespaceURI()).ne(xsiType.invoke("getNamespaceURI"));
                if (xmlType.getNamespaceURI().equals("")) {
                    nsInv = nsInv.cand(xsiType.invoke("getNamespaceURI").ne(JExpr._null()));
                }
                JExpression qnameCompare = localInv.cor(nsInv);

                anyBlock = xsiTypesBlock.addCondition(qnameCompare);
            } else if (!xsiTypesBlock.ifConditions().isEmpty()) {
                anyBlock = xsiTypesBlock._else().block();
            }
            writeReader(anyBlock, xsrVar, unexpectedXsiType, null);
        }
    }

    private void writeElementReader(Map<QName, ExpectedElement> elements, JBlock block, JVar xsrVar, boolean global) {
        if (depth == 1 && !global && checkXsiTypes && returnType == null) {
            writeXsiChecks(block);
        }

        JIfElseBlock elementsBlock = new JIfElseBlock();
        block.add(elementsBlock);

        if (mixedElement != null) {
            JBlock stringBlock = elementsBlock.addCondition(xsrVar.invoke("isCharacters"));
            writeReader(stringBlock, xsrVar, mixedElement, null);
        }

        for (Map.Entry<QName, ExpectedElement> entry : elements.entrySet()) {
            QName name = entry.getKey();
            ExpectedElement expectedElement = entry.getValue();

            JExpression qnameCompare = buildQNameCompare(name, xsrVar.invoke("getLocalName"), xsrVar.invoke("getNamespaceURI"));
            JBlock elementBlock = elementsBlock.addCondition(qnameCompare);

            writeReader(elementBlock, xsrVar, expectedElement, expectedElement.getVars());
        }

        if (anyElement != null) {
            JBlock anyBlock = block;
            if (!elementsBlock.ifConditions().isEmpty()) {
                anyBlock = elementsBlock._else().block();
            }
            
            writeReader(anyBlock, xsrVar, anyElement, null);
        }
    }

    private void writeReader(JBlock block, JVar xsrVar, Expected expected, List<? extends JExpression> vars) {
        JBlock readBlock = expected.getReadBlock();
        AbstractParserBuilder builder = expected.getParserBuilder();
        if (builder != null) {
            JMethod readMethod = builder.getMethod();

            JInvocation invocation = JExpr.invoke(readMethod).arg(xsrVar).arg(rtContextVar);

            // Global reader methods don't have arguments
            if (vars == null) vars = builder.getVariables();
            if (vars != null) {
                for (JExpression var : vars) {
                    invocation.arg(var);
                }
            }

            if (readBlock != null) {
                if (expected.getReadVar() != null) {
                    JVar readVar = expected.getReadVar();
                    readVar.init(invocation);
                } else {
                    readBlock.add(invocation);
                }
                block.add(removeBraces(readBlock));
            } else if (root && builder.returnType != null) {
                block._return(invocation);
            } else {
                block.add(invocation);
            }

            // TODO: throw exception if unknown elements are encountered and allowUnknown == false
            if (builder != this) {
                builder.write();
            }
        } else if (readBlock != null) {
            block.add(removeBraces(readBlock));
        }
    }

    private JExpression buildQNameCompare(QName name, JExpression localPart, JExpression namespaceUri) {
        JExpression localInv = JExpr.lit(name.getLocalPart()).eq(localPart);
        String ns = name.getNamespaceURI();
        JExpression nsInv = JExpr.lit(ns).eq(namespaceUri);
        if (ns.equals("")) {
            nsInv = nsInv.cor(namespaceUri.eq(JExpr._null()));
        }

        JExpression qnameCompare = localInv.cand(nsInv);
        return qnameCompare;
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

    public static interface Expected {
        public AbstractParserBuilder getParserBuilder();
        public JBlock getReadBlock();
        public JVar getReadVar();
    }

    public static class ExpectedAttribute implements Expected {
        private AttributeParserBuilderImpl parserBuilder;
        private JVar readVar;
        private JBlock readBlock;

        public AttributeParserBuilderImpl getParserBuilder() {
            return parserBuilder;
        }

        public void setParserBuilder(AttributeParserBuilderImpl parserBuilder) {
            this.parserBuilder = parserBuilder;
        }

        public JBlock getReadBlock() {
            return readBlock;
        }

        public void setReadBlock(JBlock readBlock) {
            this.readBlock = readBlock;
        }

        public JVar getReadVar() {
            return readVar;
        }

        public void setReadVar(JVar readVar) {
            this.readVar = readVar;
        }
    }

    public static class ExpectedElement implements Expected {
        private ElementParserBuilderImpl parserBuilder;
        private List<JExpression> vars;
        private JVar readVar;
        private JBlock readBlock;

        public ElementParserBuilderImpl getParserBuilder() {
            return parserBuilder;
        }

        public void setParserBuilder(ElementParserBuilderImpl parserBuilder) {
            this.parserBuilder = parserBuilder;
        }

        public List<JExpression> getVars() {
            return vars;
        }

        public void setVars(List<JExpression> vars) {
            this.vars = vars;
        }

        public void setVars(JExpression... vars) {
            this.vars = new ArrayList<JExpression>(Arrays.asList(vars));
        }

        public JVar getReadVar() {
            return readVar;
        }

        public void setReadVar(JVar readVar) {
            this.readVar = readVar;
        }

        public JBlock getReadBlock() {
            return readBlock;
        }

        public void setReadBlock(JBlock readBlock) {
            this.readBlock = readBlock;
        }
    }

    public static class ExpectedXsiType implements Expected {
        private ElementParserBuilderImpl parserBuilder;
        private JVar readVar;
        private JBlock readBlock;

        public ElementParserBuilderImpl getParserBuilder() {
            return parserBuilder;
        }

        public void setParserBuilder(ElementParserBuilderImpl parserBuilder) {
            this.parserBuilder = parserBuilder;
        }

        public JVar getReadVar() {
            return readVar;
        }

        public void setReadVar(JVar readVar) {
            this.readVar = readVar;
        }

        public JBlock getReadBlock() {
            return readBlock;
        }

        public void setReadBlock(JBlock readBlock) {
            this.readBlock = readBlock;
        }
    }

    private static JBlock removeBraces(JBlock block) {
        try {
            Field field = JBlock.class.getDeclaredField("bracesRequired");
            field.setAccessible(true);
            field.setBoolean(block, false);
            field = JBlock.class.getDeclaredField("indentRequired");
            field.setAccessible(true);
            field.setBoolean(block, false);
        } catch(Throwable ignored) {
        }
        return block;
    }
}
