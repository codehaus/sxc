/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.envoisolutions.sxc.util;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.xml.namespace.NamespaceContext;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class W3CNamespaceContext implements NamespaceContext {
    private Element currentNode;

    public String getNamespaceURI(String prefix) {
        String name = prefix;
        if (name.length() == 0) {
            name = "xmlns";
        } else {
            name = "xmlns:" + prefix;
        }
        
        return getNamespaceURI(currentNode, name);
    }

    private String getNamespaceURI(Element e, String name) {
        Attr attr = e.getAttributeNode(name);
        if (attr == null) {
            Node n = e.getParentNode();
            if (n instanceof Element && n != e) {
                return getNamespaceURI((Element)n, name);
            }
        } else {
            return attr.getValue();
        }

        return null;
    }

    public String getPrefix(String uri) {
        return getPrefix(currentNode, uri);
    }

    private String getPrefix(Element e, String uri) {
        NamedNodeMap attributes = e.getAttributes();
        if (attributes != null) {
            for (int i = 0; i < attributes.getLength(); i++) {
                Attr a = (Attr)attributes.item(i);

                String val = a.getValue();
                if (val != null && val.equals(uri)) {
                    String name = a.getNodeName();
                    if ("xmlns".equals(name)) {
                        return "";
                    } else {
                        return name.substring(6);
                    }
                }
            }
        }

        Node n = e.getParentNode();
        if (n instanceof Element && n != e) {
            return getPrefix((Element)n, uri);
        }

        return null;
    }

    public Iterator getPrefixes(String uri) {
        List<String> prefixes = new ArrayList<String>();

        String prefix = getPrefix(uri);
        if (prefix != null) {
            prefixes.add(prefix);
        }
        
        return prefixes.iterator();
    }

    public Element getElement() {
        return currentNode;
    }

    public void setElement(Element node) {
        this.currentNode = node;
    }
}
