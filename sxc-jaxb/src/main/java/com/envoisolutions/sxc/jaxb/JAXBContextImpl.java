package com.envoisolutions.sxc.jaxb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.Marshaller;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import com.envoisolutions.sxc.builder.BuildException;
import com.envoisolutions.sxc.jaxb.model.Bean;
import com.envoisolutions.sxc.jaxb.model.Model;
import com.envoisolutions.sxc.jaxb.model.RiModelBuilder;

public class JAXBContextImpl extends JAXBContext {
    private static final Logger logger = Logger.getLogger(JAXBContextImpl.class.getName());
    private static Map<Set<String>, WeakReference<JAXBContextImpl>> contexts = new HashMap<Set<String>, WeakReference<JAXBContextImpl>>();

    public static synchronized JAXBContextImpl createContext(Class[] classes, Map<String, Object> properties) throws JAXBException {
        try {
            Set<String> classNames = new TreeSet<String>();
            for (Class c : classes) {
                classNames.add(c.getName());
            }

            WeakReference<JAXBContextImpl> contextRef = contexts.get(classNames);
            if (contextRef != null) {
                JAXBContextImpl jaxbContext = contextRef.get();
                if (jaxbContext != null) {
                    return jaxbContext;
                }
                contexts.remove(classNames);
            }

            JAXBContextImpl jaxbContext = new JAXBContextImpl(properties, classes);
            contexts.put(classNames, new WeakReference<JAXBContextImpl>(jaxbContext));

            return jaxbContext;
        } catch (BuildException e) {
            throw new JAXBException("Could not compile parser/writer!", e);
        }
    }

    public static synchronized JAXBContextImpl createContext(String contextPath, ClassLoader classLoader, Map<String, Object> properties) throws JAXBException, BuildException, IOException {
        Set<Class> classes = new HashSet<Class>();
        for (String pkg : contextPath.split(":")) {
            // look for ObjectFactory and load it
            Class<?> objectFactory;
            try {
                objectFactory = classLoader.loadClass(pkg + ".ObjectFactory");
                classes.add(objectFactory);
                continue;
            } catch (ClassNotFoundException e) {
                // not necessarily an error
            }

            // look for jaxb.index and load the list of classes
            try {
                List<Class> indexedClasses = loadIndexedClasses(pkg, classLoader);

                if (indexedClasses == null) {
                    throw new JAXBException("Package must contain a jaxb.index file or ObjectFactory class: " + pkg);
                }

                classes.addAll(indexedClasses);
            } catch (IOException e) {
                throw new JAXBException(e);
            }
        }

        JAXBContextImpl jaxbContext = createContext(classes.toArray(new Class[classes.size()]), properties);
        return jaxbContext;
    }

    private final Model model;
    private final ClassLoader generatedCL;
    private final Callable<JAXBContext> schemaGenerator;

    public JAXBContextImpl(Class[] classes, Map<String, Object> properties) throws JAXBException {
        this(properties, classes);
    }
    
    public JAXBContextImpl(Class... classes) throws JAXBException, BuildException, IOException {
        this(null, classes);
    }
    
    public JAXBContextImpl(Map<String, Object> properties, Class... classes) throws JAXBException {
        RiModelBuilder modelBuilder = new RiModelBuilder(properties, classes);
        model = modelBuilder.getModel();
        schemaGenerator = modelBuilder.getContext();

        BuilderContext builder = new BuilderContext();
        new ReaderIntrospector(builder, model);
        new WriterIntrospector(builder, model);

        generatedCL = builder.compile();

        logger.info("Created SXC JAXB Context.");
    }
    
    @Override
    public Marshaller createMarshaller() throws JAXBException {
        return new MarshallerImpl(model, generatedCL);
    }

    @Override
    public Unmarshaller createUnmarshaller() throws JAXBException {
        return new UnmarshallerImpl(model, generatedCL);
    }

    @SuppressWarnings("deprecation")
    @Override
    public javax.xml.bind.Validator createValidator() throws JAXBException {
        throw new UnsupportedOperationException();
    }

    @Override
    public JAXBIntrospector createJAXBIntrospector() {
        return new JAXBIntrospector() {
            public boolean isElement(Object jaxbElement) {
                return getElementName(jaxbElement) != null;
            }

            public QName getElementName(Object jaxbElement) {
                if (jaxbElement instanceof JAXBElement) {
                    JAXBElement element = (JAXBElement) jaxbElement;
                    return element.getName();
                }
                Bean bean = model.getBean(jaxbElement.getClass());
                QName elementName = null;
                if (bean != null) {
                    elementName = bean.getRootElementName();
                }
                return elementName;
            }
        };
    }

    @Override
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

    /**
     * Look for jaxb.index file in the specified package and load it's contents
     *
     * @param pkg package name to search in
     * @param classLoader ClassLoader to search in
     * @return a List of Class objects to load, null if there weren't any
     * @throws IOException if there is an error reading the index file
     * @throws JAXBException if there are any errors in the index file
     */
    private static List<Class> loadIndexedClasses(String pkg, ClassLoader classLoader) throws IOException, JAXBException {
        String resource = pkg.replace('.', '/') + "/jaxb.index";
        InputStream resourceAsStream = classLoader.getResourceAsStream(resource);

        if (resourceAsStream == null) {
            return null;
        }

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
    }
}
