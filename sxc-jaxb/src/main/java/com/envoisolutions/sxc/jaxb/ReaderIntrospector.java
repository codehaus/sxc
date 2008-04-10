package com.envoisolutions.sxc.jaxb;

import java.awt.Image;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
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
import javax.xml.stream.XMLStreamException;

import com.envoisolutions.sxc.builder.BuildException;
import com.envoisolutions.sxc.builder.impl.JIfElseBlock;
import com.envoisolutions.sxc.builder.impl.JStaticImports;
import static com.envoisolutions.sxc.jaxb.JavaUtils.isPrivate;
import static com.envoisolutions.sxc.jaxb.JavaUtils.toClass;
import com.envoisolutions.sxc.jaxb.model.Bean;
import com.envoisolutions.sxc.jaxb.model.ElementMapping;
import com.envoisolutions.sxc.jaxb.model.Model;
import com.envoisolutions.sxc.jaxb.model.Property;
import com.envoisolutions.sxc.util.ArrayUtil;
import com.envoisolutions.sxc.util.Base64;
import com.sun.codemodel.JArray;
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
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JTryBlock;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;

public class ReaderIntrospector {
    private static final Logger logger = Logger.getLogger(ReaderIntrospector.class.getName());

    private final JCodeModel codeModel;
    private final Map<Bean, MarshallerBuilder> builders = new LinkedHashMap<Bean, MarshallerBuilder>();
    private final Map<Class, JClass> enumParsers = new LinkedHashMap<Class, JClass>();

    private final Model model;

    public ReaderIntrospector(BuilderContext builderContext, Model model) {
        this.model = model;
        codeModel = builderContext.getCodeModel();

        // declare all parser methods first, so everything exists when we build the
        for (Bean bean : this.model.getBeans()) {
            if (Modifier.isAbstract(bean.getType().getModifiers())) continue;
            if (bean.getType().isEnum()) continue;

            MarshallerBuilder builder = builderContext.getMarshallerBuilder(bean.getType(), bean.getRootElementName(), bean.getSchemaTypeName());

            LinkedHashSet<Property> allProperties = new LinkedHashSet<Property>();
            for (Bean b = bean; b != null; b = b.getBaseClass()) {
                allProperties.addAll(b.getProperties());
            }

            // declare all private field accessors (so they are grouped)
            for (Property property : allProperties) {
                Field field = property.getField();
                if (field != null && isPrivate(field)) {
                    builder.getPrivateFieldAccessor(property.getField());
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

        // build parsers
        for (Bean bean : model.getBeans()) {
            if (!bean.getType().isEnum()) {
                MarshallerBuilder builder = builders.get(bean);
                if (builder != null) {
                    add(builder, bean);
                }
            }
        }
    }

    private JInvocation invokeParser(MarshallerBuilder caller, MarshallerBuilder parser) {
        // Declare dependency from caller to parser
        caller.addDependency(parser.getMarshallerClass());

        // Add a static import for the write method on the existing builder class
        JStaticImports staticImports = JStaticImports.getStaticImports(caller.getMarshallerClass());

        // Call the static method
        String methodName = "read" + parser.getReadMethod().type().name();
        staticImports.addStaticImport(parser.getMarshallerClass().fullName() + "." + methodName);

        JInvocation invocation = JExpr.invoke(methodName).arg(caller.getXSR()).arg(caller.getReadContextVar());
        return invocation;
    }

    private MarshallerBuilder add(MarshallerBuilder builder, Bean bean) {
        // read properties
        handleProperties(builder, bean, builder.getReadObject());

        // This element may be replaced with another type using the xsi:type override
        // Add xsi type support for an other Bean that can be assigned to this property according to Java type assignment rules
        for (Bean xsiTypeBean : model.getBeans()) {
            Class xsiTypeClass = xsiTypeBean.getType();
            if (bean.getType().isAssignableFrom(xsiTypeClass) && bean.getType() != xsiTypeClass && !Modifier.isAbstract(xsiTypeClass.getModifiers())) {
                JBlock block = builder.expectXsiType(xsiTypeBean.getSchemaTypeName());

                MarshallerBuilder elementBuilder = builders.get(xsiTypeBean);
                if (elementBuilder == null) {
                    throw new BuildException("Unknown bean " + bean);
                }

                // invoke the reader method
                JInvocation method = invokeParser(builder, elementBuilder);
                block._return(method);
            }
        }

        return builder;
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
        JMethod method = jaxbClass.method(JMod.PUBLIC | JMod.STATIC, type, "parse");
        JVar value = method.param(String.class, "value");

        JIfElseBlock enumCond = new JIfElseBlock();
        method.body().add(enumCond);
        for (Map.Entry<Enum, String> entry : bean.getEnumMap().entrySet()) {
            Enum enumValue = entry.getKey();
            String enumText = entry.getValue();

            JExpression textCompare = JExpr.lit(enumText).invoke("equals").arg(value);
            JBlock block = enumCond.addCondition(textCompare);
            block._return(type.staticRef(enumValue.name()));
        }

        // java.lang.IllegalArgumentException: No enum const class com.envoisolutions.sxc.jaxb.enums.AnnotatedEnum.ssssss
        // todo inaccurate message... enums mapped
        enumCond._else()._throw(JExpr._new(toJClass(IllegalArgumentException.class))
                .arg(JExpr.lit("No enum const " + type + ".").plus(value)));

        enumParsers.put(bean.getType(), jaxbClass);
    }

    private void handleProperties(MarshallerBuilder builder, Bean bean, JVar beanVar) {
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
                    // create attribute block
                    JBlock block = builder.expectAttribute(property.getXmlName());

                    // create collection var if necessary
                    JVar collectionVar = handleCollection(builder, property, beanVar);

                    // read and set
                    JExpression toSet = handleAttribute(builder, block, property);
                    doSet(builder, block, property, beanVar, toSet, collectionVar);
                }
                break;

                case ELEMENT:
                case ELEMENT_REF: {
                    MarshallerBuilder elementBuilder = builder;
                    JVar parentVar = beanVar;
                    if (property.getXmlName() != null) {
                        elementBuilder = builder.expectWrapperElement(property.getXmlName(), beanVar, property.getName());
                    }
                    
                    // create collection var if necessary
                    JVar collectionVar = handleCollection(elementBuilder, property, parentVar);

                    for (ElementMapping mapping : property.getElementMappings()) {
                        // create element block
                        JBlock block = elementBuilder.expectElement(mapping.getXmlName());

                        // read and set
                        JExpression toSet = handleElement(builder, block, property, mapping.isNillable(), mapping.getComponentType());
                        doSet(builder, block, property, parentVar, toSet, collectionVar);
                    }

                }
                break;

                case VALUE: {
                    // value is read in class block
                    JBlock block = builder.expectValue();

                    // create collection var if necessary
                    JVar collectionVar = handleCollection(builder, property, beanVar);

                    // read and set
                    JExpression toSet = handleElement(builder, block, property, false, null);
                    doSet(builder, block, property, beanVar, toSet, collectionVar);
                }
                break;
            }
        }

        // handle properties of the base class
        if (bean.getBaseClass() != null) {
            handleProperties(builder, bean.getBaseClass(), beanVar);
        }
    }

    private JExpression handleAttribute(MarshallerBuilder builder, JBlock block, Property property) {
        // Collections are not supported yet
        if (property.isCollection()) {
            logger.info("Reader: attribute lists are not supported yet!");
            return null;
        }

        // this variable is automatically defined in the attribute read block in ElementParserBuilderImpl
        JExpression valueVar = JExpr.direct("attValue");

        JExpression toSet;
        if (property.getAdapterType() != null) {
            JVar adapterVar = builder.getAdapter(property.getAdapterType());
            toSet = adapterVar.invoke("unmarshal").arg(valueVar);
        } else {
            String propertyName = property.getName();
            if (property.isCollection()) propertyName += "Item";
            propertyName = builder.getReadVariableManager().createId(propertyName);

            Class clazz = toClass(property.getType());
            if (isBuiltinType(clazz)) {
                toSet = block.decl(toJClass(clazz), propertyName, coerce(builder, valueVar, clazz));
            } else if (clazz.equals(QName.class)) {
                JVar var = as(builder, valueVar, block, String.class, propertyName);
                toSet = builder.getXSR().invoke("getAsQName").arg(var);
            } else if (clazz.equals(byte[].class)) {
                toSet = toJClass(Base64.class).staticInvoke("decode").arg(valueVar);
            } else {
                logger.severe("Could not map attribute " + propertyName + " of type " + clazz);
                toSet = JExpr._null();
            }
        }
        return toSet;
    }

    private JExpression handleElement(MarshallerBuilder builder, JBlock block, Property property, boolean nillable, Type componentType) {

        Class targetType = toClass(property.getType());
        if (property.isCollection()) {
            targetType = toClass(componentType);
        }

        String propertyName = property.getName();
        if (property.isCollection()) propertyName += "Item";
        propertyName = builder.getReadVariableManager().createId(propertyName);

        JExpression toSet;
        if (property.getAdapterType() != null) {
            JVar adapterVar = builder.getAdapter(property.getAdapterType());

            JVar valueVar = block.decl(toJClass(targetType), propertyName);
            JTryBlock tryBlock = block._try();
            tryBlock.body().assign(valueVar, adapterVar.invoke("unmarshal").arg(builder.getXSR().invoke("getElementText")));
            JCatchBlock catchBlock = tryBlock._catch(toJClass(Exception.class));
            catchBlock.body()._throw(JExpr._new(toJClass(XMLStreamException.class)).arg(catchBlock.param("e")));

            toSet = valueVar;
        } else if (targetType.equals(Byte.class) || targetType.equals(byte.class)) {
            // todo why the special read method for byte?
            toSet = JExpr.cast(toJType(byte.class), builder.getXSR().invoke("getElementAsInt"));
        } else if (isBuiltinType(targetType)) {
            toSet = as(builder, block, targetType, propertyName, nillable);
        } else if (targetType.equals(byte[].class)) {
            toSet = toJClass(BinaryUtils.class).staticInvoke("decodeAsBytes").arg(builder.getXSR());
        } else if (targetType.equals(QName.class)) {
            toSet = builder.getXSR().invoke("getElementAsQName");
        } else if (targetType.equals(DataHandler.class) || targetType.equals(Image.class)) {
            // attac
            toSet = JExpr._null();
        } else {
            // Complex type which will already have an element builder defined
            Bean targetBean = model.getBean(toClass(componentType));
            MarshallerBuilder elementBuilder = builders.get(targetBean);
            if (elementBuilder == null) {
                throw new BuildException("Unknown bean " + toClass(componentType));
            }

            // invoke the reader method
            JInvocation invocation = invokeParser(builder, elementBuilder);
            toSet = invocation;
        }

        // JaxB refs need to be wrapped with a JAXBElement
        if (Property.XmlStyle.ELEMENT_REF.equals(property.getXmlStyle())) {
            toSet = newJaxBElement(builder, targetType, toSet);
        }

        return toSet;
    }

    private JVar handleCollection(MarshallerBuilder builder, Property property, JVar beanVar) {
        if (!property.isCollection()) {
            return null;
        }

        Class propertyType = toClass(property.getType());

        JType collectionType;
        if (propertyType.isArray()) {
            Class componentType = propertyType.getComponentType();
            if (Boolean.TYPE.equals(componentType)) {
                collectionType = toJClass(ArrayUtil.BooleanArray.class);
            } else if (Character.TYPE.equals(componentType)) {
                collectionType = toJClass(ArrayUtil.CharArray.class);
            } else if (Short.TYPE.equals(componentType)) {
                collectionType = toJClass(ArrayUtil.ShortArray.class);
            } else if (Integer.TYPE.equals(componentType)) {
                collectionType = toJClass(ArrayUtil.IntArray.class);
            } else if (Long.TYPE.equals(componentType)) {
                collectionType = toJClass(ArrayUtil.LongArray.class);
            } else if (Float.TYPE.equals(componentType)) {
                collectionType = toJClass(ArrayUtil.FloatArray.class);
            } else if (Double.TYPE.equals(componentType)) {
                collectionType = toJClass(ArrayUtil.DoubleArray.class);
            } else {
                collectionType = toJClass(ArrayList.class).narrow(componentType);
            }
        } else {
            collectionType = getGenericType(property.getType());
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
                JArray newArray = JExpr.newArray(toJClass(propertyType.getComponentType()), collectionVar.invoke("size"));
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
                assignCollectionBlock.add(fieldAccessorField.invoke("setObject").arg(beanVar).arg(collectionAssignment));
            }
        } else {
            // todo private getter support
            Method setter = property.getSetter();
            if (setter != null) {
                // if there is no setter method, the collection is not assigned into the class
                // this assumes that the getter returned the a collection instance and held on to a reference
                JBlock assignCollectionBlock = builder.getReadTailBlock()._if(collectionVar.ne(JExpr._null()))._then();
                assignCollectionBlock.add(beanVar.invoke(setter.getName()).arg(collectionAssignment));
            }
        }
        return collectionVar;
    }

    private void doSet(MarshallerBuilder builder, JBlock block, Property property, JVar beanVar, JExpression toSet, JVar collectionVar) {
        if (toSet == null) {
            return;
        }

        if (!property.isCollection()) {
            setSingleValue(builder, block, property, beanVar, toSet);
        } else {
            addCollectionItem(builder, block, property, beanVar, toSet, collectionVar);
        }
    }

    private void setSingleValue(MarshallerBuilder builder, JBlock block, Property property, JVar bean, JExpression value) {
        if (property.getField() != null) {
            Field field = property.getField();

            if (!isPrivate(field)) {
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
                block.add(fieldAccessorField.invoke(methodName).arg(bean).arg(value));
            }
        } else if (property.getSetter() != null) {
            // todo private getter support
            block.add(bean.invoke(property.getSetter().getName()).arg(value));
        } else {
            throw new BuildException("Property does not have a setter: " + property.getBean().getType().getName() + "." + property.getName());
        }
    }

    private void addCollectionItem(MarshallerBuilder builder, JBlock block, Property property, JVar beanVar, JExpression toSet, JVar collectionVar) {
        // if (collection == null) {
        JBlock createCollectionBlock = block._if(collectionVar.eq(JExpr._null()))._then();

        //     collection = (Collection) bean.getItemCollection();
        Class propertyType = toClass(property.getType());
        if (!propertyType.isArray()) {
            if (property.getField() != null) {
                Field field = property.getField();

                if (!isPrivate(field)) {
                    createCollectionBlock.assign(collectionVar, beanVar.ref(field.getName()));
                } else {
                    JFieldVar fieldAccessorField = builder.getPrivateFieldAccessor(field);
                    createCollectionBlock.assign(collectionVar, fieldAccessorField.invoke("getObject").arg(beanVar));
                }
            } else if (property.getGetter() != null) {
                // todo private getter support
                Method getter = property.getGetter() ;
                createCollectionBlock.assign(collectionVar, beanVar.invoke(getter.getName()));
            } else {
                throw new BuildException("Property does not have a getter: " + property.getBean().getType().getName() + "." + property.getName());
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
                arrayFoundCondition._else()._throw(JExpr._new(toJClass(NullPointerException.class))
                        .arg("Collection " + property.getName() + " in class " + property.getBean().getType().getName() +
                        " is null and a new instance of " + propertyType.getName() + " can not be created"));
            }
        } else {
            createCollectionBlock.assign(collectionVar, JExpr._new(collectionVar.type()));
        }
        //     }
        // }

        // collection.add(item);
        block.add(collectionVar.invoke("add").arg(toSet));
    }

    private JType getCollectionClass(Type collectionType, Type itemType) {
        Class collectionClass = toClass(collectionType);
        if (!collectionClass.isInterface()) {
            try {
                collectionClass.getConstructor();
                return getGenericType(collectionType);
            } catch (NoSuchMethodException e) {
            }
        } else if (SortedSet.class.equals(collectionClass)) {
            return toJClass(TreeSet.class).narrow(getGenericType(itemType));
        } else if (Set.class.equals(collectionClass)) {
            return toJClass(LinkedHashSet.class).narrow(getGenericType(itemType));
        } else if (Queue.class.equals(collectionClass)) {
            return toJClass(LinkedList.class).narrow(getGenericType(itemType));
        } else if (List.class.equals(collectionClass)) {
            return toJClass(ArrayList.class).narrow(getGenericType(itemType));
        } else if (Collection.class.equals(collectionClass)) {
            return toJClass(ArrayList.class).narrow(getGenericType(itemType));
        }
        return null;
    }

    private JInvocation newJaxBElement(MarshallerBuilder builder, Class type, JExpression expression) {
        JType jaxbElementType = toJClass(JAXBElement.class).narrow(type);
        JInvocation newJaxBElement = JExpr._new(jaxbElementType)
                .arg(builder.getXSR().invoke("getName"))
                .arg(JExpr.dotclass(toJClass(type)))
                .arg(expression);
        return newJaxBElement;
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
        } else if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
            Type[] upperBounds = wildcardType.getUpperBounds();
            if (upperBounds.length > 0) {
                return getGenericType(upperBounds[0]);
            }
            return toJClass(Object.class);
        }
        throw new IllegalStateException();
    }

    private Class toPrimitiveWrapper(Class type) {
        if (type.equals(boolean.class)) {
            return Boolean.class;
        } else if (type.equals(byte.class)) {
            return Byte.class;
        } else if (type.equals(char.class)) {
            return Character.class;
        } else if (type.equals(short.class)) {
            return Short.class;
        } else if (type.equals(int.class)) {
            return Integer.class;
        } else if (type.equals(long.class)) {
            return Long.class;
        } else if (type.equals(float.class)) {
            return Float.class;
        } else if (type.equals(double.class)) {
            return Double.class;
        }
        return type;
    }

    private JVar as(MarshallerBuilder builder, JExpression attributeVar, JBlock block, Class<?> cls, String name) {
        return block.decl(toJType(cls), name, coerce(builder, attributeVar, cls));
    }

    private JVar as(MarshallerBuilder builder, JBlock block, Class<?> cls, String name, boolean nillable) {
        JVar xsrVar = builder.getXSR();
        JExpression value = coerce(builder, xsrVar.invoke("getElementAsString"), cls);

        JVar var;
        if (!cls.isPrimitive() && nillable) {
            var = block.decl(toJType(cls), name, JExpr._null());
            JConditional cond = block._if(xsrVar.invoke("isXsiNil").not());

            cond._then().assign(var, value);
        } else {
            var = block.decl(toJType(cls), name, value);
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

    private JExpression coerce(MarshallerBuilder builder, JExpression stringValue, Class<?> destType) {
        if (destType.isPrimitive()) {
            if (destType.equals(boolean.class)) {
                return JExpr.lit("1").invoke("equals").arg(stringValue).cor(JExpr.lit("true").invoke("equals").arg(stringValue));
            } else if (destType.equals(byte.class)) {
                return toJClass(Byte.class).staticInvoke("parseByte").arg(stringValue);
            } else if (destType.equals(short.class)) {
                return toJClass(Short.class).staticInvoke("parseShort").arg(stringValue);
            } else if (destType.equals(int.class)) {
                return toJClass(Integer.class).staticInvoke("parseInt").arg(stringValue);
            } else if (destType.equals(long.class)) {
                return toJClass(Long.class).staticInvoke("parseLong").arg(stringValue);
            } else if (destType.equals(float.class)) {
                return toJClass(Float.class).staticInvoke("parseFloat").arg(stringValue);
            } else if (destType.equals(double.class)) {
                return toJClass(Double.class).staticInvoke("parseDouble").arg(stringValue);
            }
        } else {
            if (destType.equals(String.class)) {
                return stringValue;
            } else if (destType.equals(Boolean.class)) {
                return JExpr.lit("1").invoke("equals").arg(stringValue).cor(JExpr.lit("true").invoke("equals").arg(stringValue));
            } else if (destType.equals(Byte.class)) {
                return toJClass(Byte.class).staticInvoke("valueOf").arg(stringValue);
            } else if (destType.equals(Short.class)) {
                return toJClass(Short.class).staticInvoke("valueOf").arg(stringValue);
            } else if (destType.equals(Integer.class)) {
                return toJClass(Integer.class).staticInvoke("valueOf").arg(stringValue);
            } else if (destType.equals(Long.class)) {
                return toJClass(Long.class).staticInvoke("valueOf").arg(stringValue);
            } else if (destType.equals(Float.class)) {
                return toJClass(Float.class).staticInvoke("valueOf").arg(stringValue);
            } else if (destType.equals(Double.class)) {
                return toJClass(Double.class).staticInvoke("valueOf").arg(stringValue);
            } else if (destType.equals(XMLGregorianCalendar.class)) {
                return builder.getDatatypeFactory().invoke("newXMLGregorianCalendar").arg(stringValue);
            } else if (destType.equals(Duration.class)) {
                return builder.getDatatypeFactory().invoke("newDuration").arg(stringValue);
            } else if (destType.equals(BigDecimal.class)) {
                return JExpr._new(toJClass(BigDecimal.class)).arg(stringValue);
            } else if (destType.equals(BigInteger.class)) {
                return JExpr._new(toJClass(BigInteger.class)).arg(stringValue);
            } else if (destType.isEnum()) {
                JClass parserClass = enumParsers.get(destType);
                if (parserClass == null) {
                    throw new BuildException("Unknown enum type " + destType);
                }
                return parserClass.staticInvoke("parse").arg(stringValue);
            }
        }
        throw new UnsupportedOperationException("Invalid type " + destType);
    }
}
