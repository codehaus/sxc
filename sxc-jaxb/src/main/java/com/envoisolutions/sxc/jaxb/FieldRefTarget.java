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
