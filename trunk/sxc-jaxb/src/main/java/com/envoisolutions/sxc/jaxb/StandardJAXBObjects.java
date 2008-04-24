package com.envoisolutions.sxc.jaxb;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Date;
import java.util.UUID;
import java.math.BigInteger;
import java.math.BigDecimal;
import java.net.URI;
import javax.xml.namespace.QName;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.datatype.Duration;
import javax.xml.XMLConstants;

import com.envoisolutions.sxc.util.XoXMLStreamReader;
import com.envoisolutions.sxc.util.XoXMLStreamWriter;

public class StandardJAXBObjects {
    private static final DatatypeFactory datatypeFactory;
    public static final Map<Class, JAXBObject> jaxbObjectByClass;
    public static final Map<QName, JAXBObject> jaxbObjectBySchemaType;

    static {
        try {
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }

        Map<Class, JAXBObject> byClass = new LinkedHashMap<Class, JAXBObject>();
        byClass.put(StringJAXB.INSTANCE.getType(), StringJAXB.INSTANCE);
        byClass.put(BigIntegerJAXB.INSTANCE.getType(), BigIntegerJAXB.INSTANCE);
        byClass.put(IntegerJAXB.INSTANCE.getType(), IntegerJAXB.INSTANCE);
        byClass.put(LongJAXB.INSTANCE.getType(), LongJAXB.INSTANCE);
        byClass.put(ShortJAXB.INSTANCE.getType(), ShortJAXB.INSTANCE);
        byClass.put(BigDecimalJAXB.INSTANCE.getType(), BigDecimalJAXB.INSTANCE);
        byClass.put(FloatJAXB.INSTANCE.getType(), FloatJAXB.INSTANCE);
        byClass.put(DoubleJAXB.INSTANCE.getType(), DoubleJAXB.INSTANCE);
        byClass.put(BooleanJAXB.INSTANCE.getType(), BooleanJAXB.INSTANCE);
        byClass.put(CalendarJAXB.INSTANCE.getType(), CalendarJAXB.INSTANCE);
        byClass.put(DateJAXB.INSTANCE.getType(), DateJAXB.INSTANCE);
        byClass.put(QNameJAXB.INSTANCE.getType(), QNameJAXB.INSTANCE);
        byClass.put(URIJAXB.INSTANCE.getType(), URIJAXB.INSTANCE);
        byClass.put(XMLGregorianCalendarJAXB.INSTANCE.getType(), XMLGregorianCalendarJAXB.INSTANCE);
        byClass.put(DurationJAXB.INSTANCE.getType(), DurationJAXB.INSTANCE);
        byClass.put(UUIDJAXB.INSTANCE.getType(), UUIDJAXB.INSTANCE);
        jaxbObjectByClass = Collections.unmodifiableMap(byClass);

        Map<QName, JAXBObject> bySchemaType = new LinkedHashMap<QName, JAXBObject>();
        bySchemaType.put(StringJAXB.INSTANCE.getXmlType(), StringJAXB.INSTANCE);
        bySchemaType.put(BigIntegerJAXB.INSTANCE.getXmlType(), BigIntegerJAXB.INSTANCE);
        bySchemaType.put(IntegerJAXB.INSTANCE.getXmlType(), IntegerJAXB.INSTANCE);
        bySchemaType.put(LongJAXB.INSTANCE.getXmlType(), LongJAXB.INSTANCE);
        bySchemaType.put(ShortJAXB.INSTANCE.getXmlType(), ShortJAXB.INSTANCE);
        bySchemaType.put(BigDecimalJAXB.INSTANCE.getXmlType(), BigDecimalJAXB.INSTANCE);
        bySchemaType.put(FloatJAXB.INSTANCE.getXmlType(), FloatJAXB.INSTANCE);
        bySchemaType.put(DoubleJAXB.INSTANCE.getXmlType(), DoubleJAXB.INSTANCE);
        bySchemaType.put(BooleanJAXB.INSTANCE.getXmlType(), BooleanJAXB.INSTANCE);
        // bySchemaType.put(CalendarJAXB.INSTANCE.getXmlType(), CalendarJAXB.INSTANCE);
        // bySchemaType.put(DateJAXB.INSTANCE.getXmlType(), DateJAXB.INSTANCE);
        bySchemaType.put(QNameJAXB.INSTANCE.getXmlType(), QNameJAXB.INSTANCE);
        // bySchemaType.put(URIJAXB.INSTANCE.getXmlType(), URIJAXB.INSTANCE);
        // bySchemaType.put(XMLGregorianCalendarJAXB.INSTANCE.getXmlType(), XMLGregorianCalendarJAXB.INSTANCE);
        bySchemaType.put(DurationJAXB.INSTANCE.getXmlType(), DurationJAXB.INSTANCE);
        // bySchemaType.put(UUIDJAXB.INSTANCE.getXmlType(), UUIDJAXB.INSTANCE);
        jaxbObjectBySchemaType = Collections.unmodifiableMap(bySchemaType);
    }

    // java.lang.String xs:string
    public static class StringJAXB extends JAXBObject<String> {
        public final static StringJAXB INSTANCE = new StringJAXB();

        public StringJAXB() {
            super(String.class, null, new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "string".intern()));
        }

        public String read(XoXMLStreamReader reader, RuntimeContext context) throws Exception {
            if (reader.isXsiNil()) return null;

            String value = reader.getElementAsString();
            return value;
        }

        public void write(XoXMLStreamWriter writer, String value, RuntimeContext context) throws Exception {
            if (value == null) {
                writer.writeXsiNil();
            } else {
                writer.writeCharacters(value);
            }
        }
    }

    // java.math.BigInteger xs:integer
    public static class BigIntegerJAXB extends JAXBObject<BigInteger> {
        public final static BigIntegerJAXB INSTANCE = new BigIntegerJAXB();

        public BigIntegerJAXB() {
            super(BigInteger.class, null, new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "integer".intern()));
        }

        public BigInteger read(XoXMLStreamReader reader, RuntimeContext context) throws Exception {
            if (reader.isXsiNil()) return null;

            String string = reader.getElementAsString();
            BigInteger value = new BigInteger(string);
            return value;
        }

        public void write(XoXMLStreamWriter writer, BigInteger value, RuntimeContext context) throws Exception {
            if (value == null) {
                writer.writeXsiNil();
            } else {
                writer.writeCharacters(value.toString());
            }
        }
    }

    // java.lang.Integer xs:int
    public static class IntegerJAXB extends JAXBObject<Integer> {
        public final static IntegerJAXB INSTANCE = new IntegerJAXB();

        public IntegerJAXB() {
            super(Integer.class, null, new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "int".intern()));
        }

        public Integer read(XoXMLStreamReader reader, RuntimeContext context) throws Exception {
            if (reader.isXsiNil()) return null;

            String string = reader.getElementAsString();
            Integer value = new Integer(string);
            return value;
        }

        public void write(XoXMLStreamWriter writer, Integer value, RuntimeContext context) throws Exception {
            if (value == null) {
                writer.writeXsiNil();
            } else {
                writer.writeCharacters(value.toString());
            }
        }
    }

    // java.lang.Long xs:long
    public static class LongJAXB extends JAXBObject<Long> {
        public final static LongJAXB INSTANCE = new LongJAXB();

        public LongJAXB() {
            super(Long.class, null, new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "long".intern()));
        }

        public Long read(XoXMLStreamReader reader, RuntimeContext context) throws Exception {
            if (reader.isXsiNil()) return null;

            String string = reader.getElementAsString();
            Long value = new Long(string);
            return value;
        }

        public void write(XoXMLStreamWriter writer, Long value, RuntimeContext context) throws Exception {
            if (value == null) {
                writer.writeXsiNil();
            } else {
                writer.writeCharacters(value.toString());
            }
        }
    }

    // java.lang.Short xs:short
    public static class ShortJAXB extends JAXBObject<Short> {
        public final static ShortJAXB INSTANCE = new ShortJAXB();

        public ShortJAXB() {
            super(Short.class, null, new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "short".intern()));
        }

        public Short read(XoXMLStreamReader reader, RuntimeContext context) throws Exception {
            if (reader.isXsiNil()) return null;

            String string = reader.getElementAsString();
            Short value = new Short(string);
            return value;
        }

        public void write(XoXMLStreamWriter writer, Short value, RuntimeContext context) throws Exception {
            if (value == null) {
                writer.writeXsiNil();
            } else {
                writer.writeCharacters(value.toString());
            }
        }
    }

    // java.math.BigDecimal xs:decimal
    public static class BigDecimalJAXB extends JAXBObject<BigDecimal> {
        public final static BigDecimalJAXB INSTANCE = new BigDecimalJAXB();

        public BigDecimalJAXB() {
            super(BigDecimal.class, null, new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "decimal".intern()));
        }

        public BigDecimal read(XoXMLStreamReader reader, RuntimeContext context) throws Exception {
            if (reader.isXsiNil()) return null;

            String string = reader.getElementAsString();
            BigDecimal value = new BigDecimal(string);
            return value;
        }

        public void write(XoXMLStreamWriter writer, BigDecimal value, RuntimeContext context) throws Exception {
            if (value == null) {
                writer.writeXsiNil();
            } else {
                writer.writeCharacters(value.toString());
            }
        }
    }

    // java.math.Float xs:float
    public static class FloatJAXB extends JAXBObject<Float> {
        public final static FloatJAXB INSTANCE = new FloatJAXB();

        public FloatJAXB() {
            super(Float.class, null, new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "float".intern()));
        }

        public Float read(XoXMLStreamReader reader, RuntimeContext context) throws Exception {
            if (reader.isXsiNil()) return null;

            String string = reader.getElementAsString();
            Float value = new Float(string);
            return value;
        }

        public void write(XoXMLStreamWriter writer, Float value, RuntimeContext context) throws Exception {
            if (value == null) {
                writer.writeXsiNil();
            } else {
                writer.writeCharacters(value.toString());
            }
        }
    }

    // java.math.Double xs:double
    public static class DoubleJAXB extends JAXBObject<Double> {
        public final static DoubleJAXB INSTANCE = new DoubleJAXB();

        public DoubleJAXB() {
            super(Double.class, null, new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "double".intern()));
        }

        public Double read(XoXMLStreamReader reader, RuntimeContext context) throws Exception {
            if (reader.isXsiNil()) return null;

            String string = reader.getElementAsString();
            Double value = new Double(string);
            return value;
        }

        public void write(XoXMLStreamWriter writer, Double value, RuntimeContext context) throws Exception {
            if (value == null) {
                writer.writeXsiNil();
            } else {
                writer.writeCharacters(value.toString());
            }
        }
    }

    // java.math.Boolean xs:boolean
    public static class BooleanJAXB extends JAXBObject<Boolean> {
        public final static BooleanJAXB INSTANCE = new BooleanJAXB();

        public BooleanJAXB() {
            super(Boolean.class, null, new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "boolean".intern()));
        }

        public Boolean read(XoXMLStreamReader reader, RuntimeContext context) throws Exception {
            if (reader.isXsiNil()) return null;

            String string = reader.getElementAsString();
            Boolean value = Boolean.parseBoolean(string);
            return value;
        }

        public void write(XoXMLStreamWriter writer, Boolean value, RuntimeContext context) throws Exception {
            if (value == null) {
                writer.writeXsiNil();
            } else {
                writer.writeCharacters(value.toString());
            }
        }
    }

    // java.util.Calendar xs:dateTime
    public static class CalendarJAXB extends JAXBObject<Calendar> {
        public final static CalendarJAXB INSTANCE = new CalendarJAXB();

        public CalendarJAXB() {
            super(Calendar.class, null, null);
        }

        public Calendar read(XoXMLStreamReader reader, RuntimeContext context) throws Exception {
            if (reader.isXsiNil()) return null;

            Calendar value = XMLGregorianCalendarJAXB.INSTANCE.read(reader, context).toGregorianCalendar();
            return value;
        }

        public void write(XoXMLStreamWriter writer, Calendar value, RuntimeContext context) throws Exception {
            if (value == null) {
                writer.writeXsiNil();
            } else {
                GregorianCalendar gregorianCalendar;
                if (value instanceof GregorianCalendar) {
                    gregorianCalendar = (GregorianCalendar) value;
                } else {
                    gregorianCalendar = new GregorianCalendar(value.get(Calendar.YEAR),
                            value.get(Calendar.MONTH),
                            value.get(Calendar.DAY_OF_MONTH),
                            value.get(Calendar.HOUR_OF_DAY),
                            value.get(Calendar.MINUTE),
                            value.get(Calendar.SECOND));
                }

                XMLGregorianCalendar calendar = datatypeFactory.newXMLGregorianCalendar(gregorianCalendar);
                XMLGregorianCalendarJAXB.INSTANCE.write(writer,  calendar, context);
            }
        }
    }

    // java.util.Date xs:dateTime
    public static class DateJAXB extends JAXBObject<Date> {
        public final static DateJAXB INSTANCE = new DateJAXB();

        public DateJAXB() {
            super(Date.class, null, null);
        }

        public Date read(XoXMLStreamReader reader, RuntimeContext context) throws Exception {
            if (reader.isXsiNil()) return null;

            Date value = XMLGregorianCalendarJAXB.INSTANCE.read(reader, context).toGregorianCalendar().getTime();
            return value;
        }

        public void write(XoXMLStreamWriter writer, Date value, RuntimeContext context) throws Exception {
            if (value == null) {
                writer.writeXsiNil();
            } else {
                GregorianCalendar gregorianCalendar = new GregorianCalendar();
                gregorianCalendar.setTime(value);

                XMLGregorianCalendar calendar = datatypeFactory.newXMLGregorianCalendar(gregorianCalendar);
                XMLGregorianCalendarJAXB.INSTANCE.write(writer,  calendar, context);
            }
        }
    }


    // javax.xml.namespace.QName xs:QName
    public static class QNameJAXB extends JAXBObject<QName> {
        public final static QNameJAXB INSTANCE = new QNameJAXB();

        public QNameJAXB() {
            super(QName.class, null, new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "QName".intern()));
        }

        public QName read(XoXMLStreamReader reader, RuntimeContext context) throws Exception {
            if (reader.isXsiNil()) return null;

            QName value = reader.getElementAsQName();
            return value;
        }

        public void write(XoXMLStreamWriter writer, QName value, RuntimeContext context) throws Exception {
            if (value == null) {
                writer.writeXsiNil();
            } else {
                writer.writeQName(value);
            }
        }
    }

    // java.net.URI xs:string
    public static class URIJAXB extends JAXBObject<URI> {
        public final static URIJAXB INSTANCE = new URIJAXB();

        public URIJAXB() {
            super(URI.class, null, null);
        }

        public URI read(XoXMLStreamReader reader, RuntimeContext context) throws Exception {
            if (reader.isXsiNil()) return null;

            String string = reader.getElementAsString();
            URI value = new URI(string);
            return value;
        }

        public void write(XoXMLStreamWriter writer, URI value, RuntimeContext context) throws Exception {
            if (value == null) {
                writer.writeXsiNil();
            } else {
                writer.writeCharacters(value.toString());
            }
        }
    }

    // javax.xml.datatype.XMLGregorianCalendar xs:anySimpleType
    public static class XMLGregorianCalendarJAXB extends JAXBObject<XMLGregorianCalendar> {
        public final static XMLGregorianCalendarJAXB INSTANCE = new XMLGregorianCalendarJAXB();

        public XMLGregorianCalendarJAXB() {
            super(XMLGregorianCalendar.class, null, null);
        }

        public XMLGregorianCalendar read(XoXMLStreamReader reader, RuntimeContext context) throws Exception {
            if (reader.isXsiNil()) return null;

            String string = reader.getElementAsString();
            XMLGregorianCalendar value = datatypeFactory.newXMLGregorianCalendar(string);
            return value;
        }

        public void write(XoXMLStreamWriter writer, XMLGregorianCalendar value, RuntimeContext context) throws Exception {
            if (value == null) {
                writer.writeXsiNil();
            } else {
                writer.writeCharacters(value.toString());
            }
        }
    }

    // javax.xml.datatype.Duration xs:duration
    public static class DurationJAXB extends JAXBObject<Duration> {
        public final static DurationJAXB INSTANCE = new DurationJAXB();

        public DurationJAXB() {
            super(Duration.class, null, new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "duration".intern()));
        }

        public Duration read(XoXMLStreamReader reader, RuntimeContext context) throws Exception {
            if (reader.isXsiNil()) return null;

            String string = reader.getElementAsString();
            Duration value = datatypeFactory.newDuration(string);
            return value;
        }

        public void write(XoXMLStreamWriter writer, Duration value, RuntimeContext context) throws Exception {
            if (value == null) {
                writer.writeXsiNil();
            } else {
                writer.writeCharacters(value.toString());
            }
        }
    }

    // java.util.UUID xs:string
    public static class UUIDJAXB extends JAXBObject<UUID> {
        public final static UUIDJAXB INSTANCE = new UUIDJAXB();

        public UUIDJAXB() {
            super(UUID.class, null, null);
        }

        public UUID read(XoXMLStreamReader reader, RuntimeContext context) throws Exception {
            if (reader.isXsiNil()) return null;

            String string = reader.getElementAsString();
            UUID value = UUID.fromString(string);
            return value;
        }

        public void write(XoXMLStreamWriter writer, UUID value, RuntimeContext context) throws Exception {
            if (value == null) {
                writer.writeXsiNil();
            } else {
                writer.writeCharacters(value.toString());
            }
        }
    }
}
