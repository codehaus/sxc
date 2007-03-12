package com.envoisolutions.sxc.jaxb;

import java.lang.reflect.Method;
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

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.envoisolutions.sxc.builder.BuildException;
import com.envoisolutions.sxc.builder.Builder;
import com.envoisolutions.sxc.builder.CodeBody;
import com.envoisolutions.sxc.builder.ElementParserBuilder;
import com.envoisolutions.sxc.builder.ElementWriterBuilder;
import com.envoisolutions.sxc.builder.WriterBuilder;
import com.envoisolutions.sxc.util.Base64;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JForEach;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import com.sun.xml.bind.v2.model.core.Adapter;
import com.sun.xml.bind.v2.model.runtime.RuntimeAttributePropertyInfo;
import com.sun.xml.bind.v2.model.runtime.RuntimeClassInfo;
import com.sun.xml.bind.v2.model.runtime.RuntimeElementInfo;
import com.sun.xml.bind.v2.model.runtime.RuntimeElementPropertyInfo;
import com.sun.xml.bind.v2.model.runtime.RuntimeEnumLeafInfo;
import com.sun.xml.bind.v2.model.runtime.RuntimeNonElement;
import com.sun.xml.bind.v2.model.runtime.RuntimePropertyInfo;
import com.sun.xml.bind.v2.model.runtime.RuntimeTypeInfoSet;
import com.sun.xml.bind.v2.model.runtime.RuntimeTypeRef;
import com.sun.xml.bind.v2.model.runtime.RuntimeValuePropertyInfo;

public class WriterIntrospector {
    private ElementWriterBuilder rootWriter;
    private Set<Class<?>> addedXsiTypes = new HashSet<Class<?>>();
    private Builder builder;
    private JCodeModel model;
    private Map<Class, QName> c2type = new HashMap<Class, QName>();    
    private Map<BuilderKey, ElementWriterBuilder> type2Writer 
        = new HashMap<BuilderKey, ElementWriterBuilder>();
    
    private Map<Class, JVar> adapters = new HashMap<Class, JVar>();
    private int adapterCount = 0;
    
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
            writeSimpleTypeElement(instcWriter, rei.getContentType(), null, true, c, c, jt);

            c2type.put(c, q);
        }
        
        for (Map.Entry<Class, ? extends RuntimeEnumLeafInfo> e : set.enums().entrySet()) {
            Class c = e.getKey();
            RuntimeEnumLeafInfo info = e.getValue();
            JType jt = model._ref(c);
            
            ElementWriterBuilder instcWriter = 
                rootWriter.newCondition(rootWriter.getObject()._instanceof(jt), jt);
            writeSimpleTypeElement(instcWriter, info, null, true, c, c, jt);
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
                if (rawType instanceof Class) { 
                    c = (Class) rawType;
                }
                
                JType jt = getType(rawType);
                
                String propName = JaxbUtil.getGetter(parentClass, propAt.getName(), rawType);
                
                WriterBuilder atBuilder = 
                    classBuilder.writeAttribute(propAt.getXmlName(), 
                                            jt, 
                                            classBuilder.getObject().invoke(propName));
                
                if (propAt.isCollection()) {
                    System.err.println("Writer: attribute lists are not supported yet!");
                } else {
                    writeSimpleTypeAttribute(atBuilder, rawType, c, jt);
                }
            } else if (prop instanceof RuntimeValuePropertyInfo) {
                RuntimeValuePropertyInfo propv = (RuntimeValuePropertyInfo) prop;
                
                Type rawType = propv.getRawType();
                Class c = (Class) propv.getTarget().getType();
                JType jt = getType(rawType);

                JVar var = classBuilder.getCurrentBlock().decl(jt, 
                                                               prop.getName(),
                                                               classBuilder.getObject().invoke("getValue"));
                
                writeSimpleTypeElement(classBuilder, 
                                       propv.getTarget(), 
                                       propv.getAdapter(),
                                       var, true, rawType, c, jt);
            } else {
                System.err.println("Writer: Unknown Property " + prop.getName() 
                                   + " with type " + prop.getRawType()
                                   + " on " + parentClass
                                   + " for " + prop.getClass().getName());
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
        JBlock origBlock = block;

        addType(c, target.getTypeName());

        String propName = JaxbUtil.getGetter(parentClass, propEl.getName(), rawType);
        
        JVar var = block.decl(rawJT, 
                              propEl.getName() + "_" + javify(name.getLocalPart()), 
                              b.getObject().invoke(propName));

        
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
            JForEach each = block.forEach(getGenericType(rawType), "_o", var);
            JBlock newBody = each.body();
            b.setCurrentBlock(newBody);
            var = each.var();
            
            rawType = c;
            rawJT = jt;
        }

        ElementWriterBuilder valueBuilder = b.writeElement(name, rawJT, var);
        
        if (target.isSimpleType()) {
            if (rawType instanceof Class) {
                c = (Class) rawType;
            }
            writeSimpleTypeElement(valueBuilder, 
                                   target,
                                   propEl.getAdapter(),
                                   typeRef.isNillable(), 
                                   rawType, c, jt);
        } else if (target instanceof RuntimeClassInfo) {
            RuntimeClassInfo rci = (RuntimeClassInfo)target;

            BuilderKey key = new BuilderKey();
            key.parentClass = parentClass;
            key.type = rci.getTypeName();
            
            if (key.type == null || !type2Writer.containsKey(key)) {
                if (typeRef.isNillable()) {
                    valueBuilder.writeNilIfNull();
                }
                
                if (propEl.parent() != null) {
                    valueBuilder.moveTo(rootWriter);
                } else {
                    type2Writer.put(key, valueBuilder);
                    add(valueBuilder, name, c, rci, false);
                }
            } else {
                    WriterBuilder builder2 = type2Writer.get(key);
                    
                    valueBuilder.moveTo(builder2);
            }
        } else {
            System.err.println("Writer: Unknown type " + c);
        }
        
        if (propEl.isCollection()) {
            b.setCurrentBlock(block);
        }
        
        b.setCurrentBlock(origBlock);
    }

    private String javify(String localPart) {
        return JavaUtils.makeNonJavaKeyword(localPart);
    }

    private void writeSimpleTypeElement(ElementWriterBuilder b, 
                                        RuntimeNonElement target, 
                                        Adapter<Type, Class> adapter,
                                        boolean nillable,
                                        Type rawType, 
                                        Class c, 
                                        JType jt) {
        writeSimpleTypeElement(b, target, adapter, b.getObject(), nillable, rawType, c, jt);
    }
    
    private void writeSimpleTypeElement(ElementWriterBuilder b, 
                                        RuntimeNonElement target, 
                                        Adapter<Type, Class> adapter,
                                        JVar object,
                                        boolean nillable,
                                        Type rawType, 
                                        Class c, 
                                        JType jt) {
        
        if (adapter != null) {
            JVar adapterVar = getAdapter(adapter);
            JBlock block = b.getCurrentBlock();
            JVar valueVar = block.decl(model.ref(String.class), "value", adapterVar.invoke("marshal").arg(object));
            
            JBlock writeNil = block._if(object.eq(JExpr._null()))._then();
            if (nillable) {
                writeNil.add(b.getXSW().invoke("writeXsiNil"));
            }
            writeNil._return();
            
            block.add(b.getXSW().invoke("writeCharacters").arg(valueVar));
        } else if(c.equals(String.class) 
            || c.equals(Integer.class)
            || c.equals(Double.class)
            || c.equals(Boolean.class)
            || c.equals(Float.class)
            || c.equals(Short.class)
            || c.equals(Long.class)
            || c.equals(Byte.class)
            || c.isPrimitive()) {
            JVar orig = b.getObject();
            b.setObject(object);
            b.writeAs(c, nillable);
            b.setObject(orig);
        } else if (c.equals(XMLGregorianCalendar.class)) {
            writePropertyWithMethodInvoke(b, object, jt, "toXMLFormat", nillable);
        } else if (c.isEnum()) {
            try {
                Method method = c.getMethod("value", new Class[0]);
                
                Class<?> readAs = method.getReturnType();
                JType valueType = model._ref(readAs);
                
                JBlock block = b.getCurrentBlock();
                JBlock writeNil = block._if(object.eq(JExpr._null()))._then();
                if (nillable) {
                    writeNil.add(b.getXSW().invoke("writeXsiNil"));
                }
                writeNil._return();
                
                JVar valueVar = block.decl(valueType, "_enum", object.invoke("value"));
                
                writeSimpleTypeElement(b, target, adapter, valueVar, nillable, readAs, readAs, valueType);
            } catch (Exception e) {
                throw new BuildException(e);
            }
        } else if (c.equals(BigInteger.class) || c.equals(BigDecimal.class) || c.equals(Duration.class)) {
            writePropertyWithMethodInvoke(b, object, jt, "toString", nillable);
        } else if (c.equals(byte[].class)) {
            writeBase64Binary(b, object, jt, nillable);  
        } else if (c.equals(QName.class)) {
            writeQName(b, object, jt, nillable);  
        } else if (target instanceof RuntimeClassInfo) {
//            RuntimeClassInfo rci = (RuntimeClassInfo) target;
//            
//            writeProperties(rci, c, b);
            b.moveTo(rootWriter);
        } else {
            System.err.println("Writer: Could not map! " + c);
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
            || c.equals(Byte.class)
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
            System.err.println("Writer: Could not map! " + c);
        }
    }

    private void writePropertyWithMethodInvoke(WriterBuilder b, 
                                               JVar object,
                                               JType t, 
                                               String method, 
                                               boolean nillable) {
        JBlock block = b.getCurrentBlock();

        JConditional cond = block._if(object.ne(JExpr._null()));
        JBlock nullBlock = cond._then();
        b.setCurrentBlock(nullBlock);
        JVar var = nullBlock.decl(model._ref(String.class), "_o", 
                                  object.invoke(method));
        
        if (nillable) {
            JConditional cond2 = cond._then()._if(var.ne(JExpr._null()));
            
            JBlock elseblock = cond._else();
            JBlock elseblock2 = cond2._else();
            
            b.setCurrentBlock(cond2._then());
            
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
                                   JVar var,
                                   JType t, 
                                   boolean nillable) {
        JBlock block = b.getCurrentBlock();

        JConditional cond = block._if(var.ne(JExpr._null()));
        JBlock nullBlock = cond._then();
        b.setCurrentBlock(nullBlock);
        
        if (nillable) {
            b.setCurrentBlock(cond._then());
            
            cond._else().add(b.getXSW().invoke("writeXsiNil"));
        }
        
        JClass buType = (JClass) model._ref(BinaryUtils.class);
        b.getCurrentBlock().add(buType.staticInvoke("encodeBytes").arg(b.getXSW()).arg(var));
        
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
                            JVar var,
                            JType t, 
                            boolean nillable) {
        JBlock block = b.getCurrentBlock();

        JConditional cond = block._if(var.ne(JExpr._null()));
        JBlock nullBlock = cond._then();
        b.setCurrentBlock(nullBlock);
        
        if (nillable) {
            b.writeNilIfNull();
            
            JBlock elseblock = cond._else();
            b.setCurrentBlock(elseblock);
            
            elseblock.add(b.getXSW().invoke("writeXsiNil"));
        }
        
        b.getCurrentBlock().add(b.getXSW().invoke("writeQName").arg(var));
        
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
    
    private JVar getAdapter(Adapter<Type, Class> adapter) {
        Class c = adapter.adapterType;
        JVar var = adapters.get(c);
        if (var == null) {
            JType jt = model._ref(c);
            var = rootWriter.getWriterClass().field(JMod.STATIC, jt, "adapter" + adapterCount++,
                                              JExpr._new(jt));
            adapters.put(c, var);
        }
        return var;
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
    
    private JType getGenericType(Type t) {
        if (t instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) t;

            Type[] actualTypes = pt.getActualTypeArguments();
            for (Type actual : actualTypes) {
                return getType(actual);
            }
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
