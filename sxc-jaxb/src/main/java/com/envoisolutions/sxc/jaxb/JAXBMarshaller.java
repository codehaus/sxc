package com.envoisolutions.sxc.jaxb;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Collection;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import com.envoisolutions.sxc.Context;
import com.envoisolutions.sxc.util.XoXMLStreamReader;
import com.envoisolutions.sxc.util.XoXMLStreamReaderImpl;
import com.envoisolutions.sxc.util.XoXMLStreamWriter;
import com.envoisolutions.sxc.util.XoXMLStreamWriterImpl;

public abstract class JAXBMarshaller<T> {
    protected Context context;
    protected Class type;
    protected QName xmlRootElement;
    protected QName xmlType;
    protected XMLOutputFactory xof = XMLOutputFactory.newInstance();
    protected XMLInputFactory xif = XMLInputFactory.newInstance();
    protected Collection<Class<? extends JAXBMarshaller>> dependencies;

    protected JAXBMarshaller(Context context, Class type, QName xmlRootElement, QName xmlType, Class<? extends JAXBMarshaller>... dependencies) {
        this.context = context;
        this.type = type;
        this.xmlRootElement = xmlRootElement;
        this.xmlType = xmlType;
        this.dependencies = Collections.unmodifiableCollection(Arrays.asList(dependencies));
    }

    public Class getType() {
        return type;
    }

    public QName getXmlRootElement() {
        return xmlRootElement;
    }

    public QName getXmlType() {
        return xmlType;
    }

    public Collection<Class<? extends JAXBMarshaller>> getDependencies() {
        return dependencies;
    }

    public T read(InputStream is) throws Exception {
        return read(is, null);
    }

    public T read(InputStream is, Map<String, Object> properties) throws Exception {
        XMLStreamReader reader = xif.createXMLStreamReader(is);
        try {
            return read(reader, properties);
        } finally {
            reader.close();
        }
    }

    public T read(XMLStreamReader reader) throws Exception {
        return read(reader, null);
    }

    public T read(XMLStreamReader reader, Map<String, Object> properties) throws Exception {
        return read(new XoXMLStreamReaderImpl(reader), properties);
    }

    public abstract T read(XoXMLStreamReader reader, Map<String, Object> properties) throws Exception;

    public void write(OutputStream is, T o) throws Exception {
        write(is, o, null);
    }

    public void write(OutputStream is, T o, Map<String, Object> properties) throws Exception {
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

    public void write(XMLStreamWriter writer, T o, Map<String, Object> properties) throws Exception {
        write(new XoXMLStreamWriterImpl(writer), o, properties);
    }

    public void write(XoXMLStreamWriter writer, T o) throws Exception {
        write(writer, o, null);
    }

    public abstract void write(XoXMLStreamWriter writer, T o, Map<String, Object> properties) throws Exception;
}