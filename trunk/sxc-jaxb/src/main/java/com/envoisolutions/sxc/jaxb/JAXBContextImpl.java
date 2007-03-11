package com.envoisolutions.sxc.jaxb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.Marshaller;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Validator;
import javax.xml.namespace.QName;

import com.envoisolutions.sxc.Context;
import com.envoisolutions.sxc.builder.BuildException;
import com.envoisolutions.sxc.builder.Builder;
import com.envoisolutions.sxc.builder.impl.BuilderImpl;
import com.sun.xml.bind.v2.ContextFactory;

import org.jvnet.jaxb.reflection.JAXBModelFactory;
import org.jvnet.jaxb.reflection.model.runtime.RuntimeTypeInfoSet;

public class JAXBContextImpl extends JAXBContext {
    
    private Context context;
    private Marshaller marshaller;
    private UnmarshallerImpl unmarshaller;
    private JAXBContext riContext;

    private static Map<Set<Class>, WeakReference<JAXBContextImpl>> contexts
         = new HashMap<Set<Class>, WeakReference<JAXBContextImpl>>();
    
    public static JAXBContext createContext( Class[] classes, Map<String, Object> properties ) throws JAXBException {
        try {
            Set<Class> clsSet = new HashSet<Class>();
            for (Class c : clsSet) {
                clsSet.add(c);
            }
            
            WeakReference<JAXBContextImpl> ctx = contexts.get(clsSet);
            if (ctx != null) {
                return ctx.get();
            }
            
            JAXBContextImpl c = new JAXBContextImpl(properties, classes);
            contexts.put(clsSet, new WeakReference<JAXBContextImpl>(c));
            return c;
        } catch (BuildException e) {
            throw new JAXBException("Could not compile parser/writer!", e);
        } catch (IOException e) {
            throw new JAXBException("Could not compile parser/writer!", e);
        }
    }
    
    public static JAXBContextImpl createContext(String contextPath, ClassLoader classLoader, Map<String, Object> properties)
        throws JAXBException, BuildException, IOException {
        Set<Class> classes = new HashSet<Class>();
        StringTokenizer tokens = new StringTokenizer(contextPath,":");
        List<Class> indexedClasses;

        // at least on of these must be true per package
        boolean foundObjectFactory;
        boolean foundJaxbIndex;

        while(tokens.hasMoreTokens()) {
            foundObjectFactory = foundJaxbIndex = false;
            String pkg = tokens.nextToken();

            // look for ObjectFactory and load it
            final Class<?> o;
            try {
                o = classLoader.loadClass(pkg+".ObjectFactory");
                classes.add(o);
                foundObjectFactory = true;
            } catch (ClassNotFoundException e) {
                // not necessarily an error
            }

            // look for jaxb.index and load the list of classes
            try {
                indexedClasses = loadIndexedClasses(pkg, classLoader);
            } catch (IOException e) {
                //TODO: think about this more
                throw new JAXBException(e);
            }
            if(indexedClasses != null) {
                classes.addAll(indexedClasses);
                foundJaxbIndex = true;
            }

            if( !(foundObjectFactory || foundJaxbIndex) ) {
                throw new JAXBException("Package must contain a jaxb.index or ObjectFactory: " + pkg);
            }
        }

        // Yeah, I have no idea what I'm doing here... need to look at javadocs
        // on weakreferences.
        WeakReference<JAXBContextImpl> ctx = contexts.get(classes);
        if (ctx != null) {
            JAXBContextImpl c = ctx.get();
            if (c != null) {
                return c;
            }
        }
        
        Class[] clsArray = classes.toArray(new Class[classes.size()]);
        JAXBContextImpl c = new JAXBContextImpl(clsArray, properties);
        contexts.put(classes, new WeakReference<JAXBContextImpl>(c));
        return c;
    }       
    
    public JAXBContextImpl(Class[] clsArray, Map<String, Object> properties) 
        throws JAXBException {
        this.riContext = ContextFactory.createContext(clsArray, properties);
        
        init(JAXBModelFactory.create(clsArray));
    }
    
    public JAXBContextImpl(Class... classes) throws JAXBException, BuildException, IOException {
        this(null, classes);
    }
    
    public JAXBContextImpl(Map<String, Object> properties, Class... classes) throws JAXBException, BuildException, IOException {
        this.riContext = ContextFactory.createContext(classes, properties);
        
        init(JAXBModelFactory.create(classes));
    }

    private final void init(RuntimeTypeInfoSet set) throws JAXBException {
        Builder builder = new BuilderImpl();
        
        new ReaderIntrospector(builder, set);
        WriterIntrospector wIntro = new WriterIntrospector(builder, set);
        Map<Class, QName> c2type = wIntro.getClassToType();
        context = builder.compile();
        marshaller = new MarshallerImpl(this, context);
        unmarshaller = new UnmarshallerImpl(this, c2type, context);
    }
    
    @Override
    public Marshaller createMarshaller() throws JAXBException {
        return marshaller;
    }

    @Override
    public Unmarshaller createUnmarshaller() throws JAXBException {
        return unmarshaller;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Validator createValidator() throws JAXBException {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public JAXBIntrospector createJAXBIntrospector() {
        return riContext.createJAXBIntrospector();
    }

    @Override
    public void generateSchema(SchemaOutputResolver arg0) throws IOException {
        riContext.generateSchema(arg0);
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
        final String resource = pkg.replace('.', '/') + "/jaxb.index";
        final InputStream resourceAsStream = classLoader.getResourceAsStream(resource);

        if (resourceAsStream == null) {
            return null;
        }

        BufferedReader in =
                new BufferedReader(new InputStreamReader(resourceAsStream, "UTF-8"));
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
