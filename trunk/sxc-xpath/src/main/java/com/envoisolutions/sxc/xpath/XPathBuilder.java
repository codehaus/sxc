package com.envoisolutions.sxc.xpath;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import com.envoisolutions.sxc.Context;
import com.envoisolutions.sxc.builder.Builder;
import com.envoisolutions.sxc.builder.CodeBody;
import com.envoisolutions.sxc.builder.ElementParserBuilder;
import com.envoisolutions.sxc.builder.ParserBuilder;
import com.envoisolutions.sxc.builder.impl.BuilderImpl;
import com.envoisolutions.sxc.xpath.impl.XPathEvaluatorImpl;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;

import org.jaxen.JaxenHandler;
import org.jaxen.expr.AllNodeStep;
import org.jaxen.expr.EqualityExpr;
import org.jaxen.expr.Expr;
import org.jaxen.expr.LiteralExpr;
import org.jaxen.expr.LocationPath;
import org.jaxen.expr.NameStep;
import org.jaxen.expr.Predicate;
import org.jaxen.expr.TextNodeStep;
import org.jaxen.expr.XPathExpr;
import org.jaxen.saxpath.Axis;
import org.jaxen.saxpath.SAXPathException;
import org.jaxen.saxpath.helpers.XPathReaderFactory;

public class XPathBuilder {
    private Map<String,String> namespaceContext;
    private ElementParserBuilder parserBldr;
    
    private Map<String, XPathEventHandler> eventHandlers = new HashMap<String, XPathEventHandler>();
    private Map<String, Object> vars = new HashMap<String, Object>();
    private JType eventHandlerType;
    private JType stringType;
    private Builder builder;
    private JType eventType;
    
    public XPathBuilder() {
        super();
        
        builder = new BuilderImpl();
        parserBldr = builder.getParserBuilder();
        
        eventHandlerType = parserBldr.getCodeModel()._ref(XPathEventHandler.class);
        eventType = parserBldr.getCodeModel()._ref(XPathEvent.class);
        stringType = parserBldr.getCodeModel()._ref(String.class);
        
    }

    public void listen(String expr, XPathEventHandler handler) {
        eventHandlers.put(expr, handler);
    }
    
    public XPathEvaluator compile() {
        for (Map.Entry<String, XPathEventHandler> e : eventHandlers.entrySet()) {
            compileEventHandler(e.getKey(), e.getValue());
        }
        
        Context context = builder.compile();
        context.putAll(vars);
        
        return new XPathEvaluatorImpl(context);
    }
    
    public void compileEventHandler(String expr, XPathEventHandler eventHandler) {
        String varName = "obj" + vars.size();
        vars.put(varName, eventHandler);
        
        ParserBuilder xpathBuilder = parserBldr;
        try {
            org.jaxen.saxpath.XPathReader reader = XPathReaderFactory.createReader();
            
            JaxenHandler handler = new JaxenHandler();
            reader.setXPathHandler(handler);
            reader.parse(expr);
            
            XPathExpr path = handler.getXPathExpr(true);
            
            xpathBuilder = handleExpression(parserBldr, path.getRootExpr());
        } catch (SAXPathException e) {
            throw new XPathException(e);
        }
        
        CodeBody body = xpathBuilder.getBody();
        
        // grab the event handler out of the context
        JVar handlerVar = body.decl(eventHandlerType, varName, 
                                    JExpr.cast(eventHandlerType, 
                                               JExpr.direct("context").invoke("get").arg(varName)));

        body.add(handlerVar.invoke("onMatch").arg(JExpr._new(eventType).arg(JExpr.lit(expr)).arg(xpathBuilder.getXSR())));
    }

    private ParserBuilder handleExpression(ElementParserBuilder xpathBuilder, Expr expr) {
        if (expr instanceof LocationPath) {
            return handle(xpathBuilder, (LocationPath) expr);
        } else if (expr instanceof EqualityExpr) {
            return handle(xpathBuilder, (EqualityExpr) expr);
        } else if (expr instanceof LiteralExpr) {
            return handle(xpathBuilder, (LiteralExpr) expr);
        } else {
            System.out.println("UNKNOWN EXPRESSION: " + expr );
            System.out.println(expr.getClass().getName());
        }
        return xpathBuilder;
    }

    private ParserBuilder handle(ElementParserBuilder xpathBuilder, LiteralExpr expr) {
        xpathBuilder.getBody().decl(stringType, "_literal", JExpr.lit(expr.getLiteral()));
        return xpathBuilder;
    }

    private ParserBuilder handle(ElementParserBuilder xpathBuilder, EqualityExpr expr) {
        handleExpression(xpathBuilder, expr.getLHS());
        handleExpression(xpathBuilder, expr.getRHS());
        
        JBlock block = xpathBuilder.getBody().getBlock();
        JBlock ifBlock = block._if(JExpr.direct("_literal").invoke("equals").arg(JExpr.direct("_value")))._then();
        
        return xpathBuilder.newState(ifBlock);
    }

    private ParserBuilder handle(ElementParserBuilder xpathBuilder, LocationPath path) {
        ParserBuilder returnBuilder = xpathBuilder;
        
        // look for the next part on all child elements
        boolean globalElement = false;
        for (Iterator itr = path.getSteps().iterator(); itr.hasNext();) {
            Object o = itr.next();
            
            if (o instanceof NameStep) {
                returnBuilder = handleNameStep(returnBuilder, (NameStep) o, globalElement);
                globalElement = false;
            } else if (o instanceof AllNodeStep) {
                globalElement = true;
            } else if (o instanceof TextNodeStep) {
                returnBuilder = handleTextNodeStep(returnBuilder, (TextNodeStep) o);
            } else {
                System.out.println("UNKNOWN STEP: " + o);
            }
        }
        
        return returnBuilder;
    }

    private ParserBuilder handleTextNodeStep(ParserBuilder returnBuilder, TextNodeStep step) {
        JVar var = returnBuilder.as(String.class);
        returnBuilder.getBody().decl(returnBuilder.getCodeModel()._ref(String.class), "_value", var);
        return returnBuilder;
    }

    private ParserBuilder handleNameStep(ParserBuilder returnBuilder, NameStep step, boolean globalElement) {
        String prefix = step.getPrefix();
        String ns = "";
        if (prefix != null && !prefix.equals("")) {
            ns = namespaceContext.get(prefix);
            
            if (ns == null) {
                throw new XPathException("Could not find namespace for prefix: " + prefix);
            }
        }
        
        QName n = new QName(ns, step.getLocalName());
        
        ElementParserBuilder elBuilder = ((ElementParserBuilder) returnBuilder);
        if (step.getAxis() == Axis.CHILD) {
            if (n.getLocalPart().equals("*")) {
                returnBuilder = elBuilder.expectAnyElement();
            } else if (globalElement) {
                returnBuilder = elBuilder.globalElement(n);
            } else {
                returnBuilder = elBuilder.expectElement(n);
            }
        } else if (step.getAxis() == Axis.ATTRIBUTE) {
            returnBuilder = elBuilder.expectAttribute(n);
        } else {
            System.out.println("UNKNOWN AXIS " + step.getAxis());
        }
        
        return handlePredicates(returnBuilder, step.getPredicateSet().getPredicates());
    }

    private ParserBuilder handlePredicates(ParserBuilder returnBuilder, List predicates) {
        for (Iterator pitr = predicates.iterator(); pitr.hasNext();) {
            Predicate p = (Predicate) pitr.next();
            
            Expr expr = p.getExpr();
            returnBuilder = handleExpression((ElementParserBuilder) returnBuilder, expr);
        }
        return returnBuilder;
    }

    public Map<String, String> getNamespaceContext() {
        return namespaceContext;
    }

    public void setNamespaceContext(Map<String, String> namespaceContext) {
        this.namespaceContext = namespaceContext;
    }

    public void addPrefix(String prefix, String namespace) {
        if (namespaceContext == null) {
            namespaceContext = new HashMap<String, String>();
        }
        namespaceContext.put(prefix, namespace);
    }
}
