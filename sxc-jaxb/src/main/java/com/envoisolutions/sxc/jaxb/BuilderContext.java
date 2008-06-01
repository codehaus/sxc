/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.envoisolutions.sxc.jaxb;

import java.io.IOException;
import java.io.File;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.concurrent.Callable;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import com.envoisolutions.sxc.builder.BuildException;
import com.envoisolutions.sxc.builder.impl.BuildContext;
import com.envoisolutions.sxc.builder.impl.CodeWriterImpl;
import com.envoisolutions.sxc.compiler.Compiler;
import static com.envoisolutions.sxc.jaxb.JavaUtils.toClass;
import com.envoisolutions.sxc.jaxb.model.Model;
import com.envoisolutions.sxc.jaxb.model.RiModelBuilder;
import com.sun.codemodel.CodeWriter;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JType;

public class BuilderContext {
    private BuildContext buildContext;
    private JCodeModel codeModel;
    private final Map<Class, JAXBObjectBuilder> jaxbObjectBuilders = new HashMap<Class, JAXBObjectBuilder>();
    private final Map<Class, JAXBEnumBuilder> jaxbEnumBuilders = new HashMap<Class, JAXBEnumBuilder>();
    private final Map<Class, JAXBObjectFactoryBuilder> jaxbObjectFactoryBuilders = new HashMap<Class, JAXBObjectFactoryBuilder>();
    private final Map<Class, JAXBClass> jaxbClasses = new HashMap<Class, JAXBClass>();
    private final Map<String, ?> properties;
    private Callable<JAXBContext> schemaGenerator;
    private Map<String, File> sources;

    public BuilderContext(Map<String, ?> properties, Class... classes) throws JAXBException {
        if (properties == null) properties = Collections.emptyMap();
        this.properties = properties;
        buildContext = new BuildContext();
        codeModel = buildContext.getCodeModel();
        buildContext.setUnmarshalContextClass(codeModel.ref(RuntimeContext.class));
        buildContext.setMarshalContextClass(codeModel.ref(RuntimeContext.class));

        RiModelBuilder modelBuilder = new RiModelBuilder(properties, classes);
        Model model = modelBuilder.getModel();
        schemaGenerator = modelBuilder.getContext();

        new ReaderIntrospector(this, model);
        new WriterIntrospector(this, model);
        new ObjectFactoryIntrospector(this, model);
    }

    public BuildContext getBuildContext() {
        return buildContext;
    }

    public JCodeModel getCodeModel() {
        return buildContext.getCodeModel();
    }

    public Callable<JAXBContext> getSchemaGenerator() {
        return schemaGenerator;
    }

    public JAXBObjectBuilder getJAXBObjectBuilder(Class type) {
        JAXBObjectBuilder builder = jaxbObjectBuilders.get(type);
        return builder;
    }

    public JAXBObjectBuilder createJAXBObjectBuilder(Class type, QName xmlRootElement, QName xmlType, boolean mixed) {
        JAXBObjectBuilder builder = jaxbObjectBuilders.get(type);
        if (builder == null) {
            builder = new JAXBObjectBuilder(this, type, xmlRootElement, xmlType, mixed);
            jaxbObjectBuilders.put(type, builder);
        }
        return builder;
    }

    public JAXBEnumBuilder getJAXBEnumBuilder(Class type) {
        JAXBEnumBuilder builder = jaxbEnumBuilders.get(type);
        return builder;
    }

    public JAXBEnumBuilder createJAXBEnumBuilder(Class type, QName xmlRootElement, QName xmlType) {
        JAXBEnumBuilder builder = jaxbEnumBuilders.get(type);
        if (builder == null) {
            builder = new JAXBEnumBuilder(this, type, xmlRootElement, xmlType);
            jaxbEnumBuilders.put(type, builder);
        }
        return builder;
    }

    public JAXBObjectFactoryBuilder getJAXBObjectFactoryBuilder(Class type) {
        JAXBObjectFactoryBuilder builder = jaxbObjectFactoryBuilders.get(type);
        return builder;
    }

    public JAXBObjectFactoryBuilder createJAXBObjectFactoryBuilder(Class type) {
        JAXBObjectFactoryBuilder builder = jaxbObjectFactoryBuilders.get(type);
        if (builder == null) {
            builder = new JAXBObjectFactoryBuilder(this, type);
            jaxbObjectFactoryBuilders.put(type, builder);
        }
        return builder;
    }

    public void write(CodeWriter codeWriter) throws IOException, BuildException {
        for (JAXBObjectBuilder builder : jaxbObjectBuilders.values()) {
            builder.write();
        }
        
        buildContext.getCodeModel().build(codeWriter);
    }

    public Collection<JAXBClass> compile() {
        if (!jaxbObjectBuilders.isEmpty() || !jaxbEnumBuilders.isEmpty() || !jaxbObjectFactoryBuilders.isEmpty()) {
            // compile the generated code
            Compiler compiler = Compiler.newInstance((String) properties.get("org.sxc.compiler"));
            ClassLoader classLoader = compiler.compile(getSources());

            // load the generated classes
            for (Class type : jaxbObjectBuilders.keySet()) {
                JAXBClass jaxbClass = JAXBIntrospectorImpl.loadJAXBClass(type, classLoader);
                if (jaxbClass != null) {
                    jaxbClasses.put(type, jaxbClass);
                }
            }
            for (Class type : jaxbEnumBuilders.keySet()) {
                JAXBClass jaxbClass = JAXBIntrospectorImpl.loadJAXBClass(type, classLoader);
                if (jaxbClass != null) {
                    jaxbClasses.put(type, jaxbClass);
                }
            }
            for (Class type : jaxbObjectFactoryBuilders.keySet()) {
                JAXBClass jaxbClass = JAXBIntrospectorImpl.loadJAXBClass(type, classLoader);
                if (jaxbClass != null) {
                    jaxbClasses.put(type, jaxbClass);
                }
            }

            // all generated so we can clear the generation state
            buildContext = null;
            codeModel = null;
            jaxbObjectBuilders.clear();
            jaxbEnumBuilders.clear();
            jaxbObjectFactoryBuilders.clear();
        }

        return jaxbClasses.values();
    }

    public Map<String, File> getSources() {
        // write generated to code to ouput dir
        if (sources == null) {
            CodeWriterImpl codeWriter;
            try {
                codeWriter = new CodeWriterImpl((String) properties.get("com.envoisolutions.sxc.output.directory"));
                write(codeWriter);
            } catch (IOException e) {
                throw new BuildException(e);
            }
            sources = codeWriter.getSources();
        }
        return sources;
    }

    public JClass toJClass(Class clazz) {
        if (clazz.isPrimitive()) {
            // Code model maps primitives to JPrimitiveType which not a JClass... use toJType instead
            throw new IllegalArgumentException("Internal Error: clazz is a primitive");
        }
        return codeModel.ref(clazz);
    }

    public JType toJType(Class<?> c) {
        return codeModel._ref(c);
    }

    public JClass getGenericType(Type type) {
        if (type instanceof Class) {
            Class clazz = toPrimitiveWrapper((Class) type);
            return codeModel.ref(clazz);
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            JClass raw = (JClass) toJType(toClass(pt.getRawType()));

            Type[] actualTypes = pt.getActualTypeArguments();
            List<JClass> types = new ArrayList<JClass>(actualTypes.length);
            for (Type actual : actualTypes) {
                if (actual instanceof WildcardType) {
                    WildcardType actualWildcard = (WildcardType) actual;
                    Type[] upperBounds = actualWildcard.getUpperBounds();
                    if (upperBounds.length > 0) {
                        types.add(getGenericType(upperBounds[0]).wildcard());
                    } else {
                        types.add(toJClass(Object.class));
                    }
                } else {
                    types.add(getGenericType(actual));
                }
            }
            raw = raw.narrow(types);

            return raw;
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

    public JExpression dotclass(Type type) {
        Class clazz = toClass(type);
        if (clazz.isPrimitive()) {
            return toJType(clazz).boxify().staticRef("TYPE");
        }
        return toJClass(clazz).dotclass();
    }
}