package com.envoisolutions.sxc.jaxb.adapter;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class TypeStringAdapter extends XmlAdapter<String, BoundType> {
    public BoundType unmarshal(String v) throws Exception {
        return new BoundType(v.trim());
    }

    public String marshal(BoundType v) throws Exception {
        return v.name.trim();
    }
}