package com.envoisolutions.sxc.jaxb.model;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class Model {
    private final Map<Class, Bean> beansByClass = new LinkedHashMap<Class, Bean>();

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
}
