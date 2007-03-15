package com.envoisolutions.sxc.jaxb.invoice;

import java.io.ByteArrayOutputStream;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.cxf.courseware.invoice.Customer;
import org.apache.cxf.courseware.invoice.Invoice;
import org.apache.cxf.courseware.invoice.InvoiceLine;
import org.apache.cxf.courseware.invoice.SupportLevel;
import org.w3c.dom.Document;

import com.envoisolutions.sxc.jaxb.JAXBContextImpl;
import com.envoisolutions.sxc.util.XoTestCase;
import com.sun.xml.bind.v2.ContextFactory;

public class InvoiceTest extends XoTestCase {
    public void testNoHeader() throws Exception {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");
        JAXBContextImpl ctx = new JAXBContextImpl(Invoice.class);
        
        Invoice i = (Invoice) ctx.createUnmarshaller().unmarshal(
                          getClass().getResourceAsStream("invoice-1.xml"));
        assertNotNull(i);
    }
    
    public void testInvoiceHeader() throws Exception {
        System.setProperty("com.envoisolutions.sxc.output.directory", "target/tmp-jaxb");
        JAXBContextImpl ctx = new JAXBContextImpl(Invoice.class);
        
        Invoice i = (Invoice) ctx.createUnmarshaller().unmarshal(
                          getClass().getResourceAsStream("invoice-2.xml"));
        assertNotNull(i);
        
        assertNotNull(i.getHeader());
        assertNotNull(i.getHeader().getShippingDate());
        assertNotNull(i.getHeader().getCustomer());
        
        Customer customer = i.getHeader().getCustomer();
        assertEquals("123", customer.getCustomerNumber());
        assertEquals(SupportLevel.SILVER, customer.getSupportLevel());
        
        XMLGregorianCalendar shippingDate = i.getHeader().getShippingDate();
        assertEquals(2002, shippingDate.getYear());
        
        List<InvoiceLine> invoiceLine = i.getInvoiceLine();
        assertNotNull(invoiceLine);
        assertEquals(1, invoiceLine.size());
        
        Marshaller marshaller = ctx.createMarshaller();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(i, bos);
        
        System.out.println(bos.toString());
        Document d = readDocument(bos.toByteArray());
        addNamespace("i", "http://cxf.apache.org/courseware/Invoice");
        assertValid("/i:Invoice/i:header/i:customer/i:customerNumber[text()='123']", d);
        assertValid("/i:Invoice/i:header/i:customer/i:supportLevel[text()='Silver']", d);
        assertValid("/i:Invoice/i:header/i:shippingDate[text()='2002-10-10']", d);
    }
    
    public void testJAXBContextUnmarshalReal() throws Exception {
        JAXBContext ctx = ContextFactory.createContext(new Class[] {Invoice.class}, null);
        Invoice i = (Invoice) ctx.createUnmarshaller().unmarshal(
                          getClass().getResourceAsStream("invoice-2.xml"));
        assertNotNull(i);
        List<InvoiceLine> invoiceLine = i.getInvoiceLine();
        assertNotNull(invoiceLine);
        assertEquals(1, invoiceLine.size());
        
        assertNotNull(i.getHeader());
        assertNotNull(i.getHeader().getShippingDate());

        XMLGregorianCalendar shippingDate = i.getHeader().getShippingDate();
        assertEquals(2002, shippingDate.getYear());
        
        Marshaller marshaller = ctx.createMarshaller();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(i, bos);
        
        System.out.println(bos.toString());
    }
}
