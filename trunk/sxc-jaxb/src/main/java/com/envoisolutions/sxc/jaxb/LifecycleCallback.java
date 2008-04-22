package com.envoisolutions.sxc.jaxb;

import java.lang.reflect.Method;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import static com.envoisolutions.sxc.jaxb.JavaUtils.isPrivate;

public class LifecycleCallback {
    public final Method beforeUnmarshal;
    public final Method afterUnmarshal;
    public final Method beforeMarshal;
    public final Method afterMarshal;

    public LifecycleCallback(Method beforeUnmarshal, Method afterUnmarshal, Method beforeMarshal, Method afterMarshal) {
        this.beforeUnmarshal = beforeUnmarshal;
        this.afterUnmarshal = afterUnmarshal;
        this.beforeMarshal = beforeMarshal;
        this.afterMarshal = afterMarshal;
    }

    public LifecycleCallback(Class beanType) {
        if (beanType == null) throw new NullPointerException("clazz is null");

        beforeUnmarshal = getDeclaredMethod(beanType, "beforeUnmarshal", Unmarshaller.class, Object.class);
        afterUnmarshal = getDeclaredMethod(beanType, "afterUnmarshal", Unmarshaller.class, Object.class);
        beforeMarshal = getDeclaredMethod(beanType, "beforeMarshal", Marshaller.class);
        afterMarshal = getDeclaredMethod(beanType, "afterMarshal", Marshaller.class);
    }

    private static Method getDeclaredMethod(Class type, String name, Class ... parameterTypes) {
        Method method;
        try {
            method = type.getDeclaredMethod(name, parameterTypes);
        } catch (Exception ignored) {
            return null;
        }

        if (isPrivate(method)) {
            try {
                method.setAccessible(true);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to access non-public methods");
            }
        }
        return method;
    }

    public void beforeUnmarshal(Object bean, Unmarshaller unmarshaller, Object parent) throws Exception {
        if (beforeUnmarshal != null) {
            beforeUnmarshal.invoke(bean, unmarshaller, parent);
        }
    }

    public void afterUnmarshal(Object bean, Unmarshaller unmarshaller, Object parent) throws Exception {
        if (afterUnmarshal != null) {
            afterUnmarshal.invoke(bean, unmarshaller, parent);
        }
    }

    public void beforeMarshal(Object bean, Marshaller marshaller) throws Exception {
        if (beforeMarshal != null) {
            beforeMarshal.invoke(bean, marshaller);
        }
    }

    public void afterMarshal(Object bean, Marshaller marshaller) throws Exception {
        if (afterMarshal != null) {
            afterMarshal.invoke(bean, marshaller);
        }
    }
}