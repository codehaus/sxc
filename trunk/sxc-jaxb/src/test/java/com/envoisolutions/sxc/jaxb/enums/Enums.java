package com.envoisolutions.sxc.jaxb.enums;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlType
@XmlAccessorOrder(XmlAccessOrder.ALPHABETICAL)
@XmlRootElement
public class Enums {
    private AnnotatedEnum annotatedEnum;
    private NotAnnotatedEnum notAnnotatedEnum;
    private GeneratedEnum generatedEnum;

    public AnnotatedEnum getAnnotatedEnum() {
        return annotatedEnum;
    }

    public void setAnnotatedEnum(AnnotatedEnum annotatedEnum) {
        this.annotatedEnum = annotatedEnum;
    }

    public NotAnnotatedEnum getNotAnnotatedEnum() {
        return notAnnotatedEnum;
    }

    public void setNotAnnotatedEnum(NotAnnotatedEnum notAnnotatedEnum) {
        this.notAnnotatedEnum = notAnnotatedEnum;
    }

    public GeneratedEnum getGeneratedEnum() {
        return generatedEnum;
    }

    public void setGeneratedEnum(GeneratedEnum generatedEnum) {
        this.generatedEnum = generatedEnum;
    }
}
