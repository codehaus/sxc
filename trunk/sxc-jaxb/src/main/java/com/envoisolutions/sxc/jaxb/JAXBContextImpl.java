package com.envoisolutions.sxc.jaxb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.Marshaller;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.Unmarshaller;

import com.sun.xml.bind.v2.ContextFactory;

public class JAXBContextImpl extends JAXBContext {
    private static final Logger logger = Logger.getLogger(JAXBContextImpl.class.getName());

    public static synchronized JAXBContextImpl newInstance(Class... classes) throws JAXBException {
        JAXBContextImpl jaxbContext = createContext(classes, Collections.<String, Object>emptyMap());
        return jaxbContext;
    }

    public static synchronized JAXBContextImpl newInstance(Class[] classes, Map<String, Object> properties) throws JAXBException {
        JAXBContextImpl jaxbContext = createContext(classes, properties);
        return jaxbContext;
    }

    public static synchronized JAXBContextImpl createContext(Class[] classes, Map<String, Object> properties) throws JAXBException {
        JAXBContextImpl jaxbContext = new JAXBContextImpl(properties, classes);
        return jaxbContext;
    }

    public static JAXBContext newInstance(String contextPath) throws JAXBException {
        JAXBContextImpl jaxbContext = createContext(contextPath, Thread.currentThread().getContextClassLoader(), Collections.<String, Object>emptyMap());
        return jaxbContext;
    }

    public static JAXBContext newInstance(String contextPath, ClassLoader classLoader) throws JAXBException {
        JAXBContextImpl jaxbContext = createContext(contextPath, classLoader, Collections.<String, Object>emptyMap());
        return jaxbContext;
    }

    public static synchronized JAXBContextImpl newInstance(String contextPath, ClassLoader classLoader, Map<String, Object> properties) throws JAXBException {
        JAXBContextImpl jaxbContext = createContext(contextPath, classLoader, properties);
        return jaxbContext;
    }

    public static synchronized JAXBContextImpl createContext(String contextPath, ClassLoader classLoader, Map<String, Object> properties) throws JAXBException {
        Class[] classes = loadPackageClasses(contextPath, classLoader);
        JAXBContextImpl jaxbContext = createContext(classes, properties);
        return jaxbContext;
    }

    private final JAXBIntrospectorImpl introspector = new JAXBIntrospectorImpl();
    private final Callable<JAXBContext> schemaGenerator;

    public JAXBContextImpl(Class... classes) throws JAXBException {
        this(null, classes);
    }
    
    public JAXBContextImpl(final Map<String, Object> properties, final Class... classes) throws JAXBException {
        // Check if there is a generted marshaller for the specified types
        //
        // It is important that this simple check be performed without
        // checking for annotations on the class, becuase even checking an
        // annotation is present is an expensive operation
        LinkedList<Class> unknownTypes = new LinkedList<Class>();
        for (Class xmlType : classes) {
            JAXBClass jaxbClass = JAXBIntrospectorImpl.loadJAXBClass(xmlType, null);
            if (jaxbClass != null) {
                introspector.addJAXBClass(jaxbClass);
            } else {
                unknownTypes.add(xmlType);
            }
        }

        // generate missing classes
        if (!unknownTypes.isEmpty()) {
            BuilderContext builder = new BuilderContext(properties, classes);
            schemaGenerator = builder.getSchemaGenerator();
            for (JAXBClass jaxbClass : builder.compile()) {
                introspector.addJAXBClass(jaxbClass);
            }
        } else {
            schemaGenerator = new Callable<JAXBContext>() {
                public JAXBContext call() throws Exception {
                    // use the ri to generate the schema
                    JAXBContext context = ContextFactory.createContext(classes, properties);
                    return context;
                }
            };
        }

        logger.info("Created SXC JAXB Context.");
    }
    
    public Marshaller createMarshaller() throws JAXBException {
        return new MarshallerImpl(introspector);
    }

    public Unmarshaller createUnmarshaller() throws JAXBException {
        return new UnmarshallerImpl(introspector);
    }

    public JAXBIntrospector createJAXBIntrospector() {
        return introspector;
    }

    public void generateSchema(SchemaOutputResolver outputResolver) throws IOException {
        JAXBContext jaxbContext = null;
        try {
            jaxbContext = schemaGenerator.call();
        } catch (Exception e) {
        }

        if (jaxbContext == null) {
            throw new UnsupportedOperationException("Schema generation is not supported");
        }
        jaxbContext.generateSchema(outputResolver);
    }

    @SuppressWarnings("deprecation")
    public javax.xml.bind.Validator createValidator() throws JAXBException {
        throw new UnsupportedOperationException();
    }

    public static Class[] loadPackageClasses(String contextPath, ClassLoader classLoader) throws JAXBException {
        Set<Class> classes = new HashSet<Class>();
        for (String pkg : contextPath.split(":")) {
            // look for ObjectFactory and load it
            Class objectFactoryClass = loadObjectFactory(pkg, classLoader);
            if (objectFactoryClass != null) classes.add(objectFactoryClass);

            // look for jaxb.index and load the list of classes
            List<Class> indexedClasses = loadIndexedClasses(pkg, classLoader);
            if (indexedClasses != null) classes.addAll(indexedClasses);

            if (objectFactoryClass == null && indexedClasses == null) {
                throw new JAXBException("Package must contain a jaxb.index file or ObjectFactory class: " + pkg);
            }
        }

        return classes.toArray(new Class[classes.size()]);
    }

    /**
     * Look for jaxb.index file in the specified package and load it's contents
     *
     * @param pkg package name to search
     * @param classLoader class loader used to find the jaxb.index resouce
     * @return a list of JAXB types in the package
     * @throws JAXBException if there are any errors in the index file
     */
    private static List<Class> loadIndexedClasses(String pkg, ClassLoader classLoader) throws JAXBException {
        String resource = pkg.replace('.', '/') + "/jaxb.index";
        InputStream resourceAsStream = classLoader.getResourceAsStream(resource);

        if (resourceAsStream == null) {
            return null;
        }

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(resourceAsStream, "UTF-8"));
            try {
                ArrayList<Class> classes = new ArrayList<Class>();
                String className = in.readLine();
                while (className != null) {
                    className = className.trim();
                    if (className.startsWith("#") || (className.length() == 0)) {
                        className = in.readLine();
                        continue;
                    }

                    if (className.endsWith(".class")) {
                        throw new JAXBException("Illegal entry: " + className);
                    }

                    try {
                        classes.add(classLoader.loadClass(pkg + '.' + className));
                    } catch (ClassNotFoundException e) {
                        throw new JAXBException("Error loading class: " + className, e);
                    }

                    className = in.readLine();
                }
                return classes;
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new JAXBException("Error loading jaxb.index file for " + pkg, e);
        }
    }

    private static Class loadObjectFactory(String pkg, ClassLoader classLoader) throws JAXBException {
        try {
            Class<?> objectFactory = classLoader.loadClass(pkg + ".ObjectFactory");
            return objectFactory;
        } catch (ClassNotFoundException e) {
            // not necessarily an error
            return null;
        }
    }
}
