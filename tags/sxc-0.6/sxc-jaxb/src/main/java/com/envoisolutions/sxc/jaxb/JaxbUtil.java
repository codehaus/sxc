package com.envoisolutions.sxc.jaxb;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.envoisolutions.sxc.util.Util;

public class JaxbUtil {

    public static String getGetter(Class parentClass, String name, Type rawType) {
        String propName = null;
        try {
            Field f = parentClass.getDeclaredField(name);
            
            XmlElement ann = f.getAnnotation(XmlElement.class);
            if (ann != null && !ann.name().equals("##default")) {
                StringBuilder strB = new StringBuilder(ann.name());
                for (int i = 0; i < strB.length(); i++) {
                    if (Character.isDigit(strB.charAt(i))
                        && i + 1 < strB.length() 
                        && Character.isLowerCase(strB.charAt(i+1))) {
                        strB.setCharAt(i+1, Character.toUpperCase(strB.charAt(i+1)));
                    }
                }
                propName = getGetter2(parentClass, strB.toString(), rawType);
            }
            
            XmlAttribute atann = f.getAnnotation(XmlAttribute.class);
            if (atann != null && !atann.name().equals("##default")) {
                StringBuilder strB = new StringBuilder(atann.name());
                for (int i = 0; i < strB.length(); i++) {
                    if (Character.isDigit(strB.charAt(i))
                        && i + 1 < strB.length() 
                        && Character.isLowerCase(strB.charAt(i+1))) {
                        strB.setCharAt(i+1, Character.toUpperCase(strB.charAt(i+1)));
                    }
                }
                propName = getGetter2(parentClass, strB.toString(), rawType);
            }
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (NoSuchFieldException e) {
            // System.out.println("No such field " + propEl.getName() + " on " + parentClass);
        }
        
        if (propName == null) {
            propName = getGetter2(parentClass, name, rawType);
        }
        return propName;
    }

    public static String getSetter(Class parentClass, String name) {
        String propName = null;
        try {
            Field f = parentClass.getDeclaredField(name);
            
            XmlElement ann = f.getAnnotation(XmlElement.class);
            if (ann != null && !ann.name().equals("##default")) {
                StringBuilder strB = new StringBuilder(ann.name());
                for (int i = 0; i < strB.length(); i++) {
                    if (Character.isDigit(strB.charAt(i))
                        && i + 1 < strB.length() 
                        && Character.isLowerCase(strB.charAt(i+1))) {
                        strB.setCharAt(i+1, Character.toUpperCase(strB.charAt(i+1)));
                    }
                }
                propName = getSetter(strB.toString());
            } 
            
            XmlAttribute atann = f.getAnnotation(XmlAttribute.class);
            if (atann != null && !atann.name().equals("##default")) {
                StringBuilder strB = new StringBuilder(atann.name());
                for (int i = 0; i < strB.length(); i++) {
                    if (Character.isDigit(strB.charAt(i))
                        && i + 1 < strB.length() 
                        && Character.isLowerCase(strB.charAt(i+1))) {
                        strB.setCharAt(i+1, Character.toUpperCase(strB.charAt(i+1)));
                    }
                }
                propName = getSetter(strB.toString());
            }
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (NoSuchFieldException e) {
            // System.out.println("No such field " + propEl.getName() + " on " + parentClass);
        }
        
        if (propName == null) {
            propName = Util.getSetter(name);
        }
        return propName;
    }
    
    public static String getGetter2(Class parent, String name, Type type) {
        if (name.startsWith("_")) 
            name = name.substring(1);

        String prefix;
        String post = name.substring(0, 1).toUpperCase() + name.substring(1);
        if (type != null && boolean.class.equals(type) || Boolean.class.equals(type)) {
            prefix = "is";
            
            String getter = prefix + post;
            try {
                parent.getMethod(getter, new Class[0]);
            } catch (SecurityException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                prefix = "get";
            }
        } else {
            prefix = "get";
        }
        return prefix + post;
    }

    public static String getSetter(String name) {
        if (name.startsWith("_")) 
            name = name.substring(1);
        
        return "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
    }
    
}
