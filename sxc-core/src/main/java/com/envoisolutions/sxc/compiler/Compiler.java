package com.envoisolutions.sxc.compiler;

import java.io.File;

public interface Compiler {

    ClassLoader compile(File dir);

}
