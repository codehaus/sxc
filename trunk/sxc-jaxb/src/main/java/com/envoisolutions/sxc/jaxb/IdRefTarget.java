package com.envoisolutions.sxc.jaxb;

import javax.xml.bind.JAXBException;

public interface IdRefTarget {
    void resolved(Object value) throws JAXBException;
}
