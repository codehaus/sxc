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

import java.io.InputStream;
import java.io.OutputStream;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import com.envoisolutions.sxc.util.XoXMLStreamReader;
import com.envoisolutions.sxc.util.XoXMLStreamReaderImpl;
import com.envoisolutions.sxc.util.XoXMLStreamWriter;
import com.envoisolutions.sxc.util.XoXMLStreamWriterImpl;

public abstract class JAXBObject<T> extends JAXBClass<T> {
    protected QName xmlRootElement;
    protected QName xmlType;
    protected XMLOutputFactory xof = XMLOutputFactory.newInstance();
    protected XMLInputFactory xif = XMLInputFactory.newInstance();

    protected JAXBObject(Class<T> type, QName xmlRootElement, QName xmlType, Class<? extends JAXBClass>... dependencies) {
        super(type, dependencies);
        this.xmlRootElement = xmlRootElement;
        this.xmlType = xmlType;
    }

    public QName getXmlRootElement() {
        return xmlRootElement;
    }

    public QName getXmlType() {
        return xmlType;
    }

    public T read(InputStream is) throws Exception {
        return read(is, null);
    }

    public T read(InputStream is, RuntimeContext context) throws Exception {
        XMLStreamReader reader = xif.createXMLStreamReader(is);
        try {
            return read(reader, context);
        } finally {
            reader.close();
        }
    }

    public T read(XMLStreamReader reader) throws Exception {
        return read(reader, null);
    }

    public T read(XMLStreamReader reader, RuntimeContext context) throws Exception {
        return read(new XoXMLStreamReaderImpl(reader), context);
    }

    public abstract T read(XoXMLStreamReader reader, RuntimeContext context) throws Exception;

    public void write(OutputStream is, T o) throws Exception {
        write(is, o, null);
    }

    public void write(OutputStream is, T o, RuntimeContext context) throws Exception {
        XMLStreamWriter w = xof.createXMLStreamWriter(is);
        try {
            write(w, o);
        } finally {
            w.close();
        }
    }

    public void write(XMLStreamWriter writer, T o) throws Exception {
        write(new XoXMLStreamWriterImpl(writer), o);
    }

    public void write(XMLStreamWriter writer, T o, RuntimeContext context) throws Exception {
        write(new XoXMLStreamWriterImpl(writer), o, context);
    }

    public void write(XoXMLStreamWriter writer, T o) throws Exception {
        write(writer, o, null);
    }

    public abstract void write(XoXMLStreamWriter writer, T o, RuntimeContext context) throws Exception;
}