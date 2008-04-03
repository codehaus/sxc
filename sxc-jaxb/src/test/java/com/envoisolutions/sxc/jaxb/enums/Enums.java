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
    private AnnotatedEnum annotatedEnumAttribute;
    private NotAnnotatedEnum notAnnotatedEnumAttribute;
    private GeneratedEnum generatedEnumAttribute;

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

    @XmlAttribute
    public AnnotatedEnum getAnnotatedEnumAttribute() {
        return annotatedEnumAttribute;
    }

    public void setAnnotatedEnumAttribute(AnnotatedEnum annotatedEnumAttribute) {
        this.annotatedEnumAttribute = annotatedEnumAttribute;
    }

    @XmlAttribute
    public NotAnnotatedEnum getNotAnnotatedEnumAttribute() {
        return notAnnotatedEnumAttribute;
    }

    public void setNotAnnotatedEnumAttribute(NotAnnotatedEnum notAnnotatedEnumAttribute) {
        this.notAnnotatedEnumAttribute = notAnnotatedEnumAttribute;
    }

    @XmlAttribute
    public GeneratedEnum getGeneratedEnumAttribute() {
        return generatedEnumAttribute;
    }

    public void setGeneratedEnumAttribute(GeneratedEnum generatedEnumAttribute) {
        this.generatedEnumAttribute = generatedEnumAttribute;
    }
}
