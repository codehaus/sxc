package com.envoisolutions.sxc.builder.impl;

import java.io.File;
import java.io.IOException;

import com.envoisolutions.sxc.Context;
import com.envoisolutions.sxc.builder.BuildException;
import com.envoisolutions.sxc.builder.Builder;
import com.envoisolutions.sxc.builder.ElementParserBuilder;
import com.envoisolutions.sxc.builder.ElementWriterBuilder;
import com.sun.codemodel.JCodeModel;

public class BuilderImpl implements Builder {

    private ElementParserBuilderImpl parserBuilder;
    private File file;
    private BuildContext buildContext;
    private ElementWriterBuilderImpl writerBuilder;

    public BuilderImpl() {
        this.buildContext = new BuildContext();
        parserBuilder = new ElementParserBuilderImpl(buildContext);
        writerBuilder = new ElementWriterBuilderImpl(buildContext);
    }
    
    public ElementParserBuilder getParserBuilder() {
        return parserBuilder;
    }

    public ElementWriterBuilder getWriterBuilder() {
        return writerBuilder;
    }

    public JCodeModel getCodeModel() {
        return buildContext.getCodeModel();
    }
    
    public void write(File dir) throws IOException, BuildException {
        this.file = dir;
        
        // file = new File(file, new Long(System.currentTimeMillis()).toString());
        file.mkdirs();
        
        parserBuilder.write();
        writerBuilder.write();
        
        buildContext.getCodeModel().build(file);
    }
    
    public Context compile() {
        if (file == null) {
            try {
                String cdir = System.getProperty("streax-xo.output.directory");
                
                File dir;
                
                if (cdir == null) {
                    dir = File.createTempFile("compile", "");
                } else {
                    dir = new File(cdir);
                }

                dir.delete();
                
                dir.mkdirs();
                write(dir);
                
                dir.deleteOnExit();
            } catch (IOException e) {
                throw new BuildException(e);
            }
        }
        
        ClassLoader cl = Compiler.compile(file);

        return new CompiledContext(cl, parserBuilder.readerClass.fullName(), writerBuilder.getWriterClass().fullName());
    }

}
