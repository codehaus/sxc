/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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
