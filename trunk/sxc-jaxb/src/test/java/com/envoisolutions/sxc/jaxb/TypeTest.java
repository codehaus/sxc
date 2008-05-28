/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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
