package com.envoisolutions.sxc.performance;

import com.sun.japex.Japex;

public class Main {
    public static void main(String[] args) throws Exception {
        System.setProperty("streax-xo.output.directory", "target/tmp-jaxb");
        Japex.main(new String[] { "src/japex/write.xml" });
        Japex.main(new String[] { "src/japex/read.xml" });
    }
}