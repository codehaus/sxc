package com.envoisolutions.sxc.jaxb;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;

import com.envoisolutions.sxc.builder.BuildException;
import com.envoisolutions.sxc.builder.impl.BuildContext;
import com.envoisolutions.sxc.compiler.Compiler;
import com.envoisolutions.sxc.compiler.EclipseCompiler;
import static com.envoisolutions.sxc.jaxb.JavaUtils.toClass;
import com.envoisolutions.sxc.util.Util;
import com.sun.codemodel.CodeWriter;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JType;
import com.sun.codemodel.writer.FileCodeWriter;

public class BuilderContext {
    private File file;
    private BuildContext buildContext;
    private JCodeModel codeModel;
    private final Map<Class, MarshallerBuilder> marshallers = new HashMap<Class, MarshallerBuilder>();
    private Compiler compiler = new EclipseCompiler();

    public BuilderContext() {
        this.buildContext = new BuildContext();
        codeModel = buildContext.getCodeModel();
    }

    public BuildContext getBuildContext() {
        return buildContext;
    }

    public JCodeModel getCodeModel() {
        return buildContext.getCodeModel();
    }

    public MarshallerBuilder getMarshallerBuilder(Class type, QName xmlRootElement, QName xmlType) {
        MarshallerBuilder builder = marshallers.get(type);
        if (builder == null) {
            builder = new MarshallerBuilder(this, type, xmlRootElement, xmlType);
            marshallers.put(type, builder);
        }
        return builder;
    }

    public void write(File dir) throws IOException, BuildException {
        this.file = dir;

        // file = new File(file, new Long(System.currentTimeMillis()).toString());
        file.mkdirs();

        write(new FileCodeWriter(file));
    }

    public void write(CodeWriter codeWriter) throws IOException, BuildException {
        for (MarshallerBuilder builder : marshallers.values()) {
            builder.getParserBuilder().write();
            builder.getWriterBuilder().write();
        }
        
        buildContext.getCodeModel().build(codeWriter);
    }

    public ClassLoader compile() {
        boolean delete = true;
        File dir = null;
        if (file == null) {
            try {
                String cdir = System.getProperty("com.envoisolutions.sxc.output.directory");

                if (cdir == null) {
                    dir = File.createTempFile("compile", "");
                } else {
                    dir = new File(cdir);
                    delete = false;
                }

                dir.delete();

                dir.mkdirs();
                write(dir);


            } catch (IOException e) {
                throw new BuildException(e);
            }
        }

        ClassLoader classLoader = compiler.compile(file);

        // Only delete if the output directory hasn't been set
        if (delete && file == null) {
            Util.delete(dir);
        }

        return classLoader;
    }

    public Compiler getCompiler() {
        return compiler;
    }

    public void setCompiler(Compiler compiler) {
        this.compiler = compiler;
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

}