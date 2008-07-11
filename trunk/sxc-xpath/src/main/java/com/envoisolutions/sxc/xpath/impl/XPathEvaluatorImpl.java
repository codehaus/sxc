package com.envoisolutions.sxc.xpath.impl;

import com.envoisolutions.sxc.Context;

import javax.xml.stream.XMLStreamReader;

public class XPathEvaluatorImpl extends AbstractXPathEvaluator {
    private final Context context;

    public XPathEvaluatorImpl(Context context) {
        this.context = context;
    }

    public void evaluate(XMLStreamReader xsr) throws Exception {
        context.createReader().read(xsr);
    }

}
