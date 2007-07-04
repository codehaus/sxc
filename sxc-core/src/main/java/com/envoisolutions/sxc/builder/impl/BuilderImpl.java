package com.envoisolutions.sxc.builder.impl;

import com.envoisolutions.sxc.Context;
import com.envoisolutions.sxc.Reader;
import com.envoisolutions.sxc.Writer;
import com.envoisolutions.sxc.builder.BuildException;
import com.envoisolutions.sxc.builder.Builder;
import com.envoisolutions.sxc.builder.ElementParserBuilder;
import com.envoisolutions.sxc.builder.ElementWriterBuilder;
import com.envoisolutions.sxc.compiler.Compiler;
import com.envoisolutions.sxc.compiler.JavacCompiler;
import com.envoisolutions.sxc.util.Util;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JClassAlreadyExistsException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

public class BuilderImpl implements Builder {

    private ElementParserBuilderImpl parserBuilder;
    private File file;
    private BuildContext buildContext;
    private ElementWriterBuilderImpl writerBuilder;
    private Compiler compiler = new JavacCompiler();
    private String contextClassName;

    /**
     *
     * @param readerClassName
     *      Fully qualified class name of the reader to be generated.
     *      null to not generate the reader.
     * @param writerClassName
     *      Fully qualified class name of the writer to be generated.
     *      null to not generate the writer.
     * @param contextClassName
     *      Fully qualified class name of the {@link Context}-derived class to be generated.
     *      null to not generate one.
     */
    public BuilderImpl(String readerClassName, String writerClassName, String contextClassName) {
        this.buildContext = new BuildContext();
        if(readerClassName!=null)
            parserBuilder = new ElementParserBuilderImpl(buildContext,readerClassName);
        if(writerClassName!=null)
            writerBuilder = new ElementWriterBuilderImpl(buildContext,writerClassName);

        if(contextClassName!=null) {
            try {
                JDefinedClass cc = buildContext.getCodeModel()._class(contextClassName);
                cc.method(JMod.PUBLIC, Reader.class,"createReader")
                    .body()._return( parserBuilder!=null
                        ? JExpr._new(parserBuilder.getReaderClass())
                        : JExpr._null());
                cc.method(JMod.PUBLIC, Writer.class,"createWriter")
                    .body()._return( writerBuilder!=null
                        ? JExpr._new(writerBuilder.getWriterClass())
                        : JExpr._null());
            } catch (JClassAlreadyExistsException e) {
                throw new BuildException(e);
            }
        }
        this.contextClassName = contextClassName;
    }

    /**
     * Generates both reader and writer by using default names. 
     */
    public BuilderImpl() {
        this("generated.sxc.Reader","generated.sxc.Writer","generated.sxc.Context");
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

    public JMethod getParserConstructor() {
        return parserBuilder.constructor;
    }

    public void write(File dir) throws IOException, BuildException {
        this.file = dir;
        
        // file = new File(file, new Long(System.currentTimeMillis()).toString());
        file.mkdirs();

        if(parserBuilder!=null)
            parserBuilder.write();
        if(writerBuilder!=null)
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

        // TODO: simply load the generated context class and get rid of CompiledContext
        return new CompiledContext(cl,
            parserBuilder!=null ? parserBuilder.readerClass.fullName() : null,
            writerBuilder!=null ? writerBuilder.getWriterClass().fullName() : null);
    }
    
    public Compiler getCompiler() {
        return compiler;
    }

    public void setCompiler(Compiler compiler) {
        this.compiler = compiler;
    }

    
}
