package com.envoisolutions.sxc.jaxb;

import java.lang.reflect.Method;

import javax.xml.bind.JAXBException;

import com.envoisolutions.sxc.util.XoXMLStreamReader;

public class PropertyAccessor<BeanType, FieldType> {
    public final Method getter;
    public final Method setter;

    public PropertyAccessor(Method getter, Method setter) {
        this.getter = getter;
        this.setter = setter;
    }

    public PropertyAccessor(Class<BeanType> beanType, Class<FieldType> propertyType, String getterName, String setterName) {
        if (beanType == null) throw new NullPointerException("clazz is null");
        if (getterName == null) throw new NullPointerException("getterName is null");
        if (setterName == null) throw new NullPointerException("getterName is null");

        try {
            if (getterName != null) {
                getter = beanType.getDeclaredMethod(getterName);
                getter.setAccessible(true);
            } else {
                getter = null;
            }

            if (setterName != null) {
                setter = beanType.getDeclaredMethod(setterName, propertyType);
                setter.setAccessible(true);
            } else {
                setter = null;
            }
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        } catch (SecurityException e) {
            throw new IllegalStateException("Unable to access non-public methods");
        }
    }

    @SuppressWarnings({"unchecked"})
    public FieldType getObject(Object location, RuntimeContext context, BeanType instance) throws JAXBException {
        FieldType result = null;
        try {
            result = (FieldType) getter.invoke(instance);
        } catch (Exception e) {
            if (location instanceof XoXMLStreamReader) {
                XoXMLStreamReader reader = (XoXMLStreamReader) location;
                context.fieldGetError(reader, getter.getDeclaringClass(), getter.getName(), e);
            } else {
                context.fieldGetError(location, getter.getName(), getter.getDeclaringClass(), getter.getName(), e);
            }
            if (getter.getReturnType().isPrimitive()) {
                Class<?> propertyType = getter.getReturnType();
                if (Boolean.TYPE.equals(propertyType)) {
                    result = (FieldType) Boolean.FALSE;
                } else if (Byte.TYPE.equals(propertyType)) {
                    result = (FieldType) new Byte((byte) 0);
                } else if (Character.TYPE.equals(propertyType)) {
                    result = (FieldType) new Character((char) 0);
                } else if (Short.TYPE.equals(propertyType)) {
                    result = (FieldType) new Short((short) 0);
                } else if (Integer.TYPE.equals(propertyType)) {
                    result = (FieldType) new Integer(0);
                } else if (Long.TYPE.equals(propertyType)) {
                    result = (FieldType) new Long(0);
                } else if (Float.TYPE.equals(propertyType)) {
                    result = (FieldType) new Float(0);
                } else if (Double.TYPE.equals(propertyType)) {
                    result = (FieldType) new Double(0);
                }
            }
        }
        return result;
    }

    public void setObject(XoXMLStreamReader reader, RuntimeContext context, BeanType instance, FieldType value) throws JAXBException {
        try {
            setter.invoke(instance, (Object) value);
        } catch (Exception e) {
            if (context == null) throw new JAXBException(e);
            context.setterError(reader, setter.getDeclaringClass(), setter.getName(), setter.getParameterTypes()[0], e);
        }
    }
}