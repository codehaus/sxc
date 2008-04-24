package com.envoisolutions.sxc.jaxb.root;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

import com.envoisolutions.node.NamedNode;

@XmlRegistry
public class ObjectFactory {
    private final static QName _ObjectFactoryRoot_QNAME = new QName("http://envoisolutions.com/root", "object-factory-root");
    private final static QName _AlternateRootName_QNAME = new QName("http://envoisolutions.com/root", "alternate-root-name");
    private final static QName _ExternalRoot_QNAME = new QName("http://envoisolutions.com/root", "external-root");

    public ObjectFactory() {
    }

    public NoRoot createNoRoot() {
        return new NoRoot();
    }

    public AnnotatedRoot createAnnotatedRoot() {
        return new AnnotatedRoot();
    }

    @XmlElementDecl(namespace = "http://envoisolutions.com/root", name = "object-factory-root")
    public JAXBElement<ObjectFactoryRoot> createObjectFactoryRoot(ObjectFactoryRoot value) {
        return new JAXBElement<ObjectFactoryRoot>(_ObjectFactoryRoot_QNAME, ObjectFactoryRoot.class, null, value);
    }

    @XmlElementDecl(namespace = "http://envoisolutions.com/root", name = "alternate-root-name")
    public JAXBElement<ObjectFactoryRoot> createAlternateRootName(ObjectFactoryRoot value) {
        return new JAXBElement<ObjectFactoryRoot>(_AlternateRootName_QNAME, ObjectFactoryRoot.class, null, value);
    }

    @XmlElementDecl(namespace = "http://envoisolutions.com/node", name = "external-root")
    public JAXBElement<NamedNode> createExternalRoot(NamedNode value) {
        return new JAXBElement<NamedNode>(_ExternalRoot_QNAME, NamedNode.class, null, value);
    }
}