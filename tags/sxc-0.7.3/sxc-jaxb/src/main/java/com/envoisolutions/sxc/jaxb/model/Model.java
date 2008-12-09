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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;

public class Model {
    private final Map<Class, Bean> beansByClass = new LinkedHashMap<Class, Bean>();
    private final Map<Class, EnumInfo> enumsByClass = new LinkedHashMap<Class, EnumInfo>();
    private final Set<ObjectFactory> objectFactories = new LinkedHashSet<ObjectFactory>();

    public Collection<Bean> getBeans() {
        return beansByClass.values();
    }

    public Bean getBean(Class clazz) {
        return beansByClass.get(clazz);
    }

    public void addBean(Bean bean) {
        if (!beansByClass.containsKey(bean.getType())) {
            beansByClass.put(bean.getType(), bean);
        }
    }

    public Collection<EnumInfo> getEnums() {
        return enumsByClass.values();
    }

    public EnumInfo getEnum(Class clazz) {
        return enumsByClass.get(clazz);
    }

    public void addEnum(EnumInfo enumInfo) {
        if (!enumsByClass.containsKey(enumInfo.getType())) {
            enumsByClass.put(enumInfo.getType(), enumInfo);
        }
    }

    public Set<ObjectFactory> getObjectFactories() {
        return objectFactories;
    }
}
