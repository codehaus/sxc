package com.envoisolutions.sxc.jaxb;

import java.util.Collection;
import javax.xml.bind.JAXBException;

public class CollectionRefTarget implements IdRefTarget {
    private Collection collection;

    public CollectionRefTarget(Collection collection) {
        this.collection = collection;
    }

    @SuppressWarnings({"unchecked"})
    public void resolved(Object value) throws JAXBException {
        collection.add(value);
    }
}