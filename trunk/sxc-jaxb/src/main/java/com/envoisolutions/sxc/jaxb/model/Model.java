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
