package com.envoisolutions.sxc.xpath;

import java.io.InputStream;

import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;

public interface XPathEvaluator {
    public void evaluate(XMLStreamReader reader) throws Exception;

    public void evaluate(InputStream resourceAsStream) throws Exception;

    public void evaluate(Source resourceAsSource) throws Exception;
}
