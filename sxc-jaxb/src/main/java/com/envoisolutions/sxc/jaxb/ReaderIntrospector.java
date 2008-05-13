package com.envoisolutions.sxc.jaxb;

import java.awt.Image;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;
import javax.activation.DataHandler;
import javax.xml.bind.JAXBElement;
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
import com.envoisolutions.sxc.util.ArrayUtil;
import com.envoisolutions.sxc.util.Base64;
import com.sun.codemodel.JArray;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JCatchBlock;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JTryBlock;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import com.sun.codemodel.JForEach;
import org.w3c.dom.Element;

public class ReaderIntrospector {
    private static final Logger logger = Logger.getLogger(ReaderIntrospector.class.getName());

    private final BuilderContext context;
    private final Model model;
    private final Map<Bean, JAXBObjectBuilder> builders = new LinkedHashMap<Bean, JAXBObjectBuilder>();
    private final Map<Class, JAXBEnumBuilder> enumBuilders = new LinkedHashMap<Class, JAXBEnumBuilder>();

    public ReaderIntrospector(BuilderContext context, Model model) {
        this.context = context;
        this.model = model;

        // build all enum parsers so they are available for use by bean readers
        for (EnumInfo enumInfo : model.getEnums()) {
            addEnum(enumInfo);
        }

        // declare all parser methods first, so everything exists when we build
        for (Bean bean : this.model.getBeans()) {
            JAXBObjectBuilder builder = context.createJAXBObjectBuilder(bean.getType(), bean.getRootElementName(), bean.getSchemaTypeName());

            LinkedHashSet<Property> allProperties = new LinkedHashSet<Property>();
            for (Bean b = bean; b != null; b = b.getBaseClass()) {
                allProperties.addAll(b.getProperties());
            }

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

        // build parsers
        for (Bean bean : model.getBeans()) {
            if (!bean.getType().isEnum()) {
                JAXBObjectBuilder builder = builders.get(bean);
                if (builder != null) {
                    add(builder, bean);
                }
            }
        }
    }

    private JInvocation invokeParser(JAXBObjectBuilder caller, JVar callerXsrVar, JAXBObjectBuilder parser) {
        // Declare dependency from caller to parser
        caller.addDependency(parser.getJAXBObjectClass());

        // Add a static import for the read method on the existing builder class
        String methodName = "read" + parser.getReadMethod().type().name();
        if (caller != parser) {
            JStaticImports staticImports = JStaticImports.getStaticImports(caller.getJAXBObjectClass());
            staticImports.addStaticImport(parser.getJAXBObjectClass().fullName() + "." + methodName);
        }

        // Call the static method
        JInvocation invocation = JExpr.invoke(methodName).arg(callerXsrVar).arg(caller.getReadContextVar());
        return invocation;
    }

    private JInvocation invokeEnumParser(JAXBObjectBuilder caller, JVar callerXsrVar, JAXBEnumBuilder parser, JExpression value) {
        // Declare dependency from caller to parser
        caller.addDependency(parser.getJAXBEnumClass());

        // Add a static import for the parse method on the existing builder class
        String methodName = "parse" + parser.getType().getSimpleName();
        JStaticImports staticImports = JStaticImports.getStaticImports(caller.getJAXBObjectClass());
        staticImports.addStaticImport(parser.getJAXBEnumClass().fullName() + "." + methodName);

        // Call the static method
        JInvocation invocation = JExpr.invoke(methodName).arg(callerXsrVar).arg(caller.getReadContextVar()).arg(value);
        return invocation;
    }

    private JAXBObjectBuilder add(JAXBObjectBuilder builder, Bean bean) {
        // read properties
        if (!Modifier.isAbstract(bean.getType().getModifiers())) {
            handleProperties(builder, bean, builder.getReadObject());
        }

        // This element may be replaced with another type using the xsi:type override
        // Add xsi type support for an other Bean that can be assigned to this property according to Java type assignment rules
        for (Bean xsiTypeBean : model.getBeans()) {
            Class xsiTypeClass = xsiTypeBean.getType();
            if (bean.getType().isAssignableFrom(xsiTypeClass) && bean.getType() != xsiTypeClass && !Modifier.isAbstract(xsiTypeClass.getModifiers())) {
                JBlock block = builder.expectXsiType(xsiTypeBean.getSchemaTypeName());

                JAXBObjectBuilder elementBuilder = builders.get(xsiTypeBean);
                if (elementBuilder == null) {
                    throw new BuildException("Unknown bean " + bean);
                }

                // invoke the reader method
                JInvocation method = invokeParser(builder, builder.getXSR(), elementBuilder);
                block._return(method);
            }
        }

        return builder;
    }

    private void addEnum(EnumInfo enumInfo) {
        JAXBEnumBuilder builder = context.createJAXBEnumBuilder(enumInfo.getType(), enumInfo.getRootElementName(), enumInfo.getSchemaTypeName());

        JMethod method = builder.getParseMethod();

        JIfElseBlock enumCond = new JIfElseBlock();
        method.body().add(enumCond);
        for (Map.Entry<Enum, String> entry : enumInfo.getEnumMap().entrySet()) {
            Enum enumValue = entry.getKey();
            String enumText = entry.getValue();

            JExpression textCompare = JExpr.lit(enumText).invoke("equals").arg(builder.getParseValue());
            JBlock block = enumCond.addCondition(textCompare);
            block._return(context.toJClass(enumInfo.getType()).staticRef(enumValue.name()));
        }

        JInvocation unexpectedInvoke = enumCond._else().invoke(builder.getParseContext(), "unexpectedEnumValue")
                .arg(builder.getParseXSR())
                .arg(context.toJClass(enumInfo.getType()).dotclass())
                .arg(builder.getParseValue());

        for (String expectedValue : enumInfo.getEnumMap().values()) {
            unexpectedInvoke.arg(expectedValue);
        }
        enumCond._else()._return(JExpr._null());

        enumBuilders.put(enumInfo.getType(), builder);
    }

    private void handleProperties(JAXBObjectBuilder builder, Bean bean, JVar beanVar) {
        for (Property property : bean.getProperties()) {
            // DS: the JaxB spec does not define the mapping for char or char[]
            // the RI reads the string as an int and then converts it to a char so 42 becomes '*'
            // I think this is lame so I didn't implement it. You can use a XmlAdapter to covert
            // if you really want to handle char.
            if (char.class.equals(property.getType()) || char[].class.equals(property.getType())) {
                logger.info("(JAXB Reader) JaxB specification does not support property " + property.getName()
                                   + " with type " + property.getType()
                                   + " on " + bean.getClass() + ": Use a XmlAdapter");
                continue;
            }

            switch (property.getXmlStyle()) {
                case ATTRIBUTE: {
                    if (!property.isXmlAny()) {
                        // create attribute block
                        JBlock block = builder.expectAttribute(property.getXmlName());

                        // add comment for readability
                        block.add(new JLineComment(property.getXmlStyle() + ": " + property.getName()));

                        // create collection var if necessary
                        JVar collectionVar = handleCollection(builder, property, beanVar);

                        // get the value to evaluate
                        JExpression value;
                        if (!property.isCollection()) {
                            value = builder.getAttributeVar().invoke("getValue");
                        } else {
                            JForEach forEach = block.forEach(context.toJClass(String.class), builder.getReadVariableManager().createId(property.getName() + "Item"), builder.getAttributeVar().invoke("getXmlListValue"));
                            block = forEach.body();
                            value = forEach.var();
                        }
                        
                        // read and set
                        JExpression toSet = handleAttribute(builder, block, property, value);
                        doSet(builder, block, property, beanVar, toSet, collectionVar);
                    } else {
                        handleAnyAttribute(builder, property, beanVar);
                    }

                }
                break;

                case ELEMENT:
                case ELEMENT_REF: {
                    JAXBObjectBuilder elementBuilder = builder;
                    JVar parentVar = beanVar;
                    if (property.getXmlName() != null && !property.isXmlList()) {
                        elementBuilder = builder.expectWrapperElement(property.getXmlName(), beanVar, property.getName());
                    }

                    // create collection var if necessary
                    JVar collectionVar = handleCollection(elementBuilder, property, parentVar);

                    for (ElementMapping mapping : property.getElementMappings()) {
                        // create element block
                        JBlock block = elementBuilder.expectElement(mapping.getXmlName());

                        // add comment for readability
                        block.add(new JLineComment(property.getXmlStyle() + ": " + property.getName()));

                        // get the value to evaluate
                        JVar xsrVar = builder.getChildElementVar();
                        if (!property.isXmlList()) {
                            // read and set
                            JExpression toSet = handleElement(builder, xsrVar, block, property, mapping.isNillable(), mapping.getComponentType());
                            doSet(builder, block, property, parentVar, toSet, collectionVar);
                        } else {
                            JForEach forEach = block.forEach(context.toJClass(String.class), builder.getReadVariableManager().createId(property.getName() + "Item"), xsrVar.invoke("getElementAsXmlList"));
                            block = forEach.body();
                            JExpression value = forEach.var();

                            // read and set
                            String propertyName = property.getName();
                            if (property.isCollection()) propertyName += "Item";
                            propertyName = builder.getReadVariableManager().createId(propertyName);

                            JExpression toSet;
                            if (property.isIdref() || property.getAdapterType() == null) {
                                toSet = coerce(builder, xsrVar, value, toClass(mapping.getComponentType()));
                            } else {
                                // adapted type
                                JVar adapterVar = builder.getAdapter(property.getAdapterType());
                    
                                block.add(new JBlankLine());

                                // convert raw value into bound type
                                Class targetType = toClass(mapping.getComponentType());
                                JVar valueVar = block.decl(context.toJClass(targetType), propertyName);
                                JTryBlock tryBlock = block._try();
                                tryBlock.body().assign(valueVar, adapterVar.invoke("unmarshal").arg(value));

                                JCatchBlock catchException = tryBlock._catch(context.toJClass(Exception.class));
                                JBlock catchBody = catchException.body();
                                catchBody.invoke(builder.getReadContextVar(), "xmlAdapterError")
                                        .arg(xsrVar)
                                        .arg(context.dotclass(property.getAdapterType()))
                                        .arg(context.dotclass(targetType))
                                        .arg(context.dotclass(targetType))
                                        .arg(catchException.param("e"));
                                catchBody._continue();

                                block.add(new JBlankLine());

                                toSet = valueVar;
                            }

                            // JaxB refs need to be wrapped with a JAXBElement
                            if (toClass(property.getComponentType()).equals(JAXBElement.class)) {
                                toSet = newJaxBElement(xsrVar, toClass(mapping.getComponentType()), toSet);
                            }

                            doSet(builder, block, property, parentVar, toSet, collectionVar);
                        }

                    }

                    if (property.isXmlAny()) {
                        // create element block
                        JBlock block = elementBuilder.expectAnyElement();

                        // add comment for readability
                        block.add(new JLineComment(property.getXmlStyle() + ": " + property.getName()));

                        // read and set
                        JInvocation toSet = builder.getReadContextVar().invoke("readXmlAny")
                                .arg(builder.getXSR())
                                .arg(context.dotclass(property.getComponentType()))
                                .arg(property.isLax() ? JExpr.TRUE : JExpr.FALSE);
                        doSet(builder, block, property, parentVar, toSet, collectionVar);
                    }
                }
                break;

                case VALUE: {
                    // value is read in class block
                    JBlock block = builder.expectValue();

                    // add comment for readability
                    block.add(new JBlankLine());
                    block.add(new JLineComment(property.getXmlStyle() + ": " + property.getName()));

                    // read and set value
                    handleValue(builder, builder.getXSR(), block, property, beanVar);
                }
                break;
            }
        }

        // handle properties of the base class
        if (bean.getBaseClass() != null) {
            handleProperties(builder, bean.getBaseClass(), beanVar);
        }
    }

    private JExpression handleAttribute(JAXBObjectBuilder builder, JBlock block, Property property, JExpression value) {
        JExpression toSet;
        if (property.isIdref()) {
            toSet = value;
        } else if (property.getAdapterType() != null) {
            JVar adapterVar = builder.getAdapter(property.getAdapterType());
            toSet = adapterVar.invoke("unmarshal").arg(value);
        } else {
            String propertyName = property.getName();
            if (property.isCollection() || property.isXmlAny()) propertyName += "Item";
            propertyName = builder.getReadVariableManager().createId(propertyName);

            Class clazz = toClass(property.getComponentType());
            if (isBuiltinType(clazz)) {
                toSet = block.decl(context.toJType(clazz), propertyName, coerce(builder, builder.getXSR(), value, clazz));
            } else if (clazz.equals(byte[].class)) {
                toSet = context.toJClass(Base64.class).staticInvoke("decode").arg(value);
            } else if (clazz.equals(QName.class)) {
                JVar var = as(builder, value, block, String.class, propertyName);
                toSet = builder.getXSR().invoke("getAsQName").arg(var);
            } else if (clazz.equals(DataHandler.class) || clazz.equals(Image.class)) {
                // todo support AttachmentMarshaller
                toSet = JExpr._null();
            } else {
                logger.severe("Could not map attribute " + propertyName + " of type " + clazz);
                toSet = JExpr._null();
            }
        }
        return toSet;
    }

    private void handleAnyAttribute(JAXBObjectBuilder builder, Property property, JVar beanVar) {
        // create element block
        JBlock block = builder.expectAnyAttribute();

        // add comment for readability
        block.add(new JLineComment(property.getXmlStyle() + ": " + property.getName()));

        //
        // declare any attribute map property at top of method
        String mapVarName = builder.getReadVariableManager().createId(property.getName());
        JVar mapVar = builder.getReadMethod().body().decl(context.getGenericType(property.getType()), mapVarName, JExpr._null());

        //
        // read attribute
        JExpression toSet = handleAttribute(builder, block, property, builder.getAttributeVar().invoke("getValue"));

        //
        // add attribute to map

        // if (map == null) {
        JBlock createMapBlock = block._if(mapVar.eq(JExpr._null()))._then();
        //     map = (Map) bean.getAnyTypeMap();
        if (property.getField() == null && property.getGetter() == null) {
            // write only maps are not allowed by the spec, but we can support it anyway
            JType mapType = getMapClass(property.getType(), property.getComponentType());
            if (mapType == null) {
                throw new BuildException("AnyAttribute map property does not have a getter and map does not have a default constructor: " +
                        property.getBean().getType().getName() + "." + property.getName());
            }
            createMapBlock.assign(mapVar, JExpr._new(mapType));
        } else {
            if (property.getField() != null) {
                Field field = property.getField();

                if (!isPrivate(field)) {
                    createMapBlock.assign(mapVar, beanVar.ref(field.getName()));
                } else {
                    JFieldVar fieldAccessorField = builder.getPrivateFieldAccessor(field);
                    createMapBlock.assign(mapVar, fieldAccessorField.invoke("getObject").arg(builder.getXSR()).arg(builder.getReadContextVar()).arg(beanVar));
                }
            } else {
                Method getter = property.getGetter() ;
                if (!isPrivate(getter)) {
                    JTryBlock tryGetter = createMapBlock._try();
                    tryGetter.body().assign(mapVar, beanVar.invoke(getter.getName()));

                    JCatchBlock catchException = tryGetter._catch(context.toJClass(Exception.class));
                    catchException.body().invoke(builder.getReadContextVar(), "getterError")
                            .arg(builder.getXSR())
                            .arg(context.dotclass(property.getBean().getType()))
                            .arg(getter.getName())
                            .arg(catchException.param("e"));
                    catchException.body()._continue();
                } else {
                    JFieldVar propertyAccessorField = builder.getPrivatePropertyAccessor(property.getGetter(), property.getSetter(), property.getName());
                    createMapBlock.assign(mapVar, propertyAccessorField.invoke("getObject").arg(builder.getXSR()).arg(builder.getReadContextVar()).arg(beanVar));
                }
            }

            //     if (map != null) {
            //         map.clear();
            JConditional arrayFoundCondition = createMapBlock._if(mapVar.ne(JExpr._null()));
            arrayFoundCondition._then().invoke(mapVar, "clear");

            //     } else {
            //         map = new ArrayList();
            JType mapType = getMapClass(property.getType(), property.getComponentType());
            if (mapType != null) {
                arrayFoundCondition._else().assign(mapVar, JExpr._new(mapType));
            } else {
                arrayFoundCondition._else().invoke(builder.getReadContextVar(), "uncreatableMap")
                        .arg(builder.getXSR())
                        .arg(context.dotclass(property.getBean().getType()))
                        .arg(property.getName())
                        .arg(context.dotclass(property.getType()));
                arrayFoundCondition._else()._continue();
            }
        }
        //     }
        // }

        // collection.add(item);
        block.add(mapVar.invoke("put").arg(builder.getAttributeVar().invoke("getName")).arg(toSet));


        //
        // set the map into the bean at the bottom of the method
        //
        // if (map != null) {
        //     bean.setAnyAttribute(map);
        // }
        if (property.getField() != null) {
            Field field = property.getField();

            JBlock assignMapBlock = builder.getReadTailBlock()._if(mapVar.ne(JExpr._null()))._then();

            if (!isPrivate(field)) {
                assignMapBlock.assign(beanVar.ref(field.getName()), mapVar);
            } else {
                JFieldVar fieldAccessorField = builder.getPrivateFieldAccessor(field);
                assignMapBlock.add(fieldAccessorField.invoke("setObject").arg(builder.getXSR()).arg(builder.getReadContextVar()).arg(beanVar).arg(mapVar));
            }
        } else {
            // if there is no setter method, the map is not assigned into the class
            // this assumes that the getter returned the a map instance and held on to a reference
            Method setter = property.getSetter();
            if (setter != null) {
                JBlock assignMapBlock = builder.getReadTailBlock()._if(mapVar.ne(JExpr._null()))._then();
                if (!isPrivate(setter)) {

                    JTryBlock trySetter = assignMapBlock._try();
                    trySetter.body().add(beanVar.invoke(setter.getName()).arg(mapVar));

                    JCatchBlock catchException = trySetter._catch(context.toJClass(Exception.class));
                    catchException.body().invoke(builder.getReadContextVar(), "setterError")
                            .arg(builder.getXSR())
                            .arg(context.dotclass(property.getBean().getType()))
                            .arg(setter.getName())
                            .arg(context.dotclass(setter.getParameterTypes()[0]))
                            .arg(catchException.param("e"));
                } else {
                    JFieldVar propertyAccessorField = builder.getPrivatePropertyAccessor(property.getGetter(), property.getSetter(), property.getName());
                    assignMapBlock.add(propertyAccessorField.invoke("setObject").arg(builder.getXSR()).arg(builder.getReadContextVar()).arg(beanVar).arg(mapVar));
                }
            }
        }
    }

    private JExpression handleElement(JAXBObjectBuilder builder, JVar xsrVar, JBlock block, Property property, boolean nillable, Type componentType) {

        String propertyName = property.getName();
        if (property.isCollection()) propertyName += "Item";
        propertyName = builder.getReadVariableManager().createId(propertyName);

        JExpression toSet;
        if (property.isIdref()) {
            // id ref... read string value which will later be turned into correct type
            toSet = xsrVar.invoke("getElementAsString");
        } else if (property.getAdapterType() != null) {
            // adapted type
            JVar adapterVar = builder.getAdapter(property.getAdapterType());

            // read the raw value
            JVar xmlValueVar = readElement(builder, xsrVar, block, nillable, builder.getReadVariableManager().createId(propertyName + "Raw"), property.getComponentAdaptedType());
            block.add(new JBlankLine());

            // convert raw value into bound type
            Class targetType = toClass(componentType);
            JVar valueVar = block.decl(context.toJClass(targetType), propertyName);
            JTryBlock tryBlock = block._try();
            tryBlock.body().assign(valueVar, adapterVar.invoke("unmarshal").arg(xmlValueVar));

            JCatchBlock catchException = tryBlock._catch(context.toJClass(Exception.class));
            JBlock catchBody = catchException.body();
            catchBody.invoke(builder.getReadContextVar(), "xmlAdapterError")
                    .arg(xsrVar)
                    .arg(context.dotclass(property.getAdapterType()))
                    .arg(context.dotclass(targetType))
                    .arg(context.dotclass(targetType))
                    .arg(catchException.param("e"));
            catchBody._continue();

            block.add(new JBlankLine());

            toSet = valueVar;
        } else {
            // plain old element type
            toSet = readElement(builder,  xsrVar, block, nillable, propertyName, toClass(componentType));
        }

        // JaxB refs need to be wrapped with a JAXBElement
        if (toClass(property.getComponentType()).equals(JAXBElement.class)) {
            toSet = newJaxBElement(xsrVar, toClass(componentType), toSet);
        }

        return toSet;
    }

    private JVar readElement(JAXBObjectBuilder builder, JVar xsrVar, JBlock block, boolean nillable, String propertyName, Class targetType) {
        JVar toSet;
        if (targetType.equals(Byte.class) || targetType.equals(byte.class)) {
            // todo why the special read method for byte?
            toSet = block.decl(context.toJType(byte.class), propertyName, JExpr.cast(context.toJType(byte.class), xsrVar.invoke("getElementAsInt")));
        } else if (isBuiltinType(targetType)) {
            toSet = as(builder, xsrVar, block, targetType, propertyName, nillable);
        } else if (targetType.equals(byte[].class)) {
            toSet = block.decl(context.toJClass(byte[].class), propertyName, context.toJClass(BinaryUtils.class).staticInvoke("decodeAsBytes").arg(xsrVar));
        } else if (targetType.equals(QName.class)) {
            toSet = block.decl(context.toJClass(QName.class), propertyName, xsrVar.invoke("getElementAsQName"));
        } else if (targetType.equals(DataHandler.class) || targetType.equals(Image.class)) {
            // todo support AttachmentMarshaller
            toSet = block.decl(context.toJClass(targetType), propertyName, JExpr._null());
        } else if (targetType.equals(Object.class) || targetType.equals(Element.class)) {
            toSet = block.decl(context.toJClass(Element.class), propertyName, xsrVar.invoke("getElementAsDomElement"));
        } else {
            // Complex type which will already have an element builder defined
            Bean targetBean = model.getBean(targetType);
            JAXBObjectBuilder elementBuilder = builders.get(targetBean);
            if (elementBuilder == null) {
                toSet = block.decl(context.toJClass(targetType), propertyName, builder.getReadContextVar().invoke("unexpectedXsiType").arg(builder.getXSR()).arg(JExpr.dotclass(context.toJClass(targetType))));
            } else {
                // invoke the reader method
                JInvocation invocation = invokeParser(builder, builder.getChildElementVar(), elementBuilder);
                toSet = block.decl(context.toJClass(targetType), propertyName, invocation);
            }
        }
        return toSet;
    }

    private void handleValue(JAXBObjectBuilder builder, JVar xsrVar, JBlock block, Property property, JVar beanVar) {
        // if this is a collection, create the collection variable
        JVar collectionVar = handleCollection(builder, property, beanVar);

        // get the value to evaluate 
        JExpression value;
        if (!property.isCollection()) {
            value = xsrVar.invoke("getElementText");
        } else {
            JForEach forEach = block.forEach(context.toJClass(String.class), builder.getReadVariableManager().createId(property.getName() + "Item"), builder.getXSR().invoke("getElementAsXmlList"));
            block = forEach.body();
            value = forEach.var();
        }

        // read and set value
        Class targetType = toClass(property.getComponentType());

        String propertyName = property.getName();
        propertyName = builder.getReadVariableManager().createId(propertyName);

        JExpression toSet;
        if (property.getAdapterType() != null) {
            JVar adapterVar = builder.getAdapter(property.getAdapterType());

            JVar xmlValueVar = block.decl(context.toJClass(String.class), builder.getReadVariableManager().createId(propertyName + "Raw"), value);
            block.add(new JBlankLine());

            JVar valueVar = block.decl(context.toJClass(targetType), propertyName, JExpr._null());
            JVar isConvertedVar = block.decl(context.toJType(boolean.class), builder.getReadVariableManager().createId(propertyName + "Converted"));

            JTryBlock tryBlock = block._try();
            tryBlock.body().assign(valueVar, adapterVar.invoke("unmarshal").arg(xmlValueVar));
            tryBlock.body().assign(isConvertedVar, JExpr.TRUE);

            JCatchBlock catchException = tryBlock._catch(context.toJClass(Exception.class));
            JBlock catchBody = catchException.body();
            catchBody.invoke(builder.getReadContextVar(), "xmlAdapterError")
                    .arg(xsrVar)
                    .arg(context.dotclass(property.getAdapterType()))
                    .arg(context.dotclass(targetType)) // currently we only support conversion between same type
                    .arg(context.dotclass(targetType))
                    .arg(catchException.param("e"));
            catchBody.assign(isConvertedVar, JExpr.FALSE);

            block.add(new JBlankLine());

            toSet = valueVar;
            block = block._if(isConvertedVar)._then();
        } else if (!property.isCollection() && (targetType.equals(Byte.class) || targetType.equals(byte.class))) {
            // todo why the special read method for byte?
            toSet = JExpr.cast(context.toJType(byte.class), xsrVar.invoke("getElementAsInt"));
        } else if (isBuiltinType(targetType)) {
            toSet = coerce(builder, builder.getXSR(), value, targetType);
        } else if (!property.isCollection() && targetType.equals(byte[].class)) {
            toSet = context.toJClass(BinaryUtils.class).staticInvoke("decodeAsBytes").arg(xsrVar);
        } else if (!property.isCollection() && targetType.equals(QName.class)) {
            toSet = xsrVar.invoke("getElementAsQName");
        } else if (!property.isCollection() && (targetType.equals(DataHandler.class) || targetType.equals(Image.class))) {
            // todo support AttachmentMarshaller
            toSet = JExpr._null();
        } else {
            logger.severe("Could not map element value " + propertyName + " of type " + property.getType());
            toSet = JExpr._null();
        }
        doSet(builder, block, property, beanVar, toSet, collectionVar);
    }

    private JVar handleCollection(JAXBObjectBuilder builder, Property property, JVar beanVar) {
        if (!property.isCollection()) {
            return null;
        }

        Class propertyType = toClass(property.getType());

        JType collectionType;
        if (propertyType.isArray()) {
            Class componentType = propertyType.getComponentType();
            if (Boolean.TYPE.equals(componentType)) {
                collectionType = context.toJClass(ArrayUtil.BooleanArray.class);
            } else if (Character.TYPE.equals(componentType)) {
                collectionType = context.toJClass(ArrayUtil.CharArray.class);
            } else if (Short.TYPE.equals(componentType)) {
                collectionType = context.toJClass(ArrayUtil.ShortArray.class);
            } else if (Integer.TYPE.equals(componentType)) {
                collectionType = context.toJClass(ArrayUtil.IntArray.class);
            } else if (Long.TYPE.equals(componentType)) {
                collectionType = context.toJClass(ArrayUtil.LongArray.class);
            } else if (Float.TYPE.equals(componentType)) {
                collectionType = context.toJClass(ArrayUtil.FloatArray.class);
            } else if (Double.TYPE.equals(componentType)) {
                collectionType = context.toJClass(ArrayUtil.DoubleArray.class);
            } else {
                collectionType = context.toJClass(ArrayList.class).narrow(componentType);
            }
        } else {
            collectionType = context.getGenericType(property.getType());
        }

        String collectionVarName = builder.getReadVariableManager().createId(property.getName());
        JVar collectionVar = builder.getReadMethod().body().decl(collectionType, collectionVarName, JExpr._null());

        // inside of the tail block of the expect element method (at the bottom)...
        //
        // if (collection != null) {
        //     bean.setItemCollection(collection);
        // }
        JExpression collectionAssignment = collectionVar;
        if (propertyType.isArray()) {
            if (propertyType.getComponentType().isPrimitive()) {
                collectionAssignment = collectionVar.invoke("toArray");
            } else {
                JArray newArray = JExpr.newArray(context.toJClass(propertyType.getComponentType()), collectionVar.invoke("size"));
                collectionAssignment = collectionVar.invoke("toArray").arg(newArray);
            }
        }
        if (property.getField() != null) {
            Field field = property.getField();

            JBlock assignCollectionBlock = builder.getReadTailBlock()._if(collectionVar.ne(JExpr._null()))._then();

            if (!isPrivate(field)) {
                assignCollectionBlock.assign(beanVar.ref(field.getName()), collectionAssignment);
            } else {
                JFieldVar fieldAccessorField = builder.getPrivateFieldAccessor(field);
                assignCollectionBlock.add(fieldAccessorField.invoke("setObject").arg(builder.getXSR()).arg(builder.getReadContextVar()).arg(beanVar).arg(collectionAssignment));
            }
        } else {
            // if there is no setter method, the collection is not assigned into the class
            // this assumes that the getter returned the a collection instance and held on to a reference
            Method setter = property.getSetter();
            if (setter != null) {
                JBlock assignCollectionBlock = builder.getReadTailBlock()._if(collectionVar.ne(JExpr._null()))._then();
                if (!isPrivate(setter)) {

                    JTryBlock trySetter = assignCollectionBlock._try();
                    trySetter.body().add(beanVar.invoke(setter.getName()).arg(collectionAssignment));

                    JCatchBlock catchException = trySetter._catch(context.toJClass(Exception.class));
                    catchException.body().invoke(builder.getReadContextVar(), "setterError")
                            .arg(builder.getXSR())
                            .arg(context.dotclass(property.getBean().getType()))
                            .arg(setter.getName())
                            .arg(context.dotclass(setter.getParameterTypes()[0]))
                            .arg(catchException.param("e"));
                } else {
                    JFieldVar propertyAccessorField = builder.getPrivatePropertyAccessor(property.getGetter(), property.getSetter(), property.getName());
                    assignCollectionBlock.add(propertyAccessorField.invoke("setObject").arg(builder.getXSR()).arg(builder.getReadContextVar()).arg(beanVar).arg(collectionAssignment));
                }
            }
        }
        return collectionVar;
    }

    private void doSet(JAXBObjectBuilder builder, JBlock block, Property property, JVar beanVar, JExpression toSet, JVar collectionVar) {
        if (toSet == null) {
            return;
        }

        if (property.isId()) {
            JVar id;
            if (toSet instanceof JVar) {
                id = (JVar) toSet;
            } else {
                id = block.decl(context.toJClass(String.class), builder.getReadVariableManager().createId(property.getName()), toSet);
                toSet = id;
            }
            block.invoke(builder.getReadContextVar(), "addXmlId").arg(builder.getXSR()).arg(id).arg(beanVar);
        }

        if (!property.isCollection()) {
            setSingleValue(builder, block, property, beanVar, toSet);
        } else {
            addCollectionItem(builder, block, property, beanVar, toSet, collectionVar);
        }
    }

    private void setSingleValue(JAXBObjectBuilder builder, JBlock block, Property property, JVar bean, JExpression value) {
        // If enum returned null, then the enum value was invalid, but ValidationEventHandler
        // chose to continue processing.  Since the enum is a bad value we don't wan't to set
        // null into the bean
        if (toClass(property.getComponentType()).isEnum()) {
            block = block._if(value.ne(JExpr._null()))._then();
        }

        if (property.getField() != null) {
            Field field = property.getField();

            if (property.isIdref()) {
                JFieldVar fieldAccessorField = builder.getPrivateFieldAccessor(field);
                JExpression target = JExpr._new(context.toJClass(FieldRefTarget.class)).arg(builder.getXSR()).arg(builder.getReadContextVar()).arg(bean).arg(fieldAccessorField);
                block.add(builder.getReadContextVar().invoke("resolveXmlIdRef").arg(builder.getXSR()).arg(value).arg(target));
            } else if (!isPrivate(field)) {
                block.assign(bean.ref(field.getName()), value);
            } else {
                JFieldVar fieldAccessorField = builder.getPrivateFieldAccessor(field);

                String methodName;
                if (Boolean.TYPE.equals(field.getType())) {
                    methodName = "setBoolean";
                } else if (Byte.TYPE.equals(field.getType())) {
                    methodName = "setByte";
                } else if (Character.TYPE.equals(field.getType())) {
                    methodName = "setChar";
                } else if (Short.TYPE.equals(field.getType())) {
                    methodName = "setShort";
                } else if (Integer.TYPE.equals(field.getType())) {
                    methodName = "setInt";
                } else if (Long.TYPE.equals(field.getType())) {
                    methodName = "setLong";
                } else if (Float.TYPE.equals(field.getType())) {
                    methodName = "setFloat";
                } else if (Double.TYPE.equals(field.getType())) {
                    methodName = "setDouble";
                } else {
                    methodName = "setObject";
                }
                block.add(fieldAccessorField.invoke(methodName).arg(builder.getXSR()).arg(builder.getReadContextVar()).arg(bean).arg(value));
            }
        } else if (property.getSetter() != null) {
            Method setter = property.getSetter();
            if (property.isIdref()) {
                JFieldVar propertyAccessorField = builder.getPrivatePropertyAccessor(property.getGetter(), property.getSetter(), property.getName());
                JExpression target = JExpr._new(context.toJClass(FieldRefTarget.class)).arg(builder.getXSR()).arg(builder.getReadContextVar()).arg(bean).arg(propertyAccessorField);
                block.add(builder.getReadContextVar().invoke("resolveXmlIdRef").arg(builder.getXSR()).arg(value).arg(target));
            } else if (!isPrivate(setter)) {
                JTryBlock trySetter = block._try();
                trySetter.body().add(bean.invoke(property.getSetter().getName()).arg(value));

                JCatchBlock catchException = trySetter._catch(context.toJClass(Exception.class));
                catchException.body().invoke(builder.getReadContextVar(), "setterError")
                        .arg(builder.getXSR())
                        .arg(context.dotclass(property.getBean().getType()))
                        .arg(setter.getName())
                        .arg(context.dotclass(setter.getParameterTypes()[0]))
                        .arg(catchException.param("e"));
            } else {
                JFieldVar propertyAccessorField = builder.getPrivatePropertyAccessor(property.getGetter(), property.getSetter(), property.getName());
                block.add(propertyAccessorField.invoke("setObject").arg(builder.getXSR()).arg(builder.getReadContextVar()).arg(bean).arg(value));
            }
        } else {
            throw new BuildException("Property does not have a setter: " + property.getBean().getType().getName() + "." + property.getName());
        }
    }

    private void addCollectionItem(JAXBObjectBuilder builder, JBlock block, Property property, JVar beanVar, JExpression toSet, JVar collectionVar) {
        // if (collection == null) {
        JBlock createCollectionBlock = block._if(collectionVar.eq(JExpr._null()))._then();

        //     collection = (Collection) bean.getItemCollection();
        Class propertyType = toClass(property.getType());
        if (propertyType.isArray()) {
            createCollectionBlock.assign(collectionVar, JExpr._new(collectionVar.type()));
        } else if (property.getField() == null && property.getGetter() == null) {
            // write only collections are not allowed by the spec, but we can support it anyway
            JType collectionType = getCollectionClass(property.getType(), property.getComponentType());
            if (collectionType == null) {
                throw new BuildException("Collection property does not have a getter and collection does not have a default constructor: " +
                        property.getBean().getType().getName() + "." + property.getName());
            }
            createCollectionBlock.assign(collectionVar, JExpr._new(collectionType));
        } else {
            if (property.getField() != null) {
                Field field = property.getField();

                if (!isPrivate(field)) {
                    createCollectionBlock.assign(collectionVar, beanVar.ref(field.getName()));
                } else {
                    JFieldVar fieldAccessorField = builder.getPrivateFieldAccessor(field);
                    createCollectionBlock.assign(collectionVar, fieldAccessorField.invoke("getObject").arg(builder.getXSR()).arg(builder.getReadContextVar()).arg(beanVar));
                }
            } else {
                Method getter = property.getGetter() ;
                if (!isPrivate(getter)) {
                    JTryBlock tryGetter = createCollectionBlock._try();
                    tryGetter.body().assign(collectionVar, beanVar.invoke(getter.getName()));

                    JCatchBlock catchException = tryGetter._catch(context.toJClass(Exception.class));
                    catchException.body().invoke(builder.getReadContextVar(), "getterError")
                            .arg(builder.getXSR())
                            .arg(context.dotclass(property.getBean().getType()))
                            .arg(getter.getName())
                            .arg(catchException.param("e"));
                    catchException.body()._continue();
                } else {
                    JFieldVar propertyAccessorField = builder.getPrivatePropertyAccessor(property.getGetter(), property.getSetter(), property.getName());
                    createCollectionBlock.assign(collectionVar, propertyAccessorField.invoke("getObject").arg(builder.getXSR()).arg(builder.getReadContextVar()).arg(beanVar));
                }
            }

            //     if (collection != null) {
            //         collection.clear();
            JConditional arrayFoundCondition = createCollectionBlock._if(collectionVar.ne(JExpr._null()));
            arrayFoundCondition._then().invoke(collectionVar, "clear");

            //     } else {
            //         collection = new ArrayList();
            JType collectionType = getCollectionClass(property.getType(), property.getComponentType());
            if (collectionType != null) {
                arrayFoundCondition._else().assign(collectionVar, JExpr._new(collectionType));
            } else {
                arrayFoundCondition._else().invoke(builder.getReadContextVar(), "uncreatableCollection")
                        .arg(builder.getXSR())
                        .arg(context.dotclass(property.getBean().getType()))
                        .arg(property.getName())
                        .arg(context.dotclass(property.getType()));
                arrayFoundCondition._else()._continue();
            }
        }
        //     }
        // }

        // collection.add(item);
        if (property.isIdref()) {
            JExpression target = JExpr._new(context.toJClass(CollectionRefTarget.class)).arg(collectionVar);
            block.add(builder.getReadContextVar().invoke("resolveXmlIdRef").arg(builder.getXSR()).arg(toSet).arg(target));
        } else {
            block.add(collectionVar.invoke("add").arg(toSet));
        }
    }

    private JType getCollectionClass(Type collectionType, Type itemType) {
        Class collectionClass = toClass(collectionType);
        if (!collectionClass.isInterface()) {
            try {
                collectionClass.getConstructor();
                return context.getGenericType(collectionType);
            } catch (NoSuchMethodException e) {
            }
        } else if (SortedSet.class.equals(collectionClass)) {
            return context.toJClass(TreeSet.class).narrow(context.getGenericType(itemType));
        } else if (Set.class.equals(collectionClass)) {
            return context.toJClass(LinkedHashSet.class).narrow(context.getGenericType(itemType));
        } else if (Queue.class.equals(collectionClass)) {
            return context.toJClass(LinkedList.class).narrow(context.getGenericType(itemType));
        } else if (List.class.equals(collectionClass)) {
            return context.toJClass(ArrayList.class).narrow(context.getGenericType(itemType));
        } else if (Collection.class.equals(collectionClass)) {
            return context.toJClass(ArrayList.class).narrow(context.getGenericType(itemType));
        }
        return null;
    }

    private JType getMapClass(Type mapType, Type itemType) {
        Class mapClass = toClass(mapType);
        if (!mapClass.isInterface()) {
            try {
                mapClass.getConstructor();
                return context.getGenericType(mapType);
            } catch (NoSuchMethodException e) {
            }
        } else if (Map.class.equals(mapClass)) {
            return context.toJClass(LinkedHashMap.class).narrow(context.getGenericType(QName.class), context.getGenericType(itemType));
        }
        return null;
    }

    private JInvocation newJaxBElement(JVar xsrVar, Class type, JExpression expression) {
        if (JAXBElement.class.equals(type)) {
            throw new IllegalArgumentException("Can't wrap a JAXBElement with a JAXBElement");
        }
        JType jaxbElementType = context.toJClass(JAXBElement.class).narrow(type);
        JInvocation newJaxBElement = JExpr._new(jaxbElementType)
                .arg(xsrVar.invoke("getName"))
                .arg(JExpr.dotclass(context.toJClass(type)))
                .arg(expression);
        return newJaxBElement;
    }

    private JVar as(JAXBObjectBuilder builder, JExpression attributeVar, JBlock block, Class<?> cls, String name) {
        return block.decl(context.toJType(cls), name, coerce(builder, builder.getXSR(), attributeVar, cls));
    }

    private JVar as(JAXBObjectBuilder builder, JVar xsrVar, JBlock block, Class<?> cls, String name, boolean nillable) {
        JExpression value = coerce(builder, xsrVar, xsrVar.invoke("getElementAsString"), cls);

        JVar var;
        if (!cls.isPrimitive() && nillable) {
            var = block.decl(context.toJType(cls), name, JExpr._null());
            JConditional cond = block._if(xsrVar.invoke("isXsiNil").not());

            cond._then().assign(var, value);
        } else {
            var = block.decl(context.toJType(cls), name, value);
        }

        return var;
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

    private JExpression coerce(JAXBObjectBuilder builder, JVar xsrVar, JExpression stringValue, Class<?> destType) {
        if (destType.isPrimitive()) {
            if (destType.equals(boolean.class)) {
                return JExpr.lit("1").invoke("equals").arg(stringValue).cor(JExpr.lit("true").invoke("equals").arg(stringValue));
            } else if (destType.equals(byte.class)) {
                return context.toJClass(Byte.class).staticInvoke("parseByte").arg(stringValue);
            } else if (destType.equals(short.class)) {
                return context.toJClass(Short.class).staticInvoke("parseShort").arg(stringValue);
            } else if (destType.equals(int.class)) {
                return context.toJClass(Integer.class).staticInvoke("parseInt").arg(stringValue);
            } else if (destType.equals(long.class)) {
                return context.toJClass(Long.class).staticInvoke("parseLong").arg(stringValue);
            } else if (destType.equals(float.class)) {
                return context.toJClass(Float.class).staticInvoke("parseFloat").arg(stringValue);
            } else if (destType.equals(double.class)) {
                return context.toJClass(Double.class).staticInvoke("parseDouble").arg(stringValue);
            }
        } else {
            if (destType.equals(String.class)) {
                return stringValue;
            } else if (destType.equals(Boolean.class)) {
                return JExpr.lit("1").invoke("equals").arg(stringValue).cor(JExpr.lit("true").invoke("equals").arg(stringValue));
            } else if (destType.equals(Byte.class)) {
                return context.toJClass(Byte.class).staticInvoke("valueOf").arg(stringValue);
            } else if (destType.equals(Short.class)) {
                return context.toJClass(Short.class).staticInvoke("valueOf").arg(stringValue);
            } else if (destType.equals(Integer.class)) {
                return context.toJClass(Integer.class).staticInvoke("valueOf").arg(stringValue);
            } else if (destType.equals(Long.class)) {
                return context.toJClass(Long.class).staticInvoke("valueOf").arg(stringValue);
            } else if (destType.equals(Float.class)) {
                return context.toJClass(Float.class).staticInvoke("valueOf").arg(stringValue);
            } else if (destType.equals(Double.class)) {
                return context.toJClass(Double.class).staticInvoke("valueOf").arg(stringValue);
            } else if (destType.equals(XMLGregorianCalendar.class)) {
                return builder.getDatatypeFactory().invoke("newXMLGregorianCalendar").arg(stringValue);
            } else if (destType.equals(Duration.class)) {
                return builder.getDatatypeFactory().invoke("newDuration").arg(stringValue);
            } else if (destType.equals(BigDecimal.class)) {
                return JExpr._new(context.toJClass(BigDecimal.class)).arg(stringValue);
            } else if (destType.equals(BigInteger.class)) {
                return JExpr._new(context.toJClass(BigInteger.class)).arg(stringValue);
            } else if (destType.isEnum()) {
                JAXBEnumBuilder enumBuilder = enumBuilders.get(destType);
                if (enumBuilder == null) {
                    throw new BuildException("Unknown enum type " + destType);
                }
                return invokeEnumParser(builder, xsrVar, enumBuilder, stringValue);
            }
        }
        throw new UnsupportedOperationException("Invalid type " + destType);
    }
}
