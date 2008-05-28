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
package com.envoisolutions.sxc.jaxb.model;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.LinkedHashSet;
import javax.xml.namespace.QName;

public class ObjectFactory {
    /**
     * The model that owns this bean.
     */
    private final Model model;

    /**
     * The ObjectFactory class.
     */
    private final Class<?> type;

    /**
     * Root element declarations by element name.
     */
    private final Map<QName, Class> rootElements = new LinkedHashMap<QName, Class>();

    /**
     * Dependencies
     */
    private final Set<Bean> dependencies = new LinkedHashSet<Bean>();

    public ObjectFactory(Model model, Class<?> type) {
        this.model = model;
        this.type = type;
    }

    public Model getModel() {
        return model;
    }

    public Class<?> getType() {
        return type;
    }

    public Map<QName, Class> getRootElements() {
        return rootElements;
    }

    public Set<Bean> getDependencies() {
        return dependencies;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ObjectFactory that = (ObjectFactory) o;

        return type.equals(that.type);
    }

    public int hashCode() {
        return type.hashCode();
    }

    public String toString() {
        return type.getName();
    }
}
