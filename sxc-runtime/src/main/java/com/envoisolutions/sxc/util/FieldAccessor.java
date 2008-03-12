package com.envoisolutions.sxc.util;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

public class FieldAccessor {
    private static final Unsafe unsafe;
    static {
        Unsafe theUnsafe = null;
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            theUnsafe = (Unsafe) field.get(null);
        } catch (Throwable ignored) {
        }
        unsafe = theUnsafe;
    }

    public final Field field;
    public final long offset;

    public FieldAccessor(Field field) {
        if (field == null) throw new NullPointerException("field is null");
        this.field = field;
        if (unsafe != null) {
            offset = unsafe.objectFieldOffset(field);
        } else {
            offset = -1;
        }
    }

    public FieldAccessor(Class clazz, String fieldName) {
        if (clazz == null) throw new NullPointerException("clazz is null");
        if (fieldName == null) throw new NullPointerException("fieldName is null");

        try {
            this.field = clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException(e);
        }

        if (unsafe != null) {
            offset = unsafe.objectFieldOffset(field);
        } else {
            offset = -1;
            try {
                field.setAccessible(true);
            } catch (SecurityException e) {
                throw new IllegalStateException("Unable to access non-public fields");
            }
        }
    }

    public Object getObject(Object instance) {
        if (unsafe == null) {
            return reflectionGet(instance);
        }
        return unsafe.getObject(instance, offset);
    }

    public void setObject(Object instance, Object value) {
        if (unsafe == null) {
            reflectionSet(instance, value);
            return;
        }
        unsafe.putObject(instance, offset, value);
    }

    public boolean getBoolean(Object instance) {
        if (unsafe == null) {
            return (Boolean) reflectionGet(instance);
        }
        return unsafe.getBoolean(instance, offset);
    }

    public void setBoolean(Object instance, boolean value) {
        if (unsafe == null) {
            reflectionSet(instance, value);
            return;
        }
        unsafe.putBoolean(instance, offset, value);
    }

    public byte getByte(Object instance) {
        if (unsafe == null) {
            return (Byte) reflectionGet(instance);
        }
        return unsafe.getByte(instance, offset);
    }

    public void setByte(Object instance, byte value) {
        if (unsafe == null) {
            reflectionSet(instance, value);
            return;
        }
        unsafe.putByte(instance, offset, value);
    }

    public char getChar(Object instance) {
        if (unsafe == null) {
            return (Character) reflectionGet(instance);
        }
        return unsafe.getChar(instance, offset);
    }

    public void setChar(Object instance, char value) {
        if (unsafe == null) {
            reflectionSet(instance, value);
            return;
        }
        unsafe.putChar(instance, offset, value);
    }

    public short getShort(Object instance) {
        if (unsafe == null) {
            return (Short) reflectionGet(instance);
        }
        return unsafe.getShort(instance, offset);
    }

    public void setShort(Object instance, short value) {
        if (unsafe == null) {
            reflectionSet(instance, value);
            return;
        }
        unsafe.putShort(instance, offset, value);
    }

    public int getInt(Object instance) {
        if (unsafe == null) {
            return (Integer) reflectionGet(instance);
        }
        return unsafe.getInt(instance, offset);
    }

    public void setInt(Object instance, int value) {
        if (unsafe == null) {
            reflectionSet(instance, value);
            return;
        }
        unsafe.putInt(instance, offset, value);
    }

    public long getLong(Object instance) {
        if (unsafe == null) {
            return (Long) reflectionGet(instance);
        }
        return unsafe.getLong(instance, offset);
    }

    public void setLong(Object instance, long value) {
        if (unsafe == null) {
            reflectionSet(instance, value);
            return;
        }
        unsafe.putLong(instance, offset, value);
    }

    public float getFloat(Object instance) {
        if (unsafe == null) {
            return (Float) reflectionGet(instance);
        }
        return unsafe.getFloat(instance, offset);
    }

    public void setFloat(Object instance, float value) {
        if (unsafe == null) {
            reflectionSet(instance, value);
            return;
        }
        unsafe.putFloat(instance, offset, value);
    }

    public double getDouble(Object instance) {
        if (unsafe == null) {
            return (Double) reflectionGet(instance);
        }
        return unsafe.getDouble(instance, offset);
    }

    public void setDouble(Object instance, double value) {
        if (unsafe == null) {
            reflectionSet(instance, value);
            return;
        }
        unsafe.putDouble(instance, offset, value);
    }

    private Object reflectionGet(Object instance) {
        try {
            return field.get(instance);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to access non-public field " + field.getDeclaringClass().getName() + "." + field.getName());
        }
    }

    private void reflectionSet(Object instance, Object value) {
        try {
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to access non-public field " + field.getDeclaringClass().getName() + "." + field.getName());
        }
    }
}
