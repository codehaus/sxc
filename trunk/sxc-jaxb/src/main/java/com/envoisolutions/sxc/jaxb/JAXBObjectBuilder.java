/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.envoisolutions.sxc.jaxb;

import static java.beans.Introspector.decapitalize;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.namespace.QName;
import javax.xml.bind.JAXBException;

import com.envoisolutions.sxc.builder.BuildException;
import com.envoisolutions.sxc.builder.impl.ElementParserBuilderImpl;
import com.envoisolutions.sxc.builder.impl.ElementWriterBuilderImpl;
import com.envoisolutions.sxc.builder.impl.IdentityManager;
import com.envoisolutions.sxc.builder.impl.JBlankLine;
import com.envoisolutions.sxc.builder.impl.JLineComment;
import static com.envoisolutions.sxc.builder.impl.IdentityManager.toValidId;
import static com.envoisolutions.sxc.jaxb.JavaUtils.capitalize;
import com.envoisolutions.sxc.util.XoXMLStreamReader;
import com.envoisolutions.sxc.util.XoXMLStreamWriter;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JCatchBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JTryBlock;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;

public class JAXBObjectBuilder {
    private final JAXBObjectBuilder parent;
    private final BuilderContext builderContext;
    private final Class type;
    private final QName xmlRootElement;
    private final QName xmlType;
    private final JDefinedClass jaxbObjectClass;
    private final boolean mixed;
    private final boolean wrapperElement;
    private ElementParserBuilderImpl parserBuilder;
    private ElementWriterBuilderImpl writerBuilder;
    private final IdentityManager fieldManager;
    private final Map<String, JFieldVar> adapters;
    private final Map<String, JFieldVar> privateFieldAccessors;
    private final Map<String, JFieldVar> privatePropertyAccessors;
    private JFieldVar datatypeFactory;
    private JMethod constructor;
    private JVar readObject;
    private final Set<QName> expectedAttributes = new LinkedHashSet<QName>();
    private boolean expectAnyAttribute;
    private final Set<QName> expectedElements = new LinkedHashSet<QName>();
    private boolean expectAnyElement;
    private boolean expectMixedElement;
    private boolean expectValue;
    private JVar writerDefaultPrefix;
    private String writerDefaultNS;
    private JInvocation superInvocation;
    private Set<String> dependencies = new TreeSet<String>();
    private JFieldVar lifecycleCallbackVar;

    public JAXBObjectBuilder(JAXBObjectBuilder parent, ElementParserBuilderImpl parserBuilder, boolean mixed) {
        this.parent = parent;
        this.builderContext = parent.builderContext;
        this.type = parent.type;
        this.xmlRootElement = parent.xmlRootElement;
        this.xmlType = parent.xmlType;
        this.jaxbObjectClass = parent.jaxbObjectClass;
        this.parserBuilder = parserBuilder;
        this.mixed = mixed;
        wrapperElement = true;
        fieldManager = parent.fieldManager;
        adapters = parent.adapters;
        privateFieldAccessors = parent.privateFieldAccessors;
        privatePropertyAccessors = parent.privatePropertyAccessors;
    }

    public JAXBObjectBuilder(BuilderContext builderContext, Class type, QName xmlRootElement, QName xmlType, boolean mixed) {
        this.parent = null;
        this.builderContext = builderContext;
        this.type = type;
        this.xmlRootElement = xmlRootElement;
        this.xmlType = xmlType;
        this.mixed = mixed;
        wrapperElement = false;
        fieldManager = new IdentityManager();
        adapters = new TreeMap<String, JFieldVar>();
        privateFieldAccessors = new TreeMap<String, JFieldVar>();
        privatePropertyAccessors = new TreeMap<String, JFieldVar>();

        try {
            jaxbObjectClass = builderContext.getCodeModel()._class("sxc." + type.getName() + "JAXB");
            jaxbObjectClass._extends(builderContext.getCodeModel().ref(JAXBObject.class));
        } catch (JClassAlreadyExistsException e) {
            throw new BuildException(e);
        }

        constructor = jaxbObjectClass.constructor(JMod.PUBLIC);
        superInvocation = constructor.body().invoke("super")
                .arg(JExpr.dotclass(builderContext.toJClass(type)))
                .arg(newQName(xmlRootElement))
                .arg(newQName(xmlType));


        // INSTANCE static field
        JVar instance = jaxbObjectClass.field(JMod.PUBLIC | JMod.STATIC | JMod.FINAL, jaxbObjectClass, "INSTANCE", JExpr._new(jaxbObjectClass));

        // add static read method
        JMethod method = jaxbObjectClass.method(JMod.PUBLIC | JMod.STATIC, type, "read" + type.getSimpleName())._throws(Exception.class);
        JVar xsrVar = method.param(XoXMLStreamReader.class, "reader");
        JVar contextVar = method.param(builderContext.getBuildContext().getUnmarshalContextClass(), "context");
        method.body()._return(instance.invoke("read").arg(xsrVar).arg(contextVar));

        // add static write method
        method = jaxbObjectClass.method(JMod.PUBLIC | JMod.STATIC, void.class, "write" + type.getSimpleName())._throws(Exception.class);
        xsrVar = method.param(XoXMLStreamWriter.class, "writer");
        JVar item = method.param(type, toValidId(decapitalize(type.getSimpleName())));
        contextVar = method.param(builderContext.getBuildContext().getMarshalContextClass(), "context");
        method.body().add(instance.invoke("write").arg(xsrVar).arg(item).arg(contextVar));

        // add lifecycle callabck field
        JClass callbackClass = builderContext.toJClass(LifecycleCallback.class);
        lifecycleCallbackVar = jaxbObjectClass.field(JMod.PRIVATE | JMod.STATIC | JMod.FINAL, callbackClass, fieldManager.createId("lifecycleCallback"), JExpr._new(callbackClass).arg(JExpr.dotclass(builderContext.toJClass(type))));
    }

    private JExpression newQName(QName xmlRootElement) {
        if (xmlRootElement == null) {
            return JExpr._null();
        }
        return JExpr._new(builderContext.toJClass(QName.class))
                .arg(JExpr.lit(xmlRootElement.getNamespaceURI()).invoke("intern"))
                .arg(JExpr.lit(xmlRootElement.getLocalPart()).invoke("intern"));
    }

    public Class getType() {
        return type;
    }

    public QName getXmlRootElement() {
        return xmlRootElement;
    }

    public QName getXmlType() {
        return xmlType;
    }

    public JDefinedClass getJAXBObjectClass() {
        return jaxbObjectClass;
    }

    public void addDependency(JClass dependency) {
        if (jaxbObjectClass.fullName().equals(dependency.fullName())) return;
        
        if (parent == null) {
            if (dependencies.add(dependency.fullName())) {
                superInvocation.arg(dependency.dotclass());
            }
        } else {
            parent.addDependency(dependency);
        }
    }

    public void write() {
        getParserBuilder();
        if (!Modifier.isAbstract(type.getModifiers())) {
            if (!expectAnyAttribute) {
                JBlock block = new JBlock();
                parserBuilder.setAnyAttributeBlock(null, block);
                JInvocation invocation = block.invoke(parserBuilder.getContextVar(), "unexpectedAttribute").arg(getAttributeVar());
                for (QName expectedAttribute : expectedAttributes) {
                    invocation.arg(JExpr._new(builderContext.toJClass(QName.class)).arg(expectedAttribute.getNamespaceURI()).arg(expectedAttribute.getLocalPart()));
                }
            }

            if (!expectAnyElement && !expectValue) {
                JBlock block = new JBlock();
                parserBuilder.setAnyElementBlock(null, block);
                JInvocation invocation = block.invoke(parserBuilder.getContextVar(), "unexpectedElement").arg(getChildElementVar());
                for (QName expectedElement : expectedElements) {
                    invocation.arg(JExpr._new(builderContext.toJClass(QName.class)).arg(expectedElement.getNamespaceURI()).arg(expectedElement.getLocalPart()));
                }
            }

            // add afterUnmarshal
            getReadTailBlock().add(new JBlankLine());
            JExpression lifecycleCallbackRef = lifecycleCallbackVar;
            if (parserBuilder.getVariableManager().containsId(lifecycleCallbackVar.name())) {
                lifecycleCallbackRef = jaxbObjectClass.staticRef(lifecycleCallbackVar.name());
            }
            getReadTailBlock().invoke(getReadContextVar(), "afterUnmarshal").arg(readObject).arg(lifecycleCallbackRef);
        }

        JBlock block = new JBlock();
        parserBuilder.setUnexpectedXsiTypeBlock(null, block);
        block._return(parserBuilder.getContextVar().invoke("unexpectedXsiType").arg(getXSR()).arg(JExpr.dotclass(builderContext.toJClass(type))));

        parserBuilder.write();

        getWriterBuilder();
        if (!Modifier.isAbstract(type.getModifiers())) {
            // add afterMarshal
            getWriteMethod().body().add(new JBlankLine());
            JExpression lifecycleCallbackRef = lifecycleCallbackVar;
            if (writerBuilder.getVariableManager().containsId(lifecycleCallbackVar.name())) {
                lifecycleCallbackRef = jaxbObjectClass.staticRef(lifecycleCallbackVar.name());
            }
            getWriteMethod().body().invoke(getReadContextVar(), "afterMarshal").arg(getWriteObject()).arg(lifecycleCallbackRef);
        }
        writerBuilder.write();
    }

    public ElementParserBuilderImpl getParserBuilder() {
        if (parserBuilder == null) {
            parserBuilder = new ElementParserBuilderImpl(builderContext.getBuildContext(), jaxbObjectClass, type, mixed);
            parserBuilder.setXmlType(xmlType);
            parserBuilder.setAllowUnkown(false);
            parserBuilder.setBaseClass(builderContext.getCodeModel().ref(JAXBObject.class).narrow(type));
            parserBuilder.getMethod()._throws(Exception.class);

            // @SuppressWarnings({"StringEquality"})
            parserBuilder.getReaderClass().annotate(SuppressWarnings.class).paramArray("value").param("StringEquality");

            if (!type.isEnum()) {
                // if isXsiNil return null;
                JBlock body = parserBuilder.getMethod().body();
                body.add(new JBlankLine());
                body.add(new JLineComment("Check for xsi:nil"));
                body._if(parserBuilder.getXSR().invoke("isXsiNil"))._then()._return(JExpr._null());

                // if context is null, initialize context
                body.add(new JBlankLine());
                JBlock contextNullBlock = body._if(parserBuilder.getContextVar().eq(JExpr._null()))._then();
                contextNullBlock.assign(parserBuilder.getContextVar(), JExpr._new(builderContext.toJClass(RuntimeContext.class)));
                body.add(new JBlankLine());

                if (!Modifier.isAbstract(type.getModifiers())) {
                    // create bean instance
                    String varName = decapitalize(type.getSimpleName());
                    varName = parserBuilder.getVariableManager().createId(varName);
                    JType beanType = builderContext.getCodeModel()._ref(type);
                    readObject = body.decl(beanType, decapitalize(varName), JExpr._new(beanType));

                    // add beforeUnmarshal
                    JExpression lifecycleCallbackRef = lifecycleCallbackVar;
                    if (parserBuilder.getVariableManager().containsId(lifecycleCallbackVar.name())) {
                        lifecycleCallbackRef = jaxbObjectClass.staticRef(lifecycleCallbackVar.name());
                    }
                    body.invoke(getReadContextVar(), "beforeUnmarshal").arg(readObject).arg(lifecycleCallbackRef);
                    body.add(new JBlankLine());
                    
                    // return the bean
                    parserBuilder.getBody()._return(readObject);
                } else {
                    parserBuilder.getTailBlock()._throw(JExpr._new(builderContext.toJClass(JAXBException.class)).arg(""));
                }
            }
        }
        return parserBuilder;
    }

    public ElementWriterBuilderImpl getWriterBuilder() {
        if (wrapperElement) throw new IllegalStateException("Wrapper elements do not have a write builder");
        if (writerBuilder == null) {
            writerBuilder = new ElementWriterBuilderImpl(builderContext.getBuildContext(), jaxbObjectClass, type);
            writerBuilder.getMethod()._throws(Exception.class);

            if (!type.isEnum()) {
                JBlock body = writerBuilder.getMethod().body();

                // check for null
                JBlock nullBlock = body._if(getWriteObject().eq(JExpr._null()))._then();
                nullBlock.invoke(getXSW(), "writeXsiNil");
                nullBlock._return();
                body.add(new JBlankLine());

                // if context is null, initialize context
                JBlock contextNullBlock = body._if(writerBuilder.getContextVar().eq(JExpr._null()))._then();
                contextNullBlock.assign(writerBuilder.getContextVar(), JExpr._new(builderContext.toJClass(RuntimeContext.class)));
                body.add(new JBlankLine());
            }

        }
        return writerBuilder;
    }

    public JVar getAdapter(Class adapterType) {
        String adapterId = adapterType.getName();
        JFieldVar var = adapters.get(adapterId);
        if (var == null) {
            String fieldName = decapitalize(adapterType.getSimpleName()) + "Adapter";
            fieldName = fieldManager.createId(fieldName);
            JClass jClass = builderContext.toJClass(adapterType);
            var = jaxbObjectClass.field(JMod.PRIVATE | JMod.STATIC | JMod.FINAL, jClass, fieldName, JExpr._new(jClass));
            adapters.put(adapterId, var);
        }
        return var;
    }

    public JFieldVar getPrivateFieldAccessor(Field field) {
        String fieldId = field.getDeclaringClass().getSimpleName() + "." + field.getName();
        JFieldVar fieldAccessorField = privateFieldAccessors.get(fieldId);
        if (fieldAccessorField == null) {
            JClass fieldAccessorType = builderContext.toJClass(FieldAccessor.class).narrow(builderContext.toJClass(field.getDeclaringClass()), builderContext.getGenericType(field.getGenericType()));
            JInvocation newFieldAccessor = JExpr._new(fieldAccessorType)
                    .arg(builderContext.getCodeModel().ref(field.getDeclaringClass()).staticRef("class"))
                    .arg(JExpr.lit(field.getName()));

            String fieldName = fieldManager.createId(decapitalize(field.getDeclaringClass().getSimpleName()) + capitalize(field.getName()));
            fieldAccessorField = jaxbObjectClass.field(JMod.PRIVATE | JMod.STATIC | JMod.FINAL, fieldAccessorType, fieldName, newFieldAccessor);
            privateFieldAccessors.put(fieldId, fieldAccessorField);
        }
        return fieldAccessorField;
    }

    public JFieldVar getPrivatePropertyAccessor(Method getter, Method setter, String propertyName) {
        Class beanClass = getter != null ? getter.getDeclaringClass() : setter.getDeclaringClass();
        Type propertyType = getter != null ? getter.getGenericReturnType() : setter.getGenericParameterTypes()[0];

        String fieldId = beanClass.getSimpleName() + "." + propertyName;
        JFieldVar fieldAccessorField = privatePropertyAccessors.get(fieldId);
        if (fieldAccessorField == null) {
            JClass fieldAccessorType = builderContext.toJClass(PropertyAccessor.class).narrow(builderContext.toJClass(beanClass), builderContext.getGenericType(propertyType));
            JInvocation newFieldAccessor = JExpr._new(fieldAccessorType)
                    .arg(builderContext.dotclass(beanClass))
                    .arg(builderContext.dotclass(propertyType))
                    .arg(getter == null ? JExpr._null() : JExpr.lit(getter.getName()))
                    .arg(setter == null ? JExpr._null() : JExpr.lit(setter.getName()));

            String fieldName = fieldManager.createId(decapitalize(beanClass.getSimpleName()) + capitalize(propertyName));
            fieldAccessorField = jaxbObjectClass.field(JMod.PRIVATE | JMod.STATIC | JMod.FINAL, fieldAccessorType, fieldName, newFieldAccessor);
            privatePropertyAccessors.put(fieldId, fieldAccessorField);
        }
        return fieldAccessorField;
    }

    public JFieldVar getDatatypeFactory() {
        if (datatypeFactory == null) {
            datatypeFactory = jaxbObjectClass.field(JMod.PRIVATE | JMod.FINAL, builderContext.toJClass(DatatypeFactory.class), fieldManager.createId("datatypeFactory"));

            // add code to constructor which initializes the static dtFactory field
            JTryBlock tryBlock = constructor.body()._try();
            tryBlock.body().assign(datatypeFactory, builderContext.toJClass(DatatypeFactory.class).staticInvoke("newInstance"));

            JCatchBlock catchBlock = tryBlock._catch((builderContext.toJClass(DatatypeConfigurationException.class)));
            catchBlock.body()._throw(JExpr._new(builderContext.toJClass(RuntimeException.class)).arg("Unable to initialize DatatypeFactory").arg(catchBlock.param("e")));
        }

        return datatypeFactory;
    }

    public JMethod getReadMethod() {
        return getParserBuilder().getMethod();
    }

    public IdentityManager getReadVariableManager() {
        return getParserBuilder().getVariableManager();
    }

    public JBlock expectAttribute(QName attributeName) {
        if (expectedAttributes.contains(attributeName)) throw new IllegalArgumentException("Attribute is alredy expected " + attributeName);
        expectedAttributes.add(attributeName);

        JBlock block = new JBlock();
        getParserBuilder().setAttributeBlock(attributeName, null, block);
        return block;
    }

    public JBlock expectAnyAttribute() {
        if (expectAnyAttribute) throw new IllegalArgumentException("Any attribute is alredy expected");
        expectAnyAttribute = true;

        JBlock block = new JBlock();
        getParserBuilder().setAnyAttributeBlock(null, block);
        return block;
    }

    public JBlock expectElement(QName elementName) {
        if (expectValue) throw new IllegalArgumentException("A value is alredy expected");
        if (expectedElements.contains(elementName)) {
            throw new IllegalArgumentException("Element is alredy expected " + elementName);
        }
        expectedElements.add(elementName);

        JBlock block = new JBlock();
        getParserBuilder().setElementBlock(elementName, null, block);
        return block;
    }

    public JBlock expectAnyElement() {
        if (expectValue) throw new IllegalArgumentException("A value is alredy expected");
        if (expectAnyElement) throw new IllegalArgumentException("Any element is alredy expected");
        expectAnyElement = true;

        JBlock block = new JBlock();
        getParserBuilder().setAnyElementBlock(null, block);
        return block;
    }

    public JBlock expectMixedElement() {
        if (expectValue) throw new IllegalArgumentException("A value is alredy expected");
        if (expectMixedElement) throw new IllegalArgumentException("Mixed element is alredy expected");
        expectMixedElement = true;

        JBlock block = new JBlock();
        getParserBuilder().setMixedElementBlock(null, block);
        return block;
    }

    public JBlock expectValue() {
        if (!expectedElements.isEmpty()) throw new IllegalArgumentException("Elements are alredy expected " + expectedElements);
        if (expectAnyElement) throw new IllegalArgumentException("Any element is alredy expected");

        expectValue = true;
        return getParserBuilder().getBody().getBlock();
    }

    public JAXBObjectBuilder expectWrapperElement(QName elementName, JVar beanVar, String propertyName, boolean mixed) {
        if (expectedElements.contains(elementName)) throw new IllegalArgumentException("Element is alredy expected " + elementName);
        expectedElements.add(elementName);

        ElementParserBuilderImpl parserBuilder = new ElementParserBuilderImpl(builderContext.getBuildContext(), jaxbObjectClass, null, mixed, 2, propertyName);
        parserBuilder.setAllowUnkown(false);

        String name = parserBuilder.getVariableManager().createId(decapitalize(beanVar.type().name()));
        parserBuilder.getMethod().param(beanVar.type(), name);

        parserBuilder.getMethod()._throws(Exception.class);

        getParserBuilder().expectElement(elementName, parserBuilder, beanVar);

        JBlock block = new JBlock();
        block.add(new JLineComment("ELEMENT WRAPPER: " + propertyName));
        getParserBuilder().setElementBlock(elementName, null, block);

        JAXBObjectBuilder builder = new JAXBObjectBuilder(this, parserBuilder, mixed);
        return builder;
    }

    public JBlock expectXsiType(QName typeName) {
        JBlock block = new JBlock();
        getParserBuilder().setXsiTypeBlock(typeName, null, block);
        return block;
    }

    public JFieldVar getLifecycleCallbackVar() {
        return lifecycleCallbackVar;
    }

    public JBlock getReadTailBlock() {
        return getParserBuilder().getTailBlock();
    }

    public JVar getXSR() {
        return getParserBuilder().getXSR();
    }

    public JVar getReadContextVar() {
        return getParserBuilder().getContextVar();
    }

    public JVar getReadObject() {
        getParserBuilder();
        return readObject;
    }

    public JMethod getWriteMethod() {
        return getWriterBuilder().getMethod();
    }

    public JVar getAttributeVar() {
        return getParserBuilder().getAttributeVar();
    }

    public JVar getChildElementVar() {
        return getParserBuilder().getChildElementVar();
    }

    public IdentityManager getWriteVariableManager() {
        return getWriterBuilder().getVariableManager();
    }

    public JVar getXSW() {
        return getWriterBuilder().getXSW();
    }

    public JVar getWriteContextVar() {
        return getWriterBuilder().getContextVar();
    }

    public JVar getWriteObject() {
        return getWriterBuilder().getObject();
    }

    public String getWriterDefaultNS() {
        return writerDefaultNS;
    }

    public void setWriterDefaultNS(String writerDefaultNS) {
        if (writerDefaultNS == null) throw new NullPointerException("writerDefaultNS is null");
        if (this.writerDefaultNS != null) throw new IllegalStateException("writerDefaultNS already set");

        getWriterBuilder();

        this.writerDefaultNS = writerDefaultNS;
        writerDefaultPrefix = writerBuilder.getMethod().body().decl(builderContext.toJClass(String.class), writerBuilder.getVariableManager().createId("prefix"));
        writerDefaultPrefix.init(writerBuilder.getXSW().invoke("getUniquePrefix").arg(writerDefaultNS));
    }

    public JExpression getWriterPrefix(String namespaceURI) {
        getWriterBuilder();

        if ("http://www.w3.org/XML/1998/namespace".equals(namespaceURI)) {
            return JExpr.lit("xml");
        } else if (writerDefaultNS != null && writerDefaultNS.equals(namespaceURI)) {
            return writerDefaultPrefix;
        } else {
            return getXSW().invoke("getUniquePrefix").arg(namespaceURI);
        }
    }

    public JInvocation getWriteStartElement(QName name) {
        getWriterBuilder();

        if ("http://www.w3.org/XML/1998/namespace".equals(name.getNamespaceURI())) {
            return getXSW().invoke("writeStartElement").arg("xml").arg(name.getLocalPart()).arg(name.getNamespaceURI());
        } else if (writerDefaultNS != null && writerDefaultNS.equals(name.getNamespaceURI())) {
            return getXSW().invoke("writeStartElement").arg(writerDefaultPrefix).arg(name.getLocalPart()).arg(name.getNamespaceURI());
        } else {
            return getXSW().invoke("writeStartElementWithAutoPrefix").arg(name.getNamespaceURI()).arg(name.getLocalPart());
        }
    }
}
