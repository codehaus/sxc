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
package com.envoisolutions.sxc.jaxb.listener;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorOrder;
import javax.xml.bind.annotation.XmlAccessOrder;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Marshaller;

import junit.framework.Assert;

@XmlType
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlAccessorOrder(XmlAccessOrder.ALPHABETICAL)
@SuppressWarnings({"UnusedDeclaration"})
public class Listener {
    private String name;
    private Listener listener;
    @XmlTransient
    private Unmarshaller beforeUnmarshaller;
    @XmlTransient
    private Object beforeParent;
    @XmlTransient
    private Unmarshaller afterUnmarshaller;
    @XmlTransient
    private Object afterParent;
    @XmlTransient
    private Marshaller beforeMarshaller;
    @XmlTransient
    private Marshaller afterMarshaller;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Listener getListener() {
        return listener;
    }

    public void resetCallbackData() {
        beforeUnmarshaller = null;
        beforeParent = null;
        afterUnmarshaller = null;
        afterParent = null;
        beforeMarshaller = null;
        afterMarshaller = null;
    }

    public void assertUnmarhsalCallbacks(Unmarshaller unmarshaller, Object parent) {
        Assert.assertNotNull("beforeUnmarshaller is null", beforeUnmarshaller);
        Assert.assertNotNull("afterUnmarshaller is null", afterUnmarshaller);
        Assert.assertSame(unmarshaller, beforeUnmarshaller);
        Assert.assertSame(unmarshaller, afterUnmarshaller);

        if (parent == null) {
            Assert.assertNull("listener.getBeforeParent() is not null", beforeParent);
            Assert.assertNull("listener.getAfterParent() is not null", afterParent);
        } else {
            Assert.assertNotNull("beforeParent is null", beforeParent);
            Assert.assertNotNull("afterParent is null", afterParent);
            Assert.assertSame(parent, beforeParent);
            Assert.assertSame(parent, afterParent);
        }

        Assert.assertNull("beforeMarshaller is not null", beforeMarshaller);
        Assert.assertNull("afterMarshaller is not null", afterMarshaller);
    }
    public void assertMarhsalCallbacks(Marshaller marshaller) {
        Assert.assertNotNull("beforeMarshaller is null", beforeMarshaller);
        Assert.assertNotNull("afterMarshaller is null", afterMarshaller);
        Assert.assertSame(marshaller, beforeMarshaller);
        Assert.assertSame(marshaller, afterMarshaller);

        Assert.assertNull("beforeUnmarshaller is not null", beforeUnmarshaller);
        Assert.assertNull("afterUnmarshaller is not null", afterUnmarshaller);
        Assert.assertNull("beforeParent is not null", beforeParent);
        Assert.assertNull("afterParent is not null", afterParent);
    }

    //
    // Callbacks
    //

    private void beforeUnmarshal(Unmarshaller unmarshaller, Object parent) {
        this.beforeUnmarshaller = unmarshaller;
        this.beforeParent = parent;
    }

    private void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
        this.afterUnmarshaller = unmarshaller;
        this.afterParent = parent;
    }

    private void beforeMarshal(Marshaller marshaller) {
        this.beforeMarshaller = marshaller;
    }

    private void afterMarshal(Marshaller marshaller) {
        afterMarshaller = marshaller;
    }
}
