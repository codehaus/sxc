package com.envoisolutions.sxc.builder.impl;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.HashMap;

import com.sun.codemodel.CodeWriter;
import com.sun.codemodel.JPackage;

public class CodeWriterImpl extends CodeWriter {
    private final File baseDir;
    private final boolean overwrite;
    private final Map<String, File> sources = new HashMap<String, File>();

    public CodeWriterImpl() throws IOException {
        this(null);
    }
    public CodeWriterImpl(String outputDir) throws IOException {
        overwrite = true;

        if (outputDir == null) {
            outputDir = System.getProperty("com.envoisolutions.sxc.output.directory");
        }

        if (outputDir == null) {
            baseDir = File.createTempFile("compile", "");
            baseDir.delete();
        } else {
            baseDir = new File(outputDir);
        }
        baseDir.mkdirs();
    }

    public CodeWriterImpl(File baseDir, boolean overwrite) {
        this.baseDir = baseDir;
        this.overwrite = overwrite;
    }

    public Map<String, File> getSources() {
        return sources;
    }

    public File getBaseDir() {
        return baseDir;
    }

    public OutputStream openBinary(JPackage jpackage, String fileName) throws IOException {
        File file;
        if (jpackage.isUnnamed()) {
            file = new File(baseDir, fileName);
        } else {
            file = new File(new File(baseDir, jpackage.name().replace('.', File.separatorChar)), fileName);
        }

        file.getParentFile().mkdirs();
        if (file.exists()) {
            if (!overwrite) {
                // ignore output
                return new OutputStream() {
                    public void write(int ignored) throws IOException { }
                };
            }

            file.delete();
            if (file.exists()) {
                throw new IOException("Unable to overwrite " + file.getName());
            }
        }

        String className = jpackage.name() + "." + fileName.replace(".java", "");
        sources.put(className, file);

        return new FileOutputStream(file);
    }

    public void close() throws IOException {
    }
}
