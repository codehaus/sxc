package com.envoisolutions.sxc.jaxb.adapter;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class TypeAdapter extends XmlAdapter<ValueType, BoundType> {
    public BoundType unmarshal(ValueType v) throws Exception {
        return new BoundType(v.name);
    }

    public ValueType marshal(BoundType v) throws Exception {
        return new ValueType(v.name);
    }
}
