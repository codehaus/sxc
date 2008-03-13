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
}