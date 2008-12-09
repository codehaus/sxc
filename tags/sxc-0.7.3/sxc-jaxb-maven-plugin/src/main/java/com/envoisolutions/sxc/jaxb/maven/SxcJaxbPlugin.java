package com.envoisolutions.sxc.jaxb.maven;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Map;
import javax.xml.bind.JAXBException;

import com.envoisolutions.sxc.jaxb.JAXBGenerator;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * @goal generate
 * @description Generates SXC JaxB implementation
 * @phase process-classes
 * @requiresDependencyResolution runtime
 */
public class SxcJaxbPlugin extends AbstractMojo {
    /**
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;

    /**
     * Directory source files will be written.
     *
     * @parameter expression="${project.build.directory}/sxc"
     * @required
     */
    private File sourceOutputDirectory;

    /**
     * Directory class files will be written.
     *
     * @parameter expression="${project.build.directory}/classes"
     * @required
     */
    private File classesOutputDirectory;

    /**
     * @parameter
     * @required
     */
    private String[] classes;

    /**
     * @parameter
     */
    private Map<String,String> properties;

    public void execute() throws MojoExecutionException {
        try {
            JAXBGenerator jaxbGenerator = new JAXBGenerator();
            jaxbGenerator.setSourceOutputDirectory(sourceOutputDirectory.getAbsolutePath());
            jaxbGenerator.setClassesOutputDirectory(classesOutputDirectory.getAbsolutePath());
            jaxbGenerator.getClasses().addAll(Arrays.asList(classes));
            if (properties != null) {
                jaxbGenerator.getProperties().putAll(properties);
            }

            // need to manually create the classloader since maven won't give me one
            String directory = project.getBuild().getOutputDirectory();
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null) classLoader = getClass().getClassLoader();
            classLoader = new URLClassLoader(new URL[]{new File(directory).toURI().toURL()}, classLoader);
            jaxbGenerator.setClassLoader(classLoader);

            jaxbGenerator.generate();
        } catch (JAXBException e) {
            throw new MojoExecutionException("Error generating JaxB parser: " + e.getMessage(), e);
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("Invalid build outputDirectory " + project.getBuild().getOutputDirectory());
        }
    }

    public File getSourceOutputDirectory() {
        return sourceOutputDirectory;
    }

    public void setSourceOutputDirectory(File sourceOutputDirectory) {
        this.sourceOutputDirectory = sourceOutputDirectory;
    }

    public File getClassesOutputDirectory() {
        return classesOutputDirectory;
    }

    public void setClassesOutputDirectory(File classesOutputDirectory) {
        this.classesOutputDirectory = classesOutputDirectory;
    }

    public String[] getClasses() {
        return classes;
    }

    public void setClasses(String[] classes) {
        this.classes = classes;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}
