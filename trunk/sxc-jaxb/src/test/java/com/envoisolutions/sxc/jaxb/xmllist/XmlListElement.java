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
package com.envoisolutions.sxc.jaxb.xmllist;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessOrder;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorOrder;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlAttribute;

@XmlType
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlAccessorOrder(XmlAccessOrder.ALPHABETICAL)
@SuppressWarnings({"UnusedDeclaration"})
public class XmlListElement {
    @XmlList public List<String> stringList;
    @XmlList public List<Boolean> booleanList;
    @XmlList public List<Short> shortList;
    @XmlList public List<Integer> intList;
    @XmlList public List<Long> longList;
    @XmlList public List<Float> floatList;
    @XmlList public List<Double> doubleList;

    @XmlList public String[] stringArray;
    @XmlList public boolean[] booleanArray;
    @XmlList public short[] shortArray;
    @XmlList public int[] intArray;
    @XmlList public long[] longArray;
    @XmlList public float[] floatArray;
    @XmlList public double[] doubleArray;

    @XmlAttribute public List<String> stringListAttribute;
    @XmlAttribute public List<Boolean> booleanListAttribute;
    @XmlAttribute public List<Byte> byteListAttribute;
    @XmlAttribute public List<Short> shortListAttribute;
    @XmlAttribute public List<Integer> intListAttribute;
    @XmlAttribute public List<Long> longListAttribute;
    @XmlAttribute public List<Float> floatListAttribute;
    @XmlAttribute public List<Double> doubleListAttribute;

    @XmlAttribute public String[] stringArrayAttribute;
    @XmlAttribute public boolean[] booleanArrayAttribute;
    @XmlAttribute public short[] shortArrayAttribute;
    @XmlAttribute public int[] intArrayAttribute;
    @XmlAttribute public long[] longArrayAttribute;
    @XmlAttribute public float[] floatArrayAttribute;
    @XmlAttribute public double[] doubleArrayAttribute;
}