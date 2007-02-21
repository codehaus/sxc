package com.envoisolutions.sxc.builder;

import java.io.File;
import java.util.Map;

import javax.xml.namespace.QName;

import com.envoisolutions.sxc.Context;
import com.envoisolutions.sxc.Reader;
import com.envoisolutions.sxc.builder.Builder;
import com.envoisolutions.sxc.builder.CodeBody;
import com.envoisolutions.sxc.builder.ElementParserBuilder;
import com.envoisolutions.sxc.builder.ParserBuilder;
import com.envoisolutions.sxc.builder.impl.BuilderImpl;
import com.envoisolutions.sxc.builder.impl.ElementParserBuilderImpl;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;

import junit.framework.TestCase;

public class GlobalElementTest extends TestCase {

    public void testBuilder() throws Exception {
        JCodeModel model = new JCodeModel();
        JType mapClass = model._ref(Map.class);
        
        Builder builder = new BuilderImpl();
        ElementParserBuilder b = builder.getParserBuilder();
        
        // handle <root>
        ElementParserBuilder root = b.expectElement(new QName("root"));
        CodeBody body = root.getBody();
        JVar var = body.decl(mapClass, "map", JExpr.direct("context"));
        body._return(var);
        
        // handle <id>
        ParserBuilder globalBuilder = root.globalElement(new QName("global"));
        JVar bal = globalBuilder.as(String.class);
        body = globalBuilder.getBody();
        body.add(JExpr.direct("context").invoke("put").arg(bal).arg(bal));

        builder.write(new File("target/tmp-global"));
        
        // Compile written classes
        Context context = builder.compile();
        
        // unmarshal the xml
        Reader reader = context.createReader();
        Object object = reader.read(getClass().getResourceAsStream("/com/envoisolutions/sxc/builder/global.xml"));
        
        assertNotNull(object);
        assertTrue(object instanceof Map);
        
        Map c = (Map) object;
        assertEquals("global1", c.get("global1"));
    }
}
