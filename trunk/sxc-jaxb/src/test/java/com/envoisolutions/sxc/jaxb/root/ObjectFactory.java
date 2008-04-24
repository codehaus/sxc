package com.envoisolutions.sxc.jaxb.root;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.net.URI;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import com.envoisolutions.node.NamedNode;

@XmlRegistry
public class ObjectFactory {
    public static final String ROOT_URI = "http://envoisolutions.com/root";

    public ObjectFactory() {
    }

    public NoRoot createNoRoot() {
        return new NoRoot();
    }

    public AnnotatedRoot createAnnotatedRoot() {
        return new AnnotatedRoot();
    }

    @XmlElementDecl(namespace = ROOT_URI, name = "object-factory-root")
    public JAXBElement<ObjectFactoryRoot> createObjectFactoryRoot(ObjectFactoryRoot value) {
        return new JAXBElement<ObjectFactoryRoot>(new QName(ROOT_URI, "object-factory-root"), ObjectFactoryRoot.class, null, value);
    }

    @XmlElementDecl(namespace = ROOT_URI, name = "alternate-root-name")
    public JAXBElement<ObjectFactoryRoot> createAlternateRootName(ObjectFactoryRoot value) {
        return new JAXBElement<ObjectFactoryRoot>(new QName(ROOT_URI, "alternate-root-name"), ObjectFactoryRoot.class, null, value);
    }

    @XmlElementDecl(namespace = "http://envoisolutions.com/node", name = "external-root")
    public JAXBElement<NamedNode> createExternalRoot(NamedNode value) {
        return new JAXBElement<NamedNode>(new QName(ROOT_URI, "external-root"), NamedNode.class, null, value);
    }

    @XmlElementDecl(namespace = ROOT_URI, name = "object-factory-enum-root")
    public JAXBElement<ObjectFactoryEnumRoot> createObjectFactoryEnumRoot(ObjectFactoryEnumRoot value) {
        return new JAXBElement<ObjectFactoryEnumRoot>(new QName(ROOT_URI, "object-factory-enum-root"), ObjectFactoryEnumRoot.class, null, value);
    }

    @XmlElementDecl(namespace = ROOT_URI, name = "alternate-enum-root-name")
    public JAXBElement<ObjectFactoryEnumRoot> createAlternateEnumRootName(ObjectFactoryEnumRoot value) {
        return new JAXBElement<ObjectFactoryEnumRoot>(new QName(ROOT_URI, "alternate-root-enum-name"), ObjectFactoryEnumRoot.class, null, value);
    }

    @XmlElementDecl(namespace = ROOT_URI, name = "string-root")
    public JAXBElement<String> createStringRoot(String value) {
        return new JAXBElement<String>(new QName(ROOT_URI, "string-root"), String.class, null, value);
    }

    @XmlElementDecl(namespace = ROOT_URI, name = "short-root")
    public JAXBElement<Short> createShortRoot(Short value) {
        return new JAXBElement<Short>(new QName(ROOT_URI, "short-root"), Short.class, null, value);
    }

    @XmlElementDecl(namespace = ROOT_URI, name = "integer-root")
    public JAXBElement<Integer> createIntegerRoot(Integer value) {
        return new JAXBElement<Integer>(new QName(ROOT_URI, "integer-root"), Integer.class, null, value);
    }

    @XmlElementDecl(namespace = ROOT_URI, name = "long-root")
    public JAXBElement<Long> createLongRoot(Long value) {
        return new JAXBElement<Long>(new QName(ROOT_URI, "long-root"), Long.class, null, value);
    }

    @XmlElementDecl(namespace = ROOT_URI, name = "bigInteger-root")
    public JAXBElement<BigInteger> createBigintegerRoot(BigInteger value) {
        return new JAXBElement<BigInteger>(new QName(ROOT_URI, "bigInteger-root"), BigInteger.class, null, value);
    }

    @XmlElementDecl(namespace = ROOT_URI, name = "float-root")
    public JAXBElement<Float> createFloatRoot(Float value) {
        return new JAXBElement<Float>(new QName(ROOT_URI, "float-root"), Float.class, null, value);
    }

    @XmlElementDecl(namespace = ROOT_URI, name = "double-root")
    public JAXBElement<Double> createDoubleRoot(Double value) {
        return new JAXBElement<Double>(new QName(ROOT_URI, "double-root"), Double.class, null, value);
    }

    @XmlElementDecl(namespace = ROOT_URI, name = "bigDecimal-root")
    public JAXBElement<BigDecimal> createBigdecimalRoot(BigDecimal value) {
        return new JAXBElement<BigDecimal>(new QName(ROOT_URI, "bigDecimal-root"), BigDecimal.class, null, value);
    }

    @XmlElementDecl(namespace = ROOT_URI, name = "boolean-root")
    public JAXBElement<Boolean> createBooleanRoot(Boolean value) {
        return new JAXBElement<Boolean>(new QName(ROOT_URI, "boolean-root"), Boolean.class, null, value);
    }

    @XmlElementDecl(namespace = ROOT_URI, name = "calendar-root")
    public JAXBElement<Calendar> createCalendarRoot(Calendar value) {
        return new JAXBElement<Calendar>(new QName(ROOT_URI, "calendar-root"), Calendar.class, null, value);
    }

    @XmlElementDecl(namespace = ROOT_URI, name = "date-root")
    public JAXBElement<Date> createDateRoot(Date value) {
        return new JAXBElement<Date>(new QName(ROOT_URI, "date-root"), Date.class, null, value);
    }

    @XmlElementDecl(namespace = ROOT_URI, name = "QName-root")
    public JAXBElement<QName> createQnameRoot(QName value) {
        return new JAXBElement<QName>(new QName(ROOT_URI, "QName-root"), QName.class, null, value);
    }

    @XmlElementDecl(namespace = ROOT_URI, name = "URI-root")
    public JAXBElement<URI> createUriRoot(URI value) {
        return new JAXBElement<URI>(new QName(ROOT_URI, "URI-root"), URI.class, null, value);
    }

    @XmlElementDecl(namespace = ROOT_URI, name = "duration-root")
    public JAXBElement<Duration> createDurationRoot(Duration value) {
        return new JAXBElement<Duration>(new QName(ROOT_URI, "duration-root"), Duration.class, null, value);
    }

    @XmlElementDecl(namespace = ROOT_URI, name = "XMLGregorianCalendar-root")
    public JAXBElement<XMLGregorianCalendar> createXmlgregoriancalendarjaxbRoot(XMLGregorianCalendar value) {
        return new JAXBElement<XMLGregorianCalendar>(new QName(ROOT_URI, "XMLGregorianCalendar-root"), XMLGregorianCalendar.class, null, value);
    }

    @XmlElementDecl(namespace = ROOT_URI, name = "UUID-root")
    public JAXBElement<UUID> createUuidRoot(UUID value) {
        return new JAXBElement<UUID>(new QName(ROOT_URI, "UUID-root"), UUID.class, null, value);
    }
}