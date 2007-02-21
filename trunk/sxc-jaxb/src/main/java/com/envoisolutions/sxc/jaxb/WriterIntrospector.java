package com.envoisolutions.sxc.jaxb;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.envoisolutions.sxc.builder.Builder;
import com.envoisolutions.sxc.builder.ElementWriterBuilder;
import com.envoisolutions.sxc.builder.WriterBuilder;
import com.envoisolutions.sxc.util.Base64;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JForEach;
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

public class WriterIntrospector {
    private ElementWriterBuilder rootWriter;
    private Set<Class<?>> addedXsiTypes = new HashSet<Class<?>>();
    private Builder builder;
    private JCodeModel model;
    private Map<Class, QName> c2type = new HashMap<Class, QName>();    
    private Map<BuilderKey, ElementWriterBuilder> type2Parser 
        = new HashMap<BuilderKey, ElementWriterBuilder>();
    
    
    public WriterIntrospector(Builder b, RuntimeTypeInfoSet set) throws JAXBException {
        this.builder = b;
        rootWriter = this.builder.getWriterBuilder();
        this.model = rootWriter.getCodeModel();
        
        // TODO sort classes by hierarchy
        List<Class> classes = getSortedClasses(set.beans().keySet());

        Map<Class, ? extends RuntimeClassInfo> beans = set.beans();
        for (Class cls : classes) {
            JType jt = getType(cls);
            ElementWriterBuilder instcWriter = rootWriter.newCondition(rootWriter.getObject()._instanceof(jt), jt);
            
            add(instcWriter, cls, beans.get(cls));
            instcWriter.getCurrentBlock()._return();
        }

        Map<QName, ? extends RuntimeElementInfo> elementMappings = set.getElementMappings(null);
        for (Map.Entry<QName, ? extends RuntimeElementInfo> e : elementMappings.entrySet()) {
            QName q = e.getKey();
            RuntimeElementInfo rei = e.getValue();
            Class<?> c = (Class<?>) rei.getContentType().getType();
            JType jt = model._ref(c);
            
            ElementWriterBuilder instcWriter = 
                rootWriter.newCondition(rootWriter.getObject()._instanceof(jt), jt);
            writeSimpleTypeElement(instcWriter, rei.getContentType(), true, c, c, jt);

            c2type.put(c, q);
        }
    }

    private List<Class> getSortedClasses(Set<Class> name) {
        List<Class> l = new ArrayList<Class>();
        l.addAll(name);
        
        Collections.sort(l, new Comparator<Class>() {

            @SuppressWarnings("unchecked")
            public int compare(Class o1, Class o2) {
                if (o1 == o2) return 0;
                
                if (o1.isAssignableFrom(o2)) 
                    return 1;
                
                return -1;
            }
            
        });
        return l;
    }

    private void add(ElementWriterBuilder b, Class cls, RuntimeClassInfo info) {
        if (!info.isElement()) { 
            if ( info.getTypeName() != null) { // anonymous schema type
                addComplexType(b, cls, info);
            }
            return;
        }
        
        add(b, info.getElementName(), cls, info, false);
    }
    
    private void addComplexType(ElementWriterBuilder b, Class<?> cls, RuntimeClassInfo info) {
        if (addedXsiTypes.contains(cls)) return;
        
        addedXsiTypes.add(cls);
        
        add(b, info.getTypeName(), cls, info, true);
    }

    private void add(ElementWriterBuilder b, 
                     QName name, 
                     Class cls, 
                     RuntimeClassInfo info,
                     boolean wrap) {
        addType(cls, info.getTypeName());
        
        JType type = b.getCodeModel()._ref(cls);
        ElementWriterBuilder classBuilder;
        if (info.isElement()) {
            JBlock block = b.getCurrentBlock();
            JVar var = block.decl(type, "_o", JExpr.cast(type, b.getObject()));
            JBlock nullBlock = block._if(var.ne(JExpr._null()))._then();
            b.setCurrentBlock(nullBlock);
            
            classBuilder = b.writeElement(name, type, var);

            b.setCurrentBlock(block);
        } else {
            if (info.getTypeName() != null) {
                QName typeName = info.getTypeName();
                b.getCurrentBlock().add(
                    b.getXSW().invoke("writeXsiType").arg(typeName.getNamespaceURI()).arg(typeName.getLocalPart()));
            }
            classBuilder = b;
        }
        
        writeProperties(info, cls, classBuilder);
    }

    private void addType(Class cls, QName name) {
        c2type.put(cls, name);
    }

    private void writeProperties(RuntimeClassInfo info, Class parentClass, ElementWriterBuilder classBuilder) {
        for (RuntimePropertyInfo prop : info.getProperties()) {
            if (prop instanceof RuntimeElementPropertyInfo) {
                RuntimeElementPropertyInfo propEl = (RuntimeElementPropertyInfo) prop;
                
                for (RuntimeTypeRef typeRef : propEl.getTypes()) {
                    handleProperty(classBuilder, parentClass, propEl, typeRef);
                }
            } else if (prop instanceof RuntimeAttributePropertyInfo) {
                RuntimeAttributePropertyInfo propAt = (RuntimeAttributePropertyInfo) prop;
                
                Type rawType = propAt.getRawType();

                Class c = (Class) propAt.getTarget().getType();
                JType jt = getType(c);
                
                String propName = JaxbUtil.getGetter(parentClass, propAt.getName(), rawType);
                
                WriterBuilder atBuilder = 
                    classBuilder.writeAttribute(propAt.getXmlName(), 
                                            jt, 
                                            classBuilder.getObject().invoke(propName));
                
                writeSimpleTypeAttribute(atBuilder, rawType, c, jt);
            }
        }
        
        if (info.getBaseClass() != null) {
            writeProperties(info.getBaseClass(), parentClass, classBuilder);
        }
    }

    private void handleProperty(ElementWriterBuilder b, 
                                Class parentClass, 
                                RuntimeElementPropertyInfo propEl, 
                                RuntimeTypeRef typeRef) {
        RuntimeNonElement target = typeRef.getTarget();
        
        Type rawType = propEl.getRawType();

        Class c = (Class) target.getType();
        JType jt = getType(c);
        JType rawJT = getType(rawType);
        
        QName name = typeRef.getTagName();
        JBlock block = b.getCurrentBlock();
        
        addType(c, target.getTypeName());

        String propName = JaxbUtil.getGetter(parentClass, propEl.getName(), rawType);
        
        JVar var = block.decl(rawJT, 
                              propEl.getName(), 
                              b.getObject().invoke(propName));

        JBlock origBlock = block;
        if (!propEl.isRequired()) {
            JConditional nullCond = block._if(var.ne(JExpr._null()));
            block = nullCond._then();
            b.setCurrentBlock(block);
            
            if (typeRef.isNillable()) {
                nullCond._else().add(b.getXSW().invoke("writeXsiNil"));
            } else if (propEl.isCollection() && propEl.isCollectionNillable()) {
                nullCond._else().add(b.getXSW().invoke("writeXsiNil"));
            }
        }
        
        if (propEl.isCollection()) {
            JForEach each = block.forEach(jt, "_o", var);
            JBlock newBody = each.body();
            b.setCurrentBlock(newBody);
            var = each.var();
            
            rawType = c;
            rawJT = jt;
        }

        ElementWriterBuilder valueBuilder = b.writeElement(name, jt, var);
        
        if (target.isSimpleType()) {
            writeSimpleTypeElement(valueBuilder, 
                                   target,
                                   typeRef.isNillable(), 
                                   rawType, c, jt);
        } else if (target instanceof RuntimeClassInfo) {
            RuntimeClassInfo rci = (RuntimeClassInfo)target;

            BuilderKey key = new BuilderKey();
            key.parentClass = parentClass;
            key.type = rci.getTypeName();
            
            if (key.type == null || !type2Parser.containsKey(key)) {
                if (typeRef.isNillable()) {
                    valueBuilder.writeNilIfNull();
                }
                
                if (propEl.parent() != null) {
                    valueBuilder.moveTo(rootWriter);
                } else {
                    type2Parser.put(key, valueBuilder);
                    add(valueBuilder, name, c, rci, false);
                }
            } else {
                    WriterBuilder builder2 = type2Parser.get(key);
                    
                    valueBuilder.moveTo(builder2);
            }
        } else {
            System.err.println("Unknown type " + c);
        }
        
        if (propEl.isCollection()) {
            b.setCurrentBlock(block);
        }
        
        b.setCurrentBlock(origBlock);
    }

    private void writeSimpleTypeElement(ElementWriterBuilder b, 
                                        RuntimeNonElement target, 
                                        boolean nillable,
                                        Type rawType, 
                                        Class c, 
                                        JType jt) {
        if(c.equals(String.class) 
            || c.equals(Integer.class)
            || c.equals(Double.class)
            || c.equals(Boolean.class)
            || c.equals(Float.class)
            || c.equals(Short.class)
            || c.equals(Long.class)
            || c.isPrimitive()) {
            b.writeAs(c, nillable);
        } else if (c.equals(XMLGregorianCalendar.class)) {
            writePropertyWithMethodInvoke(b, jt, "toXMLFormat", nillable);
        } else if (c.isEnum()) {
            writePropertyWithMethodInvoke(b, jt, "value", nillable);
        } else if (c.equals(BigInteger.class) || c.equals(BigDecimal.class) || c.equals(Duration.class)) {
            writePropertyWithMethodInvoke(b, jt, "toString", nillable);
        } else if (c.equals(byte[].class)) {
            writeBase64Binary(b, jt, nillable);  
        } else if (c.equals(QName.class)) {
            writeQName(b, jt, nillable);  
        } else if (target instanceof RuntimeClassInfo) {
            RuntimeClassInfo rci = (RuntimeClassInfo) target;
            
//            writeProperties(rci, c, b);
            b.moveTo(rootWriter);
        } else {
            System.out.println("Could not map! " + c);
        }
    }
    
    private void writeSimpleTypeAttribute(WriterBuilder b, 
                                          Type rawType, 
                                          Class c, 
                                          JType jt) {
        if(c.equals(String.class) 
            || c.equals(Integer.class)
            || c.equals(Double.class)
            || c.equals(Boolean.class)
            || c.equals(Float.class)
            || c.equals(Short.class)
            || c.equals(Long.class)
            || c.isPrimitive()) {
            b.writeAs(c);
        } else if (c.equals(XMLGregorianCalendar.class)) {
            writeAttributeWithMethodInvoke(b, jt, "toXMLFormat");
        } else if (c.isEnum()) {
            writeAttributeWithMethodInvoke(b, jt, "value");
        } else if (c.equals(BigInteger.class) || c.equals(BigDecimal.class) || c.equals(Duration.class)) {
            writeAttributeWithMethodInvoke(b, jt, "toString");
        } else if (c.equals(byte[].class)) {
            writeBase64BinaryAttribute(b, jt);  
        } else if (c.equals(QName.class)) {
            writeQNameAttribute(b, jt);  
        } else {
            System.out.println("Could not map! " + c);
        }
    }

    private void writePropertyWithMethodInvoke(WriterBuilder b, 
                                               JType t, 
                                               String method, 
                                               boolean nillable) {
        JBlock block = b.getCurrentBlock();

        JConditional cond = block._if(b.getObject().ne(JExpr._null()));
        JBlock nullBlock = cond._then();
        b.setCurrentBlock(nullBlock);
        JVar var = nullBlock.decl(model._ref(String.class), "_o", 
                                  b.getObject().invoke(method));
        
        if (nillable) {
            JConditional cond2 = cond._then()._if(var.ne(JExpr._null()));
            
            JBlock elseblock = cond._else();
            JBlock elseblock2 = cond2._else();
            
            b.setCurrentBlock(elseblock2);
            
            elseblock.add(b.getXSW().invoke("writeXsiNil"));
            elseblock2.add(b.getXSW().invoke("writeXsiNil"));
        }

        b.getCurrentBlock().add(b.getXSW().invoke("writeCharacters").arg(var));
        
        b.setCurrentBlock(block);
    }
    

    private void writeAttributeWithMethodInvoke(WriterBuilder b, 
                                                JType t, 
                                                String method) {
        JBlock block = b.getCurrentBlock();

        JConditional cond = block._if(b.getObject().ne(JExpr._null()));
        JBlock nullBlock = cond._then();
        b.setCurrentBlock(nullBlock);
        JVar var = nullBlock.decl(model._ref(String.class), 
                                  "_o", 
                                  b.getObject().invoke(method));
        
        JConditional cond2 = cond._then()._if(var.ne(JExpr._null()));
            
        QName name = b.getName();
        cond2._then().add(b.getXSW().invoke("writeAttribute")
                  .arg(JExpr.lit(name.getPrefix()))
                  .arg(JExpr.lit(name.getNamespaceURI()))
                  .arg(JExpr.lit(name.getLocalPart()))
                  .arg(var));
        
        b.setCurrentBlock(block);
    }
    private void writeBase64Binary(WriterBuilder b, 
                                   JType t, 
                                   boolean nillable) {
        JBlock block = b.getCurrentBlock();

        JConditional cond = block._if(b.getObject().ne(JExpr._null()));
        JBlock nullBlock = cond._then();
        b.setCurrentBlock(nullBlock);
        
        if (nillable) {
            JBlock elseblock = cond._else();
            b.setCurrentBlock(elseblock);
            
            elseblock.add(b.getXSW().invoke("writeXsiNil"));
        }
        
        JClass buType = (JClass) model._ref(BinaryUtils.class);
        b.getCurrentBlock().add(buType.staticInvoke("encodeBytes").arg(b.getXSW()).arg(b.getObject()));
        
        b.setCurrentBlock(block);
    }
    
    private void writeBase64BinaryAttribute(WriterBuilder b, 
                                            JType t) {
        JBlock block = b.getCurrentBlock();

        JConditional cond = block._if(b.getObject().ne(JExpr._null()));
        JBlock nullBlock = cond._then();
        b.setCurrentBlock(nullBlock);
        
        JClass buType = (JClass) model._ref(Base64.class);

        QName name = b.getName();
        nullBlock.add(b.getXSW().invoke("writeAttribute")
                          .arg(JExpr.lit(name.getPrefix()))
                          .arg(JExpr.lit(name.getNamespaceURI()))
                          .arg(JExpr.lit(name.getLocalPart()))
                          .arg(buType.staticInvoke("encode").arg(b.getObject())));
        
        b.setCurrentBlock(block);
    }
    
    private void writeQName(ElementWriterBuilder b,
                            JType t, 
                            boolean nillable) {
        JBlock block = b.getCurrentBlock();

        JConditional cond = block._if(b.getObject().ne(JExpr._null()));
        JBlock nullBlock = cond._then();
        b.setCurrentBlock(nullBlock);
        
        if (nillable) {
            b.writeNilIfNull();
            
            JBlock elseblock = cond._else();
            b.setCurrentBlock(elseblock);
            
            elseblock.add(b.getXSW().invoke("writeXsiNil"));
        }
        
        b.getCurrentBlock().add(b.getXSW().invoke("writeQName").arg(b.getObject()));
        
        b.setCurrentBlock(block);
    }
    
    private void writeQNameAttribute(WriterBuilder b, JType t) {
        JBlock block = b.getCurrentBlock();

        JConditional cond = block._if(b.getObject().ne(JExpr._null()));
        JBlock nullBlock = cond._then();
        b.setCurrentBlock(nullBlock);

        QName name = b.getName();
        nullBlock.add(b.getXSW().invoke("writeAttribute")
                          .arg(JExpr.lit(name.getPrefix()))
                          .arg(JExpr.lit(name.getNamespaceURI()))
                          .arg(JExpr.lit(name.getLocalPart()))
                          .arg(b.getXSW().invoke("getQNameAsString").arg(b.getObject())));
        
        b.setCurrentBlock(block);
    }
    
    private JType getType(Type t) {
        if (t instanceof Class) {
            return rootWriter.getCodeModel()._ref((Class)t);
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
        return rootWriter.getCodeModel()._ref(c);
    }

    public Builder getBuilder() {
        return builder;
    }

    public Map<Class, QName> getClassToType() {
        return c2type;
    }
}
