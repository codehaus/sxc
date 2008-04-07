package com.envoisolutions.sxc.jaxb;

import java.util.Map;
import java.util.TreeMap;
import java.lang.reflect.Field;
import static java.beans.Introspector.decapitalize;
import javax.xml.stream.XMLStreamException;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.namespace.QName;

import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JVar;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JTryBlock;
import com.sun.codemodel.JCatchBlock;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JType;
import com.sun.codemodel.JExpression;
import com.envoisolutions.sxc.builder.impl.ElementParserBuilderImpl;
import com.envoisolutions.sxc.builder.impl.ElementWriterBuilderImpl;
import com.envoisolutions.sxc.builder.impl.IdentityManager;
import com.envoisolutions.sxc.builder.BuildException;
import com.envoisolutions.sxc.Context;
import com.envoisolutions.sxc.util.XoXMLStreamReader;
import com.envoisolutions.sxc.util.XoXMLStreamWriter;
import com.envoisolutions.sxc.util.FieldAccessor;

public class MarshallerBuilder {
    private final BuilderContext builderContext;
    private final Class type;
    private final QName xmlRootElement;
    private final QName xmlType;
    private final JDefinedClass marshallerClass;
    private final boolean wrapperElement;
    private ElementParserBuilderImpl parserBuilder;
    private ElementWriterBuilderImpl writerBuilder;
    private final IdentityManager fieldManager = new IdentityManager();
    private final Map<String, JFieldVar> adapters = new TreeMap<String, JFieldVar>();
    private final Map<String, JFieldVar> privateFieldAccessors = new TreeMap<String, JFieldVar>();
    private JFieldVar datatypeFactory;
    private JMethod constructor;
    private JVar readObject;
    private JVar writerDefaultPrefix;
    private String writerDefaultNS;

    public MarshallerBuilder(MarshallerBuilder parent, ElementParserBuilderImpl parserBuilder) {
        this.builderContext = parent.builderContext;
        this.type = parent.type;
        this.xmlRootElement = parent.xmlRootElement;
        this.xmlType = parent.xmlType;
        this.marshallerClass = parent.marshallerClass;
        this.parserBuilder = parserBuilder;
        wrapperElement = true;
    }

    public MarshallerBuilder(BuilderContext builderContext, Class type, QName xmlRootElement, QName xmlType) {
        this.builderContext = builderContext;
        this.type = type;
        this.xmlRootElement = xmlRootElement;
        this.xmlType = xmlType;
        wrapperElement = false;

        try {
            marshallerClass = builderContext.getCodeModel()._class("generated.sxc." + type.getName() + "JaxB");
            marshallerClass._extends(builderContext.getCodeModel().ref(JAXBMarshaller.class));
        } catch (JClassAlreadyExistsException e) {
            throw new BuildException(e);
        }

        constructor = marshallerClass.constructor(JMod.PUBLIC);
        constructor.body().invoke("super").arg(constructor.param(Context.class,"context"));

        // INSTANCE static field
        JVar instance = marshallerClass.field(JMod.PUBLIC | JMod.STATIC | JMod.FINAL, marshallerClass, "INSTANCE", JExpr._new(marshallerClass).arg(JExpr._null()));

        // add static read method
        JMethod method = marshallerClass.method(JMod.PUBLIC | JMod.STATIC, type, "read" + type.getSimpleName())._throws(XMLStreamException.class);
        JVar xsrVar = method.param(XoXMLStreamReader.class, "reader");
        JVar contextVar = method.param(builderContext.getBuildContext().getStringToObjectMap(), "properties");
        method.body()._return(instance.invoke("read").arg(xsrVar).arg(contextVar));

        // add static write method
        method = marshallerClass.method(JMod.PUBLIC | JMod.STATIC, void.class, "write" + type.getSimpleName())._throws(XMLStreamException.class)._throws(JAXBException.class);
        xsrVar = method.param(XoXMLStreamWriter.class, "writer");
        JVar item = method.param(type, java.beans.Introspector.decapitalize(type.getSimpleName()));
        contextVar = method.param(builderContext.getBuildContext().getStringToObjectMap(), "properties");
        method.body().add(instance.invoke("write").arg(xsrVar).arg(item).arg(contextVar));
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

    public JDefinedClass getMarshallerClass() {
        return marshallerClass;
    }

    public ElementParserBuilderImpl getParserBuilder() {
        if (parserBuilder == null) {
            parserBuilder = new ElementParserBuilderImpl(builderContext.getBuildContext(), marshallerClass, type);
            parserBuilder.setAllowUnkown(false);
            parserBuilder.setBaseClass(builderContext.getCodeModel().ref(JAXBMarshaller.class).narrow(type));

            // @SuppressWarnings({"StringEquality"})
            parserBuilder.getReaderClass().annotate(SuppressWarnings.class).paramArray("value").param("StringEquality");

            if (!type.isEnum()) {
                // if isXsiNil return null;
                JBlock body = parserBuilder.getMethod().body();
                body._if(parserBuilder.getXSR().invoke("isXsiNil"))._then()._return(JExpr._null());

                // create bean instance
                String varName = decapitalize(type.getSimpleName());
                varName = parserBuilder.getVariableManager().createId(varName);
                JType beanType = builderContext.getCodeModel()._ref(type);
                readObject = body.decl(beanType, decapitalize(varName), JExpr._new(beanType));

                // return the bean
                parserBuilder.getBody()._return(readObject);


            }
        }
        return parserBuilder;
    }

    public ElementWriterBuilderImpl getWriterBuilder() {
        if (wrapperElement) throw new IllegalStateException("Wrapper elements do not have a write builder");
        if (writerBuilder == null) {
            writerBuilder = new ElementWriterBuilderImpl(builderContext.getBuildContext(), marshallerClass, type);
            writerBuilder.getMethod()._throws(JAXBException.class);
        }
        return writerBuilder;
    }

    public JVar getAdapter(Class adapterType) {
        String adapterId = adapterType.getName();
        JFieldVar var = adapters.get(adapterId);
        if (var == null) {
            String fieldName = java.beans.Introspector.decapitalize(adapterType.getSimpleName()) + "Adapter";
            fieldName = fieldManager.createId(fieldName);
            JClass jClass = builderContext.toJClass(adapterType);
            var = marshallerClass.field(JMod.PRIVATE | JMod.STATIC | JMod.FINAL, jClass, fieldName, JExpr._new(jClass));
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

            String fieldName = fieldManager.createId(java.beans.Introspector.decapitalize(field.getDeclaringClass().getSimpleName()) + com.envoisolutions.sxc.jaxb.JavaUtils.capitalize(field.getName()));
            fieldAccessorField = marshallerClass.field(JMod.PRIVATE | JMod.STATIC | JMod.FINAL, fieldAccessorType, fieldName, newFieldAccessor);
            privateFieldAccessors.put(fieldId, fieldAccessorField);
        }
        return fieldAccessorField;
    }

    public JFieldVar getDatatypeFactory() {
        if (datatypeFactory == null) {
            datatypeFactory = marshallerClass.field(JMod.PRIVATE | JMod.FINAL, builderContext.toJClass(DatatypeFactory.class), fieldManager.createId("datatypeFactory"));

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
        JBlock block = new JBlock();
        getParserBuilder().setAttributeBlock(attributeName, null, block);
        return block;
    }

    public JBlock expectElement(QName elementName) {
        JBlock block = new JBlock();
        getParserBuilder().setElementBlock(elementName, null, block);
        return block;
    }

    public JBlock expectValue() {
        return getParserBuilder().getBody().getBlock();
    }

    public MarshallerBuilder expectWrapperElement(QName elementName, JVar beanVar, String propertyName) {
        ElementParserBuilderImpl parserBuilder = (ElementParserBuilderImpl) getParserBuilder().expectElement(elementName, propertyName);
        parserBuilder.passParentVariable(beanVar);

        MarshallerBuilder builder = new MarshallerBuilder(this, parserBuilder);
        return builder;
    }

    public JBlock expectXsiType(QName typeName) {
        JBlock block = new JBlock();
        getParserBuilder().setXsiTypeBlock(typeName, null, block);
        return block;
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
}
