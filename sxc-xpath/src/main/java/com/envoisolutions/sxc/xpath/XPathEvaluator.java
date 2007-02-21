package com.envoisolutions.sxc.xpath;

import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import java.io.InputStream;

public interface XPathEvaluator {
    public void evaluate(XMLStreamReader reader) throws Exception;

    public void evaluate(InputStream resourceAsStream) throws Exception;

    public void evaluate(Source resourceAsSource) throws Exception;
}
