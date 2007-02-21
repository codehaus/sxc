package com.envoisolutions.sxc.builder;

import java.io.File;

import javax.xml.namespace.QName;

import com.envoisolutions.sxc.Context;
import com.envoisolutions.sxc.Reader;
import com.envoisolutions.sxc.builder.Builder;
import com.envoisolutions.sxc.builder.CodeBody;
import com.envoisolutions.sxc.builder.ElementParserBuilder;
import com.envoisolutions.sxc.builder.ParserBuilder;
import com.envoisolutions.sxc.builder.impl.BuilderImpl;
import com.envoisolutions.sxc.builder.impl.ElementParserBuilderImpl;
import com.envoisolutions.sxc.customer.Customer;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;

import junit.framework.TestCase;

public class ParserBuilderTest extends TestCase {

    public void testBuilder() throws Exception {
        JCodeModel model = new JCodeModel();
        JType cusClass = model._ref(Customer.class);
        
        Builder builder = new BuilderImpl();
        ElementParserBuilder b = builder.getParserBuilder();
        
        // handle <customer>
        ElementParserBuilder root = b.expectElement(new QName("customer"));
        CodeBody body = root.getBody();
        JVar var = body.decl(cusClass, "customer", JExpr._new(model._ref(Customer.class)));
        body._return(var);
        
        ParserBuilder attBuilder = root.expectAttribute(new QName("att"));
        JVar methodVar = attBuilder.passParentVariable(var);
        JVar attVar = attBuilder.as(String.class);
        body = attBuilder.getBody();
        body.add(methodVar.invoke("setAttribute").arg(attVar));
        
        // handle <id>
        ElementParserBuilder idBuilder = root.expectElement(new QName("id"));
        methodVar = idBuilder.passParentVariable(var);
        JVar id = idBuilder.as(int.class, false);
        body = idBuilder.getBody();
        body.add(methodVar.invoke("setId").arg(id));
        
        // handle <name>
        ElementParserBuilder nameBuilder = root.expectElement(new QName("name"));
        methodVar = nameBuilder.passParentVariable(var);
        JVar name = nameBuilder.as(String.class, false);
        body = nameBuilder.getBody();
        body.add(methodVar.invoke("setName").arg(name));
        
        builder.write(new File("target/tmp"));
        
        // Compile written classes
        Context context = builder.compile();
        
        // unmarshal the xml
        Reader reader = context.createReader();
        Object object = reader.read(getClass().getResourceAsStream("/com/envoisolutions/sxc/customer/customer.xml"));
        
        assertTrue(object instanceof Customer);
        
        Customer c = (Customer) object;
        assertEquals(1, c.getId());
        assertEquals("Dan Diephouse", c.getName());
        assertEquals("attValue", c.getAttribute());
    }
}
