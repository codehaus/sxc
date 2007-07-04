package com.envoisolutions.sxc.builder;

import java.io.File;
import java.io.IOException;

import com.envoisolutions.sxc.Context;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.CodeWriter;


public interface Builder {
    
    ElementParserBuilder getParserBuilder();
    
    ElementWriterBuilder getWriterBuilder();

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