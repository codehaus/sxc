package com.envoisolutions.sxc.jaxb;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

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

import org.jvnet.jaxb.reflection.model.runtime.RuntimeAttributePropertyInfo;
import org.jvnet.jaxb.reflection.model.runtime.RuntimeClassInfo;
import org.jvnet.jaxb.reflection.model.runtime.RuntimeElementInfo;
import org.jvnet.jaxb.reflection.model.runtime.RuntimeElementPropertyInfo;
import org.jvnet.jaxb.reflection.model.runtime.RuntimeNonElement;
import org.jvnet.jaxb.reflection.model.runtime.RuntimePropertyInfo;
import org.jvnet.jaxb.reflection.model.runtime.RuntimeTypeInfoSet;
import org.jvnet.jaxb.reflection.model.runtime.RuntimeTypeRef;

public class ReaderIntrospector {
    private ElementParserBuilder rootReader;
    private JType type_dtFactory;
    private Set<Class<?>> addedXsiTypes = new HashSet<Class<?>>();
    private Builder builder;
    private JFieldVar dtf;
    private JCodeModel model;
    private Map<BuilderKey, ElementParserBuilder> type2Parser 
        = new HashMap<BuilderKey, ElementParserBuilder>();
    private RuntimeTypeInfoSet set;
    
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
        
        Map<Class, ? extends RuntimeClassInfo> beans = set.beans();
        for (Map.Entry<Class, ? extends RuntimeClassInfo> e : beans.entrySet()) {
            Class cls = e.getKey();
            if (!Modifier.isAbstract(cls.getModifiers())) {
                add(rootReader, cls, e.getValue());
            }
        }
        
        Map<QName, ? extends RuntimeElementInfo> elementMappings = set.getElementMappings(null);
        for (Map.Entry<QName, ? extends RuntimeElementInfo> e : elementMappings.entrySet()) {
            QName q = e.getKey();
            RuntimeElementInfo rei = e.getValue();
            
            ElementParserBuilder elBuilder = rootReader.expectElement(q);
            Class<?> c = (Class<?>) rei.getContentType().getType();
            JExpression expression = handleElement(elBuilder, rei.getContentType(), c, !c.isPrimitive());
            
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

        
//        
//        for (Map.Entry<Class, ? extends RuntimeEnumLeafInfo> e : set.enums().entrySet()) {
//            Class cls = e.getKey();
//            JType enumType = model._ref(cls);
//            
//            
//        }
    }


    private void add(ElementParserBuilder b, Class cls, RuntimeClassInfo info) {
        if (!info.isElement()) {
            if (b == rootReader &&
                info.getTypeName() != null) { // this is an anonymous schema type
                addComplexType(b, cls, info);
            }
            return;
        }
        
        add(b, info.getElementName(), cls, null, info, false);
    }
    
    private void addComplexType(ElementParserBuilder b, Class<?> cls, RuntimeClassInfo info) {
        if (addedXsiTypes.contains(cls)) return;
        
        ElementParserBuilder builder = b.onXsiType(info.getTypeName());
        
        add(builder, info.getTypeName(), cls, null, info, true);
    }

    private ElementParserBuilder add(ElementParserBuilder b, 
                                     QName name, 
                                     Class cls, 
                                     Class parentCls,
                                     RuntimeClassInfo info, 
                                     boolean wrap) {
        ElementParserBuilder classBuilder;
        if (info.isElement()) {
            classBuilder = b.expectElement(name);
        } else {
            classBuilder = b;
        }
        
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
                                  JVar var) {
        for (RuntimePropertyInfo prop : info.getProperties()) {
            if (prop instanceof RuntimeElementPropertyInfo) {
                RuntimeElementPropertyInfo propEl = (RuntimeElementPropertyInfo) prop;
                
                // TODO: not sure why there are multiple typeRefs allowed for properties...
                for (RuntimeTypeRef typeRef : propEl.getTypes()) {
                    QName name = typeRef.getTagName();
                    
                    // Lets check to see if we alredy we have a parser to handle this scenario
                    BuilderKey key = new BuilderKey();
                    key.parentClass = beanClass;
                    key.type = name;
                    
                    if (beanClass != null && type2Parser.containsKey(key)) {
                        ElementParserBuilder child = type2Parser.get(key);
                        classBuilder.expectElement(name, child);
                        continue;
                    } else {
                        // No previous parser available, lets build one.
                        ElementParserBuilder propBuilder = classBuilder.expectElement(name);
                        JVar propVar = propBuilder.passParentVariable(var);
                        
                        Class<?> propCls = (Class<?>) typeRef.getTarget().getType();
                        
                        type2Parser.put(key, propBuilder);
                        
                        // Check to see if the property class is abstract
                        handlePropertyElement(propBuilder, propVar, beanClass, 
                                              propEl, typeRef, typeRef.getTarget());
                        
                        // Handle all the possible child types
                        Map<Class, ? extends RuntimeClassInfo> beans = set.beans();
                        for (Map.Entry<Class, ? extends RuntimeClassInfo> e : beans.entrySet()) {
                            Class c = e.getKey();
                            RuntimeClassInfo clsInfo = e.getValue();
                            
                            if (propCls.isAssignableFrom(c) 
                                && propCls != c
                                && !Modifier.isAbstract(c.getModifiers())) {
                                ElementParserBuilder xsiBuilder = propBuilder.onXsiType(clsInfo.getTypeName());
                                JVar xsiVar = xsiBuilder.passParentVariable(propVar);
                                handlePropertyElement(xsiBuilder, xsiVar, beanClass, 
                                                      propEl, typeRef, clsInfo);
                            }
                        }
                    }
                }
            } else if (prop instanceof RuntimeAttributePropertyInfo) {
                RuntimeAttributePropertyInfo propAt = (RuntimeAttributePropertyInfo) prop;
                RuntimeNonElement target = propAt.getTarget();

                ParserBuilder attBuilder = classBuilder.expectAttribute(propAt.getXmlName());
                JVar propVar = attBuilder.passParentVariable(var);
                
                handlePropertyAttribute(attBuilder, propVar, beanClass, propAt, target, set);
            } else {
                System.out.println("Unknown Property " + prop.getName());
            }
        }
        
        if (info.getBaseClass() != null) {
            handleProperties(info.getBaseClass(), beanClass, classBuilder, var);
        }
     }

    
    private void handlePropertyElement(ElementParserBuilder builder, 
                                       JVar bean, 
                                       Class beanClass,
                                       RuntimeElementPropertyInfo propEl, 
                                       RuntimeTypeRef typeRef, 
                                       RuntimeNonElement target) {
        if (propEl.isRequired()) {
            builder.setRequired(true);
        }
        
        Class<?> c = (Class<?>) target.getType();
        QName name = typeRef.getTagName();

        
        if (target.isSimpleType()) {
            // type to create for this property
            JBlock block = builder.getBody().getBlock();
            JExpression toSet = handleElement(builder, target, c, typeRef.isNillable());

            if (toSet != null)
                doSet(block, bean, beanClass, propEl, toSet); 
        } else if (target instanceof RuntimeClassInfo) {
            RuntimeClassInfo rci = (RuntimeClassInfo) target;
            
            ElementParserBuilder propBuilder = add(builder, name, c, beanClass, rci, false);
            if (propBuilder != null) {
                JExpression exp = JExpr.direct(c.getSimpleName());
                doSet(propBuilder.getBody().getBlock(), bean, beanClass, propEl, exp);
            }
        } else {
            System.err.println("Unknown type " + c + " - " + target.getClass());
        }
           
        
    }
    
    private JExpression handleElement(ElementParserBuilder builder, 
                                      RuntimeNonElement target,
                                      Class c,
                                      boolean nillable) {
        JExpression toSet;
        if (c.equals(String.class) 
            || c.equals(Integer.class)
            || c.equals(Double.class)
            || c.equals(Float.class)
            || c.equals(Short.class)
            || c.equals(Long.class)
            || c.equals(Boolean.class)
            || c.isPrimitive()) {
            toSet = builder.as(c, nillable);
        } else if (c.equals(XMLGregorianCalendar.class)) {
            toSet = handleXMLGregorianCalendar(builder, builder.as(String.class, nillable));
        } else if (c.equals(Duration.class)) {
            toSet = handleDuration(builder, builder.as(String.class, nillable));
        } else if (c.isEnum()) {
            toSet = handleEnum(builder, builder.as(String.class, nillable), c);
        } else if (c.equals(byte[].class)) {
            JClass buType = (JClass) builder.getCodeModel()._ref(BinaryUtils.class);
            
            toSet = buType.staticInvoke("decodeAsBytes").arg(builder.getXSR());
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
            
            System.err.println("Could not map simple type " + c);
            return JExpr._null();
        }
        return toSet;
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

        Class<?> c = (Class<?>)target.getType();
        
        if (target.isSimpleType()) {
            // type to create for this property
            JBlock block = builder.getBody().getBlock();
            JExpression toSet = handleAttribute(builder, target, c);

            if (toSet != null)
                doSet(block, bean, beanClass, propEl, toSet);
        } else {
            System.err.println("Unknown type " + c + " - " + target.getClass());
        }

    }
    
    private JExpression handleAttribute(ParserBuilder builder, 
                                        RuntimeNonElement target,
                                        Class c) {
        JExpression toSet;
        if (c.equals(String.class) 
            || c.equals(Integer.class)
            || c.equals(Double.class)
            || c.equals(Float.class)
            || c.equals(Short.class)
            || c.equals(Long.class)
            || c.equals(Boolean.class)
            || c.isPrimitive()) {
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
    
    private JExpression handleEnum(ParserBuilder builder, JVar value, Class c) {
        JBlock body =  builder.getBody().getBlock();
        JClass jc = (JClass) builder.getCodeModel()._ref(c);
        
        return body.staticInvoke(jc, "fromValue").arg(value);
    }

    public Builder getBuilder() {
        return builder;
    }
}
