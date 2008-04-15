package com.envoisolutions.sxc.jaxb;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import com.envoisolutions.sxc.builder.BuildException;
import com.envoisolutions.sxc.builder.impl.BuildContext;
import com.envoisolutions.sxc.compiler.Compiler;
import com.envoisolutions.sxc.compiler.EclipseCompiler;
import static com.envoisolutions.sxc.jaxb.JavaUtils.toClass;
import com.envoisolutions.sxc.jaxb.model.Model;
import com.envoisolutions.sxc.jaxb.model.RiModelBuilder;
import com.sun.codemodel.CodeWriter;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JType;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.writer.FileCodeWriter;

public class BuilderContext {
    private BuildContext buildContext;
    private JCodeModel codeModel;
    private final Map<Class, MarshallerBuilder> marshallerBuilders = new HashMap<Class, MarshallerBuilder>();
    private final Map<Class, JAXBMarshaller> marshallers = new HashMap<Class, JAXBMarshaller>();
    private Callable<JAXBContext> schemaGenerator;

    public BuilderContext(Map<String, Object> properties, Class... classes) throws JAXBException {
        buildContext = new BuildContext();
        codeModel = buildContext.getCodeModel();
        buildContext.setUnmarshalContextClass(codeModel.ref(RuntimeContext.class));
        buildContext.setMarshalContextClass(codeModel.ref(RuntimeContext.class));

        RiModelBuilder modelBuilder = new RiModelBuilder(properties, classes);
        Model model = modelBuilder.getModel();
        schemaGenerator = modelBuilder.getContext();

        new ReaderIntrospector(this, model);
        new WriterIntrospector(this, model);
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

    public MarshallerBuilder getMarshallerBuilder(Class type, QName xmlRootElement, QName xmlType) {
        MarshallerBuilder builder = marshallerBuilders.get(type);
        if (builder == null) {
            builder = new MarshallerBuilder(this, type, xmlRootElement, xmlType);
            marshallerBuilders.put(type, builder);
        }
        return builder;
    }

    public void write(CodeWriter codeWriter) throws IOException, BuildException {
        for (MarshallerBuilder builder : marshallerBuilders.values()) {
            builder.write();
        }
        
        buildContext.getCodeModel().build(codeWriter);
    }

    public Collection<JAXBMarshaller> compile() {
        if (!marshallerBuilders.isEmpty()) {
            // write generated to code to ouput dir
            File dir;
            try {
                String outputDir = System.getProperty("com.envoisolutions.sxc.output.directory");

                if (outputDir == null) {
                    dir = File.createTempFile("compile", "");
                    dir.delete();
                } else {
                    dir = new File(outputDir);
                }
                dir.mkdirs();

                write(new FileCodeWriter(dir));
            } catch (IOException e) {
                throw new BuildException(e);
            }

            // compile the generated code
            Compiler compiler = new EclipseCompiler();
            ClassLoader classLoader = compiler.compile(dir);

            // load the generated classes
            for (Class type : marshallerBuilders.keySet()) {
                JAXBMarshaller marshaller = JAXBIntrospectorImpl.loadJAXBMarshaller(type, classLoader);
                if (marshaller != null) {
                    marshallers.put(type, marshaller);
                }
            }

            // all generated so we can clear the generation state
            buildContext = null;
            codeModel = null;
            marshallerBuilders.clear();
        }

        return marshallers.values();
    }

    public JClass toJClass(Class clazz) {
        if (clazz.isPrimitive()) {
            // Code model maps primitives to JPrimitiveType which not a JClass... use toJType instead
            throw new IllegalArgumentException("Internal Error: clazz is a primitive");
        }
        return codeModel.ref(clazz);
    }

    private JType toJType(Class<?> c) {
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

    public JExpression dotclass(Type type) {
        Class clazz = toClass(type);
        if (clazz.isPrimitive()) {
            return toJType(clazz).boxify().staticRef("TYPE");
        }
        return toJClass(clazz).dotclass();
    }
}