package com.envoisolutions.sxc.jaxb;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.envoisolutions.sxc.builder.BuildException;
import com.envoisolutions.sxc.builder.Builder;
import com.envoisolutions.sxc.builder.CodeBody;
import com.envoisolutions.sxc.builder.ElementParserBuilder;
import com.envoisolutions.sxc.builder.ParserBuilder;
import com.envoisolutions.sxc.util.Base64;
import com.envoisolutions.sxc.util.FieldAccessor;
import com.envoisolutions.sxc.util.ArrayUtil;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JTryBlock;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JArray;
import com.sun.xml.bind.v2.model.core.Adapter;
import com.sun.xml.bind.v2.model.core.ElementInfo;
import com.sun.xml.bind.v2.model.runtime.RuntimeAttributePropertyInfo;
import com.sun.xml.bind.v2.model.runtime.RuntimeClassInfo;
import com.sun.xml.bind.v2.model.runtime.RuntimeElement;
import com.sun.xml.bind.v2.model.runtime.RuntimeElementInfo;
import com.sun.xml.bind.v2.model.runtime.RuntimeElementPropertyInfo;
import com.sun.xml.bind.v2.model.runtime.RuntimeEnumLeafInfo;
import com.sun.xml.bind.v2.model.runtime.RuntimeNonElement;
import com.sun.xml.bind.v2.model.runtime.RuntimePropertyInfo;
import com.sun.xml.bind.v2.model.runtime.RuntimeReferencePropertyInfo;
import com.sun.xml.bind.v2.model.runtime.RuntimeTypeInfoSet;
import com.sun.xml.bind.v2.model.runtime.RuntimeTypeRef;
import com.sun.xml.bind.v2.model.runtime.RuntimeValuePropertyInfo;
import com.sun.xml.bind.v2.runtime.Transducer;
import com.sun.xml.bind.v2.runtime.reflect.Accessor;
import com.sun.xml.bind.api.AccessorException;

public class ReaderIntrospector {
    private static final Logger logger = Logger.getLogger(ReaderIntrospector.class.getName());
	
    private ElementParserBuilder rootReader;
    private JType type_dtFactory;
    private Set<Class<?>> addedXsiTypes = new HashSet<Class<?>>();
    private Builder builder;
    private JFieldVar dtf;
    private JCodeModel model;
    private Map<BuilderKey, ElementParserBuilder> type2Parser 
        = new HashMap<BuilderKey, ElementParserBuilder>();
    private Map<RuntimeElementInfo, ElementParserBuilder> ref2Parser 
        = new HashMap<RuntimeElementInfo, ElementParserBuilder>();
    private RuntimeTypeInfoSet set;
    private Map<Class, JVar> adapters = new HashMap<Class, JVar>();
    private int adapterCount = 0;

    private Map<String, JFieldVar> privateFieldAccessors = new TreeMap<String, JFieldVar>();

    public ReaderIntrospector(Builder builder2, RuntimeTypeInfoSet set) {
        this.builder = builder2;
        rootReader = this.builder.getParserBuilder();
        this.model = rootReader.getCodeModel();
        this.set = set;
        
        type_dtFactory = model._ref(DatatypeFactory.class);
        
        JDefinedClass readerCls = rootReader.getReaderClass();
        // static DatatypeFactory dtFactory;
        dtf = readerCls.field(JMod.STATIC, type_dtFactory, "dtFactory");

        // add code to constructor which initializes the static dtFactory field
        // todo why do this in the constructor instead of a static block
        JMethod constructor = builder.getParserConstructor();
        JTryBlock tb = constructor.body()._try();
        JBlock body = tb.body();
        body.assign(dtf, JExpr.direct(DatatypeFactory.class.getName()+".newInstance()"));
        tb._catch((JClass) model._ref(DatatypeConfigurationException.class));

        Map<QName, ? extends ElementInfo<Type, Class>> elementMappings = set.getElementMappings(null);
        for (Map.Entry<QName, ? extends ElementInfo<Type, Class>> e : elementMappings.entrySet()) {
            QName q = e.getKey();
            RuntimeElementInfo rei = (RuntimeElementInfo) e.getValue();
            
            if (!ref2Parser.containsKey(rei)) {
                createRefParser(rootReader, q, rei);
            } 
        }        
        
        Map<Class, ? extends RuntimeClassInfo> beans = set.beans();
        for (Map.Entry<Class, ? extends RuntimeClassInfo> e : beans.entrySet()) {
            Class cls = e.getKey();
            if (!Modifier.isAbstract(cls.getModifiers())) {
                add(rootReader, cls, e.getValue());
            }
        }
        
        for (Map.Entry<Class, ? extends RuntimeEnumLeafInfo> e : set.enums().entrySet()) {
            Class cls = e.getKey();
            RuntimeEnumLeafInfo info = e.getValue();
            
            ElementParserBuilder enumBuilder;
            if (info.isElement()) {
                enumBuilder = rootReader.expectElement(info.getElementName());
            } else {
                enumBuilder = rootReader.expectXsiType(info.getTypeName());
            }
            JExpression toSet = handleElement(enumBuilder, info, null, cls, true);
            
            CodeBody cb = enumBuilder.getBody();
            
            JType type = model._ref(cls);
            JType jaxbElementType = model._ref(JAXBElement.class);
            JType qnameType = model._ref(QName.class);
            JVar qname = cb.decl(qnameType, "_xname", JExpr.direct("reader").invoke("getName"));
            
            enumBuilder.getBody()._return(jaxbElementType,
                                          JExpr._new(jaxbElementType)
                                          .arg(qname).arg(JExpr.dotclass((JClass) type))
                                          .arg(toSet));
        }
    }


    private void createRefParser(ElementParserBuilder builder, QName q, RuntimeElementInfo rei) {
        ElementParserBuilder elBuilder = builder.expectElement(q);
        ref2Parser.put(rei, elBuilder);

        Class<?> c = (Class<?>) rei.getContentType().getType();
        JExpression expression = handleElement(elBuilder, rei.getContentType(), null, c, !c.isPrimitive());
        
        CodeBody cb = elBuilder.getBody();
        
        JType type = model._ref(c);
        JType jaxbElementType = model._ref(JAXBElement.class);
        JType qnameType = model._ref(QName.class);
        JVar qname = cb.decl(qnameType, "_xname", JExpr.direct("reader").invoke("getName"));
        
        elBuilder.getBody()._return(jaxbElementType,
                                    JExpr._new(jaxbElementType)
                                    .arg(qname).arg(JExpr.dotclass((JClass) type))
                                    .arg(expression));
    }


    private void add(ElementParserBuilder b, Class cls, RuntimeClassInfo info) {
        if (!info.isElement()) {
            if (b == rootReader &&
                info.getTypeName() != null) { // this is an anonymous schema type
                addComplexType(b, cls, info);
            }
            return;
        }
        
        ElementParserBuilder classBuilder = b.expectElement(info.getElementName());
        add(classBuilder, info.getElementName(), cls, null, info, false);
        // return statement added in "add" method above
    }
    
    private void addComplexType(ElementParserBuilder b, Class<?> cls, RuntimeClassInfo info) {
        if (addedXsiTypes.contains(cls)) return;
        
        ElementParserBuilder builder = b.expectXsiType(info.getTypeName());
        
        add(builder, info.getTypeName(), cls, null, info, true);
    }

    private ElementParserBuilder add(ElementParserBuilder classBuilder, 
                                     QName name, 
                                     Class cls, 
                                     Class parentCls,
                                     RuntimeClassInfo info, 
                                     boolean wrap) {
        CodeBody body = classBuilder.getBody();
        JType type = model._ref(cls);
        
        JBlock xsiNilReturnBlock = body.getBlock()._if(JExpr.direct("reader").invoke("isXsiNil"))._then();

        // Method factoryMethod = info.getFactoryMethod();
        JVar var = body.decl(type, info.getClazz().getSimpleName(), JExpr._new(type));
        if (wrap) {
            JType jaxbElementType = model._ref(JAXBElement.class);
            JType qnameType = model._ref(QName.class);
            JVar qname = body.getBlock().decl(qnameType, "_xname", JExpr.direct("reader").invoke("getName"));
            body._return(jaxbElementType, 
                         JExpr._new(jaxbElementType)
                                .arg(qname).arg(JExpr.dotclass((JClass) type)).arg(var));
            xsiNilReturnBlock._return(JExpr._null());
        } else {
            body._return(var);
            xsiNilReturnBlock._return(JExpr._null());
        }
        
        
        if (info.isOrdered()) {
            // classBuilder.beginSequence();
        }
        
        handleProperties(info, cls, classBuilder, var);
        
        if (info.isOrdered()) {
            // classBuilder.endSequence();
        }
        
        return classBuilder;
    }

    private void handleProperties(RuntimeClassInfo info, 
                                  Class beanClass, 
                                  ElementParserBuilder classBuilder, 
                                  JVar beanVar) {
        for (RuntimePropertyInfo prop : info.getProperties()) {
            // DS: the JaxB spec does not define the mapping for char or char[]
            // the RI reads the string as an int and then converts it to a char so 42 becomes '*'
            // I think this is lame so I didn't implement it. You can use a XmlAdapter to covert
            // if you really want to handle char.
            if (char.class.equals(prop.getRawType()) || char[].class.equals(prop.getRawType())) {
                logger.info("(JAXB Reader) JaxB specification does not support property " + prop.getName()
                                   + " with type " + prop.getRawType()
                                   + " on " + beanClass + ": Use a XmlAdapter");
                continue;
            }

            if (prop.isCollection()) {
                addCollectionItem(classBuilder, beanVar, beanClass, prop);
            }

            if (prop instanceof RuntimeElementPropertyInfo) {
                // Handle an reference to a <complexType>
                RuntimeElementPropertyInfo propEl = (RuntimeElementPropertyInfo) prop;

                // handle the possible choices for this property.
                for (RuntimeTypeRef typeRef : propEl.getTypes()) {
                    handleTypeRef(classBuilder, propEl, typeRef, beanClass, beanVar);
                }
            } else if (prop instanceof RuntimeAttributePropertyInfo) {
                // Handle an attribute
                RuntimeAttributePropertyInfo propAt = (RuntimeAttributePropertyInfo) prop;
                RuntimeNonElement target = propAt.getTarget();

                ParserBuilder attBuilder = classBuilder.expectAttribute(propAt.getXmlName());
                JVar propVar = attBuilder.passParentVariable(beanVar);
                
                if (propAt.isCollection()) {
                	logger.info("Reader: attribute lists are not supported yet!");
                } else {
                    handlePropertyAttribute(attBuilder, propVar, beanClass, propAt, target);
                }
            } else if (prop instanceof RuntimeReferencePropertyInfo) {
                
                // Handle a reference to an <element> or choice of elements (java type is JAXBElement<T>)
                RuntimeReferencePropertyInfo propRef = (RuntimeReferencePropertyInfo) prop;
                for (RuntimeElement re : propRef.getElements()) {
                    RuntimeElementInfo rei = (RuntimeElementInfo) re;
                   
                    ElementParserBuilder elBuilder = classBuilder.expectElement(rei.getElementName());
                    JVar beanVar2 = elBuilder.passParentVariable(beanVar);

                    ElementParserBuilder objBuilder = ref2Parser.get(rei);
                    if (objBuilder == null) {
                        createRefParser(rootReader, rei.getElementName(), rei);
                        objBuilder = ref2Parser.get(rei);
                    } 
                    
                    JVar retVar = elBuilder.call(model._ref(rei.getType()), "_ret", objBuilder);
                    doSet(elBuilder.getBody().getBlock(), beanVar2, beanClass, propRef, retVar);
                    elBuilder.getBody()._return(retVar);
                }
            } else if (prop instanceof RuntimeValuePropertyInfo) {
            	logger.info("Reader: Attributes on simple types are not supported yet!");
                RuntimeValuePropertyInfo propv = (RuntimeValuePropertyInfo) prop;
                
                ElementParserBuilder builder2 = (ElementParserBuilder) classBuilder.newState();
                JVar var = builder2.passParentVariable(beanVar);
                
                handlePropertyElement(builder2, var, beanClass, propv, propv.getTarget().getTypeName(), 
                                      true, propv.getTarget());
            } else {
            	logger.info("(JAXB Reader) Cannot yet map property " + prop.getName() 
                                   + " with type " + prop.getRawType()
                                   + " on " + beanClass
                                   + " for " + prop.getClass().getName());
            }
        }
        
        if (info.getBaseClass() != null) {
            handleProperties(info.getBaseClass(), beanClass, classBuilder, beanVar);
        }
     }

    
    private void handleTypeRef(ElementParserBuilder classBuilder,
                               RuntimeElementPropertyInfo propEl,
                               RuntimeTypeRef typeRef, 
                               Class beanClass,
                               JVar beanVar) {
        QName name = typeRef.getTagName();
     
        // Lets check to see if we alredy we have a parser to handle this scenario
        BuilderKey key = new BuilderKey();
        key.parentClass = beanClass;
        key.type = name;
        
        if (key.type != null && type2Parser.containsKey(key)) {
            ElementParserBuilder child = type2Parser.get(key);
            classBuilder.expectElement(name, child, beanVar);
        } else {
            // No previous parser available, lets build one.
            ElementParserBuilder propBuilder = classBuilder.expectElement(name);
            JVar propVar = propBuilder.passParentVariable(beanVar);
            type2Parser.put(key, propBuilder);
            
            // Check to see if the property class is abstract
            JVar var = handlePropertyElement(propBuilder, propVar, beanClass,
                                  propEl, 
                                  typeRef.getTagName(), 
                                  typeRef.isNillable(),
                                  typeRef.getTarget());
            
            // Handle all the possible child types
            Map<Class, ? extends RuntimeClassInfo> beans = set.beans();
            for (Map.Entry<Class, ? extends RuntimeClassInfo> e : beans.entrySet()) {
                Class c = e.getKey();
                RuntimeClassInfo clsInfo = e.getValue();
                
                Class<?> propCls = (Class<?>) typeRef.getTarget().getType();
                if (propCls.isAssignableFrom(c)
                    && propCls != c
                    && !Modifier.isAbstract(c.getModifiers())) {
                    ElementParserBuilder xsiBuilder = propBuilder.expectXsiType(clsInfo.getTypeName());
                    JVar xsiVar = xsiBuilder.passParentVariable(propVar);
                    JVar xsiElement = handlePropertyElement(xsiBuilder, xsiVar, beanClass,
                                          propEl, 
                                          typeRef.getTagName(), 
                                          typeRef.isNillable(),
                                          clsInfo);
                    xsiBuilder.getBody()._return(xsiElement);
                }
            }
            propBuilder.getBody()._return(var);
        }
    }

    private JVar handlePropertyElement(ElementParserBuilder builder,
                                       JVar bean, 
                                       Class beanClass,
                                       RuntimePropertyInfo propEl, 
                                       QName name,
                                       boolean nillable,
                                       RuntimeNonElement target) {
//        if (propEl.isRequired()) {
//            builder.setRequired(true);
//        }
        
        Adapter<Type,Class> adapter = propEl.getAdapter();
        Class c = (Class) target.getType();

        if (target.isSimpleType()) {
            Type type = propEl.getRawType();
            if (type instanceof Class) {
                c = (Class) type;
                if (c.isArray() && !Byte.TYPE.equals(c.getComponentType())) {
                    c = c.getComponentType();
                }
            }

            // type to create for this property
            JBlock block = builder.getBody().getBlock();
            JExpression toSet = handleElement(builder, target, adapter, c, nillable);

            if (toSet != null) {
                JVar var;
                if (toSet instanceof JVar) {
                    var = (JVar) toSet;
                } else {
                    var = block.decl(model._ref(c), "_returnValue", toSet);
                }

                doSet(block, bean, beanClass, propEl, var);
                return var;
            }
        } else if (target instanceof RuntimeClassInfo) {
            RuntimeClassInfo rci = (RuntimeClassInfo) target;
            
            BuilderKey key = new BuilderKey();
            key.parentClass = beanClass;
            key.type = name;
            
            ElementParserBuilder propBuilder = add(builder, name, c, beanClass, rci, false);
            if (propBuilder != null) {
                JExpression exp = JExpr.direct(c.getSimpleName());
                JBlock block = propBuilder.getBody().getBlock();

                JVar var;
                if (exp instanceof JVar) {
                    var = (JVar) exp;
                } else {
                    var = block.decl(model._ref(c), "_returnValue", exp);
                }

                doSet(block, bean, beanClass, propEl, var);
                return var;
            }
        } else {
        	logger.info("(JAXB Reader) Cannot map type " + c + " yet on " + beanClass);
        }
        return null;
    }
    
    private JExpression handleElement(ElementParserBuilder builder,
                                      RuntimeNonElement target,
                                      Adapter<Type, Class> adapter, 
                                      Class c,
                                      boolean nillable) {
        JExpression toSet;
        if (adapter != null) {
             JVar adapterVar = getAdapter(adapter);
             toSet = adapterVar.invoke("unmarshal").arg(builder.getXSR().invoke("getElementText"));
        } else if ((c.equals(String.class)
            || c.equals(Integer.class)
            || c.equals(Double.class)
            || c.equals(Float.class)
            || c.equals(Short.class)
            || c.equals(Long.class)
            || c.equals(Boolean.class)
            || c.equals(Byte.class)
            || c.isPrimitive())
            && c != byte.class) {
            toSet = builder.as(c, nillable);
        } else if (c.equals(XMLGregorianCalendar.class)) {
            toSet = handleXMLGregorianCalendar(builder, builder.as(String.class, nillable));
        } else if (c.equals(Duration.class)) {
            toSet = handleDuration(builder, builder.as(String.class, nillable));
        } else if (c.isEnum()) {
            try {
                JExpression val = handleElement(builder, target, adapter, String.class, nillable);
                toSet = handleEnum(builder, val, (Transducer) target, c);
            } catch (Exception e) {
                throw new BuildException(e);
            }
        } else if (c.equals(byte[].class)) {
            JClass buType = (JClass) model._ref(BinaryUtils.class);
            
            toSet = buType.staticInvoke("decodeAsBytes").arg(builder.getXSR());
        } else if (c.equals(Byte.class) || c.equals(byte.class)) {
            toSet = JExpr.cast(model.BYTE, builder.getXSR().invoke("getElementAsInt"));
        } else if (c.equals(BigDecimal.class)) {
            JType bdType = model._ref(BigDecimal.class);
            JVar var = builder.as(String.class, nillable);
            toSet = JExpr._new(bdType).arg(var);
        } else if (c.equals(BigInteger.class)) {
            JType bdType = model._ref(BigInteger.class);
            JVar var = builder.as(String.class, nillable);
            toSet = JExpr._new(bdType).arg(var);
        } else if (c.equals(QName.class)) {
            toSet = builder.getXSR().invoke("getElementAsQName");
        } else if (target instanceof RuntimeClassInfo) {
            RuntimeClassInfo rci = (RuntimeClassInfo) target;
            JType type = model._ref(c);
            
            JVar var = builder.getBody().decl(type, type.name(), JExpr._new(type));
            
            handleProperties(rci, c, builder, var);
            
            return var;
        } else {
        	logger.info("(JAXB Reader) Can not map simple type yet: " + c);
            return JExpr._null();
        }
        return toSet;
    }

    private JVar getAdapter(Adapter<Type, Class> adapter) {
        Class c = adapter.adapterType;
        JVar var = adapters.get(c);
        if (var == null) {
            JType jt = model._ref(c);
            var = rootReader.getReaderClass().field(JMod.STATIC, jt, "adapter" + adapterCount++,
                                              JExpr._new(jt));
            adapters.put(c, var);
        }
        return var;
    }


    private void handlePropertyAttribute(ParserBuilder builder,
            JVar bean,
            Class beanClass,
            RuntimeAttributePropertyInfo propEl,
            RuntimeNonElement target) {

        if (propEl.isRequired()) {
            builder.setRequired(true);
        }

        Class<?> c = (Class<?>)propEl.getRawType();
        
        if (target.isSimpleType()) {
            // type to create for this property
            JBlock block = builder.getBody().getBlock();
            JExpression toSet = handleAttribute(builder, target, propEl.getAdapter(), c);

            if (toSet != null)
                doSet(block, bean, beanClass, propEl, toSet);
        } else {
            System.err.println("Unknown type " + c + " - " + target.getClass());
        }

    }
    
    private JExpression handleAttribute(ParserBuilder builder, 
                                        RuntimeNonElement target,
                                        Adapter<Type, Class> adapter,
                                        Class c) {
        JExpression toSet;
        if (adapter != null) {
            JVar adapterVar = getAdapter(adapter);
            toSet = adapterVar.invoke("unmarshal").arg(JExpr.direct("_attValue"));
       } else if ((c.equals(String.class)
            || c.equals(Integer.class)
            || c.equals(Double.class)
            || c.equals(Float.class)
            || c.equals(Short.class)
            || c.equals(Long.class)
            || c.equals(Boolean.class)
            || c.equals(Byte.class)
            || c.isPrimitive())
            && c != byte.class) {
            toSet = builder.as(c);
        } else if (c.equals(XMLGregorianCalendar.class)) {
            toSet = handleXMLGregorianCalendar(builder, builder.as(String.class));
        } else if (c.equals(Duration.class)) {
            toSet = handleDuration(builder, builder.as(String.class));
        } else if (c.isEnum()) {
            toSet = handleEnum(builder, builder.as(String.class), (Transducer) target, c);
        } else if (c.equals(byte[].class)) {
            JClass b64Type = model.ref(Base64.class);
            
            toSet = b64Type.staticInvoke("decode").arg(builder.as(String.class));
        } else if (c.equals(BigDecimal.class)) {
            JType bdType = model._ref(BigDecimal.class);
            JVar var = builder.as(String.class);
            toSet = JExpr._new(bdType).arg(var);
        } else if (c.equals(BigInteger.class)) {
            JType bdType = model._ref(BigInteger.class);
            JVar var = builder.as(String.class);
            toSet = JExpr._new(bdType).arg(var);
        } else if (c.equals(QName.class)) {
            JVar var = builder.as(String.class);
            toSet = builder.getXSR().invoke("getAsQName").arg(var);
        } else if (target instanceof RuntimeClassInfo) {
            RuntimeClassInfo rci = (RuntimeClassInfo) target;
            rci.getProperties();
            System.err.println("Could not map attribute " + c);
            return JExpr._null();
        } else {
            
            System.err.println("Could not map attribute " + c);
            return JExpr._null();
        }
        return toSet;
    }

    private void doSet(JBlock block,
                       JVar bean,
                       Class beanClass,
                       RuntimePropertyInfo propEl, 
                       JExpression var) {

        Accessor accessor = propEl.getAccessor();

        // if we have an AdaptedAccessor wrapper, strip off wrapper but preserve the name of the adapter class
        Class<XmlAdapter> xmlAdapterClass = null;
        if ("com.sun.xml.bind.v2.runtime.reflect.AdaptedAccessor".equals(accessor.getClass().getName())) {
            try {
                // fields on AdaptedAccessor are private so use set accessible to grab the values
                Field adapterClassField = accessor.getClass().getDeclaredField("adapter");
                adapterClassField.setAccessible(true);
                xmlAdapterClass = (Class<XmlAdapter>) adapterClassField.get(accessor);

                Field coreField = accessor.getClass().getDeclaredField("core");
                coreField.setAccessible(true);
                accessor = (Accessor) coreField.get(accessor);
            } catch (Throwable e) {
                throw new BuildException("Unable to access private fields of AdaptedAccessor class", e);
            }
        }

        if (accessor instanceof Accessor.FieldReflection) {
            Accessor.FieldReflection fieldReflection = (Accessor.FieldReflection) accessor;
            Field field = fieldReflection.f;

            if (Modifier.isPublic(field.getDeclaringClass().getModifiers()) && Modifier.isPublic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) {
                if (!propEl.isCollection()) {
                    block.assign(bean.ref(field.getName()), var);
                }
            } else {
                JFieldVar fieldAccessorField = getPrivateFieldAccessor(field);

                if (!propEl.isCollection()) {
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
                    block.add(fieldAccessorField.invoke(methodName).arg(bean).arg(var));
                }
            }
        } else if (accessor instanceof Accessor.GetterSetterReflection) {
            Accessor.GetterSetterReflection getterSetterReflection = (Accessor.GetterSetterReflection) accessor;
            // todo private getter support
            if (!propEl.isCollection()) {
                Method setter = getterSetterReflection.setter;
                block.add(bean.invoke(setter.getName()).arg(var));
            }
        } else {
            throw new BuildException("Unknown property accessor type '" + accessor.getClass().getName() + "' for property " + beanClass.getName() + "." + propEl.getName());
        }
    }

    private void addCollectionItem(ElementParserBuilder classBuilder,
            JVar bean,
            Class beanClass,
            RuntimePropertyInfo prop) {

        // determine all of the qnames that are mapped to this collection
        // JaxB allows for multiple element names to be dumped into the same collection
        Collection<QName> elementNames = new ArrayList<QName>();
        if (prop instanceof RuntimeElementPropertyInfo) {
            RuntimeElementPropertyInfo propEl = (RuntimeElementPropertyInfo) prop;
            for (RuntimeTypeRef typeRef : propEl.getTypes()) {
                elementNames.add(typeRef.getTagName());
            }
        } else if (prop instanceof RuntimeAttributePropertyInfo) {
            RuntimeAttributePropertyInfo propAt = (RuntimeAttributePropertyInfo) prop;
            elementNames.add(propAt.getXmlName());
        } else if (prop instanceof RuntimeReferencePropertyInfo) {
            RuntimeReferencePropertyInfo propRef = (RuntimeReferencePropertyInfo) prop;
            for (RuntimeElement re : propRef.getElements()) {
                RuntimeElementInfo rei = (RuntimeElementInfo) re;
                elementNames.add(rei.getElementName());
            }
        } else if (prop instanceof RuntimeValuePropertyInfo) {
            RuntimeValuePropertyInfo propv = (RuntimeValuePropertyInfo) prop;
            elementNames.add(propv.getTarget().getTypeName());
        } else {
            logger.info("(JAXB Reader) Cannot yet map property " + prop.getName()
                               + " with type " + prop.getRawType()
                               + " on " + beanClass
                               + " for " + prop.getClass().getName());
            return;
        }


        // inside the header of the expect element method...
        //
        // Collection collection = null;

        JType collectionType;
        Class collectionClass;
        Accessor accessor = prop.getAccessor();
        if (accessor instanceof Accessor.FieldReflection) {
            Accessor.FieldReflection fieldReflection = (Accessor.FieldReflection) accessor;
            Field field = fieldReflection.f;
            collectionClass = field.getType();
        } else if (accessor instanceof Accessor.GetterSetterReflection) {
            Accessor.GetterSetterReflection getterSetterReflection = (Accessor.GetterSetterReflection) accessor;
            collectionClass = getterSetterReflection.getter.getReturnType();
            if (collectionClass.isArray() && getterSetterReflection.setter == null) {
                throw new BuildException("Array property " + beanClass.getName() + "." + prop.getName() + " does not have a setter method.  Either add a setter, convert the property to field access, or use a Collection");
            }
        } else {
            throw new BuildException("Unknown property accessor type '" + accessor.getClass().getName() + "' for property " + beanClass.getName() + "." + prop.getName());
        }
        if (collectionClass.isArray()) {
            Class componentType = collectionClass.getComponentType();
            if (Boolean.TYPE.equals(componentType)) {
                collectionType = getType(ArrayUtil.BooleanArray.class);
            } else if (Character.TYPE.equals(componentType)) {
                collectionType = getType(ArrayUtil.CharArray.class);
            } else if (Short.TYPE.equals(componentType)) {
                collectionType = getType(ArrayUtil.ShortArray.class);
            } else if (Integer.TYPE.equals(componentType)) {
                collectionType = getType(ArrayUtil.IntArray.class);
            } else if (Long.TYPE.equals(componentType)) {
                collectionType = getType(ArrayUtil.LongArray.class);
            } else if (Float.TYPE.equals(componentType)) {
                collectionType = getType(ArrayUtil.FloatArray.class);
            } else if (Double.TYPE.equals(componentType)) {
                collectionType = getType(ArrayUtil.DoubleArray.class);
            } else {
                collectionType = getType(ArrayList.class);
            }
        } else {
            collectionType = getType(collectionClass);
        }

        JVar collectionVar = classBuilder.getBody().decl(collectionType,
                "col_" + beanClass.getSimpleName() + "_" + prop.getName(),
                JExpr._null());

        // inside the if block that reads each element
        //
        for (QName elementName : elementNames) {
            // item = read5(reader, properties)
            JBlock block = new JBlock();
            JVar itemVar;
            if (collectionClass.isArray() && collectionClass.getComponentType().isPrimitive()) {
                itemVar = block.decl(model._ref(collectionClass.getComponentType()), "item");
            } else {
                itemVar = block.decl(model._ref(Object.class), "item");
            }
            classBuilder.setReadBlock(elementName, itemVar, block);

            // if (collection == null) {
            JBlock createCollectionBlock = block._if(collectionVar.eq(JExpr._null()))._then();

            //     collection = (Collection) bean.getItemCollection();
            if (!collectionClass.isArray()) {
                if (accessor instanceof Accessor.FieldReflection) {
                    Accessor.FieldReflection fieldReflection = (Accessor.FieldReflection) accessor;
                    Field field = fieldReflection.f;

                    if (Modifier.isPublic(field.getDeclaringClass().getModifiers()) && Modifier.isPublic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) {
                        createCollectionBlock.assign(collectionVar, bean.ref(field.getName()));
                    } else {
                        JFieldVar fieldAccessorField = getPrivateFieldAccessor(field);
                        createCollectionBlock.assign(collectionVar, JExpr.cast(collectionType, fieldAccessorField.invoke("getObject").arg(bean)));
                    }
                } else if (accessor instanceof Accessor.GetterSetterReflection) {
                    Accessor.GetterSetterReflection getterSetterReflection = (Accessor.GetterSetterReflection) accessor;
                    // todo private getter support
                    Method getter = getterSetterReflection.getter;
                    createCollectionBlock.assign(collectionVar, bean.invoke(getter.getName()));
                } else {
                    throw new BuildException("Unknown property accessor type '" + accessor.getClass().getName() + "' for property " + beanClass.getName() + "." + prop.getName());
                }

                //     if (collection != null) {
                //         collection.clear();
                JConditional arrayFoundCondition = createCollectionBlock._if(collectionVar.ne(JExpr._null()));
                arrayFoundCondition._then().invoke(collectionVar, "clear");
                //     } else {
                //         collection = new ArrayList();
                Class clazz = getCollectionClass(collectionClass);
                if (clazz != null) {
                    arrayFoundCondition._else().assign(collectionVar, JExpr._new(builder.getCodeModel().ref(clazz)));
                } else {
                    arrayFoundCondition._else()._throw(JExpr._new(model.ref(NullPointerException.class))
                            .arg("Collection " + prop.getName() + " in class " + beanClass.getName() +
                            " is null and a new instance of " + collectionClass.getName() + " can not be created"));
                }
            } else {
                createCollectionBlock.assign(collectionVar, JExpr._new(collectionType));
            }
            //     }
            // }

            // collection.add(item);
            block.add(collectionVar.invoke("add").arg(itemVar));
        }

        // inside of the tail block of the expect element method (at the bottom)...
        //
        // if (collection != null) {
        //     bean.setItemCollection(collection);
        // }
        JExpression collectionAssignment = collectionVar;
        if (collectionClass.isArray()) {
            if (collectionClass.getComponentType().isPrimitive()) {
                collectionAssignment = collectionVar.invoke("toArray");
            } else {
                JArray newArray = JExpr.newArray(model._ref(collectionClass.getComponentType()), collectionVar.invoke("size"));
                collectionAssignment = JExpr.cast(model._ref(collectionClass), collectionVar.invoke("toArray").arg(newArray));
            }
        }
        if (accessor instanceof Accessor.FieldReflection) {
            JBlock assignCollectionBlock = classBuilder.getTailBlock()._if(collectionVar.ne(JExpr._null()))._then();

            Accessor.FieldReflection fieldReflection = (Accessor.FieldReflection) accessor;
            Field field = fieldReflection.f;

            if (Modifier.isPublic(field.getDeclaringClass().getModifiers()) && Modifier.isPublic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) {
                assignCollectionBlock.assign(bean.ref(field.getName()), collectionAssignment);
            } else {
                JFieldVar fieldAccessorField = getPrivateFieldAccessor(field);
                assignCollectionBlock.add(fieldAccessorField.invoke("setObject").arg(bean).arg(collectionAssignment));
            }
        } else if (accessor instanceof Accessor.GetterSetterReflection) {
            Accessor.GetterSetterReflection getterSetterReflection = (Accessor.GetterSetterReflection) accessor;
            // todo private getter support
            Method setter = getterSetterReflection.setter;
            if (setter != null) {
                // if there is no setter method, the collection is not assigned into the class
                // this assumes that the getter returned the a collection instance and held on to a reference
                JBlock assignCollectionBlock = classBuilder.getTailBlock()._if(collectionVar.ne(JExpr._null()))._then();
                assignCollectionBlock.add(bean.invoke(setter.getName()).arg(collectionAssignment));
            }
        } else {
            throw new BuildException("Unknown property accessor type '" + accessor.getClass().getName() + "' for property " + beanClass.getName() + "." + prop.getName());
        }
        // }
    }

    private Class getCollectionClass(Class collectionType) {
        if (!collectionType.isInterface()) {
            try {
                collectionType.getConstructor();
                return collectionType;
            } catch (NoSuchMethodException e) {
            }
        } else if (SortedSet.class.equals(collectionType)) {
            return TreeSet.class;
        } else if (Set.class.equals(collectionType)) {
            return LinkedHashSet.class;
        } else if (Queue.class.equals(collectionType)) {
            return LinkedList.class;
        } else if (List.class.equals(collectionType)) {
            return ArrayList.class;
        } else if (Collection.class.equals(collectionType)) {
            return ArrayList.class;
        }
        return null;
    }

    private JFieldVar getPrivateFieldAccessor(Field field) {
        String fieldName = "_" + field.getDeclaringClass().getSimpleName() + "_" + field.getName();
        JFieldVar fieldAccessorField = privateFieldAccessors.get(fieldName);
        if (fieldAccessorField == null) {

            JDefinedClass readerCls = rootReader.getReaderClass();


            JInvocation newFieldAccessor = JExpr._new(builder.getCodeModel()._ref(FieldAccessor.class))
                    .arg(builder.getCodeModel().ref(field.getDeclaringClass()).staticRef("class"))
                    .arg(JExpr.lit(field.getName()));

            fieldAccessorField = readerCls.field(JMod.PRIVATE | JMod.STATIC,
                    FieldAccessor.class, fieldName,
                    newFieldAccessor);

            privateFieldAccessors.put(fieldName, fieldAccessorField);
        }
        return fieldAccessorField;
    }

    private JExpression handleXMLGregorianCalendar(ParserBuilder builder, JVar value) {
        return dtf.invoke("newXMLGregorianCalendar").arg(value);
    }
    
    private JExpression handleDuration(ParserBuilder builder, JVar value) {
        return dtf.invoke("newDuration").arg(value);
    }
    
    private JExpression handleEnum(ParserBuilder builder, JExpression value, Transducer transducer, Class c) {
        JBlock body =  builder.getBody().getBlock();

        JClass jc = model.ref(c);
        JVar targetValue = body.decl(jc, "enumValue");

        JConditional xsiCond = null;
        Enum[] enumValues;
        try {
            Method method = c.getMethod("values");
            enumValues = (Enum[]) method.invoke(null);
        } catch (Exception e) {
            throw new BuildException("Class is not an enumeration " + c.getName());
        }

        if (enumValues.length == 0) {
            throw new BuildException("Enum contains no values " + c.getName());
        }
        for (Enum enumValue : enumValues) {
            String enumText;
            try {
                enumText = transducer.print(enumValue).toString();
            } catch (AccessorException e) {
                throw new BuildException(e);
            }

            JExpression textCompare = JExpr.lit(enumText).invoke("equals").arg(value);
            if (xsiCond == null) {
                xsiCond = body._if(textCompare);
            } else {
                xsiCond = xsiCond._else()._if(textCompare);
            }
            JBlock block = xsiCond._then();
            block.assign(targetValue, jc.staticRef(enumValue.name()));
        }

        // java.lang.IllegalArgumentException: No enum const class com.envoisolutions.sxc.jaxb.enums.AnnotatedEnum.ssssss
        xsiCond._else()._throw(JExpr._new(model.ref(IllegalArgumentException.class))
                .arg(JExpr.lit("No enum const " + c + ".").plus(value)));

        return targetValue;
    }

    public Builder getBuilder() {
        return builder;
    }

    private JType getGenericType(Type t) {
        if (t instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) t;

            Type[] actualTypes = pt.getActualTypeArguments();
            for (Type actual : actualTypes) {
                return getType(actual);
            }
        }
        if (t instanceof Class) {
            Class clazz = (Class) t;
            if (clazz.isArray()) {
                return getType(clazz.getComponentType());
            }
        }
        throw new IllegalStateException("Can not determine generic type of " + t);
    }

    private JType getType(Type t) {
        if (t instanceof Class) {
            return rootReader.getCodeModel()._ref((Class)t);
        } else if (t instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) t;
            JClass raw = (JClass) getType(pt.getRawType());

            Type[] actualTypes = pt.getActualTypeArguments();
            for (Type actual : actualTypes) {
                raw = raw.narrow((JClass) getType(actual));
            }

            return raw;
        }
        throw new IllegalStateException();
    }

    private JType getType(Class<?> c) {
        return rootReader.getCodeModel()._ref(c);
    }
}
