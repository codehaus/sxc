package com.envoisolutions.sxc.jaxb.enums;

import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlEnum;

@XmlType
@XmlEnum
public enum AnnotatedEnum {
    @XmlEnumValue("uno")ONE,
    @XmlEnumValue("dos")TWO,
    @XmlEnumValue("tres")THREE
}
