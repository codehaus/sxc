package com.envoisolutions.sxc.builder;

import java.io.File;
import java.io.IOException;

import com.envoisolutions.sxc.Context;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JMethod;


public interface Builder {
    
    ElementParserBuilder getParserBuilder();
    
    ElementWriterBuilder getWriterBuilder();
    
    Context compile();
    
    void write(File file) throws BuildException, IOException;

    JCodeModel getCodeModel();

    JMethod getParserConstructor();
}