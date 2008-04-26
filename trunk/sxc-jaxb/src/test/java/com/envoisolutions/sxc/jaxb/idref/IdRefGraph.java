package com.envoisolutions.sxc.jaxb.idref;

import java.util.List;
import java.util.ArrayList;
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
public class IdRefGraph {
    private List<IdRefNode> node;

    public List<IdRefNode> getNode() {
        if (node == null) node = new ArrayList<IdRefNode>();
        return node;
    }
}