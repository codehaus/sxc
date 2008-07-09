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
package com.envoisolutions.sxc.jaxb.array;

import javax.xml.bind.annotation.XmlAccessOrder;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorOrder;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
@XmlAccessorOrder(XmlAccessOrder.ALPHABETICAL)
public class Arrays {
    //
    // Public fields

    // intefaces type fields
    public String[] stringField;
    public boolean[] booleanField;
    public byte[] byteField;
    public char[] charField;
    public short[] shortField;
    public int[] intField;
    public long[] longField;
    public float[] floatField;
    public double[] doubleField;

    // field is final and JaxB impl will fill in a value later
    public final String[] finalField = null;

    //
    // Properties

    // intefaces type properties
    private String[] stringProperty;
    private boolean[] booleanProperty;
    private byte[] byteProperty;
    private char[] charProperty;
    private short[] shortProperty;
    private int[] intProperty;
    private long[] longProperty;
    private float[] floatProperty;
    private double[] doubleProperty;

    public String[] getStringProperty() {
        return stringProperty;
    }

    public void setStringProperty(String[] stringProperty) {
        this.stringProperty = stringProperty;
    }

    public boolean[] getBooleanProperty() {
        return booleanProperty;
    }

    public void setBooleanProperty(boolean[] booleanProperty) {
        this.booleanProperty = booleanProperty;
    }

    public byte[] getByteProperty() {
        return byteProperty;
    }

    public void setByteProperty(byte[] byteProperty) {
        this.byteProperty = byteProperty;
    }

    public char[] getCharProperty() {
        return charProperty;
    }

    public void setCharProperty(char[] charProperty) {
        this.charProperty = charProperty;
    }

    public short[] getShortProperty() {
        return shortProperty;
    }

    public void setShortProperty(short[] shortProperty) {
        this.shortProperty = shortProperty;
    }

    public int[] getIntProperty() {
        return intProperty;
    }

    public void setIntProperty(int[] intProperty) {
        this.intProperty = intProperty;
    }

    public long[] getLongProperty() {
        return longProperty;
    }

    public void setLongProperty(long[] longProperty) {
        this.longProperty = longProperty;
    }

    public float[] getFloatProperty() {
        return floatProperty;
    }

    public void setFloatProperty(float[] floatProperty) {
        this.floatProperty = floatProperty;
    }

    public double[] getDoubleProperty() {
        return doubleProperty;
    }

    public void setDoubleProperty(double[] doubleProperty) {
        this.doubleProperty = doubleProperty;
    }
}