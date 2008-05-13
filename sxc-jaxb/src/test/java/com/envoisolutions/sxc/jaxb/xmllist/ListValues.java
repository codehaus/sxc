package com.envoisolutions.sxc.jaxb.xmllist;

import javax.xml.bind.annotation.XmlAccessOrder;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorOrder;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlAccessorOrder(XmlAccessOrder.ALPHABETICAL)
@SuppressWarnings({"UnusedDeclaration"})
public class ListValues {
    StringListValue stringListValue;
    BooleanListValue booleanListValue;
    ShortListValue shortListValue;
    IntListValue intListValue;
    LongListValue longListValue;
    FloatListValue floatListValue;
    DoubleListValue doubleListValue;

    StringArrayValue stringArrayValue;
    BooleanArrayValue booleanArrayValue;
    ShortArrayValue shortArrayValue;
    IntArrayValue intArrayValue;
    LongArrayValue longArrayValue;
    FloatArrayValue floatArrayValue;
    DoubleArrayValue doubleArrayValue;
}