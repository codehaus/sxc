package com.envoisolutions.sxc.builder;

import javax.xml.namespace.QName;

import junit.framework.TestCase;

import com.envoisolutions.sxc.Context;
import com.envoisolutions.sxc.Reader;
import com.envoisolutions.sxc.builder.impl.BuilderImpl;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;

public class ParserBuilderTest extends TestCase {
    public void testBuilder() throws Exception {
        // START SNIPPET: parser
        // Create a Parser/Writer Builder 
        Builder builder = new BuilderImpl();
        ElementParserBuilder b = builder.getParserBuilder();
        
        // Get the CodeModel - an API for building Java classes
        JCodeModel model = builder.getCodeModel();
        JType cusClass = model._ref(Customer.class);
        
        // Tell SXC to expect a <customer> element
        ElementParserBuilder root = b.expectElement(new QName("customer"));
        // When we see <customer> create a new Customer object
        CodeBody body = root.getBody();
        JVar var = body.decl(cusClass, "customer", JExpr._new(model._ref(Customer.class)));
        // Return the customer object once we're done parsing
        body._return(var);
        
        // Tell SXC to expect an attribute "id" on the <customer> element
        ParserBuilder idBuilder = root.expectAttribute(new QName("id"));
        // Pass the customer object to the ParserBuilder that handles the ID
        JVar methodVar = idBuilder.passParentVariable(var);
        // Read the attribute as a int. 
        JVar attVar = idBuilder.as(int.class);
        // pass this variable to setId on the customer object
        idBuilder.getBody().add(methodVar.invoke("setId").arg(attVar));
        
        // Handle the <name> element
        ElementParserBuilder nameBuilder = root.expectElement(new QName("name"));
        // Pass the Customer object to this builder
        methodVar = nameBuilder.passParentVariable(var);
        // Read the element as a non nillable String
        JVar name = nameBuilder.as(String.class, false);
        // Call setName with this String as an argument
        nameBuilder.getBody().add(methodVar.invoke("setName").arg(name));

        // Compile the written classes
        Context context = builder.compile();
        
        // unmarshal the xml
        Reader reader = context.createReader();
        Object object = reader.read(getClass().getResourceAsStream("customer.xml"));
        
        assertTrue(object instanceof Customer);
        
        Customer c = (Customer) object;
        assertEquals(1, c.getId());
        assertEquals("Dan Diephouse", c.getName());
        // END SNIPPET: parser
    }
}
