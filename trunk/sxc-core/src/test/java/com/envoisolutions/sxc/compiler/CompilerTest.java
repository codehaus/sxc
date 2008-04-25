package com.envoisolutions.sxc.compiler;

import junit.framework.TestCase;

public class CompilerTest extends TestCase {
    public void testSelectJavac() {
        Compiler compiler = Compiler.newInstance(Compiler.JAVAC);
        assertNotNull("compiler is null", compiler);
        assertTrue("compiler should be an instance of JavacCompiler", compiler instanceof JavacCompiler);
    }

    public void testSelectEclipse() {
        Compiler compiler = Compiler.newInstance(Compiler.ECLIPSE);
        assertNotNull("compiler is null", compiler);
        assertTrue("compiler should be an instance of EclipseCompiler", compiler instanceof EclipseCompiler);
    }

    public void testSelectAutomatic() {
        // eclipse classes are available so eclipse should be selected
        Compiler compiler = Compiler.newInstance();
        assertNotNull("compiler is null", compiler);
        assertTrue("compiler should be an instance of EclipseCompiler", compiler instanceof EclipseCompiler);

        compiler = Compiler.newInstance(null);
        assertNotNull("compiler is null", compiler);
        assertTrue("compiler should be an instance of EclipseCompiler", compiler instanceof EclipseCompiler);
    }

    public void testSelectUnknown() {
        try {
            Compiler.newInstance("unknown");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
    }
}
