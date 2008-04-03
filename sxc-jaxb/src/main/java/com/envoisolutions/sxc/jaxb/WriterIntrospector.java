package com.envoisolutions.sxc.jaxb;

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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.envoisolutions.sxc.builder.BuildException;
import com.envoisolutions.sxc.builder.Builder;
import com.envoisolutions.sxc.builder.ElementWriterBuilder;
import com.envoisolutions.sxc.builder.WriterBuilder;
import com.envoisolutions.sxc.builder.impl.AbstractWriterBuilder;
import com.envoisolutions.sxc.builder.impl.ElementWriterBuilderImpl;
import com.envoisolutions.sxc.builder.impl.JBlankLine;
import com.envoisolutions.sxc.builder.impl.JIfElseBlock;
import com.envoisolutions.sxc.builder.impl.JLineComment;
import static com.envoisolutions.sxc.jaxb.JavaUtils.toClass;
import static com.envoisolutions.sxc.jaxb.JavaUtils.toPrimitiveWrapper;
import static com.envoisolutions.sxc.jaxb.JavaUtils.capitalize;
import static com.envoisolutions.sxc.jaxb.JavaUtils.isPrivate;
import com.envoisolutions.sxc.jaxb.model.Bean;
import com.envoisolutions.sxc.jaxb.model.Model;
import com.envoisolutions.sxc.jaxb.model.Property;
import com.envoisolutions.sxc.jaxb.model.XmlMapping;
import com.envoisolutions.sxc.util.Base64;
import com.envoisolutions.sxc.util.FieldAccessor;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JCatchBlock;
import com.sun.codemodel.JClass;
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
	
	private final ElementWriterBuilder rootWriter;
    private final Builder builder;
    private final Model model;
    private final JCodeModel codeModel;
    private final Map<Bean, ElementWriterBuilder> beanWriters = new LinkedHashMap<Bean, ElementWriterBuilder>();
    private final Map<Class, String> enumWriters = new LinkedHashMap<Class, String>();

    private final Map<Class, JVar> adapters = new HashMap<Class, JVar>();
    private final Map<String, JFieldVar> privateFieldAccessors = new TreeMap<String, JFieldVar>();

    public WriterIntrospector(Builder builder, Model model) throws JAXBException {
        this.builder = builder;
        this.model = model;
        rootWriter = builder.getWriterBuilder();
        rootWriter.declareException(JAXBException.class);
        this.codeModel = rootWriter.getCodeModel();

        // reserve variables
        rootWriter.getVariableManager().addId("item");
        rootWriter.getVariableManager().addId("marshaller");

        List<Bean> mybeans = new ArrayList<Bean>(model.getBeans());
        // TODO sort classes by hierarchy
        Collections.sort(mybeans, new BeanComparator());

        // declare all writer methods first, so everything exists when we build the
        for (Bean bean : mybeans) {
            if (Modifier.isAbstract(bean.getType().getModifiers())) continue;
            if (bean.getType().isEnum()) continue;

            ElementWriterBuilder classBuilder = rootWriter.newCondition(rootWriter.getObject()._instanceof(toJType(bean.getType())), toJType(bean.getType()));
            beanWriters.put(bean, classBuilder);
        }

        // declare all parse enum methods (after read methods so they are grouped together)
        for (Bean bean : model.getBeans()) {
            if (bean.getType().isEnum()) {
                addEnum(bean);
            }
        }
        
        // declare all adapters(so they are grouped)
        for (Bean bean : model.getBeans()) {
            for (Property property : bean.getProperties()) {
                if (property.getAdapterType() != null) {
                    getAdapter(property.getAdapterType());
                }
            }
        }

        // declare all private field accessors (so they are grouped)
        for (Bean bean : model.getBeans()) {
            for (Property property : bean.getProperties()) {
                Field field = property.getField();
                if (field != null && isPrivate(field)) {
                    getPrivateFieldAccessor(field);
                }
            }
        }

        // build the writer methods
        for (Bean bean : model.getBeans()) {
            if (!bean.getType().isEnum()) {
                ElementWriterBuilder classBuilder = beanWriters.get(bean);
                if (classBuilder != null) {
                    writeProperties(classBuilder, bean);
                }
            } else {
                JExpression inst = rootWriter.getObject()._instanceof(toJType(bean.getType()));
                JBlock block = ((ElementWriterBuilderImpl)rootWriter).newBlock(inst);

                writeSimpleTypeElement(rootWriter, null, JExpr.cast(toJClass(bean.getType()), rootWriter.getObject()), bean.getType(), block);
            }
        }
    }

    private void addEnum(Bean bean) {
        JClass type = toJClass(bean.getType());
        JMethod method = rootWriter.getWriterClass().method(JMod.PRIVATE, String.class, "toString");
        JVar value = method.param(type, decapitalize(bean.getType().getSimpleName()));

        JIfElseBlock enumSwitch = new JIfElseBlock();
        method.body().add(enumSwitch);
        for (Map.Entry<Enum, String> entry : bean.getEnumMap().entrySet()) {
            Enum enumValue = entry.getKey();
            String enumText = entry.getValue();

            JBlock enumCase = enumSwitch.addCondition(type.staticRef(enumValue.name()).eq(value));
            enumCase._return(JExpr.lit(enumText));
        }
        // java.lang.IllegalArgumentException: No enum const class com.envoisolutions.sxc.jaxb.enums.AnnotatedEnum.ssssss
        // todo inaccurate message... enums mapped
        enumSwitch._else()._throw(JExpr._new(toJClass(IllegalArgumentException.class))
                .arg(JExpr.lit("No value mapped to ").plus(value).plus(JExpr.lit(" for enum " + bean.getType().getName()))));

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

        enumWriters.put(bean.getType(), method.name());
    }

    private void writeProperties(ElementWriterBuilder classBuilder, Bean bean) {
        for (Property property : bean.getProperties()) {
            if (property.getXmlStyle() == Property.XmlStyle.ATTRIBUTE) {
                classBuilder.getCurrentBlock().add(new JBlankLine());
                classBuilder.getCurrentBlock().add(new JLineComment(property.getXmlStyle() + ": " + property.getName()));

                String propertyName = classBuilder.getVariableManager().createId(property.getName());
                JVar propertyVar = classBuilder.getCurrentBlock().decl(
                        toJType(toClass(property.getType())),
                        propertyName,
                        getValue(classBuilder, property));

                for (XmlMapping mapping : property.getXmlMappings()) {
                    if (!property.isCollection()) {

                        JBlock block = classBuilder.getCurrentBlock();
                        if (!toClass(property.getType()).isPrimitive()) {
                            JConditional nilCond = block._if(propertyVar.ne(JExpr._null()));
                            block = nilCond._then();
                        }

                        writeSimpleTypeAttribute(classBuilder, block, mapping.getXmlName(), toClass(property.getType()), propertyVar);
                    } else {
                        logger.info("(JAXB Writer) Attribute lists are not supported yet!");
                    }
                }
            }
        }

        for (Property property : bean.getProperties()) {
            if (property.getXmlStyle() == Property.XmlStyle.ATTRIBUTE) continue;

            classBuilder.getCurrentBlock().add(new JBlankLine());
            classBuilder.getCurrentBlock().add(new JLineComment(property.getXmlStyle() + ": " + property.getName()));

            String propertyName = classBuilder.getVariableManager().createId(property.getName());
            JVar propertyVar = classBuilder.getCurrentBlock().decl(
                    getGenericType(property.getType()),
                    propertyName,
                    getValue(classBuilder, property));

            switch (property.getXmlStyle()) {
                case ELEMENT:
                    JBlock origBlock = classBuilder.getCurrentBlock();

                    // if the element is required, add a null check that writes xsi nil
                    Class type = toClass(property.getType());
                    JVar outerVar = propertyVar;
                    JBlock outerBlock = origBlock;

                    // determine types that may be substuited for this value
                    Map<Class, XmlMapping> expectedTypes = new TreeMap<Class, XmlMapping>(new ClassComparator());
                    for (XmlMapping mapping : property.getXmlMappings()) {
                        if (mapping.getComponentType() != null) {
                            expectedTypes.put(toClass(mapping.getComponentType()), mapping);
                            for (Bean substitutionBean : getSubstitutionTypes(toClass(mapping.getComponentType()))) {
                                expectedTypes.put(substitutionBean.getType(), mapping);
                            }
                        } else {
                            expectedTypes.put(toClass(property.getType()), mapping);
                        }
                    }

                    if (property.isCollection()) {
                        JConditional nullCond = outerBlock._if(outerVar.ne(JExpr._null()));
                        JType itemType;
                        if (!toClass(property.getComponentType()).isPrimitive()) {
                            itemType = getGenericType(property.getComponentType());
                        } else {
                            itemType = toJType(toClass(property.getComponentType()));
                        }

                        String itemName;
                        if (expectedTypes.size() == 1) {
                            itemName = classBuilder.getVariableManager().createId( property.getName() + "Item");
                        } else {
                            itemName = "item";
                        }
                        JForEach each = nullCond._then().forEach(itemType, itemName, outerVar);
                        outerBlock = each.body();
                        outerVar = each.var();
                    }

                    if (expectedTypes.size() == 1) {
                        XmlMapping mapping = property.getXmlMappings().iterator().next();

                        // null check for non-nillable elements
                        JBlock block = outerBlock;
                        JConditional nullCond = null;
                        if (!mapping.isNillable() && !type.isPrimitive()) {
                            nullCond = outerBlock._if(outerVar.ne(JExpr._null()));
                            block = nullCond._then();
                        }

                        // write element
                        writeElement(classBuilder, block, mapping, outerVar, toClass(property.getComponentType()), mapping.isNillable());

                        // property is required and does not support nill, then an error is reported if the value was null
                        if (property.isRequired() && !mapping.isNillable() && nullCond != null) {
                            JExpression message;
                            if (property.isCollection()) {
                                message = JExpr.lit("Property " + property + " collection cannot contain a null item");
                            } else {
                                message = JExpr.lit("Property " + property + " cannot be null");
                            }
                            // todo report error
                            nullCond._else().add(toJClass(System.class).staticRef("out").invoke("println").arg(message));
                        }
                    } else {
                        JIfElseBlock conditional = new JIfElseBlock();
                        outerBlock.add(conditional);

                        XmlMapping nilMapping = null;
                        String itemName = classBuilder.getVariableManager().createId( property.getName() + "Item");
                        for (Map.Entry<Class, XmlMapping> entry : expectedTypes.entrySet()) {
                            Class itemType = entry.getKey();
                            XmlMapping mapping = entry.getValue();

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
                            JVar itemVar = block.decl(toJClass(itemType), itemName, JExpr.cast(toJClass(itemType), outerVar));
                            writeElement(classBuilder, block, mapping, itemVar, itemType, false);
                        }

                        // if item was null, write xsi:nil or report an error
                        JBlock nullBlock = conditional.addCondition(outerVar.eq(JExpr._null()));
                        if (nilMapping != null) {
                            // write start element
                            QName name = nilMapping.getXmlName();
                            nullBlock.add(getXSW(classBuilder).invoke("writeStartElement").arg(name.getPrefix()).arg(name.getLocalPart()).arg(name.getNamespaceURI()));
                            if (classBuilder.getName() == null || !classBuilder.getName().getNamespaceURI().equals(name.getNamespaceURI())) {
                                nullBlock.add(getXSW(classBuilder).invoke("writeAndDeclareIfUndeclared").arg(JExpr.lit("")).arg(name.getNamespaceURI()));
                            }

                            // write xsi:nil
                            nullBlock.add(classBuilder.getXSW().invoke("writeXsiNil"));

                            // close element
                            nullBlock.add(getXSW(classBuilder).invoke("writeEndElement"));
                        } else {
                            JExpression message;
                            if (property.isCollection()) {
                                message = JExpr.lit("Property " + property + " collection cannot contain a null item");
                            } else {
                                message = JExpr.lit("Property " + property + " cannot be null");
                            }
                            // todo report error
                            nullBlock.add(toJClass(System.class).staticRef("out").invoke("println").arg(message));
                        }

                        // if not a recogonized type or null, reprot unknown type error
                        List<String> expectedTypeNames = new ArrayList<String>(expectedTypes.size());
                        for (Class expectedType : expectedTypes.keySet()) {
                            expectedTypeNames.add(expectedType.getName());
                        }
                        if (nilMapping != null) {
                            expectedTypeNames.add("null");
                        }

                        JExpression message;
                        if (property.isCollection()) {
                            message = JExpr.lit("Property " + property + " collection contains an element of the unexpected type ")
                                    .plus(outerVar.invoke("getClass").invoke("getName"))
                                    .plus(JExpr.lit("; expected types are " + expectedTypeNames));
                        } else {
                            message = JExpr.lit("Property " + property + " value is the unexpected type ")
                                    .plus(outerVar.invoke("getClass").invoke("getName"))
                                    .plus(JExpr.lit("; expected types are " + expectedTypeNames));
                        }
                        // todo report error
                        conditional._else().add(toJClass(System.class).staticRef("out").invoke("println").arg(message));
                    }
                    break;
                case ELEMENT_REF:
                    JBlock block = classBuilder.getCurrentBlock().block();

                    JClass marshallerType = toJClass(MarshallerImpl.class);
                    JVar marshaller = block.decl(marshallerType, "marshaller", JExpr.cast(marshallerType, getContextVar(classBuilder).invoke("get").arg(JExpr.lit(MarshallerImpl.MARSHALLER))));

                    JVar itemVar = propertyVar;
                    if (property.isCollection()) {
                        JBlock collectionNotNull = block._if(propertyVar.ne(JExpr._null()))._then();

                        JType itemType;
                        if (!toClass(property.getComponentType()).isPrimitive()) {
                            itemType = getGenericType(property.getComponentType());
                        } else {
                            itemType = toJType(toClass(property.getComponentType()));
                        }

                        String itemName = classBuilder.getVariableManager().createId( property.getName() + "Item");
                        JForEach each = collectionNotNull.forEach(itemType, itemName, propertyVar);

                        JBlock newBody = each.body();
                        block = newBody;
                        itemVar = each.var();
                    }

                    block.add(marshaller.invoke("marshal").arg(itemVar).arg(classBuilder.getXSW()));
                    break;
                case VALUE:
                    writeSimpleTypeElement(classBuilder, property.getAdapterType(), propertyVar, toClass(property.getType()), classBuilder.getCurrentBlock());
                    break;
                default:
                    throw new BuildException("Unknown XmlMapping type " + property.getXmlStyle());
            }
        }
        
        if (bean.getBaseClass() != null) {
            writeProperties(classBuilder, bean.getBaseClass());
        }
    }

    private void writeElement(ElementWriterBuilder classBuilder, JBlock block, XmlMapping mapping, JVar itemVar, Class type, boolean nillable) {
        // write start element
        QName name = mapping.getXmlName();
        block.add(getXSW(classBuilder).invoke("writeStartElement").arg(name.getPrefix()).arg(name.getLocalPart()).arg(name.getNamespaceURI()));
        if (classBuilder.getName() == null || !classBuilder.getName().getNamespaceURI().equals(name.getNamespaceURI())) {
            block.add(getXSW(classBuilder).invoke("writeAndDeclareIfUndeclared").arg(JExpr.lit("")).arg(name.getNamespaceURI()));
        }

        JBlock elementWriteBlock = block;
        if (nillable && !type.isPrimitive()) {
            JConditional nilCond = block._if(itemVar.ne(JExpr._null()));
            elementWriteBlock = nilCond._then();
            nilCond._else().add(classBuilder.getXSW().invoke("writeXsiNil"));
        }

        // write element
        Bean targetBean = model.getBean(type);
        if (targetBean == null || targetBean.getType().isEnum()) {
            writeSimpleTypeElement(classBuilder,
                    mapping.getProperty().getAdapterType(),
                    itemVar,
                    type,
                    elementWriteBlock);
        } else {
            if (mapping.getTargetBean() != targetBean) {
                QName typeName = targetBean.getSchemaTypeName();
                elementWriteBlock.add(getXSW(classBuilder).invoke("writeXsiType").arg(typeName.getNamespaceURI()).arg(typeName.getLocalPart()));
            }

            writeClassWriter(classBuilder, targetBean, elementWriteBlock, itemVar);
        }

        // close element
        block.add(getXSW(classBuilder).invoke("writeEndElement"));
    }

    private JExpression getValue(ElementWriterBuilder classBuilder, Property property) {
        final JExpression valueExp;
        Class propertyType = toClass(property.getType());
        if (property.getField() != null) {
            Field field = property.getField();

            if (!isPrivate(field)) {
                valueExp = classBuilder.getObject().ref(field.getName());
            } else {
                JFieldVar fieldAccessorField = getPrivateFieldAccessor(field);

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

                valueExp = fieldAccessorField.invoke(methodName).arg(classBuilder.getObject());
            }
        } else if (property.getGetter() != null) {
            Method getter = property.getGetter();
            valueExp = classBuilder.getObject().invoke(getter.getName());
        } else {
            throw new BuildException("Property does not have a getter " + property.getBean().getClass().getName() + "." + property.getName());
        }
        return valueExp;
    }

    private JFieldVar getPrivateFieldAccessor(Field field) {
        String fieldId = field.getDeclaringClass().getName() + "." + field.getName();
        JFieldVar fieldAccessorField = privateFieldAccessors.get(fieldId);
        if (fieldAccessorField == null) {
            JDefinedClass readerCls = rootWriter.getWriterClass();

            JClass fieldAccessorType = toJClass(FieldAccessor.class).narrow(toJClass(field.getDeclaringClass()), getGenericType(field.getGenericType()));
            JInvocation newFieldAccessor = JExpr._new(fieldAccessorType)
                    .arg(builder.getCodeModel().ref(field.getDeclaringClass()).staticRef("class"))
                    .arg(JExpr.lit(field.getName()));

            String fieldName = rootWriter.getFieldManager().createId(decapitalize(field.getDeclaringClass().getSimpleName()) + capitalize(field.getName()));
            fieldAccessorField = readerCls.field(JMod.PRIVATE | JMod.STATIC | JMod.FINAL, fieldAccessorType, fieldName, newFieldAccessor);
            privateFieldAccessors.put(fieldId, fieldAccessorField);
        }
        return fieldAccessorField;
    }

    private void writeClassWriter(ElementWriterBuilder classBuilder, Bean bean, JBlock block, JExpression propertyVar) {
        // Complex type which will already have an element builder defined
        WriterBuilder existingBuilder = beanWriters.get(bean);
        if (existingBuilder == null) {
            throw new BuildException("Unknown bean " + bean);
        }

        block.invoke(((AbstractWriterBuilder) existingBuilder).getMethod())
                .arg(getXSW(classBuilder))
                .arg(propertyVar)
                .arg(getContextVar(classBuilder));

    }

    private List<Bean> getSubstitutionTypes(Class<?> c) {
        List<Bean> beans = new ArrayList<Bean>();
        for (Bean bean : model.getBeans()) {
            if (c.isAssignableFrom(bean.getType())) {
                beans.add(bean);
            }
        }

        Collections.sort(beans, new BeanComparator());
        return beans;
    }

    private void writeSimpleTypeElement(ElementWriterBuilder classBuilder,
            Class adapterType,
            JExpression object,
            Class type,
            JBlock block) {

        if (adapterType != null) {
            JVar adapterVar = getAdapter(adapterType);
            JVar valueVar = block.decl(toJClass(String.class), "marshalValue");
            JTryBlock tryBlock = block._try();
            tryBlock.body().assign(valueVar, adapterVar.invoke("marshal").arg(object));
            JCatchBlock catchBlock = tryBlock._catch(toJClass(Exception.class));
            catchBlock.body()._throw(JExpr._new(toJClass(UnmarshalException.class)).arg(catchBlock.param("e")));

            block.add(classBuilder.getXSW().invoke("writeCharacters").arg(valueVar));
        } else if(isBuiltinType(type)) {
            block.add(getXSW(classBuilder).invoke("writeCharacters").arg(toString(object, type)));
        } else if (type.equals(byte[].class)) {
            block.add(toJClass(BinaryUtils.class).staticInvoke("encodeBytes").arg(getXSW(classBuilder)).arg(object));
        } else if (type.equals(QName.class)) {
            block.add(getXSW(classBuilder).invoke("writeQName").arg(object));
        } else {
        	logger.info("(JAXB Writer) Cannot map simple type yet: " + type);
        }
    }

    private void writeSimpleTypeAttribute(WriterBuilder classBuilder, JBlock block, QName name, Class type, JExpression value) {
        if(isBuiltinType(type)) {
            value = toString(value, type);
        } else if (type.equals(byte[].class)) {
            value = toJClass(Base64.class).staticInvoke("encode").arg(value);
        } else if (type.equals(QName.class)) {
            value = getXSW(classBuilder).invoke("getQNameAsString").arg(value);
        } else {
            logger.info("(JAXB Writer) Cannot map simple attribute type yet: " + type);
            return;
        }

        block.add(getXSW(classBuilder).invoke("writeAttribute")
                  .arg(JExpr.lit(name.getPrefix()))
                  .arg(JExpr.lit(name.getNamespaceURI()))
                  .arg(JExpr.lit(name.getLocalPart()))
                  .arg(value));
    }

    private JVar getAdapter(Class adapterType) {
        JVar var = adapters.get(adapterType);
        if (var == null) {
            String fieldName = rootWriter.getFieldManager().createId(decapitalize(adapterType.getSimpleName()) + "Adapter");
            JClass jClass = toJClass(adapterType);
            var = rootWriter.getWriterClass().field(JMod.PRIVATE | JMod.STATIC | JMod.FINAL, jClass, fieldName, JExpr._new(jClass));
            adapters.put(adapterType, var);
        }
        return var;
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

    public Builder getBuilder() {
        return builder;
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

    private JExpression toString(JExpression value, Class<?> type) {
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
                String methodName = enumWriters.get(type);
                if (methodName == null) {
                    throw new BuildException("Unknown enum type " + type);
                }
                return JExpr.invoke(methodName).arg(value);
            }

        }
        throw new UnsupportedOperationException("Invalid type " + type);
    }

    private JVar getXSW(WriterBuilder builder) {
        return builder.getXSW();
    }

    private JVar getContextVar(WriterBuilder builder) {
        return ((AbstractWriterBuilder) builder).getContextVar();
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
