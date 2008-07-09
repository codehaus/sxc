/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.envoisolutions.sxc.jaxb;

import java.net.URL;
import java.net.MalformedURLException;
import javax.xml.bind.ValidationEventLocator;
import javax.xml.stream.Location;

import org.w3c.dom.Node;

public class ValidationEventLocatorImpl implements ValidationEventLocator {
    private Location location;
    private Object object;
    private String propertyName;

    public ValidationEventLocatorImpl() {
    }

    public ValidationEventLocatorImpl(Location location) {
        this.location = location;
    }

    public ValidationEventLocatorImpl(Object object, String propertyName) {
        this.object = object;
        this.propertyName = propertyName;
    }

    public URL getURL() {
        if (location != null) {
            try {
                return new URL(location.getSystemId());
            } catch (MalformedURLException e) {
            }
        }
        return null;
    }

    public int getOffset() {
        return -1;
    }

    public int getLineNumber() {
        if (location == null) {
            return -1;
        }
        return location.getLineNumber();
    }

    public int getColumnNumber() {
        if (location == null) {
            return -1;
        }
        return location.getColumnNumber();
    }

    public Object getObject() {
        return object;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Node getNode() {
        return null;
    }

    public String toString() {
        if (location != null) {
            return location.toString();
        } else if (object != null) {
            return "[object=" + object + ", propertyName=" + propertyName + "]";
        } else {
            return super.toString();
        }
    }
}
