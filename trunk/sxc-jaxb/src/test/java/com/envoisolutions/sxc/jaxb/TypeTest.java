package com.envoisolutions.sxc.jaxb;

import java.util.HashMap;

import org.apache.type_test.types1.DecimalEnum;

import com.envoisolutions.sxc.util.XoTestCase;

public class TypeTest extends XoTestCase {
    public void testEverything1() throws Exception {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");
        JAXBContextImpl ctx = JAXBContextImpl.createContext("org.apache.type_test.types1", getClass()
            .getClassLoader(), null);

        assertNotNull(ctx);
    }

    public void xtestEverything2() throws Exception {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");
        JAXBContextImpl ctx = JAXBContextImpl.createContext("org.apache.type_test.types2", getClass()
            .getClassLoader(), null);

        assertNotNull(ctx);
    }

    public void xtestEverything3() throws Exception {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");
        JAXBContextImpl ctx = JAXBContextImpl.createContext("org.apache.type_test.types3", getClass()
            .getClassLoader(), null);

        assertNotNull(ctx);
    }

    public void xtestSingleEnum() throws Exception {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");
        JAXBContextImpl ctx = JAXBContextImpl.createContext(new Class[] {DecimalEnum.class},
                                                            new HashMap<String, Object>());

        assertNotNull(ctx);
    }
}
