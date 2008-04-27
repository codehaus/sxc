package com.envoisolutions.sxc.jaxb;

import java.awt.Image;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
import static com.envoisolutions.sxc.jaxb.JavaUtils.isPrivate;
import static com.envoisolutions.sxc.jaxb.JavaUtils.toClass;
import com.envoisolutions.sxc.jaxb.model.Bean;
import com.envoisolutions.sxc.jaxb.model.ElementMapping;
import com.envoisolutions.sxc.jaxb.model.EnumInfo;
import com.envoisolutions.sxc.jaxb.model.Model;
import com.envoisolutions.sxc.jaxb.model.Property;
import com.envoisolutions.sxc.util.Base64;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JCatchBlock;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JForEach;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JTryBlock;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import org.w3c.dom.Element;

public class WriterIntrospector {
	private static final Logger logger = Logger.getLogger(WriterIntrospector.class.getName());

    private final BuilderContext context;
    private final Model model;
    private final Map<Bean, JAXBObjectBuilder> builders = new LinkedHashMap<Bean, JAXBObjectBuilder>();
    private final Map<Class, JAXBEnumBuilder> enumBuilders = new LinkedHashMap<Class, JAXBEnumBuilder>();

    public WriterIntrospector(BuilderContext context, Model model) throws JAXBException {
        this.context = context;
        this.model = model;

        List<Bean> mybeans = new ArrayList<Bean>(model.getBeans());
        Collections.sort(mybeans, new BeanComparator());

        // build all enum toString methods so they are available for use by bean readers
        for (EnumInfo enumInfo : model.getEnums()) {
            addEnum(enumInfo);
        }

        // declare all writer methods first, so everything exists when we build
        for (Bean bean : mybeans) {
            if (bean.getType().isEnum()) continue;

            JAXBObjectBuilder builder = context.createJAXBObjectBuilder(bean.getType(), bean.getRootElementName(), bean.getSchemaTypeName());

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

        // build the writer methods
        for (Bean bean : model.getBeans()) {
            if (!bean.getType().isEnum()) {
                JAXBObjectBuilder builder = builders.get(bean);
                if (builder != null) {
                    add(builder, bean);
                }
            }
        }
    }

    private void add(JAXBObjectBuilder builder, Bean bean) {
        JBlock block = builder.getWriteMethod().body();

        // perform instance checks
        JIfElseBlock ifElseBlock = new JIfElseBlock();
        block.add(ifElseBlock);
        JInvocation unexpectedSubclass = builder.getWriteContextVar().invoke("unexpectedSubclass").arg(builder.getXSW()).arg(builder.getWriteObject()).arg(context.dotclass(bean.getType()));
        for (Bean altBean : getSubstitutionTypes(builder.getType())) {
            if (bean == altBean) continue;

            // add condition
            JBlock altBlock = ifElseBlock.addCondition(context.dotclass(altBean.getType()).eq(builder.getWriteObject().invoke("getClass")));

            // write xsi:type
            QName typeName = altBean.getSchemaTypeName();
            altBlock.invoke(builder.getXSW(), "writeXsiType").arg(typeName.getNamespaceURI()).arg(typeName.getLocalPart());

            // call alternate marshaller
            writeClassWriter(builder, altBean, altBlock, JExpr.cast(context.toJClass(altBean.getType()), builder.getWriteObject()));
            altBlock._return();

            // add as expected subclass arg
            unexpectedSubclass.arg(context.dotclass(altBean.getType()));
        }

        // if the class isn't exactally this bean's type, then we have an unexpceted subclass
        JBlock unknownSubclassBlock = ifElseBlock.addCondition(context.dotclass(builder.getType()).ne(builder.getWriteObject().invoke("getClass")));
        unknownSubclassBlock.add(unexpectedSubclass);
        unknownSubclassBlock._return();

        block.add(new JBlankLine());

        // add beforeMarshal
        JExpression lifecycleCallbackRef = builder.getLifecycleCallbackVar();
        if (builder.getWriteVariableManager().containsId(builder.getLifecycleCallbackVar().name())) {
            lifecycleCallbackRef = builder.getJAXBObjectClass().staticRef(builder.getLifecycleCallbackVar().name());
        }
        block.invoke(builder.getWriteContextVar(), "beforeMarshal").arg(builder.getWriteObject()).arg(lifecycleCallbackRef);

        block.add(new JBlankLine());


        writeProperties(builder, bean);
    }

    private void addEnum(EnumInfo enumInfo) {
        JAXBEnumBuilder builder = context.createJAXBEnumBuilder(enumInfo.getType(), enumInfo.getRootElementName(), enumInfo.getSchemaTypeName());

        JMethod method = builder.getToStringMethod();

        JIfElseBlock enumSwitch = new JIfElseBlock();
        method.body().add(enumSwitch);
        for (Map.Entry<Enum, String> entry : enumInfo.getEnumMap().entrySet()) {
            Enum enumValue = entry.getKey();
            String enumText = entry.getValue();

            JBlock enumCase = enumSwitch.addCondition(context.toJClass(enumInfo.getType()).staticRef(enumValue.name()).eq(builder.getToStringValue()));
            enumCase._return(JExpr.lit(enumText));
        }

        JInvocation unexpectedInvoke = enumSwitch._else().invoke(builder.getToStringContext(), "unexpectedEnumConst")
                .arg(builder.getToStringBean())
                .arg(builder.getToStringParameterName())
                .arg(builder.getToStringValue());

        for (Enum expectedValue : enumInfo.getEnumMap().keySet()) {
            unexpectedInvoke.arg(context.toJClass(enumInfo.getType()).staticRef(expectedValue.name()));
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

        enumBuilders.put(enumInfo.getType(), builder);
    }

    private void writeProperties(JAXBObjectBuilder builder, Bean bean) {
        writeAttributes(builder, bean);
        writeElementsAndValue(builder, bean);

    }

    private void writeAttributes(JAXBObjectBuilder builder, Bean bean) {
        if (bean.getBaseClass() != null) {
            writeAttributes(builder, bean.getBaseClass());
        }

        for (Property property : bean.getProperties()) {
            if (property.getXmlStyle() == Property.XmlStyle.ATTRIBUTE) {
                JBlock block = builder.getWriteMethod().body();
                block.add(new JBlankLine());
                block.add(new JLineComment(property.getXmlStyle() + ": " + property.getName()));

                JExpression propertyVar = getValue(builder, property, block);

                if (!property.isXmlAny()) {
                    if (property.isCollection()) {
                        logger.info("(JAXB Writer) Attribute lists are not supported yet!");
                        continue;
                    }

                    if (!toClass(property.getType()).isPrimitive()) {
                        JConditional nullCond = block._if(propertyVar.ne(JExpr._null()));
                        block = nullCond._then();
                    }

                    writeSimpleTypeAttribute(builder, block, property, propertyVar);
                } else {
                    // if (value != null)
                    JConditional nullCond = block._if(propertyVar.ne(JExpr._null()));

                    String entryName = builder.getWriteVariableManager().createId(property.getName() + "Entry");

                    boolean needsCast = true;
                    if (property.getType() instanceof ParameterizedType) {
                        ParameterizedType parameterizedType = (ParameterizedType) property.getType();
                        Type[] arguments = parameterizedType.getActualTypeArguments();
                        if (arguments.length == 2 &&
                                QName.class.equals(arguments[0]) &&
                                property.getComponentType().equals(arguments[1])) {
                            needsCast = false;
                        }
                    }

                    if (needsCast) {
                        propertyVar = JExpr.cast(context.toJClass(Map.class).narrow(context.toJClass(QName.class), context.getGenericType(property.getComponentType())), propertyVar);
                    }

                    JForEach each = nullCond._then().forEach(context.toJClass(Map.Entry.class).narrow(context.toJClass(QName.class), context.getGenericType(property.getComponentType())), entryName, propertyVar.invoke("entrySet"));
                    writeSimpleTypeAttribute(builder, each.body(), each.var().invoke("getKey"), toClass(property.getComponentType()), each.var().invoke("getValue"));
                }
            }
        }
    }

    private void writeElementsAndValue(JAXBObjectBuilder builder, Bean bean) {
        if (bean.getBaseClass() != null) {
            writeElementsAndValue(builder, bean.getBaseClass());
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
                            wrapperElementBlock.add(builder.getWriteStartElement(wrapperElement));
                        }

                        JType itemType;
                        if (!toClass(property.getComponentType()).isPrimitive()) {
                            itemType = context.getGenericType(property.getComponentType());
                        } else {
                            itemType = context.toJType((Class<?>) toClass(property.getComponentType()));
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
                    Class propertyType = toClass(property.getComponentType());

                    // process value through adapter
                    outerVar = writeAdapterConversion(builder, outerBlock, property, outerVar);
                    if (property.getAdapterType() != null) {
                        propertyType = property.getComponentAdaptedType();
                    }

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
                        if (!mapping.isNillable() && !propertyType.isPrimitive()) {
                            nullCond = outerBlock._if(outerVar.ne(JExpr._null()));
                            block = nullCond._then();
                        }

                        // write element
                        writeElement(builder, block, mapping, outerVar, propertyType, mapping.isNillable());

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
                            JExpression isInstance = outerVar._instanceof(context.toJClass(itemType));
                            JBlock block = conditional.addCondition(isInstance);

                            // declare item variable
                            JVar itemVar;
                            if (toClass(property.getComponentType()) == itemType) {
                                itemVar = outerVar;
                            } else {
                                String itemName = builder.getWriteVariableManager().createId(itemType.getSimpleName());
                                itemVar = block.decl(context.toJClass(itemType), itemName, JExpr.cast(context.toJClass(itemType), outerVar));
                            }
                            writeElement(builder, block, mapping, itemVar, itemType, false);
                        }

                        // if item was null, write xsi:nil or report an error
                        JBlock nullBlock = conditional.addCondition(outerVar.eq(JExpr._null()));
                        if (nilMapping != null) {
                            // write start element
                            QName name = nilMapping.getXmlName();
                            nullBlock.add(builder.getWriteStartElement(name));

                            // write xsi:nil
                            nullBlock.add(builder.getXSW().invoke("writeXsiNil"));

                            // close element
                            nullBlock.add(builder.getXSW().invoke("writeEndElement"));
                        } else {
                            nullBlock.invoke(builder.getWriteContextVar(), "unexpectedNullValue").arg(builder.getWriteObject()).arg(property.getName());
                        }

                        // if not a recogonized type or null, report unknown type error
                        JInvocation unexpected = conditional._else().invoke(builder.getWriteContextVar(), "unexpectedElementType").arg(builder.getXSW()).arg(builder.getWriteObject()).arg(property.getName()).arg(outerVar);
                        for (Class expectedType : expectedTypes.keySet()) {
                            unexpected.arg(context.dotclass(expectedType));
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
                            itemType = context.getGenericType(property.getComponentType());
                        } else {
                            itemType = context.toJType((Class<?>) toClass(property.getComponentType()));
                        }

                        String itemName = builder.getWriteVariableManager().createId( property.getName() + "Item");
                        JForEach each = collectionNotNull.forEach(itemType, itemName, propertyVar);

                        JBlock newBody = each.body();
                        block = newBody;
                        itemVar = each.var();
                    }

                    // process value through adapter
                    itemVar = writeAdapterConversion(builder, block, property, itemVar);

                    if (!property.isXmlAny()) {
                        block.invoke(builder.getWriteContextVar(), "unexpectedElementRef").arg(builder.getXSW()).arg(builder.getWriteObject()).arg(property.getName()).arg(itemVar);
                    } else {
                        block.invoke(builder.getWriteContextVar(), "writeXmlAny").arg(builder.getXSW()).arg(builder.getWriteObject()).arg(property.getName()).arg(itemVar);
                    }
                    break;
                case VALUE:
                    block = builder.getWriteMethod().body();

                    // process value through adapter
                    propertyVar = writeAdapterConversion(builder, block, property, propertyVar);

                    writeSimpleTypeElement(builder, propertyVar, toClass(property.getComponentType()), block);
                    break;
                default:
                    throw new BuildException("Unknown XmlMapping type " + property.getXmlStyle());
            }
        }
    }

    private void writeElement(JAXBObjectBuilder builder, JBlock block, ElementMapping mapping, JVar itemVar, Class type, boolean nillable) {
        // if this is an id ref we write the ID property of the target bean instead of the bean itself
        if (mapping.getProperty().isIdref()) {
            Property property = mapping.getProperty();
            Property idProperty = findReferencedIdProperty(property);

            // read the id value
            itemVar = getValue(builder, itemVar, idProperty, property.getName() + JavaUtils.capitalize(idProperty.getName()), block);

            // the written type is always a non-nillable String
            type = String.class;
            nillable = false;

            // if (id != null) write the value
            JConditional nullCond = block._if(itemVar.ne(JExpr._null()));
            block = nullCond._then();
        }

        // write start element
        QName name = mapping.getXmlName();
        block.add(builder.getWriteStartElement(name));

        // if nillable, we need to write xsi:nil when value is null
        JBlock elementWriteBlock = block;
        if (nillable && !type.isPrimitive()) {
            JConditional nilCond = block._if(itemVar.ne(JExpr._null()));
            elementWriteBlock = nilCond._then();
            nilCond._else().add(builder.getXSW().invoke("writeXsiNil"));
        }

        // write element
        Bean targetBean = model.getBean(type);
        if (targetBean == null || targetBean.getType().isEnum()) {
            // simple built in types like String
            writeSimpleTypeElement(builder, itemVar, type, elementWriteBlock);
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

    private JVar getValue(JAXBObjectBuilder builder, Property property, JBlock block) {
        return getValue(builder, builder.getWriteObject(), property, block);
    }

    private JVar getValue(JAXBObjectBuilder builder, JExpression beanVar, Property property, JBlock block) {
        String propertyName = property.getName();
        if (property.getAdapterType() != null) {
            propertyName += "Raw";
        }

        return getValue(builder, beanVar, property, propertyName, block);
    }

    private JVar getValue(JAXBObjectBuilder builder, JExpression beanVar, Property property, String propertyNameHint, JBlock block) {
        Class propertyType = toClass(property.getType());

        String propertyName = builder.getWriteVariableManager().createId(propertyNameHint);

        JVar propertyVar = block.decl(
                context.getGenericType(property.getType()),
                propertyName);

        if (property.getField() != null) {
            Field field = property.getField();

            if (!isPrivate(field)) {
                propertyVar.init(beanVar.ref(field.getName()));
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

                propertyVar.init(fieldAccessorField.invoke(methodName).arg(beanVar).arg(builder.getWriteContextVar()).arg(beanVar));
            }
        } else if (property.getGetter() != null) {
            Method getter = property.getGetter();
            if (!isPrivate(getter)) {
                propertyVar.init(JExpr._null());

                JTryBlock tryGetter = block._try();
                tryGetter.body().assign(propertyVar, beanVar.invoke(getter.getName()));

                JCatchBlock catchException = tryGetter._catch(context.toJClass(Exception.class));
                catchException.body().invoke(builder.getReadContextVar(), "getterError")
                        .arg(beanVar)
                        .arg(property.getName())
                        .arg(context.dotclass(property.getBean().getType()))
                        .arg(getter.getName())
                        .arg(catchException.param("e"));
            } else {
                JFieldVar propertyAccessorField = builder.getPrivatePropertyAccessor(property.getGetter(), property.getSetter(), property.getName());
                propertyVar.init(propertyAccessorField.invoke("getObject").arg(beanVar).arg(builder.getWriteContextVar()).arg(beanVar));
            }
        } else {
            throw new BuildException("Property does not have a getter " + property.getBean().getClass().getName() + "." + property.getName());
        }

        return propertyVar;
    }

    private void writeClassWriter(JAXBObjectBuilder builder, Bean bean, JBlock block, JExpression propertyVar) {
        // Complex type which will already have an element builder defined
        JAXBObjectBuilder existingBuilder = builders.get(bean);
        if (existingBuilder == null) {
            throw new BuildException("Unknown bean " + bean);
        }

        // Declare dependency from builder to existingBuilder
        builder.addDependency(existingBuilder.getJAXBObjectClass());

        // Add a static import for the write method on the existing builder class
        String methodName = "write" + bean.getType().getSimpleName();
        if (builder != existingBuilder) {
            JStaticImports staticImports = JStaticImports.getStaticImports(builder.getJAXBObjectClass());
            staticImports.addStaticImport(existingBuilder.getJAXBObjectClass().fullName() + "." + methodName);
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

    private <T extends JExpression> T writeAdapterConversion(JAXBObjectBuilder builder, JBlock block, Property property, T propertyVar) {
        if (property.getAdapterType() != null) {
            JVar adapterVar = builder.getAdapter(property.getAdapterType());
            JVar valueVar = block.decl(context.toJClass(property.getComponentAdaptedType()), builder.getWriteVariableManager().createId(property.getName()), JExpr._null());

            JTryBlock tryBlock = block._try();
            tryBlock.body().assign(valueVar, adapterVar.invoke("marshal").arg(propertyVar));

            JCatchBlock catchException = tryBlock._catch(context.toJClass(Exception.class));
            JBlock catchBody = catchException.body();
            catchBody.invoke(builder.getReadContextVar(), "xmlAdapterError")
                    .arg(builder.getWriteObject())
                    .arg(property.getName())
                    .arg(context.dotclass(property.getAdapterType()))
                    .arg(context.dotclass(toClass(property.getType())))  // currently we only support conversion between same type
                    .arg(context.dotclass(toClass(property.getType())))
                    .arg(catchException.param("e"));

            //noinspection unchecked
            propertyVar = (T) valueVar;
        }
        return propertyVar;
    }

    private void writeSimpleTypeElement(JAXBObjectBuilder builder, JExpression object, Class type, JBlock block) {
        if(isBuiltinType(type)) {
            block.add(builder.getXSW().invoke("writeCharacters").arg(toString(builder, object, type)));
        } else if (type.equals(byte[].class)) {
            block.add(context.toJClass(BinaryUtils.class).staticInvoke("encodeBytes").arg(builder.getXSW()).arg(object));
        } else if (type.equals(QName.class)) {
            block.add(builder.getXSW().invoke("writeQName").arg(object));
        } else if (type.equals(DataHandler.class) || type.equals(Image.class)) {
            // todo support AttachmentMarshaller
        } else if (type.equals(Object.class)) {
            block.add(builder.getXSW().invoke("writeDomElement").arg(JExpr.cast(context.toJClass(Element.class), object)).arg(JExpr.FALSE));
        } else {
        	logger.info("(JAXB Writer) Cannot map simple type yet: " + type);
        }
    }

    private void writeSimpleTypeAttribute(JAXBObjectBuilder builder, JBlock block, Property property, JExpression propertyVar) {
        propertyVar = writeAdapterConversion(builder, block, property, propertyVar);

        Class type;
        if (property.getAdapterType() == null) {
            type = toClass(property.getComponentType());
        } else {
            type = property.getComponentAdaptedType();
        }

        // if this is an id ref we write the ID property of the target bean instead of the bean itself
        if (property.isIdref()) {
            Property idProperty = findReferencedIdProperty(property);

            // read the id value
            propertyVar = getValue(builder, propertyVar, idProperty, property.getName() + JavaUtils.capitalize(idProperty.getName()), block);

            // the written type is always String
            type = String.class;

            // if (id != null) write the value
            JConditional nullCond = block._if(propertyVar.ne(JExpr._null()));
            block = nullCond._then();
        }

        if(isBuiltinType(type)) {
            propertyVar = toString(builder, propertyVar, type);
        } else if (type.equals(byte[].class)) {
            propertyVar = context.toJClass(Base64.class).staticInvoke("encode").arg(propertyVar);
        } else if (type.equals(QName.class)) {
            propertyVar = builder.getXSW().invoke("getQNameAsString").arg(propertyVar);
        } else if (type.equals(DataHandler.class) || type.equals(Image.class)) {
            // todo support AttachmentMarshaller
        } else {
            logger.info("(JAXB Writer) Cannot map simple attribute type yet: " + type);
            return;
        }

        QName name = property.getXmlName();
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
                  .arg(propertyVar));
    }

    private Property findReferencedIdProperty(Property property) {
        // find referenced bean
        Bean targetBean = model.getBean(toClass(property.getComponentType()));
        if (targetBean == null) {
            throw new BuildException("Unknown bean " + toClass(property.getType()));
        }

        // find id property on referenced bean
        Property idProperty = null;
        while (idProperty == null) {
            for (Property targetProperty : targetBean.getProperties()) {
                if (targetProperty.isId()) {
                    idProperty = targetProperty;
                    break;
                }
            }
            if (idProperty == null) {
                if (targetBean.getBaseClass() == null) {
                    throw new BuildException("Property " + property + " is an IDREF, but property type " + toClass(property.getType()).getName() + " does not have an ID property");
                }
                targetBean = targetBean.getBaseClass();
            }
        }
        return idProperty;
    }

    private void writeSimpleTypeAttribute(JAXBObjectBuilder builder, JBlock block, JExpression qnameVar, Class type, JExpression value) {
        if(isBuiltinType(type)) {
            value = toString(builder, value, type);
        } else if (type.equals(byte[].class)) {
            value = context.toJClass(Base64.class).staticInvoke("encode").arg(value);
        } else if (type.equals(QName.class)) {
            value = builder.getXSW().invoke("getQNameAsString").arg(value);
        } else if (type.equals(DataHandler.class) || type.equals(Image.class)) {
            // todo support AttachmentMarshaller
        } else {
            logger.info("(JAXB Writer) Cannot map simple attribute type yet: " + type);
            return;
        }

        block.add(builder.getXSW().invoke("writeAttribute").arg(qnameVar).arg(value));
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

    private JExpression toString(JAXBObjectBuilder builder, JExpression value, Class<?> type) {
        if (type.isPrimitive()) {
            if (type.equals(boolean.class)) {
                return context.toJClass(Boolean.class).staticInvoke("toString").arg(value);
            } else if (type.equals(byte.class)) {
                return context.toJClass(Byte.class).staticInvoke("toString").arg(value);
            } else if (type.equals(short.class)) {
                return context.toJClass(Short.class).staticInvoke("toString").arg(value);
            } else if (type.equals(int.class)) {
                return context.toJClass(Integer.class).staticInvoke("toString").arg(value);
            } else if (type.equals(long.class)) {
                return context.toJClass(Long.class).staticInvoke("toString").arg(value);
            } else if (type.equals(float.class)) {
                return context.toJClass(Float.class).staticInvoke("toString").arg(value);
            } else if (type.equals(double.class)) {
                return context.toJClass(Double.class).staticInvoke("toString").arg(value);
            }
        } else {
            if (type.equals(String.class)) {
                return value;
            } else if (type.equals(Boolean.class)) {
                return context.toJClass(Boolean.class).staticInvoke("toString").arg(value);
            } else if (type.equals(Byte.class)) {
                return context.toJClass(Byte.class).staticInvoke("toString").arg(value);
            } else if (type.equals(Short.class)) {
                return context.toJClass(Short.class).staticInvoke("toString").arg(value);
            } else if (type.equals(Integer.class)) {
                return context.toJClass(Integer.class).staticInvoke("toString").arg(value);
            } else if (type.equals(Long.class)) {
                return context.toJClass(Long.class).staticInvoke("toString").arg(value);
            } else if (type.equals(Float.class)) {
                return context.toJClass(Float.class).staticInvoke("toString").arg(value);
            } else if (type.equals(Double.class)) {
                return context.toJClass(Double.class).staticInvoke("toString").arg(value);
            } else if (type.equals(XMLGregorianCalendar.class)) {
                return value.invoke("toXMLFormat");
            } else if (type.equals(Duration.class)) {
                return value.invoke("toString");
            } else if (type.equals(BigDecimal.class)) {
                return value.invoke("toString");
            } else if (type.equals(BigInteger.class)) {
                return value.invoke("toString");
            } else if (type.isEnum()) {
                JAXBEnumBuilder enumBuilder = enumBuilders.get(type);
                if (enumBuilder == null) {
                    throw new BuildException("Unknown enum type " + type);
                }

                return invokeEnumToString(builder, builder.getWriteObject(), JExpr._null(), enumBuilder, value);
            }
        }
        throw new UnsupportedOperationException("Invalid type " + type);
    }

    private JInvocation invokeEnumToString(JAXBObjectBuilder caller, JVar beanVar, JExpression parameterName, JAXBEnumBuilder enumBuilder, JExpression value) {
        // Declare dependency from caller to parser
        caller.addDependency(enumBuilder.getJAXBEnumClass());

        // Add a static import for the toString method on the existing builder class
        String methodName = "toString" + enumBuilder.getType().getSimpleName();
        JStaticImports staticImports = JStaticImports.getStaticImports(caller.getJAXBObjectClass());
        staticImports.addStaticImport(enumBuilder.getJAXBEnumClass().fullName() + "." + methodName);

        // Call the static method
        JInvocation invocation = JExpr.invoke(methodName)
                .arg(beanVar)
                .arg(parameterName)
                .arg(caller.getWriteContextVar())
                .arg(value);

        return invocation;
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
