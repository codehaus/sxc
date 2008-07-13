/*
 $Id$

 Copyright 2003 The Werken Company. All Rights Reserved.
 
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

  * Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.

  * Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.

  * Neither the name of the Jaxen Project nor the names of its
    contributors may be used to endorse or promote products derived 
    from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */
package com.envoisolutions.sxc.xpath;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

import com.ctc.wstx.stax.WstxInputFactory;
import com.envoisolutions.sxc.xpath.XPathBuilder;
import com.envoisolutions.sxc.xpath.XPathEvaluator;
import com.envoisolutions.sxc.xpath.XPathEvent;
import com.envoisolutions.sxc.xpath.XPathEventHandler;

class StreaxPerformance {
    
    static int found = 0;
    public static void main(String[] args) {
        
        try {
            URL url = new URL("http://www.ibiblio.org/xml/examples/shakespeare/much_ado.xml");
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            
            Document document = docBuilder.parse(url.openStream());
            
            WstxInputFactory xif = new WstxInputFactory();
            
            XPathEventHandler handler = new XPathEventHandler() {

                @Override
                public void onMatch(XPathEvent event) throws XMLStreamException {
                    found++;
                    throw new RuntimeException();
                }
            };
            
            XPathBuilder builder = new XPathBuilder();
            builder.listen("/PLAY/ACT/SCENE/SPEECH/SPEAKER[text()='HERO']", handler);
            XPathEvaluator evaluator = builder.compile();

            run(document, xif, evaluator, 200000);
            run(document, xif, evaluator, 100000);

            System.out.println(found);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    private static void run(Document document, WstxInputFactory xif, XPathEvaluator evaluator, int times) throws XMLStreamException, Exception {
        long start = System.currentTimeMillis();
        
        int count = 0;
        for (int i = 0; i < times; i++) {
            XMLStreamReader reader = xif.createXMLStreamReader(new DOMSource(document));
            try {
                evaluator.evaluate(reader);
            } catch (InvocationTargetException e) {
            }
            reader.close();
            count++;
        }
        
        long end = System.currentTimeMillis();
        System.out.println((end - start));
        System.out.println(count);
    }
    
}
