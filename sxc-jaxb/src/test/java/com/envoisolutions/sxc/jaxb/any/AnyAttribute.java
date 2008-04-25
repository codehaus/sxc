package com.envoisolutions.sxc.jaxb.any;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorOrder;
import javax.xml.bind.annotation.XmlAccessOrder;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.namespace.QName;

@XmlType
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlAccessorOrder(XmlAccessOrder.ALPHABETICAL)
@SuppressWarnings({"UnusedDeclaration"})
public class AnyAttribute {
    @XmlAttribute
    private String one;

    @XmlAttribute
    private String two;

    @XmlAttribute
    private String three;

    @XmlAnyAttribute
    private LinkedHashMap<QName,String> attributes;

    public String getOne() {
        return one;
    }

    public String getTwo() {
        return two;
    }

    public String getThree() {
        return three;
    }

    public Map<QName, String> getAttributes() {
        return attributes;
    }
}