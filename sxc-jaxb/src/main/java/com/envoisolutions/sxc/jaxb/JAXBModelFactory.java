package com.envoisolutions.sxc.jaxb;

import java.lang.reflect.Type;
import java.util.HashMap;

import com.sun.xml.bind.v2.model.annotation.RuntimeAnnotationReader;
import com.sun.xml.bind.v2.model.annotation.RuntimeInlineAnnotationReader;
import com.sun.xml.bind.v2.model.core.ErrorHandler;
import com.sun.xml.bind.v2.model.core.Ref;
import com.sun.xml.bind.v2.model.impl.RuntimeModelBuilder;
import com.sun.xml.bind.v2.model.runtime.RuntimeTypeInfoSet;
import com.sun.xml.bind.v2.runtime.IllegalAnnotationsException;
import com.sun.xml.bind.v2.runtime.JAXBContextImpl;

/**
 * Factory methods to build JAXB models.
 * 
 * @author Kohsuke Kawaguchi
 */
// this is a facade to ModelBuilder
public abstract class JAXBModelFactory {
    private JAXBModelFactory() {
    } // no instanciation please

    /**
     * Creates a new JAXB model from classes represented in
     * <tt>java.lang.reflect</tt>.
     * 
     * @param reader used to read annotations from classes. must not be null.
     * @param errorHandler Receives errors found during the processing.
     * @return null if any error was reported during the processing. If no error
     *         is reported, a non-null valid object.
     */
    public static RuntimeTypeInfoSet create(JAXBContextImpl context, 
                                            RuntimeAnnotationReader reader, ErrorHandler errorHandler,
                                            Class... classes) {

        RuntimeModelBuilder builder = new RuntimeModelBuilder(context, reader, new HashMap<Class, Class>(), null);
        builder.setErrorHandler(errorHandler);
        for (Class c : classes)
            builder.getTypeInfo(new Ref<Type, Class>(c));

        return builder.link();
    }

    /**
     * Creates a new JAXB model from classes represented in
     * <tt>java.lang.reflect</tt>.
     * <p>
     * This version reads annotations from the classes directly.
     * 
     * @param errorHandler Receives errors found during the processing.
     * @return null if any error was reported during the processing. If no error
     *         is reported, a non-null valid object.
     */
    public static RuntimeTypeInfoSet create(JAXBContextImpl context, ErrorHandler errorHandler, Class... classes) {

        return create(context, new RuntimeInlineAnnotationReader(), errorHandler, classes);
    }

    /**
     * Creates a new JAXB model from classes represented in
     * <tt>java.lang.reflect</tt>.
     * <p>
     * This version reads annotations from the classes directly, and throw any
     * error reported as an exception
     * 
     * @return null if any error was reported during the processing. If no error
     *         is reported, a non-null valid object.
     * @throws IllegalAnnotationsException if there was any incorrect use of
     *             annotations in the specified set of classes.
     */
    public static RuntimeTypeInfoSet create(JAXBContextImpl context, Class... classes) throws IllegalAnnotationsException {
        IllegalAnnotationsException.Builder errorListener = new IllegalAnnotationsException.Builder();
        RuntimeTypeInfoSet r = create(context, errorListener, classes);
        errorListener.check();
        return r;
    }
}
