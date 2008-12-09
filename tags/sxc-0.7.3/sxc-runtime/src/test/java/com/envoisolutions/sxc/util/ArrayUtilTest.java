package com.envoisolutions.sxc.util;

import junit.framework.TestCase;
import static com.envoisolutions.sxc.util.ArrayUtil.INITIAL_SIZE;

public class ArrayUtilTest extends TestCase {
    public void testBooleanArray() throws Exception {
        // fill array to it's initial size (just short of causing it to grow)
        ArrayUtil.BooleanArray booleanArray = new ArrayUtil.BooleanArray();
        for (int i = 0; i < INITIAL_SIZE; i ++) {
            booleanArray.add(i % 2 == 0);
        }

        // verify array
        boolean[] booleans = booleanArray.toArray();
        assertEquals(INITIAL_SIZE, booleans.length);
        for (int i = 0; i < booleans.length; i ++) {
            assertEquals(i % 2 == 0, booleans[i]);
        }

        // double the size of the array... this should cause it to grow twice
        for (int i = 0; i < INITIAL_SIZE; i ++) {
            booleanArray.add((i + INITIAL_SIZE) % 2 == 0);
        }

        // verify array
        booleans = booleanArray.toArray();
        assertEquals(INITIAL_SIZE * 2, booleans.length);
        for (int i = 0; i < booleans.length; i ++) {
            assertEquals(i % 2 == 0, booleans[i]);
        }
    }

    public void testCharArray() throws Exception {
        // fill array to it's initial size (just short of causing it to grow)
        ArrayUtil.CharArray charArray = new ArrayUtil.CharArray();
        for (int i = 0; i < INITIAL_SIZE; i ++) {
            charArray.add((char) ('a' + i));
        }

        // verify array
        char[] chars = charArray.toArray();
        assertEquals(INITIAL_SIZE, chars.length);
        for (int i = 0; i < chars.length; i ++) {
            assertEquals((char) ('a' + i), chars[i]);
        }

        // double the size of the array... this should cause it to grow twice
        for (int i = 0; i < INITIAL_SIZE; i ++) {
            charArray.add((char) ('a' + i + INITIAL_SIZE));
        }

        // verify array
        chars = charArray.toArray();
        assertEquals(INITIAL_SIZE * 2, chars.length);
        for (int i = 0; i < chars.length; i ++) {
            assertEquals((char) ('a' + i), chars[i]);
        }
    }

    public void testShortArray() throws Exception {
        // fill array to it's initial size (just short of causing it to grow)
        ArrayUtil.ShortArray shortArray = new ArrayUtil.ShortArray();
        for (int i = 0; i < INITIAL_SIZE; i ++) {
            shortArray.add((short) i);
        }

        // verify array
        short[] shorts = shortArray.toArray();
        assertEquals(INITIAL_SIZE, shorts.length);
        for (int i = 0; i < shorts.length; i ++) {
            assertEquals((short) i, shorts[i]);
        }

        // double the size of the array... this should cause it to grow twice
        for (int i = 0; i < INITIAL_SIZE; i ++) {
            shortArray.add((short) (i + INITIAL_SIZE));
        }

        // verify array
        shorts = shortArray.toArray();
        assertEquals(INITIAL_SIZE * 2, shorts.length);
        for (int i = 0; i < shorts.length; i ++) {
            assertEquals((short) (i), shorts[i]);
        }
    }

    public void testIntArray() throws Exception {
        // fill array to it's initial size (just int of causing it to grow)
        ArrayUtil.IntArray intArray = new ArrayUtil.IntArray();
        for (int i = 0; i < INITIAL_SIZE; i ++) {
            intArray.add(i);
        }

        // verify array
        int[] ints = intArray.toArray();
        assertEquals(INITIAL_SIZE, ints.length);
        for (int i = 0; i < ints.length; i ++) {
            assertEquals(i, ints[i]);
        }

        // double the size of the array... this should cause it to grow twice
        for (int i = 0; i < INITIAL_SIZE; i ++) {
            intArray.add(i + INITIAL_SIZE);
        }

        // verify array
        ints = intArray.toArray();
        assertEquals(INITIAL_SIZE * 2, ints.length);
        for (int i = 0; i < ints.length; i ++) {
            assertEquals(i, ints[i]);
        }
    }

    public void testLongArray() throws Exception {
        // fill array to it's initial size (just long of causing it to grow)
        ArrayUtil.LongArray longArray = new ArrayUtil.LongArray();
        for (int i = 0; i < INITIAL_SIZE; i ++) {
            longArray.add((long) i);
        }

        // verify array
        long[] longs = longArray.toArray();
        assertEquals(INITIAL_SIZE, longs.length);
        for (int i = 0; i < longs.length; i ++) {
            assertEquals((long) i, longs[i]);
        }

        // double the size of the array... this should cause it to grow twice
        for (int i = 0; i < INITIAL_SIZE; i ++) {
            longArray.add((long) (i + INITIAL_SIZE));
        }

        // verify array
        longs = longArray.toArray();
        assertEquals(INITIAL_SIZE * 2, longs.length);
        for (int i = 0; i < longs.length; i ++) {
            assertEquals((long) (i), longs[i]);
        }
    }

    public void testFloatArray() throws Exception {
        // fill array to it's initial size (just float of causing it to grow)
        ArrayUtil.FloatArray floatArray = new ArrayUtil.FloatArray();
        for (int i = 0; i < INITIAL_SIZE; i ++) {
            floatArray.add((float) i);
        }

        // verify array
        float[] floats = floatArray.toArray();
        assertEquals(INITIAL_SIZE, floats.length);
        for (int i = 0; i < floats.length; i ++) {
            assertEquals((float) i, floats[i]);
        }

        // double the size of the array... this should cause it to grow twice
        for (int i = 0; i < INITIAL_SIZE; i ++) {
            floatArray.add((float) (i + INITIAL_SIZE));
        }

        // verify array
        floats = floatArray.toArray();
        assertEquals(INITIAL_SIZE * 2, floats.length);
        for (int i = 0; i < floats.length; i ++) {
            assertEquals((float) (i), floats[i]);
        }
    }

    public void testDoubleArray() throws Exception {
        // fill array to it's initial size (just double of causing it to grow)
        ArrayUtil.DoubleArray doubleArray = new ArrayUtil.DoubleArray();
        for (int i = 0; i < INITIAL_SIZE; i ++) {
            doubleArray.add((double) i);
        }

        // verify array
        double[] doubles = doubleArray.toArray();
        assertEquals(INITIAL_SIZE, doubles.length);
        for (int i = 0; i < doubles.length; i ++) {
            assertEquals((double) i, doubles[i]);
        }

        // double the size of the array... this should cause it to grow twice
        for (int i = 0; i < INITIAL_SIZE; i ++) {
            doubleArray.add((double) (i + INITIAL_SIZE));
        }

        // verify array
        doubles = doubleArray.toArray();
        assertEquals(INITIAL_SIZE * 2, doubles.length);
        for (int i = 0; i < doubles.length; i ++) {
            assertEquals((double) (i), doubles[i]);
        }
    }
}
