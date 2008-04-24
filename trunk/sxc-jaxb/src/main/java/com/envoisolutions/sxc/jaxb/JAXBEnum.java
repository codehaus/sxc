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
