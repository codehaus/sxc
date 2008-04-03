package com.envoisolutions.sxc.util;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

public class FieldAccessor<BeanType, FieldType> {
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

    @SuppressWarnings({"unchecked"})
    public FieldType getObject(BeanType instance) {
        if (unsafe == null) {
            return reflectionGet(instance);
        }
        return (FieldType) unsafe.getObject(instance, offset);
    }

    public void setObject(BeanType instance, FieldType value) {
        if (unsafe == null) {
            reflectionSet(instance, value);
            return;
        }
        unsafe.putObject(instance, offset, value);
    }

    public boolean getBoolean(BeanType instance) {
        if (unsafe == null) {
            return (Boolean) reflectionGet(instance);
        }
        return unsafe.getBoolean(instance, offset);
    }

    public void setBoolean(BeanType instance, boolean value) {
        if (unsafe == null) {
            reflectionSet(instance, value);
            return;
        }
        unsafe.putBoolean(instance, offset, value);
    }

    public byte getByte(BeanType instance) {
        if (unsafe == null) {
            return (Byte) reflectionGet(instance);
        }
        return unsafe.getByte(instance, offset);
    }

    public void setByte(BeanType instance, byte value) {
        if (unsafe == null) {
            reflectionSet(instance, value);
            return;
        }
        unsafe.putByte(instance, offset, value);
    }

    public char getChar(BeanType instance) {
        if (unsafe == null) {
            return (Character) reflectionGet(instance);
        }
        return unsafe.getChar(instance, offset);
    }

    public void setChar(BeanType instance, char value) {
        if (unsafe == null) {
            reflectionSet(instance, value);
            return;
        }
        unsafe.putChar(instance, offset, value);
    }

    public short getShort(BeanType instance) {
        if (unsafe == null) {
            return (Short) reflectionGet(instance);
        }
        return unsafe.getShort(instance, offset);
    }

    public void setShort(BeanType instance, short value) {
        if (unsafe == null) {
            reflectionSet(instance, value);
            return;
        }
        unsafe.putShort(instance, offset, value);
    }

    public int getInt(BeanType instance) {
        if (unsafe == null) {
            return (Integer) reflectionGet(instance);
        }
        return unsafe.getInt(instance, offset);
    }

    public void setInt(BeanType instance, int value) {
        if (unsafe == null) {
            reflectionSet(instance, value);
            return;
        }
        unsafe.putInt(instance, offset, value);
    }

    public long getLong(BeanType instance) {
        if (unsafe == null) {
            return (Long) reflectionGet(instance);
        }
        return unsafe.getLong(instance, offset);
    }

    public void setLong(BeanType instance, long value) {
        if (unsafe == null) {
            reflectionSet(instance, value);
            return;
        }
        unsafe.putLong(instance, offset, value);
    }

    public float getFloat(BeanType instance) {
        if (unsafe == null) {
            return (Float) reflectionGet(instance);
        }
        return unsafe.getFloat(instance, offset);
    }

    public void setFloat(BeanType instance, float value) {
        if (unsafe == null) {
            reflectionSet(instance, value);
            return;
        }
        unsafe.putFloat(instance, offset, value);
    }

    public double getDouble(BeanType instance) {
        if (unsafe == null) {
            return (Double) reflectionGet(instance);
        }
        return unsafe.getDouble(instance, offset);
    }

    public void setDouble(BeanType instance, double value) {
        if (unsafe == null) {
            reflectionSet(instance, value);
            return;
        }
        unsafe.putDouble(instance, offset, value);
    }

    @SuppressWarnings({"unchecked"})
    private FieldType reflectionGet(BeanType instance) {
        try {
            return (FieldType) field.get(instance);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to access non-public field " + field.getDeclaringClass().getName() + "." + field.getName());
        }
    }

    private void reflectionSet(BeanType instance, Object value) {
        try {
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to access non-public field " + field.getDeclaringClass().getName() + "." + field.getName());
        }
    }
}
