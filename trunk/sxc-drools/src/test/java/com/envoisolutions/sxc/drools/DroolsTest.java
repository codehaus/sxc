package com.envoisolutions.sxc.drools;

import java.io.InputStreamReader;

import junit.framework.TestCase;

import com.envoisolutions.sxc.xpath.XPathBuilder;
import com.envoisolutions.sxc.xpath.XPathEvaluator;
import com.envoisolutions.sxc.xpath.XPathEventHandler;

public class DroolsTest extends TestCase {
    String value = null;

    public void testDan() throws Exception {
        System.setProperty("streax-xo.output.directory", "target/tmp-xpath");

        final DroolsXPathEvaluatorFactory evaluatorFactory = 
            new DroolsXPathEvaluatorFactory();
        evaluatorFactory.setPackageStream(getClass().getResourceAsStream("test.drl"));
        evaluatorFactory.addPrefix("c", "urn:customer");
        
        XPathEvaluator evaluator = evaluatorFactory.create();
        // FactHandle someFactHandle = evaluator.assertObject(someObject);
        evaluator.evaluate(getClass().getResourceAsStream("test.xml"));
        // evaluator.getObject(someFactHandle);
//        idHandler.getWorkingMemory().fireAllRules();


    }


}
