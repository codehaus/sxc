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

import javax.xml.bind.JAXBException;

import com.envoisolutions.sxc.util.XoXMLStreamReader;

public class FieldRefTarget implements IdRefTarget {
    private XoXMLStreamReader reader;
    private RuntimeContext context;
    private Object instance;
    private FieldAccessor fieldAccessor;

    public FieldRefTarget(XoXMLStreamReader reader, RuntimeContext context, Object instance, FieldAccessor fieldAccessor) {
        this.reader = reader;
        this.context = context;
        this.instance = instance;
        this.fieldAccessor = fieldAccessor;
    }

    @SuppressWarnings({"unchecked"})
    public void resolved(Object value) throws JAXBException {
        fieldAccessor.setObject(reader, context, instance, value);
    }
}
