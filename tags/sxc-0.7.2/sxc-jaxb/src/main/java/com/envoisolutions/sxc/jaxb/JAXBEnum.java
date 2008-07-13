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

import javax.xml.namespace.QName;

import com.envoisolutions.sxc.util.XoXMLStreamReader;
import com.envoisolutions.sxc.util.XoXMLStreamWriter;

public abstract class JAXBEnum<T> extends JAXBObject<T> {
    public JAXBEnum(Class<T> type, QName xmlRootElement, QName xmlType) {
        super(type, xmlRootElement, xmlType);
    }

    public abstract T parse(XoXMLStreamReader reader, RuntimeContext context, String value) throws Exception;

    public abstract String toString(Object bean, String paramName, RuntimeContext context, T enumConst) throws Exception;

    public T read(XoXMLStreamReader reader, RuntimeContext context) throws Exception {
        String value = reader.getElementAsString();
        T enumConst = parse(reader, context, value);
        return enumConst;
    }

    public void write(XoXMLStreamWriter writer, T enumConst, RuntimeContext context) throws Exception {
        String value = toString(null, null, context, enumConst);
        writer.writeCharacters(value);
    }
}
