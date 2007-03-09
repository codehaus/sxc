package com.envoisolutions.sxc.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.reflect.Type;

import com.sun.codemodel.JMethod;

public class Util {

    public static void delete(File classDir) {
        File[] files = classDir.listFiles();
        if (files != null) {
            for (File f : files) {
                delete(f);
            }
        }
        
        classDir.delete();
    }
    
    public static String getGetter(String name, Type type) {
        if (name.startsWith("_")) 
            name = name.substring(1);

        String prefix;
        if (type != null && boolean.class.equals(type)) {
            prefix = "is";
        } else {
            prefix = "get";
        }
        return prefix + name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public static String getSetter(String name) {
        if (name.startsWith("_")) 
            name = name.substring(1);
        
        return "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public static String getGetter(String name) {
        return getGetter(name, null);
    }
    
}
