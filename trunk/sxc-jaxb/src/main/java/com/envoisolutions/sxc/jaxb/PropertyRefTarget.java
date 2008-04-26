package com.envoisolutions.sxc.jaxb;

import javax.xml.bind.JAXBException;

import com.envoisolutions.sxc.util.XoXMLStreamReader;

public class PropertyRefTarget implements IdRefTarget {
    private XoXMLStreamReader reader;
    private RuntimeContext context;
    private Object instance;
    private PropertyAccessor propertyAccessor;

    public PropertyRefTarget(XoXMLStreamReader reader, RuntimeContext context, Object instance, PropertyAccessor propertyAccessor) {
        this.reader = reader;
        this.context = context;
        this.instance = instance;
        this.propertyAccessor = propertyAccessor;
    }

    @SuppressWarnings({"unchecked"})
    public void resolved(Object value) throws JAXBException {
        propertyAccessor.setObject(reader, context, instance, value);
    }
}