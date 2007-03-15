package com.envoisolutions.sxc.performance;

import com.sun.japex.Japex;

public class Main {
    public static void main(String[] args) throws Exception {
        // NOTE: BE SURE TO RUN THIS WITH APPROPRIATE JVM ARGUMENTS, LIKE -server
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");
        Japex.main(new String[] { "src/japex/read.xml" });
        Japex.main(new String[] { "src/japex/write.xml" });
    }
}