package com.envoisolutions.sxc.jaxb;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.JAXBException;

import com.envoisolutions.sxc.compiler.JavacCompiler;

public class JAXBGenerator {
    private final Map<String,Object> properties = new LinkedHashMap<String, Object>();
    private final Set<String> classes = new LinkedHashSet<String>();
    private String classesOutputDirectory;
    private ClassLoader classLoader;

    public JAXBGenerator() {
    }

    public JAXBGenerator(String... classes) {
        this.classes.addAll(Arrays.asList(classes));
    }

    public JAXBGenerator(Set<String> classes) {
        this.classes.addAll(classes);
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public Set<String> getClasses() {
        return classes;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public String getSourceOutputDirectory() {
        return (String) properties.get("com.envoisolutions.sxc.output.directory");
    }

    public void setSourceOutputDirectory(String sourceOutputDirectory) {
        properties.put("com.envoisolutions.sxc.output.directory", sourceOutputDirectory);
    }

    public String getClassesOutputDirectory() {
        return classesOutputDirectory;
    }

    public void setClassesOutputDirectory(String classesOutputDirectory) {
        this.classesOutputDirectory = classesOutputDirectory;
    }

    public void generate() throws JAXBException {
        ClassLoader classLoader = this.classLoader;
        if (classLoader == null) classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) classLoader = getClass().getClassLoader();

        Set<Class> classes = new LinkedHashSet<Class>();
        for (String className : this.classes) {
            try {
                Class<?> clazz = classLoader.loadClass(className);
                classes.add(clazz);
            } catch (ClassNotFoundException e) {
                try {
                    classes.addAll(Arrays.asList(JAXBContextImpl.loadPackageClasses(className, classLoader)));
                } catch (JAXBException e1) {
                    throw new JAXBException(className + " is not a class or a package containing a jaxb.index file or ObjectFactory class");
                }
            }
        }

        BuilderContext builderContext = new BuilderContext(properties, classes.toArray(new Class[classes.size()]));

        // generate the sources
        Map<String, File> sources = builderContext.getSources();

        // compile the generated code
        JavacCompiler javacCompiler = new JavacCompiler();
        javacCompiler.compile(sources, new File(classesOutputDirectory), classLoader);

    }
}
