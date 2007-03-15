package com.envoisolutions.sxc.builder.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

import com.envoisolutions.sxc.Context;
import com.envoisolutions.sxc.builder.BuildException;
import com.envoisolutions.sxc.builder.Builder;
import com.envoisolutions.sxc.builder.ElementParserBuilder;
import com.envoisolutions.sxc.builder.ElementWriterBuilder;
import com.envoisolutions.sxc.compiler.Compiler;
import com.envoisolutions.sxc.compiler.JavacCompiler;
import com.envoisolutions.sxc.util.Util;
import com.sun.codemodel.JCodeModel;

public class BuilderImpl implements Builder {

    private ElementParserBuilderImpl parserBuilder;
    private File file;
    private BuildContext buildContext;
    private ElementWriterBuilderImpl writerBuilder;
    private Compiler compiler = new JavacCompiler();
    
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
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(bos);
        buildContext.getCodeModel().build(file, stream);
        
        // todo: someday maybe print error messages 
        // (but its really just annoying data about what file was outputted)
    }
    
    public Context compile() {
        boolean delete = true;
        File dir = null;
        if (file == null) {
            try {
                String cdir = System.getProperty("com.envoisolutions.sxc.output.directory");
                
                if (cdir == null) {
                    dir = File.createTempFile("compile", "");
                } else {
                    dir = new File(cdir);
                    delete = false;
                }

                dir.delete();
                
                dir.mkdirs();
                write(dir);
                
                
            } catch (IOException e) {
                throw new BuildException(e);
            }
        }
        
        ClassLoader cl = compiler.compile(file);

        // Only delete if the output directory hasn't been set
        if (delete && file == null) {
            Util.delete(dir);
        }
        
        return new CompiledContext(cl, parserBuilder.readerClass.fullName(), writerBuilder.getWriterClass().fullName());
    }
    
    public Compiler getCompiler() {
        return compiler;
    }

    public void setCompiler(Compiler compiler) {
        this.compiler = compiler;
    }

    
}
