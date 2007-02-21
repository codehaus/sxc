package com.envoisolutions.sxc.drools;

import java.io.InputStreamReader;

import junit.framework.TestCase;

import com.envoisolutions.sxc.xpath.XPathBuilder;
import com.envoisolutions.sxc.xpath.XPathEvaluator;
import com.envoisolutions.sxc.xpath.XPathEventHandler;

public class DroolsTest extends TestCase {
    String value = null;

    public void test1DroolsRouterNullListeners() throws Exception {
        System.setProperty("streax-xo.output.directory", "target/tmp-xpath");


        XPathBuilder builder = new XPathBuilder();
        builder.addPrefix("c", "urn:customer");
//        XPathEventHandler idHandler = new XPathDroolsEventHandler(new InputStreamReader(getClass().getResourceAsStream("test.drl")), builder);
        builder.listen("/order/address[@country]", new XPathEventHandler());

        XPathEvaluator evaluator = builder.compile();

        evaluator.evaluate(getClass().getResourceAsStream("test.xml"));


    }

    public void test2DroolsRouterNullListeners() throws Exception {
        System.setProperty("streax-xo.output.directory", "target/tmp-xpath");


        XPathBuilder builder = new XPathBuilder();
        builder.addPrefix("c", "urn:customer");
//        XPathEventHandler idHandler = new XPathDroolsEventHandler(new InputStreamReader(getClass().getResourceAsStream("test.drl")), builder);
        builder.listen("/order/address[@country]", new XPathEventHandler());

        XPathEvaluator evaluator = builder.compile();

        evaluator.evaluate(getClass().getResourceAsStream("test.xml"));


    }

    public void test3DroolsRouterNullListeners() throws Exception {
        System.setProperty("streax-xo.output.directory", "target/tmp-xpath");


        XPathBuilder builder = new XPathBuilder();
        builder.addPrefix("c", "urn:customer");
//        XPathEventHandler idHandler = new XPathDroolsEventHandler(new InputStreamReader(getClass().getResourceAsStream("test.drl")), builder);
        builder.listen("/order/address[@country]", new XPathEventHandler());

        XPathEvaluator evaluator = builder.compile();

        evaluator.evaluate(getClass().getResourceAsStream("test.xml"));


    }

    public void test4DroolsRouterNullListeners() throws Exception {
        System.setProperty("streax-xo.output.directory", "target/tmp-xpath");


        XPathBuilder builder = new XPathBuilder();
        builder.addPrefix("c", "urn:customer");
//        XPathEventHandler idHandler = new XPathDroolsEventHandler(new InputStreamReader(getClass().getResourceAsStream("test.drl")), builder);
        builder.listen("/order/address[@country]", new XPathEventHandler());

        XPathEvaluator evaluator = builder.compile();

        evaluator.evaluate(getClass().getResourceAsStream("test.xml"));


    }


    public void testDan() throws Exception {
        System.setProperty("streax-xo.output.directory", "target/tmp-xpath");

//        XPathBuilder builder = new XPathBuilder();
//        builder.addPrefix("c", "urn:customer");
//        XPathDroolsEventHandler idHandler = new XPathDroolsEventHandler(new InputStreamReader(getClass().getResourceAsStream("test.drl")), builder, false);
//        builder.listen("/order/address[@country]", idHandler);
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
