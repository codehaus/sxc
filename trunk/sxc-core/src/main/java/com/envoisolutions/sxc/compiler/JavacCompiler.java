package com.envoisolutions.sxc.compiler;

import com.envoisolutions.sxc.builder.BuildException;
import com.envoisolutions.sxc.util.Util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

public class JavacCompiler implements Compiler {

    public ClassLoader compile(File srcDir) {
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
            
            String[] args = {srcDir.getAbsolutePath() + "/streax/generated/Reader.java",
                             srcDir.getAbsolutePath() + "/streax/generated/Writer.java", 
                             "-g", 
                             "-d", classDir.getAbsolutePath(), 
                             "-classpath", classpath,
                             "-sourcepath", srcDir.getAbsolutePath()};

            // System.out.println("Args: " + Arrays.toString(args));

            int i;
            try {
                Class<?> main = newCL.loadClass("com.sun.tools.javac.Main");
                Method method = main.getMethod("compile", new Class[] {String[].class});
                i = (Integer)method.invoke(null, new Object[] {args});
            } catch (ClassNotFoundException e1) {
                throw new BuildException("Could not find javac compiler!", e1);
            } catch (Exception e) {
                throw new BuildException("Could not invoke javac compiler!", e);
            }

            if (i != 0) {
                throw new BuildException("Could not compile generated files! Code: " + i);
            }

            Thread.currentThread().setContextClassLoader(oldCl);
            URLClassLoader cl = new URLClassLoader(new URL[] {classDir.toURL()}, oldCl);
            try {
                cl.loadClass("streax.generated.Reader");
                cl.loadClass("streax.generated.Writer");
            } catch (ClassNotFoundException e) {
                throw new BuildException("Could not load generated classes.", e);
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
                    for (URL u : clurls) {
                        urls.add(u);
                    }
                }
            }
            cl = cl.getParent();
        }
        return urls;
    }

}
