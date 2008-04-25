package com.envoisolutions.sxc.compiler;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import com.envoisolutions.sxc.builder.BuildException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;

public class EclipseCompiler extends com.envoisolutions.sxc.compiler.Compiler {
    private CompilerOptions compilerOptions;

    public EclipseCompiler() {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put(CompilerOptions.OPTION_ReportDeprecation, CompilerOptions.GENERATE);
        settings.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_5);
        settings.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_5);
        settings.put(CompilerOptions.OPTION_LocalVariableAttribute, CompilerOptions.GENERATE);
        settings.put(CompilerOptions.OPTION_LineNumberAttribute, CompilerOptions.GENERATE);
        settings.put(CompilerOptions.OPTION_SourceFileAttribute, CompilerOptions.GENERATE);
        compilerOptions = new CompilerOptions(settings);
    }

    public ClassLoader compile(Map<String, File> sources) {
        // map which holds compiled bytecode
        Map<String, byte[]> byteCode = new HashMap<String, byte[]>();

        // create the compiler
        CompilerRequestor compilerRequestor = new CompilerRequestor(byteCode);
        Compiler compiler = new Compiler(new NameEnvironment(sources, byteCode),
                DefaultErrorHandlingPolicies.proceedWithAllProblems(),
                compilerOptions,
                compilerRequestor,
                new DefaultProblemFactory(Locale.getDefault()));

        // source files must be wrapped with an eclipse CompilationUnit
        List<ICompilationUnit> compilationUnits = new ArrayList<ICompilationUnit>(sources.size());
        for (Map.Entry<String, File> entry : sources.entrySet()) {
            compilationUnits.add(new CompilationUnit(entry.getKey(), entry.getValue()));
        }

        // compiler the soruce files
        compiler.compile(compilationUnits.toArray(new ICompilationUnit[compilationUnits.size()]));

        // report errors (ignore warnings)
        int errorCount = 0;
        if (compilerRequestor.hasErrors()) {
            for (IProblem problem : compilerRequestor.getProblems()) {
                if (problem.isError()) {
                    System.out.println("ERROR " + new String(problem.getOriginatingFileName()) + ":[" + problem.getSourceLineNumber() + "] " + problem.getMessage());
                    errorCount++;
                } else {
                    System.out.println("WARNING " + new String(problem.getOriginatingFileName()) + ":[" + problem.getSourceLineNumber() + "] " + problem.getMessage());
                }
            }
        }

        // throw an exception if we had some errors
        if (errorCount > 0) {
            throw new BuildException("Compile completed with " + errorCount + " errors and " + (compilerRequestor.getProblems().size() - errorCount) + " warnings");
        }

        // wrap generted byte code with a classloader
        return new MemoryClassLoader(Thread.currentThread().getContextClassLoader(), byteCode);
    }

    private static final class CompilationUnit implements ICompilationUnit {
        private final File sourceFile;
        private final char[] typeName;
        private final char[][] packageName;

        CompilationUnit(String className, File sourceFile) {
            this.sourceFile = sourceFile;
            int dot = className.lastIndexOf('.');
            if (dot > 0) {
                typeName = className.substring(dot + 1).toCharArray();
            } else {
                typeName = className.toCharArray();
            }

            StringTokenizer tokenizer = new StringTokenizer(className, ".");
            packageName = new char[tokenizer.countTokens() - 1][];
            for (int i = 0; i < packageName.length; i++) {
                packageName[i] = tokenizer.nextToken().toCharArray();
            }
        }

        public char[] getFileName() {
            return sourceFile.getName().toCharArray();
        }

        public char[] getContents() {
            InputStream in = null;
            try {
                in = new FileInputStream(sourceFile);
                StringBuffer sb = new StringBuffer();
                in = new BufferedInputStream(in);
                int i = in.read();
                while (i != -1) {
                    sb.append((char) i);
                    i = in.read();
                }
                String content = sb.toString();
                return content.toCharArray();
            } catch (Exception e) {
                return null;
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                    }
                }
            }
        }

        public char[] getMainTypeName() {
            return typeName;
        }

        public char[][] getPackageName() {
            return packageName;
        }
    }

    private static class NameEnvironment implements INameEnvironment {
        private final Map<String, File> sources;
        private final Map<String, byte[]> byteCode;

        public NameEnvironment(Map<String, File> sources, Map<String, byte[]> byteCode) {
            this.sources = sources;
            this.byteCode = byteCode;
        }

        public NameEnvironmentAnswer findType(char[][] compoundTypeName ) {
            StringBuffer result = new StringBuffer();
            for (int i = 0; i < compoundTypeName.length; i++) {
                if (i != 0) {
                    result.append('.');
                }
                result.append(compoundTypeName[i]);
            }

            return findType(result.toString());
        }

        public NameEnvironmentAnswer findType(char[] typeName, char[][] packageNames ) {
            StringBuffer result = new StringBuffer();
            for (char[] packageName : packageNames) {
                result.append(packageName);
                result.append('.');
            }

            result.append(typeName);
            return findType(result.toString());
        }

        private NameEnvironmentAnswer findType(String className) {
            if (isPackage(className)) {
                return null;
            }

            byte[] clazzBytes = byteCode.get(className);
            if (clazzBytes != null) {
                char[] fileName = className.toCharArray();
                try {
                    ClassFileReader classFileReader = new ClassFileReader(clazzBytes, fileName, true);
                    return new NameEnvironmentAnswer(classFileReader, null);
                } catch (ClassFormatException e) {
                    return null;
                }
            }

            String resourceName = className.replace('.', '/') + ".class";
            InputStream in = getClass().getClassLoader().getResourceAsStream(resourceName);
            if (in == null) {
                return null;
            }

            byte[] buffer = new byte[8192];
            ByteArrayOutputStream out = new ByteArrayOutputStream(buffer.length);
            int count;
            try {
                while ((count = in.read(buffer, 0, buffer.length)) > 0) {
                    out.write(buffer, 0, count);
                }
                out.flush();
                char[] fileName = className.toCharArray();
                ClassFileReader classFileReader = new ClassFileReader(out.toByteArray(), fileName, true);
                return new NameEnvironmentAnswer(classFileReader, null);
            } catch (IOException e) {
                return null;
            } catch (ClassFormatException e) {
                return null;
            } finally {
                try {
                    out.close();
                } catch (IOException oe) {
                }
                try {
                    in.close();
                } catch (IOException ie) {
                }
            }
        }

        public boolean isPackage(char[][] parentPackageNames, char[] packageName) {
            StringBuffer result = new StringBuffer();
            if (parentPackageNames != null) {
                for (char[] parentPackageName : parentPackageNames) {
                    result.append(parentPackageName).append('.');
                }
            }
            result.append(packageName);

            return isPackage(result.toString());
        }

        public void cleanup() {
        }

        private boolean isPackage(String className) {
            if (sources.containsKey(className)) return false;

            InputStream in = getClass().getClassLoader().getResourceAsStream(className.replace('.', '/') + ".class");
            return in == null;

        }
    }

    private static class CompilerRequestor implements ICompilerRequestor {
        private boolean hasErrors;
        private final List<IProblem> problems = new ArrayList<IProblem>();
        private final Map<String, byte[]> byteCode;

        private CompilerRequestor(Map<String, byte[]> byteCode) {
            this.byteCode = byteCode;
        }

        public boolean hasErrors() {
            return hasErrors;
        }

        public List<IProblem> getProblems() {
            return problems;
        }

        public Map<String, byte[]> getByteCode() {
            return byteCode;
        }

        public void acceptResult( org.eclipse.jdt.internal.compiler.CompilationResult result ) {
            if (result.hasProblems()) {
                if (result.hasErrors()) hasErrors = true;
                problems.addAll(Arrays.asList((IProblem[]) result.getProblems()));
            }

            if (!result.hasErrors()) {
                ClassFile[] clazzFiles = result.getClassFiles();
                for (ClassFile clazzFile : clazzFiles) {
                    char[][] compoundName = clazzFile.getCompoundName();
                    StringBuffer clazzName = new StringBuffer();
                    for (char[] part : compoundName) {
                        if (clazzName.length() > 0) {
                            clazzName.append('.');

                        }
                        clazzName.append(part);
                    }
                    byteCode.put(clazzName.toString(), clazzFile.getBytes());
                }
            }
        }
    }

    private static class MemoryClassLoader extends ClassLoader {
        Map<String, byte[]> byteCode;

        private MemoryClassLoader(ClassLoader parent, Map<String, byte[]> byteCode) {
            super(parent);
            this.byteCode = byteCode;
        }

        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] bytes = byteCode.get(name);
            if (bytes == null) {
                throw new ClassNotFoundException(name);
            }

            // create the package if not already created
            int index = name.lastIndexOf('.');
            if (index > 0) {
                String packageName = name.substring(0, index);
                if (getPackage(packageName) == null) {
                    definePackage(packageName, null, null, null, null, null, null, null);
                }
            }

            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}
