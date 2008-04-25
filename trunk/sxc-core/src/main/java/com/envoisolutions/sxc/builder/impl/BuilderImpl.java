package com.envoisolutions.sxc.builder.impl;

import java.io.File;
import java.io.IOException;

import com.envoisolutions.sxc.Context;
import com.envoisolutions.sxc.Reader;
import com.envoisolutions.sxc.Writer;
import com.envoisolutions.sxc.builder.BuildException;
import com.envoisolutions.sxc.builder.Builder;
import com.envoisolutions.sxc.builder.ElementParserBuilder;
import com.envoisolutions.sxc.builder.ElementWriterBuilder;
import com.envoisolutions.sxc.compiler.Compiler;
import com.envoisolutions.sxc.util.Util;
import com.sun.codemodel.CodeWriter;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;

public class BuilderImpl implements Builder {
    private ElementParserBuilderImpl parserBuilder;
    private CodeWriterImpl codeWriter;
    private BuildContext buildContext;
    private ElementWriterBuilderImpl writerBuilder;
    private String compiler;

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

        if(readerClassName!=null) {
            parserBuilder = new ElementParserBuilderImpl(buildContext,readerClassName);
        }

        if(writerClassName!=null) {
            writerBuilder = new ElementWriterBuilderImpl(buildContext,writerClassName);
        }

        if(contextClassName!=null) {
            try {
                JDefinedClass cc = buildContext.getCodeModel()._class(contextClassName);
                cc._extends(Context.class);
                cc.method(JMod.PUBLIC, Reader.class,"createReader")
                    .body()._return( parserBuilder!=null
                        ? JExpr._new(parserBuilder.getReaderClass()).arg(JExpr._this())
                        : JExpr._null());
                cc.method(JMod.PUBLIC, Writer.class,"createWriter")
                    .body()._return( writerBuilder!=null
                        ? JExpr._new(writerBuilder.getWriterClass()).arg(JExpr._this())
                        : JExpr._null());
            } catch (JClassAlreadyExistsException e) {
                throw new BuildException(e);
            }
        }
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

    public void setReaderBaseClass(Class<? extends Reader> c) {
        setReaderBaseClass(getCodeModel().ref(c));
    }

    public void setReaderBaseClass(JClass c) {
        parserBuilder.setBaseClass(c);
    }

    public JCodeModel getCodeModel() {
        return buildContext.getCodeModel();
    }

    public JMethod getParserConstructor() {
        return parserBuilder.getConstructor();
    }

    public void write(File dir) throws IOException, BuildException {
        dir.mkdirs();

        codeWriter = new CodeWriterImpl(dir, true);

        write(codeWriter);
    }

    public void write(CodeWriter writer) throws IOException, BuildException {
        if(parserBuilder!=null) {
            parserBuilder.write();
        }
        if(writerBuilder!=null) {
            writerBuilder.write();
        }
        buildContext.getCodeModel().build(writer);
    }
    
    public Context compile() {
        CodeWriterImpl codeWriter = this.codeWriter;
        if (codeWriter == null) {
            try {
                codeWriter = new CodeWriterImpl();
                write(codeWriter);
            } catch (IOException e) {
                throw new BuildException(e);
            }
        }

        // compile the generated code
        Compiler compiler = Compiler.newInstance(this.compiler);
        ClassLoader classLoader = compiler.compile(codeWriter.getSources());

        // Only delete if the output directory hasn't been set
        if (System.getProperty("com.envoisolutions.sxc.output.directory") == null) {
            Util.delete(codeWriter.getBaseDir());
        }

        // TODO: simply load the generated context class and get rid of CompiledContext
        return new CompiledContext(classLoader,
            parserBuilder!=null ? parserBuilder.readerClass.fullName() : null,
            writerBuilder!=null ? writerBuilder.getWriterClass().fullName() : null);
    }

    public String getCompiler() {
        return compiler;
    }

    public void setCompiler(String compiler) {
        this.compiler = compiler;
    }
}
