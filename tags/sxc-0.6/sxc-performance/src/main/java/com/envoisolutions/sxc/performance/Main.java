package com.envoisolutions.sxc.performance;

import com.sun.japex.Japex;

public class Main {
    public static void main(String[] args) throws Exception {
        // NOTE: BE SURE TO RUN THIS WITH APPROPRIATE JVM ARGUMENTS, LIKE -server
        
//        Japex.main(new String[] { "src/japex/read.xml" });
        Japex.main(new String[] { "src/japex/write.xml" });
    }
}