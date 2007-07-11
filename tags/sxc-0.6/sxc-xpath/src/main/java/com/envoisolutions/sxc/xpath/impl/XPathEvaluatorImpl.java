package com.envoisolutions.sxc.xpath.impl;

import com.envoisolutions.sxc.Context;
import com.envoisolutions.sxc.Reader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

public class XPathEvaluatorImpl extends AbstractXPathEvaluator {
    XMLInputFactory xif = XMLInputFactory.newInstance();
    private Reader reader;

    public XPathEvaluatorImpl(Context context) {
        this.reader = context.createReader();
    }

    public void evaluate(XMLStreamReader xsr) throws Exception {
        reader.read(xsr);
    }

}
