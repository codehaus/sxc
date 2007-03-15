package com.envoisolutions.sxc.jaxb;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.bind.JAXBElement;
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
import com.sun.xml.bind.v2.model.core.Adapter;
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
    
    public ReaderIntrospector(Builder builder2, RuntimeTypeInfoSet set) {
        this.builder = builder2;
        rootReader = this.builder.getParserBuilder();
        this.model = rootReader.getCodeModel();
        this.set = set;
        
        type_dtFactory = model._ref(DatatypeFactory.class);
        
        JDefinedClass readerCls = rootReader.getReaderClass();
        dtf = readerCls.field(JMod.STATIC, type_dtFactory, "dtFactory");
        JMethod constructor = readerCls.constructor(JMod.PUBLIC);
        JTryBlock tb = constructor.body()._try();
        JBlock body = tb.body();
        body.assign(dtf, JExpr.direct(DatatypeFactory.class.getName()+".newInstance()"));
        tb._catch((JClass) rootReader.getCodeModel()._ref(DatatypeConfigurationException.class));
        
        Map<QName, ? extends RuntimeElementInfo> elementMappings = set.getElementMappings(null);
        for (Map.Entry<QName, ? extends RuntimeElementInfo> e : elementMappings.entrySet()) {
            QName q = e.getKey();
            RuntimeElementInfo rei = e.getValue();
            
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
        JCodeModel model = classBuilder.getCodeModel();
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
        } else if (parentCls == null) { 
            body._return(var);
            xsiNilReturnBlock._return(JExpr._null());
        } else {
            xsiNilReturnBlock._return();
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
                    handlePropertyAttribute(attBuilder, propVar, beanClass, propAt, target, set);
                }
            } else if (prop instanceof RuntimeReferencePropertyInfo) {
                
                // Handle a reference to an <element> or choice of elements
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
                    elBuilder.getBody()._return(retVar);
                    doSet(elBuilder.getBody().getBlock(), beanVar2, beanClass, propRef, retVar);

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
            return;
        } else {
            // No previous parser available, lets build one.
            ElementParserBuilder propBuilder = classBuilder.expectElement(name);
            JVar propVar = propBuilder.passParentVariable(beanVar);
            
            Class<?> propCls = (Class<?>) typeRef.getTarget().getType();
            
            type2Parser.put(key, propBuilder);
            
            // Check to see if the property class is abstract
            handlePropertyElement(propBuilder, propVar, beanClass, 
                                  propEl, 
                                  typeRef.getTagName(), 
                                  typeRef.isNillable(),
                                  typeRef.getTarget());
            
            // Handle all the possible child types
            Map<Class, ? extends RuntimeClassInfo> beans = set.beans();
            for (Map.Entry<Class, ? extends RuntimeClassInfo> e : beans.entrySet()) {
                Class c = e.getKey();
                RuntimeClassInfo clsInfo = e.getValue();
                
                if (propCls.isAssignableFrom(c) 
                    && propCls != c
                    && !Modifier.isAbstract(c.getModifiers())) {
                    ElementParserBuilder xsiBuilder = propBuilder.expectXsiType(clsInfo.getTypeName());
                    JVar xsiVar = xsiBuilder.passParentVariable(propVar);
                    handlePropertyElement(xsiBuilder, xsiVar, beanClass, 
                                          propEl, 
                                          typeRef.getTagName(), 
                                          typeRef.isNillable(),
                                          clsInfo);
                }
            }
        }
    }

    private void handlePropertyElement(ElementParserBuilder builder, 
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
            }
            
            // type to create for this property
            JBlock block = builder.getBody().getBlock();
            JExpression toSet = handleElement(builder, target, adapter, c, nillable);

            if (toSet != null)
                doSet(block, bean, beanClass, propEl, toSet); 
        } else if (target instanceof RuntimeClassInfo) {
            RuntimeClassInfo rci = (RuntimeClassInfo) target;
            
            BuilderKey key = new BuilderKey();
            key.parentClass = beanClass;
            key.type = name;
            
            ElementParserBuilder propBuilder = add(builder, name, c, beanClass, rci, false);
            if (propBuilder != null) {
                JExpression exp = JExpr.direct(c.getSimpleName());
                doSet(propBuilder.getBody().getBlock(), bean, beanClass, propEl, exp);
            }
        } else {
        	logger.info("(JAXB Reader) Cannot map type " + c + " yet on " + beanClass);
        }
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
                Method method = c.getMethod("value", new Class[0]);
                
                Class<?> readAs = method.getReturnType();
                JExpression val = handleElement(builder, target, adapter, readAs, nillable);
                
                toSet = handleEnum(builder, val, c);
            } catch (Exception e) {
                throw new BuildException(e);
            }
        } else if (c.equals(byte[].class)) {
            JClass buType = (JClass) builder.getCodeModel()._ref(BinaryUtils.class);
            
            toSet = buType.staticInvoke("decodeAsBytes").arg(builder.getXSR());
        } else if (c.equals(Byte.class) || c.equals(byte.class)) {
            toSet = JExpr.cast(model.BYTE, builder.getXSR().invoke("getElementAsInt"));
        } else if (c.equals(BigDecimal.class)) {
            JType bdType = builder.getCodeModel()._ref(BigDecimal.class);
            JVar var = builder.as(String.class, nillable);
            toSet = JExpr._new(bdType).arg(var);
        } else if (c.equals(BigInteger.class)) {
            JType bdType = builder.getCodeModel()._ref(BigInteger.class);
            JVar var = builder.as(String.class, nillable);
            toSet = JExpr._new(bdType).arg(var);
        } else if (c.equals(QName.class)) {
            toSet = builder.getXSR().invoke("getElementAsQName");
        } else if (target instanceof RuntimeClassInfo) {
            RuntimeClassInfo rci = (RuntimeClassInfo) target;
            JType type = builder.getCodeModel()._ref(c);
            
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
                                         RuntimeNonElement target,
                                         RuntimeTypeInfoSet set) {
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
            toSet = handleEnum(builder, builder.as(String.class), c);
        } else if (c.equals(byte[].class)) {
            JClass b64Type = (JClass) builder.getCodeModel()._ref(Base64.class);
            
            toSet = b64Type.staticInvoke("decode").arg(builder.as(String.class));
        } else if (c.equals(BigDecimal.class)) {
            JType bdType = builder.getCodeModel()._ref(BigDecimal.class);
            JVar var = builder.as(String.class);
            toSet = JExpr._new(bdType).arg(var);
        } else if (c.equals(BigInteger.class)) {
            JType bdType = builder.getCodeModel()._ref(BigInteger.class);
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
        if (propEl.isCollection()) {
            String getter = JaxbUtil.getGetter(beanClass, propEl.getName(), propEl.getRawType());
            block.add(bean.invoke(getter).invoke("add").arg(var));
        } else {
            String setter = JaxbUtil.getSetter(beanClass, propEl.getName());
            block.add(bean.invoke(setter).arg(var));
        }
    }

    private JExpression handleXMLGregorianCalendar(ParserBuilder builder, JVar value) {
        return dtf.invoke("newXMLGregorianCalendar").arg(value);
    }
    
    private JExpression handleDuration(ParserBuilder builder, JVar value) {
        return dtf.invoke("newDuration").arg(value);
    }
    
    private JExpression handleEnum(ParserBuilder builder, JExpression value, Class c) {
        JBlock body =  builder.getBody().getBlock();
        JClass jc = (JClass) builder.getCodeModel()._ref(c);
        
        return body.staticInvoke(jc, "fromValue").arg(value);
    }

    public Builder getBuilder() {
        return builder;
    }
}
