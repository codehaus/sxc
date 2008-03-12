package com.envoisolutions.sxc.jaxb.fields;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlAccessorOrder;
import javax.xml.bind.annotation.XmlAccessOrder;
import javax.xml.bind.annotation.XmlRootElement;

import junit.framework.Assert;

@XmlType
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlAccessorOrder(XmlAccessOrder.ALPHABETICAL)
public class Fields {
    public String publicField;
    String packageField;
    protected String protectedField;
    private String privateField;

    private boolean booleanField;
    private byte byteField;
    private short shortField;
    private int intField;
    private long longField;
    private float floatField;
    private double doubleField;

    public void assertPublicField(String expected) {
        Assert.assertEquals(expected, publicField);
    }

    public void assertPackageField(String expected) {
        Assert.assertEquals(expected, packageField);
    }

    public void assertProtectedField(String expected) {
        Assert.assertEquals(expected, protectedField);
    }

    public void assertPrivateField(String expected) {
        Assert.assertEquals(expected, privateField);
    }

    public void assertBooleanField(boolean expected) {
        Assert.assertEquals(expected, booleanField);
    }

    public void assertByteField(byte expected) {
        Assert.assertEquals(expected, byteField);
    }

    public void assertShortField(short expected) {
        Assert.assertEquals(expected, shortField);
    }

    public void assertIntField(int expected) {
        Assert.assertEquals(expected, intField);
    }

    public void assertLongField(long expected) {
        Assert.assertEquals(expected, longField);
    }

    public void assertFloatField(float expected) {
        Assert.assertEquals(expected, floatField);
    }

    public void assertDoubleField(double expected) {
        Assert.assertEquals(expected, doubleField);
    }
}
