package com.envoisolutions.sxc.compiler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import com.envoisolutions.sxc.builder.BuildException;
import com.envoisolutions.sxc.util.Util;

public class JavacCompiler extends Compiler {
    public ClassLoader compile(Map<String, File> sources) {
        if (sources.isEmpty()) {
            throw new IllegalArgumentException("sources is empty");
        }
        
        try {
            String tmpdir = System.getProperty("java.io.tmpdir");
            File classDir = new File(new File(tmpdir), "classes" + hashCode() + System.currentTimeMillis());

            if (!classDir.mkdir()) {
                throw new BuildException("Could not create output directory.");
            }
            ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
            Set<URL> urlSet = getClasspathURLs(oldCl);
            String classpath = createClasspath(urlSet);

            URLClassLoader newCL = createNewClassLoader();

            // build arg array
            List<String> args = new ArrayList<String>(sources.size() + 7);
            args.add("-g");
            args.add("-d");
            args.add(classDir.getAbsolutePath());
            args.add("-classpath");
            args.add(classpath);
            for (File file : sources.values()) {
                args.add(file.getAbsolutePath());
            }

            // invoke compiler
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(out);
            int exitCode;
            try {
                Class<?> main = newCL.loadClass("com.sun.tools.javac.Main");
                Method method = main.getMethod("compile", String[].class, PrintWriter.class);
                exitCode = (Integer) method.invoke(null, args.toArray(new String[args.size()]), writer);
            } catch (ClassNotFoundException e1) {
                throw new BuildException("Could not find javac compiler!", e1);
            } catch (Exception e) {
                throw new BuildException("Could not invoke javac compiler!", e);
            }

            // check exit code
            if (exitCode != 0) {
                writer.close();
                
                System.out.println(out.toString());
                
                throw new BuildException("Could not compile generated files! Code: " + exitCode);
            }

            // load classes
            Thread.currentThread().setContextClassLoader(oldCl);
            URLClassLoader cl = new URLClassLoader(new URL[] {classDir.toURL()}, oldCl);
            List<String> failedToLoad = new ArrayList<String>();
            for (String className : sources.keySet()) {
                try {
                    cl.loadClass(className);
                } catch (ClassNotFoundException e) {
                    failedToLoad.add(className);
                }
            }
            if (!failedToLoad.isEmpty()) {
                throw new BuildException("Could not load generated classes " + failedToLoad);
            }

            Util.delete(classDir);

            return cl;

        } catch (IOException e) {
            throw new BuildException(e);
        }

    }

    private URLClassLoader createNewClassLoader() {
        URL[] urls;
        File toolsJar = new File(System.getProperty("java.home"), "../lib/tools.jar");
        if (toolsJar.exists()) {
            try {
                urls = new URL[] { toolsJar.toURL() };
            } catch (MalformedURLException e) {
                throw new BuildException("Could not convert the file reference to tools.jar to a URL, path to tools.jar: '"
                                                + toolsJar.getAbsolutePath() + "'.");
            }
        } else {
            urls = new URL[0];
        }
        
        URLClassLoader newCL = new URLClassLoader(urls);
        Thread.currentThread().setContextClassLoader(newCL);
        
        return newCL;
    }

    private String createClasspath(Set<URL> urls) {
        StringBuilder cp = new StringBuilder();
        boolean first = true;
        for (URL u : urls) {
            if (u.getProtocol().equals("file")) {
  
                if (first) {
                    first = false;
                } else {
                    cp.append(File.pathSeparatorChar);
                }

                String uStr = u.toString().replaceAll(" ", "%20");
                try {
                    File file = new File(new URI(uStr));
                    cp.append(file.getAbsolutePath());
                } catch (URISyntaxException e) {
                    
                }
            }
        }
        return cp.toString();
    }

    private Set<URL> getClasspathURLs(ClassLoader cl) {
        Set<URL> urls = new HashSet<URL>();

        while (cl != null) {
            if (cl instanceof URLClassLoader) {
                URLClassLoader ucl = (URLClassLoader)cl;

                URL[] clurls = ucl.getURLs();
                if (clurls != null) {
                    urls.addAll(Arrays.asList(clurls));
                }
            }
            cl = cl.getParent();
        }
        return urls;
    }

}
