package com.envoisolutions.sxc.jaxb.enums;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

@XmlEnum
public enum GeneratedEnum {

    @XmlEnumValue("Silver")
    SILVER("Silver"),
    @XmlEnumValue("Bronze")
    BRONZE("Bronze"),
    @XmlEnumValue("Gold")
    GOLD("Gold");
    private final String value;

    GeneratedEnum(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static GeneratedEnum fromValue(String v) {
        for (GeneratedEnum c: GeneratedEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
