package com.envoisolutions.sxc.compiler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.jci.compilers.AbstractJavaCompiler;
import org.apache.commons.jci.compilers.CompilationResult;
import org.apache.commons.jci.compilers.EclipseJavaCompiler;
import org.apache.commons.jci.compilers.EclipseJavaCompilerSettings;
import org.apache.commons.jci.problems.CompilationProblem;
import org.apache.commons.jci.readers.FileResourceReader;
import org.apache.commons.jci.stores.MemoryResourceStore;
import org.apache.commons.jci.stores.ResourceStore;
import org.apache.commons.jci.stores.ResourceStoreClassLoader;
import org.apache.commons.jci.utils.ConversionUtils;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;

import com.envoisolutions.sxc.builder.BuildException;

public class EclipseCompiler implements Compiler {
    /* (non-Javadoc)
     * @see com.envoisolutions.sxc.builder.impl.ICompiler#compile(java.io.File)
     */
    @SuppressWarnings("unchecked")
    public ClassLoader compile(File dir) {
        EclipseJavaCompilerSettings settings = new EclipseJavaCompilerSettings();
        settings.setSourceVersion(CompilerOptions.VERSION_1_5);
        
        EclipseJavaCompiler compiler = new EclipseJavaCompiler(settings);
         
        if (!dir.exists()) {
            throw new BuildException("Compilation directory does not exist!");
        }
        
        FileResourceReader reader = new FileResourceReader(dir);
        
        List<String> classes = new ArrayList<String>();
        for (String s : reader.list()) {
            String name = ConversionUtils.convertResourceToClassName(s);
            name = name.replace('/', '.');
            name = name.replace('\\', '.');
            
            classes.add(name);
        }
        
        MemoryResourceStore store = new MemoryResourceStore();
        CompilationResult result 
            = compiler.compile(classes.toArray(new String[classes.size()]), reader, store);
        
        CompilationProblem[] errors = result.getErrors();
        for (CompilationProblem p : errors) {
            System.out.println(p.getMessage());
        }
        
        // TODO throw better errors!
        if (errors.length > 0) {
            throw new BuildException("Could not compile generated files!");
        }
        
        return new ResourceStoreClassLoader(Thread.currentThread().getContextClassLoader(),
                                            new ResourceStore[] { store });
    }
}
