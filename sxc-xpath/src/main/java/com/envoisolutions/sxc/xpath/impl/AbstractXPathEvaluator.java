package com.envoisolutions.sxc.xpath.impl;

import com.envoisolutions.sxc.xpath.XPathEvaluator;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import java.io.InputStream;

public abstract class AbstractXPathEvaluator implements XPathEvaluator {

    protected final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

    public void evaluate(InputStream stream) throws Exception {
        if (stream == null) {
            throw new NullPointerException("InputStream cannot be null!");
        }
        final XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(stream);
        evaluate(reader);
    }

    public void evaluate(Source resourceAsSource) throws Exception {
        if (resourceAsSource == null) {
            throw new NullPointerException("Source cannot be null!");
        }
        final XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(resourceAsSource);
        evaluate(reader);
    }

}
