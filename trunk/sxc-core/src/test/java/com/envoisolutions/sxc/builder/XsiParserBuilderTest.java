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

public class XsiParserBuilderTest extends TestCase {
    String nodeNs = "http://envoisolutions.com/node";
    JType nodeClass;
    JType namedNodeClass;
    private ElementParserBuilder childNodeBuilder;
    private ElementParserBuilder childNamedNodeBuilder;
    
    public void testBuilder() throws Exception {
        System.setProperty("streax-xo.output.directory", "target/tmp-jaxb");
        Builder builder = new BuilderImpl();
        ElementParserBuilder b = builder.getParserBuilder();
        
        JCodeModel model = b.getCodeModel();
        nodeClass = model._ref(Node.class);
        namedNodeClass = b.getCodeModel()._ref(NamedNode.class);

        addExpectXsis(b);
        
        // Compile written classes
        Context context = builder.compile();
        
        // unmarshal the xml
        Reader reader = context.createReader();
        
//        XMLStreamReader xsr = XMLInputFactory.newInstance().createXMLStreamReader(getClass().getResourceAsStream("/com/envoisolutions/sxc/jaxb/node/node.xml"));
//        Object object = new streax.generated.Reader().read(new XoXMLStreamReaderImpl(xsr), null);
        Object object = 
            reader.read(getClass().getResourceAsStream("node2.xml"));
        assertNotNull(object);
        assertTrue(object instanceof NamedNode);
        
        NamedNode n = (NamedNode) object;
        assertEquals("root", n.getName());
        assertEquals(2, n.getNode().size());
        
        Node child = n.getNode().get(0);
        assertEquals(1, child.getNode().size());
        
        child = child.getNode().get(0);
        assertEquals(1, child.getNode().size());
        
        child = child.getNode().get(0);
        assertEquals(1, child.getNode().size());

        child = child.getNode().get(0);
        assertEquals(1, child.getNode().size());

        child = child.getNode().get(0);
        assertEquals(1, child.getNode().size());

        child = child.getNode().get(0);
        assertEquals(1, child.getNode().size());

        child = child.getNode().get(0);
        assertNotNull(child);
    }

    private void addExpectXsis(ElementParserBuilder b) {
        ElementParserBuilder nodeB = b.expectXsiType(new QName(nodeNs, "Node"));
        addExpectNode(nodeB);
        
        ElementParserBuilder namedNodeB = b.expectXsiType(new QName(nodeNs, "NamedNode"));
        addExpectNamedNode(namedNodeB);
    }
    
    private void addExpectNode(ElementParserBuilder b) {
        JVar nodeVar = b.getBody().decl(nodeClass, "node", JExpr._new(nodeClass));
        b.getBody()._return(nodeVar);
        // b.getVariables().add(nodeVar);
        
        nodeExpectChildNode(b, nodeVar);
    }
    
    private void addExpectNamedNode(ElementParserBuilder b) {
        JVar nodeVar = b.getBody().decl(namedNodeClass, "node", JExpr._new(namedNodeClass));
        b.getBody()._return(nodeVar);
        // b.getVariables().add(nodeVar);
        
        namedNodeExpectChildNode(b, nodeVar);
        
        ElementParserBuilder nameB = b.expectElement(new QName(nodeNs, "name"));
        JVar nodeVar2 = nameB.passParentVariable(nodeVar);
        JVar var2 = nameB.as(String.class, false);
        nameB.getBody().add(nodeVar2.invoke("setName").arg(var2));
    }

    private void namedNodeExpectChildNode(ElementParserBuilder b, JVar nodeVar) {
        childNamedNodeBuilder = b.expectElement(new QName(nodeNs, "node"));
        addNodeHandling(childNamedNodeBuilder, nodeVar);
    }
    
    private void nodeExpectChildNode(ElementParserBuilder b, JVar nodeVar) {
        childNodeBuilder = b.expectElement(new QName(nodeNs, "node"));
        addNodeHandling(childNodeBuilder, nodeVar);
    }
    
    private void addNodeHandling(ElementParserBuilder b, JVar parentNodeVar) {
        JVar parentNodeVar2 = b.passParentVariable(parentNodeVar);

        handleXsiNode(b, parentNodeVar2);
        
        handleXsiNamedNode(b, parentNodeVar2);
        
        handleNoXsi(b, parentNodeVar2);
    }

    private void handleNoXsi(ElementParserBuilder b, JVar parentNodeVar2) {
        JVar var2 = b.getBody().decl(nodeClass, "node", JExpr._new(nodeClass));
        b.getBody().add(parentNodeVar2.invoke("getNode").invoke("add").arg(var2));

        b.expectElement(new QName(nodeNs, "node"), childNodeBuilder, var2);
    }

    private void handleXsiNode(ElementParserBuilder b, JVar parentNodeVar2) {
        ElementParserBuilder nodeB = b.expectXsiType(new QName(nodeNs, "Node"));
        JVar parentNodeVar3 = nodeB.passParentVariable(parentNodeVar2);
        
        CodeBody body = nodeB.getBody();
        JVar var = body.decl(nodeClass, "node", JExpr._new(nodeClass));
        body.add(parentNodeVar3.invoke("getNode").invoke("add").arg(var));
        
        nodeB.expectElement(new QName(nodeNs, "node"), childNodeBuilder, var);
    }

    private void handleXsiNamedNode(ElementParserBuilder b, JVar parentNodeVar2) {
        ElementParserBuilder namedNodeB = b.expectXsiType(new QName(nodeNs, "NamedNode"));
        JVar parentNodeVar3 = namedNodeB.passParentVariable(parentNodeVar2);
        
        CodeBody body = namedNodeB.getBody();
        JVar var = body.decl(namedNodeClass, "node", JExpr._new(namedNodeClass));
        body.add(parentNodeVar3.invoke("getNode").invoke("add").arg(var));
        
        if (childNamedNodeBuilder == null) {
            namedNodeExpectChildNode(b, parentNodeVar3);
        } else {
            namedNodeB.expectElement(new QName(nodeNs, "node"), childNamedNodeBuilder, var);
        }
    }
}
