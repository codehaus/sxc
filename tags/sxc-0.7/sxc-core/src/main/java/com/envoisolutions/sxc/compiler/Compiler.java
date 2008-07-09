package com.envoisolutions.sxc.compiler;

import java.io.File;
import java.util.Map;

public abstract class Compiler {
    public static String JAVAC = "javac";
    public static String ECLIPSE = "eclipse";

    public static Compiler newInstance() {
        return newInstance(null);
    }
    
    public static Compiler newInstance(String compiler) {
        if (JAVAC.equalsIgnoreCase(compiler)) {
            return new JavacCompiler();
        } else if (ECLIPSE.equalsIgnoreCase(compiler)) {
            return createEclipseCompiler();
        } else if (compiler != null) {
            throw new IllegalArgumentException("Unknown compiler " + compiler);
        } else {
            try {
                // check if eclipse classes are available
                Compiler.class.getClassLoader().loadClass("org.eclipse.jdt.internal.compiler.Compiler");
                return createEclipseCompiler();
            } catch (ClassNotFoundException e) {
                return new JavacCompiler();
            }
        }
    }

    private static Compiler createEclipseCompiler() {
        try {
            ClassLoader loader = Compiler.class.getClassLoader();
            Class<?> ecliplseCompiler = loader.loadClass("com.envoisolutions.sxc.compiler.EclipseCompiler");
            return (Compiler) ecliplseCompiler.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not create eclipse compiler");
        }
    }

    public abstract ClassLoader compile(Map<String, File> sources);
}
