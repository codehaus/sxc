package com.envoisolutions.sxc.jaxb;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import com.envoisolutions.sxc.util.XoXMLStreamReader;
import com.envoisolutions.sxc.util.XoXMLStreamReaderImpl;
import com.envoisolutions.sxc.util.XoXMLStreamWriter;
import com.envoisolutions.sxc.util.XoXMLStreamWriterImpl;
import com.envoisolutions.sxc.Context;

public abstract class JAXBMarshaller<T> {
    protected Context context;
    protected XMLOutputFactory xof = XMLOutputFactory.newInstance();
    protected XMLInputFactory xif = XMLInputFactory.newInstance();

    public JAXBMarshaller(Context context) {
        this.context = context;
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