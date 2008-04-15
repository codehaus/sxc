package com.envoisolutions.sxc.jaxb.properties;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlAccessorOrder;
import javax.xml.bind.annotation.XmlAccessOrder;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElement;

import junit.framework.Assert;

@XmlType
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlAccessorOrder(XmlAccessOrder.ALPHABETICAL)
@SuppressWarnings({"UnusedDeclaration"})
public class Properties {
    private String publicProperty;
    private String packageProperty;
    private String protectedProperty;
    private String privateProperty;

    private boolean booleanProperty;
    private byte byteProperty;
    private short shortProperty;
    private int intProperty;
    private long longProperty;
    private float floatProperty;
    private double doubleProperty;

    @XmlElement(name = "public-property")
    public String getPublicProperty() {
        return publicProperty;
    }

    public void setPublicProperty(String publicProperty) {
        this.publicProperty = publicProperty;
    }

    @XmlElement(name = "package-property")
    String getPackageProperty() {
        return packageProperty;
    }

    void setPackageProperty(String packageProperty) {
        this.packageProperty = packageProperty;
    }

    @XmlElement(name = "protected-property")
    protected String getProtectedProperty() {
        return protectedProperty;
    }

    protected void setProtectedProperty(String protectedProperty) {
        this.protectedProperty = protectedProperty;
    }

    @XmlElement(name = "private-property")
    private String getPrivateProperty() {
        return privateProperty;
    }

    private void setPrivateProperty(String privateProperty) {
        this.privateProperty = privateProperty;
    }

    private boolean isBooleanProperty() {
        return booleanProperty;
    }

    private void setBooleanProperty(boolean booleanProperty) {
        this.booleanProperty = booleanProperty;
    }

    private byte getByteProperty() {
        return byteProperty;
    }

    private void setByteProperty(byte byteProperty) {
        this.byteProperty = byteProperty;
    }

    private short getShortProperty() {
        return shortProperty;
    }

    private void setShortProperty(short shortProperty) {
        this.shortProperty = shortProperty;
    }

    private int getIntProperty() {
        return intProperty;
    }

    private void setIntProperty(int intProperty) {
        this.intProperty = intProperty;
    }

    private long getLongProperty() {
        return longProperty;
    }

    private void setLongProperty(long longProperty) {
        this.longProperty = longProperty;
    }

    private float getFloatProperty() {
        return floatProperty;
    }

    private void setFloatProperty(float floatProperty) {
        this.floatProperty = floatProperty;
    }

    private double getDoubleProperty() {
        return doubleProperty;
    }

    private void setDoubleProperty(double doubleProperty) {
        this.doubleProperty = doubleProperty;
    }

    public void assertPublicProperty(String expected) {
        Assert.assertEquals(expected, publicProperty);
    }

    public void assertPackageProperty(String expected) {
        Assert.assertEquals(expected, packageProperty);
    }

    public void assertProtectedProperty(String expected) {
        Assert.assertEquals(expected, protectedProperty);
    }

    public void assertPrivateProperty(String expected) {
        Assert.assertEquals(expected, privateProperty);
    }

    public void assertBooleanProperty(boolean expected) {
        Assert.assertEquals(expected, booleanProperty);
    }

    public void assertByteProperty(byte expected) {
        Assert.assertEquals(expected, byteProperty);
    }

    public void assertShortProperty(short expected) {
        Assert.assertEquals(expected, shortProperty);
    }

    public void assertIntProperty(int expected) {
        Assert.assertEquals(expected, intProperty);
    }

    public void assertLongProperty(long expected) {
        Assert.assertEquals(expected, longProperty);
    }

    public void assertFloatProperty(float expected) {
        Assert.assertEquals(expected, floatProperty);
    }

    public void assertDoubleProperty(double expected) {
        Assert.assertEquals(expected, doubleProperty);
    }
}