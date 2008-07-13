package com.envoisolutions.sxc.util;

public class ArrayUtil {
    public static final int INITIAL_SIZE = 10;
    public static class BooleanArray {
        private int index;
        private boolean[] array = new boolean[INITIAL_SIZE];

        public void add(boolean b) {
            if (index >= array.length) {
                boolean[] old = array;
                // grow by 50% + 1 element
                array = new boolean[(old.length * 3 / 2) + 1];
                System.arraycopy(old, 0, array, 0, old.length);
            }
            array[index++] = b;
        }

        public boolean[] toArray() {
            if (array.length != index) {
                boolean[] newArray = new boolean[index];
                System.arraycopy(array, 0, newArray, 0, index);
                return newArray;
            }
            return array;
        }
    }

    public static class CharArray {
        private int index;
        private char[] array = new char[INITIAL_SIZE];

        public void add(char b) {
            if (index >= array.length) {
                char[] old = array;
                // grow by 50% + 1 element
                array = new char[(old.length * 3 / 2) + 1];
                System.arraycopy(old, 0, array, 0, old.length);
            }
            array[index++] = b;
        }

        public char[] toArray() {
            if (array.length != index) {
                char[] newArray = new char[index];
                System.arraycopy(array, 0, newArray, 0, index);
                return newArray;
            }
            return array;
        }
    }

    public static class ShortArray {
        private int index;
        private short[] array = new short[INITIAL_SIZE];

        public void add(short b) {
            if (index >= array.length) {
                short[] old = array;
                // grow by 50% + 1 element
                array = new short[(old.length * 3 / 2) + 1];
                System.arraycopy(old, 0, array, 0, old.length);
            }
            array[index++] = b;
        }

        public short[] toArray() {
            if (array.length != index) {
                short[] newArray = new short[index];
                System.arraycopy(array, 0, newArray, 0, index);
                return newArray;
            }
            return array;
        }
    }

    public static class IntArray {
        private int index;
        private int[] array = new int[INITIAL_SIZE];

        public void add(int b) {
            if (index >= array.length) {
                int[] old = array;
                // grow by 50% + 1 element
                array = new int[(old.length * 3 / 2) + 1];
                System.arraycopy(old, 0, array, 0, old.length);
            }
            array[index++] = b;
        }

        public int[] toArray() {
            if (array.length != index) {
                int[] newArray = new int[index];
                System.arraycopy(array, 0, newArray, 0, index);
                return newArray;
            }
            return array;
        }
    }

    public static class LongArray {
        private int index;
        private long[] array = new long[INITIAL_SIZE];

        public void add(long b) {
            if (index >= array.length) {
                long[] old = array;
                // grow by 50% + 1 element
                array = new long[(old.length * 3 / 2) + 1];
                System.arraycopy(old, 0, array, 0, old.length);
            }
            array[index++] = b;
        }

        public long[] toArray() {
            if (array.length != index) {
                long[] newArray = new long[index];
                System.arraycopy(array, 0, newArray, 0, index);
                return newArray;
            }
            return array;
        }
    }

    public static class FloatArray {
        private int index;
        private float[] array = new float[INITIAL_SIZE];

        public void add(float b) {
            if (index >= array.length) {
                float[] old = array;
                // grow by 50% + 1 element
                array = new float[(old.length * 3 / 2) + 1];
                System.arraycopy(old, 0, array, 0, old.length);
            }
            array[index++] = b;
        }

        public float[] toArray() {
            if (array.length != index) {
                float[] newArray = new float[index];
                System.arraycopy(array, 0, newArray, 0, index);
                return newArray;
            }
            return array;
        }
    }

    public static class DoubleArray {
        private int index;
        private double[] array = new double[INITIAL_SIZE];

        public void add(double b) {
            if (index >= array.length) {
                double[] old = array;
                // grow by 50% + 1 element
                array = new double[(old.length * 3 / 2) + 1];
                System.arraycopy(old, 0, array, 0, old.length);
            }
            array[index++] = b;
        }

        public double[] toArray() {
            if (array.length != index) {
                double[] newArray = new double[index];
                System.arraycopy(array, 0, newArray, 0, index);
                return newArray;
            }
            return array;
        }
    }
}
