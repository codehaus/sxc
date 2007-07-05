package com.envoisolutions.sxc.builder;

import java.io.File;
import java.io.IOException;

import com.envoisolutions.sxc.Context;
import com.envoisolutions.sxc.Reader;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.CodeWriter;
import com.sun.codemodel.JClass;


public interface Builder {
    
    ElementParserBuilder getParserBuilder();
    
    ElementWriterBuilder getWriterBuilder();

    /**
     * Sets the base reader class from {@link Reader}
     * to the user-defined clas that extends from {@link Reader}.
     *
     * <p>
     * The new base class can be used to define utility methods,
     * so that you can simplify the code generation later.
     */
    void setReaderBaseClass(Class<? extends Reader> c);

    /**
     * @see #setReaderBaseClass(Class) 
     */
    void setReaderBaseClass(JClass c);

    /**
     * Generates and compiles the code, then loads it back into the current JVM. 
     */
    Context compile() throws BuildException;

    /**
     * Generates the code into the given directory.
     */
    void write(File file) throws BuildException, IOException;

    /**
     * Generates the code through a custom {@link CodeWriter}.
     */
    void write(CodeWriter writer) throws IOException, BuildException;

    JCodeModel getCodeModel();

    JMethod getParserConstructor();
}