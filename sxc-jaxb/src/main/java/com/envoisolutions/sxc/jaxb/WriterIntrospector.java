package com.envoisolutions.sxc.jaxb;

import java.awt.Image;
import static java.beans.Introspector.decapitalize;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;
import javax.activation.DataHandler;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.envoisolutions.sxc.builder.BuildException;
import com.envoisolutions.sxc.builder.impl.JBlankLine;
import com.envoisolutions.sxc.builder.impl.JIfElseBlock;
import com.envoisolutions.sxc.builder.impl.JLineComment;
import com.envoisolutions.sxc.builder.impl.JStaticImports;
import static com.envoisolutions.sxc.jaxb.JavaUtils.*;
import com.envoisolutions.sxc.jaxb.model.Bean;
import com.envoisolutions.sxc.jaxb.model.ElementMapping;
import com.envoisolutions.sxc.jaxb.model.Model;
import com.envoisolutions.sxc.jaxb.model.Property;
import com.envoisolutions.sxc.util.Base64;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JCatchBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JForEach;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JTryBlock;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;

public class WriterIntrospector {
	private static final Logger logger = Logger.getLogger(WriterIntrospector.class.getName());

    private final BuilderContext builderContext;
    private final Model model;
    private final JCodeModel codeModel;
    private final Map<Bean, MarshallerBuilder> builders = new LinkedHashMap<Bean, MarshallerBuilder>();
    private final Map<Class, JClass> enumWriters = new LinkedHashMap<Class, JClass>();

    public WriterIntrospector(BuilderContext context, Model model) throws JAXBException {
        builderContext = context;
        this.model = model;
        this.codeModel = context.getCodeModel();

        List<Bean> mybeans = new ArrayList<Bean>(model.getBeans());
        Collections.sort(mybeans, new BeanComparator());

        // declare all writer methods first, so everything exists when we build the
        for (Bean bean : mybeans) {
            if (Modifier.isAbstract(bean.getType().getModifiers())) continue;
            if (bean.getType().isEnum()) continue;

            MarshallerBuilder builder = context.getMarshallerBuilder(bean.getType(), bean.getRootElementName(), bean.getSchemaTypeName());

            LinkedHashSet<Property> allProperties = new LinkedHashSet<Property>();
            for (Bean b = bean; b != null; b = b.getBaseClass()) {
                allProperties.addAll(b.getProperties());
            }

            // set the default namespace to the most popular namespace used in properties
            String mostPopularNS = getMostPopularNS(allProperties);
            if (mostPopularNS != null) builder.setWriterDefaultNS(mostPopularNS);

            // declare all private field accessors (so they are grouped)
            for (Property property : allProperties) {
                Field field = property.getField();
                if (field != null) {
                    if (isPrivate(field)) {
                        builder.getPrivateFieldAccessor(property.getField());
                    }
                } else {
                    if (isPrivate(property.getGetter()) || isPrivate(property.getSetter())) {
                        builder.getPrivatePropertyAccessor(property.getGetter(), property.getSetter(), property.getName());
                    }
                }
            }

            // declare all adapter classes
            for (Property property : allProperties) {
                if (property.getAdapterType() != null) {
                    builder.getAdapter(property.getAdapterType());
                }
            }

            builders.put(bean, builder);
        }

        // declare all parse enum methods (after read methods so they are grouped together)
        for (Bean bean : model.getBeans()) {
            if (bean.getType().isEnum()) {
                addEnum(bean);
            }
        }
        
        // build the writer methods
        for (Bean bean : model.getBeans()) {
            if (!bean.getType().isEnum()) {
                MarshallerBuilder builder = builders.get(bean);
                if (builder != null) {
                    JBlock block = builder.getWriteMethod().body();

                    // perform instance checks
                    JIfElseBlock ifElseBlock = null;
                    for (Bean altBean : getSubstitutionTypes(builder.getType())) {
                        if (bean == altBean) continue;
                        if (ifElseBlock == null) {
                            ifElseBlock = new JIfElseBlock();
                            block.add(ifElseBlock);
                            block.add(new JBlankLine());
                        }

                        // add condition
                        JBlock altBlock = ifElseBlock.addCondition(context.dotclass(altBean.getType()).eq(builder.getWriteObject().invoke("getClass")));

                        // write xsi:type
                        QName typeName = altBean.getSchemaTypeName();
                        altBlock.invoke(builder.getXSW(), "writeXsiType").arg(typeName.getNamespaceURI()).arg(typeName.getLocalPart());

                        // call alternate marshaller
                        writeClassWriter(builder, altBean, altBlock, JExpr.cast(toJClass(altBean.getType()), builder.getWriteObject()));
                    }

                    writeProperties(builder, bean);
                }
            }
        }
    }

    private void addEnum(Bean bean) {
        String className = "generated.sxc." + bean.getType().getName() + "JaxB";
        JDefinedClass jaxbClass = codeModel._getClass(className);
        if (jaxbClass == null) {
            try {
                jaxbClass = codeModel._class(className);
            } catch (JClassAlreadyExistsException e) {
                throw new BuildException(e);
            }
        }

        JClass type = toJClass(bean.getType());
        JMethod method = jaxbClass.method(JMod.PUBLIC | JMod.STATIC, String.class, "toString")._throws(Exception.class);
        JVar xsrVar = method.param(Object.class, "bean");
        JVar parameterNameVar = method.param(String.class, "parameterName");
        JVar contextVar = method.param(toJClass(RuntimeContext.class), "context");
        JVar value = method.param(type, decapitalize(bean.getType().getSimpleName()));

        JIfElseBlock enumSwitch = new JIfElseBlock();
        method.body().add(enumSwitch);
        for (Map.Entry<Enum, String> entry : bean.getEnumMap().entrySet()) {
            Enum enumValue = entry.getKey();
            String enumText = entry.getValue();

            JBlock enumCase = enumSwitch.addCondition(type.staticRef(enumValue.name()).eq(value));
            enumCase._return(JExpr.lit(enumText));
        }

        JInvocation unexpectedInvoke = enumSwitch._else().invoke(contextVar, "unexpectedEnumConst").arg(xsrVar).arg(parameterNameVar).arg(value);
        for (Enum expectedValue : bean.getEnumMap().keySet()) {
            unexpectedInvoke.arg(type.staticRef(expectedValue.name()));
        }
        enumSwitch._else()._return(JExpr._null());

        // switch statements don't seem to compile correctly
        // JSwitch enumSwitch = method.body()._switch(value);
        // for (Map.Entry<Enum, String> entry : bean.getEnumMap().entrySet()) {
        //     Enum enumValue = entry.getKey();
        //     String enumText = entry.getValue();
        //
        //     JCase enumCase = enumSwitch._case(new JEnumLabel(enumValue.name()));
        //     enumCase.body()._return(JExpr.lit(enumText));
        // }
        //
        // enumSwitch._default().body()._throw(JExpr._new(toJClass(IllegalArgumentException.class))
        //         .arg(JExpr.lit("No value mapped to ").plus(value).plus(JExpr.lit(" for enum " + bean.getType().getName()))));

        enumWriters.put(bean.getType(), jaxbClass);
    }

    private void writeProperties(MarshallerBuilder builder, Bean bean) {
        for (Property property : bean.getProperties()) {
            if (property.getXmlStyle() == Property.XmlStyle.ATTRIBUTE) {

                JBlock block = builder.getWriteMethod().body();
                block.add(new JBlankLine());
                block.add(new JLineComment(property.getXmlStyle() + ": " + property.getName()));

                JVar propertyVar = getValue(builder, property, block);

                if (!property.isCollection()) {

                    if (!toClass(property.getType()).isPrimitive()) {
                        JConditional nilCond = block._if(propertyVar.ne(JExpr._null()));
                        block = nilCond._then();
                    }

                    writeSimpleTypeAttribute(builder, block, property.getXmlName(), toClass(property.getType()), propertyVar);
                } else {
                    logger.info("(JAXB Writer) Attribute lists are not supported yet!");
                }
            }
        }

        for (Property property : bean.getProperties()) {
            if (property.getXmlStyle() == Property.XmlStyle.ATTRIBUTE) continue;

            builder.getWriteMethod().body().add(new JBlankLine());
            builder.getWriteMethod().body().add(new JLineComment(property.getXmlStyle() + ": " + property.getName()));

            JVar propertyVar = getValue(builder, property, builder.getWriteMethod().body());

            switch (property.getXmlStyle()) {
                case ELEMENT:
                    // if the element is required, add a null check that writes xsi nil
                    JVar outerVar = propertyVar;
                    JBlock outerBlock = builder.getWriteMethod().body();

                    if (property.isCollection()) {
                        // if collection is not null, process it; otherwise write xsi:nil
                        JConditional nullCond = outerBlock._if(outerVar.ne(JExpr._null()));
                        if (property.isNillable()) {
                            nullCond._else().add(builder.getXSW().invoke("writeXsiNil"));
                        }

                        // write wraper element opening tag
                        QName wrapperElement = property.getXmlName();
                        JBlock wrapperElementBlock = outerBlock;
                        if (wrapperElement != null) {
                            if (property.isRequired() || property.isNillable()) {
                                wrapperElementBlock = nullCond._then();
                            }
                            wrapperElementBlock.add(builder.getXSW().invoke("writeStartElement")
                                    .arg(builder.getWriterPrefix(wrapperElement.getNamespaceURI()))
                                    .arg(wrapperElement.getLocalPart())
                                    .arg(wrapperElement.getNamespaceURI()));
                        }

                        JType itemType;
                        if (!toClass(property.getComponentType()).isPrimitive()) {
                            itemType = getGenericType(property.getComponentType());
                        } else {
                            itemType = toJType(toClass(property.getComponentType()));
                        }

                        String itemName = builder.getWriteVariableManager().createId(property.getName() + "Item");
                        JForEach each = nullCond._then().forEach(itemType, itemName, outerVar);

                        // write wraper element closing tag
                        if (wrapperElement != null) {
                            wrapperElementBlock.add(builder.getXSW().invoke("writeEndElement"));
                        }

                        outerBlock = each.body();
                        outerVar = each.var();
                    }

                    // process value through adapter
                    outerVar = writeAdapterConversion(builder, outerBlock, property, outerVar);

                    // determine types that may be substuited for this value
                    Map<Class, ElementMapping> expectedTypes = new TreeMap<Class, ElementMapping>(new ClassComparator());
                    for (ElementMapping mapping : property.getElementMappings()) {
                        if (mapping.getComponentType() != null) {
                            expectedTypes.put(toClass(mapping.getComponentType()), mapping);
                        } else {
                            expectedTypes.put(toClass(property.getType()), mapping);
                        }
                    }

                    if (expectedTypes.size() == 1) {
                        ElementMapping mapping = property.getElementMappings().iterator().next();

                        // null check for non-nillable elements
                        JBlock block = outerBlock;
                        JConditional nullCond = null;
                        if (!mapping.isNillable() && !toClass(property.getComponentType()).isPrimitive()) {
                            nullCond = outerBlock._if(outerVar.ne(JExpr._null()));
                            block = nullCond._then();
                        }

                        // write element
                        writeElement(builder, block, mapping, outerVar, toClass(property.getComponentType()), mapping.isNillable());

                        // property is required and does not support nill, then an error is reported if the value was null
                        if (property.isRequired() && !mapping.isNillable() && nullCond != null) {
                            nullCond._else().invoke(builder.getWriteContextVar(), "unexpectedNullValue").arg(builder.getWriteObject()).arg(property.getName());
                        }
                    } else {
                        JIfElseBlock conditional = new JIfElseBlock();
                        outerBlock.add(conditional);

                        ElementMapping nilMapping = null;
                        for (Map.Entry<Class, ElementMapping> entry : expectedTypes.entrySet()) {
                            Class itemType = entry.getKey();
                            ElementMapping mapping = entry.getValue();

                            if (mapping.isNillable()) {
                                if (nilMapping != null && nilMapping != mapping) {
                                    throw new BuildException("Property " + property + " mappings " + mapping.getXmlName() + " and " + nilMapping + " are both nillable.  Only one mapping may of an property may be nilable");
                                }
                                nilMapping = mapping;
                            }


                            // add instance of check
                            JExpression isInstance = outerVar._instanceof(toJClass(itemType));
                            JBlock block = conditional.addCondition(isInstance);

                            // declare item variable
                            JVar itemVar;
                            if (toClass(property.getComponentType()) == itemType) {
                                itemVar = outerVar;
                            } else {
                                String itemName = builder.getWriteVariableManager().createId(itemType.getSimpleName());
                                itemVar = block.decl(toJClass(itemType), itemName, JExpr.cast(toJClass(itemType), outerVar));
                            }
                            writeElement(builder, block, mapping, itemVar, itemType, false);
                        }

                        // if item was null, write xsi:nil or report an error
                        JBlock nullBlock = conditional.addCondition(outerVar.eq(JExpr._null()));
                        if (nilMapping != null) {
                            // write start element
                            QName name = nilMapping.getXmlName();
                            nullBlock.add(builder.getXSW().invoke("writeStartElement")
                                    .arg(builder.getWriterPrefix(name.getNamespaceURI()))
                                    .arg(name.getLocalPart())
                                    .arg(name.getNamespaceURI()));

                            // write xsi:nil
                            nullBlock.add(builder.getXSW().invoke("writeXsiNil"));

                            // close element
                            nullBlock.add(builder.getXSW().invoke("writeEndElement"));
                        } else {
                            nullBlock.invoke(builder.getWriteContextVar(), "unexpectedNullValue").arg(builder.getWriteObject()).arg(property.getName());
                        }

                        // if not a recogonized type or null, reprot unknown type error
                        JInvocation unexpected = conditional._else().invoke(builder.getWriteContextVar(), "unexpectedElement").arg(builder.getXSW()).arg(builder.getWriteObject()).arg(property.getName()).arg(outerVar);
                        for (Class expectedType : expectedTypes.keySet()) {
                            unexpected.arg(builderContext.dotclass(expectedType));
                        }
                    }
                    break;
                case ELEMENT_REF:
                    JBlock block = builder.getWriteMethod().body();

                    JVar itemVar = propertyVar;
                    if (property.isCollection()) {
                        JBlock collectionNotNull = block._if(propertyVar.ne(JExpr._null()))._then();

                        JType itemType;
                        if (!toClass(property.getComponentType()).isPrimitive()) {
                            itemType = getGenericType(property.getComponentType());
                        } else {
                            itemType = toJType(toClass(property.getComponentType()));
                        }

                        String itemName = builder.getWriteVariableManager().createId( property.getName() + "Item");
                        JForEach each = collectionNotNull.forEach(itemType, itemName, propertyVar);

                        JBlock newBody = each.body();
                        block = newBody;
                        itemVar = each.var();
                    }

                    // process value through adapter
                    itemVar = writeAdapterConversion(builder, block, property, itemVar);

                    block.invoke(builder.getWriteContextVar(), "unexpectedElementRef").arg(builder.getXSW()).arg(builder.getWriteObject()).arg(property.getName()).arg(itemVar);
                    break;
                case VALUE:
                    block = builder.getWriteMethod().body();

                    // process value through adapter
                    propertyVar = writeAdapterConversion(builder, block, property, propertyVar);

                    writeSimpleTypeElement(builder, propertyVar, toClass(property.getType()), block);
                    break;
                default:
                    throw new BuildException("Unknown XmlMapping type " + property.getXmlStyle());
            }
        }
        
        if (bean.getBaseClass() != null) {
            writeProperties(builder, bean.getBaseClass());
        }
    }

    private void writeElement(MarshallerBuilder builder, JBlock block, ElementMapping mapping, JVar itemVar, Class type, boolean nillable) {
        // write start element
        QName name = mapping.getXmlName();
        block.add(builder.getXSW().invoke("writeStartElement")
                .arg(builder.getWriterPrefix(name.getNamespaceURI()))
                .arg(name.getLocalPart())
                .arg(name.getNamespaceURI()));

        JBlock elementWriteBlock = block;
        if (nillable && !type.isPrimitive()) {
            JConditional nilCond = block._if(itemVar.ne(JExpr._null()));
            elementWriteBlock = nilCond._then();
            nilCond._else().add(builder.getXSW().invoke("writeXsiNil"));
        }

        // write element
        Bean targetBean = model.getBean(type);
        if (targetBean == null || targetBean.getType().isEnum()) {
            writeSimpleTypeElement(builder,
                    itemVar,
                    type,
                    elementWriteBlock);
        } else {
            if (!mapping.getComponentType().equals(type)) {
                QName typeName = targetBean.getSchemaTypeName();
                elementWriteBlock.add(builder.getXSW().invoke("writeXsiType").arg(typeName.getNamespaceURI()).arg(typeName.getLocalPart()));
            }

            writeClassWriter(builder, targetBean, elementWriteBlock, itemVar);
        }

        // close element
        block.add(builder.getXSW().invoke("writeEndElement"));
    }

    private JVar getValue(MarshallerBuilder builder, Property property, JBlock block) {
        Class propertyType = toClass(property.getType());

        String propertyName = property.getName();
        if (property.getAdapterType() != null) {
            propertyName += "Raw";
        }
        propertyName = builder.getWriteVariableManager().createId(propertyName);

        JVar propertyVar = block.decl(
                getGenericType(property.getType()),
                propertyName);

        if (property.getField() != null) {
            Field field = property.getField();

            if (!isPrivate(field)) {
                propertyVar.init(builder.getWriteObject().ref(field.getName()));
            } else {
                JFieldVar fieldAccessorField = builder.getPrivateFieldAccessor(field);

                String methodName;
                if (Boolean.TYPE.equals(propertyType)) {
                    methodName = "getBoolean";
                } else if (Byte.TYPE.equals(propertyType)) {
                    methodName = "getByte";
                } else if (Character.TYPE.equals(propertyType)) {
                    methodName = "getChar";
                } else if (Short.TYPE.equals(propertyType)) {
                    methodName = "getShort";
                } else if (Integer.TYPE.equals(propertyType)) {
                    methodName = "getInt";
                } else if (Long.TYPE.equals(propertyType)) {
                    methodName = "getLong";
                } else if (Float.TYPE.equals(propertyType)) {
                    methodName = "getFloat";
                } else if (Double.TYPE.equals(propertyType)) {
                    methodName = "getDouble";
                } else {
                    methodName = "getObject";
                }

                propertyVar.init(fieldAccessorField.invoke(methodName).arg(builder.getWriteObject()).arg(builder.getWriteContextVar()).arg(builder.getWriteObject()));
            }
        } else if (property.getGetter() != null) {
            Method getter = property.getGetter();
            if (!isPrivate(getter)) {
                propertyVar.init(JExpr._null());

                JTryBlock tryGetter = block._try();
                tryGetter.body().assign(propertyVar, builder.getWriteObject().invoke(getter.getName()));

                JCatchBlock catchException = tryGetter._catch(toJClass(Exception.class));
                catchException.body().invoke(builder.getReadContextVar(), "getterError")
                        .arg(builder.getWriteObject())
                        .arg(property.getName())
                        .arg(builderContext.dotclass(property.getBean().getType()))
                        .arg(getter.getName())
                        .arg(catchException.param("e"));
            } else {
                JFieldVar propertyAccessorField = builder.getPrivatePropertyAccessor(property.getGetter(), property.getSetter(), property.getName());
                propertyVar.init(propertyAccessorField.invoke("getObject").arg(builder.getWriteObject()).arg(builder.getWriteContextVar()).arg(builder.getWriteObject()));
            }
        } else {
            throw new BuildException("Property does not have a getter " + property.getBean().getClass().getName() + "." + property.getName());
        }

        return propertyVar;
    }

    private void writeClassWriter(MarshallerBuilder builder, Bean bean, JBlock block, JExpression propertyVar) {
        // Complex type which will already have an element builder defined
        MarshallerBuilder existingBuilder = builders.get(bean);
        if (existingBuilder == null) {
            throw new BuildException("Unknown bean " + bean);
        }

        // Declare dependency from builder to existingBuilder
        builder.addDependency(existingBuilder.getMarshallerClass());

        // Add a static import for the write method on the existing builder class
        String methodName = "write" + bean.getType().getSimpleName();
        if (builder != existingBuilder) {
            JStaticImports staticImports = JStaticImports.getStaticImports(builder.getMarshallerClass());
            staticImports.addStaticImport(existingBuilder.getMarshallerClass().fullName() + "." + methodName);
        }

        // Call the static method
        JInvocation invocation = JExpr.invoke(methodName).arg(builder.getXSW()).arg(propertyVar).arg(builder.getWriteContextVar());
        block.add(invocation);
    }

    private List<Bean> getSubstitutionTypes(Class<?> c) {
        List<Bean> beans = new ArrayList<Bean>();
        for (Bean bean : model.getBeans()) {
            if (c.isAssignableFrom(bean.getType()) && bean.getSchemaTypeName() != null) {
                beans.add(bean);
            }
        }

        Collections.sort(beans, new BeanComparator());
        return beans;
    }

    private JVar writeAdapterConversion(MarshallerBuilder builder, JBlock block, Property property, JVar propertyVar) {
        if (property.getAdapterType() != null) {
            JVar adapterVar = builder.getAdapter(property.getAdapterType());
            JVar valueVar = block.decl(toJClass(String.class), builder.getWriteVariableManager().createId(property.getName()), JExpr._null());

            JTryBlock tryBlock = block._try();
            tryBlock.body().assign(valueVar, adapterVar.invoke("marshal").arg(propertyVar));

            JCatchBlock catchException = tryBlock._catch(toJClass(Exception.class));
            JBlock catchBody = catchException.body();
            catchBody.invoke(builder.getReadContextVar(), "xmlAdapterError")
                    .arg(builder.getWriteObject())
                    .arg(property.getName())
                    .arg(builderContext.dotclass(property.getAdapterType()))
                    .arg(builderContext.dotclass(toClass(property.getType())))  // currently we only support conversion between same type
                    .arg(builderContext.dotclass(toClass(property.getType())))
                    .arg(catchException.param("e"));

            propertyVar = valueVar;
        }
        return propertyVar;
    }

    private void writeSimpleTypeElement(MarshallerBuilder builder, JExpression object, Class type, JBlock block) {
        if(isBuiltinType(type)) {
            block.add(builder.getXSW().invoke("writeCharacters").arg(toString(builder, object, type)));
        } else if (type.equals(byte[].class)) {
            block.add(toJClass(BinaryUtils.class).staticInvoke("encodeBytes").arg(builder.getXSW()).arg(object));
        } else if (type.equals(QName.class)) {
            block.add(builder.getXSW().invoke("writeQName").arg(object));
        } else if (type.equals(DataHandler.class) || type.equals(Image.class)) {
            // todo support AttachmentMarshaller
        } else {
        	logger.info("(JAXB Writer) Cannot map simple type yet: " + type);
        }
    }

    private void writeSimpleTypeAttribute(MarshallerBuilder builder, JBlock block, QName name, Class type, JExpression value) {
        if(isBuiltinType(type)) {
            value = toString(builder, value, type);
        } else if (type.equals(byte[].class)) {
            value = toJClass(Base64.class).staticInvoke("encode").arg(value);
        } else if (type.equals(QName.class)) {
            value = builder.getXSW().invoke("getQNameAsString").arg(value);
        } else if (type.equals(DataHandler.class) || type.equals(Image.class)) {
            // todo support AttachmentMarshaller
        } else {
            logger.info("(JAXB Writer) Cannot map simple attribute type yet: " + type);
            return;
        }

        JExpression prefix;
        if (name.getNamespaceURI().length() > 0) {
            prefix = builder.getWriterPrefix(name.getNamespaceURI());
        } else {
            prefix = JExpr.lit("");
        }
        block.add(builder.getXSW().invoke("writeAttribute")
                  .arg(prefix)
                  .arg(JExpr.lit(name.getNamespaceURI()))
                  .arg(JExpr.lit(name.getLocalPart()))
                  .arg(value));
    }

    private String getMostPopularNS(Set<Property> properties) {
        List<QName> names = new ArrayList<QName>();
        for (Property property : properties) {
            if (property.getXmlName() != null) {
                names.add(property.getXmlName());
            }
            for (ElementMapping mapping : property.getElementMappings()) {
                if (mapping.getXmlName() != null) {
                    names.add(mapping.getXmlName());
                }
            }
        }

        String  mostPopularNS = null;
        int mostPopularCount = 0;

        Map<String, Integer> nsCount = new TreeMap<String, Integer>();
        for (QName name : names) {
            String namespace = name.getNamespaceURI();
            if (namespace.length() > 0) {
                Integer count = nsCount.get(namespace);
                count = count == null ? 0 : count + 1;
                nsCount.put(namespace, count);
                if (count > mostPopularCount) {
                    mostPopularNS = namespace;
                    mostPopularCount = count;
                }
            }
        }

        return mostPopularNS;
    }

    private JClass toJClass(Class clazz) {
        if (clazz.isPrimitive()) {
            // Code model maps primitives to JPrimitiveType which not a JClass... use toJType instead
            throw new IllegalArgumentException("Internal Error: clazz is a primitive");
        }
        return codeModel.ref(clazz);
    }

    private JType toJType(Class<?> c) {
        return codeModel._ref(c);
    }

    private JClass getGenericType(Type type) {
        if (type instanceof Class) {
            Class clazz = toPrimitiveWrapper((Class) type);
            return codeModel.ref(clazz);
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            JClass raw = (JClass) toJType(toClass(pt.getRawType()));

            Type[] actualTypes = pt.getActualTypeArguments();
            List<JClass> types = new ArrayList<JClass>(actualTypes.length);
            for (Type actual : actualTypes) {
                types.add(getGenericType(actual));
            }
            raw = raw.narrow(types);

            return raw;
        }
        throw new IllegalStateException();
    }

    private boolean isBuiltinType(Class type) {
        return type.equals(boolean.class) ||
                type.equals(byte.class) ||
                type.equals(short.class) ||
                type.equals(int.class) ||
                type.equals(long.class) ||
                type.equals(float.class) ||
                type.equals(double.class) ||
                type.equals(String.class) ||
                type.equals(Boolean.class) ||
                type.equals(Byte.class) ||
                type.equals(Short.class) ||
                type.equals(Integer.class) ||
                type.equals(Long.class) ||
                type.equals(Float.class) ||
                type.equals(Double.class) ||
                type.equals(XMLGregorianCalendar.class) ||
                type.equals(Duration.class) ||
                type.equals(BigDecimal.class) ||
                type.equals(BigInteger.class) ||
                type.isEnum();
    }

    private JExpression toString(MarshallerBuilder builder, JExpression value, Class<?> type) {
        if (type.isPrimitive()) {
            if (type.equals(boolean.class)) {
                return toJClass(Boolean.class).staticInvoke("toString").arg(value);
            } else if (type.equals(byte.class)) {
                return toJClass(Byte.class).staticInvoke("toString").arg(value);
            } else if (type.equals(short.class)) {
                return toJClass(Short.class).staticInvoke("toString").arg(value);
            } else if (type.equals(int.class)) {
                return toJClass(Integer.class).staticInvoke("toString").arg(value);
            } else if (type.equals(long.class)) {
                return toJClass(Long.class).staticInvoke("toString").arg(value);
            } else if (type.equals(float.class)) {
                return toJClass(Float.class).staticInvoke("toString").arg(value);
            } else if (type.equals(double.class)) {
                return toJClass(Double.class).staticInvoke("toString").arg(value);
            }
        } else {
            if (type.equals(String.class)) {
                return value;
            } else if (type.equals(Boolean.class)) {
                return toJClass(Boolean.class).staticInvoke("toString").arg(value);
            } else if (type.equals(Byte.class)) {
                return toJClass(Byte.class).staticInvoke("toString").arg(value);
            } else if (type.equals(Short.class)) {
                return toJClass(Short.class).staticInvoke("toString").arg(value);
            } else if (type.equals(Integer.class)) {
                return toJClass(Integer.class).staticInvoke("toString").arg(value);
            } else if (type.equals(Long.class)) {
                return toJClass(Long.class).staticInvoke("toString").arg(value);
            } else if (type.equals(Float.class)) {
                return toJClass(Float.class).staticInvoke("toString").arg(value);
            } else if (type.equals(Double.class)) {
                return toJClass(Double.class).staticInvoke("toString").arg(value);
            } else if (type.equals(XMLGregorianCalendar.class)) {
                return value.invoke("toXMLFormat");
            } else if (type.equals(Duration.class)) {
                return value.invoke("toString");
            } else if (type.equals(BigDecimal.class)) {
                return value.invoke("toString");
            } else if (type.equals(BigInteger.class)) {
                return value.invoke("toString");
            } else if (type.isEnum()) {
                JClass writerClass = enumWriters.get(type);
                if (writerClass == null) {
                    throw new BuildException("Unknown enum type " + type);
                }
                return writerClass.staticInvoke("toString").arg(builder.getWriteObject()).arg(JExpr._null()).arg(builder.getWriteContextVar()).arg(value);
            }

        }
        throw new UnsupportedOperationException("Invalid type " + type);
    }

    private static class BeanComparator implements Comparator<Bean> {
        public int compare(Bean left, Bean right) {
            if (left.equals(right)) return 0;

            if (left.getType().isAssignableFrom(right.getType())) {
                return 1;
            }
            return -1;
        }

    }

    private static class ClassComparator implements Comparator<Class> {
        public int compare(Class left, Class right) {
            if (left.equals(right)) return 0;

            if (left.isAssignableFrom(right)) {
                return 1;
            }
            return -1;
        }
    }
}
