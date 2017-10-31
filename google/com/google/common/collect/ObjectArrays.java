

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;

import java.lang.reflect.Array;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkPositionIndexes;


@GwtCompatible(emulated = true)
public final class ObjectArrays {
    static final Object[] EMPTY_ARRAY = new Object[0];

    private ObjectArrays() {
    }


    @GwtIncompatible("Array.newInstance(Class, int)")
    @SuppressWarnings("unchecked")
    public static <T> T[] newArray(Class<T> type, int length) {
        return (T[]) Array.newInstance(type, length);
    }


    public static <T> T[] newArray(T[] reference, int length) {
        return Platform.newArray(reference, length);
    }


    @GwtIncompatible("Array.newInstance(Class, int)")
    public static <T> T[] concat(T[] first, T[] second, Class<T> type) {
        T[] result = newArray(type, first.length + second.length);
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }


    public static <T> T[] concat(@Nullable T element, T[] array) {
        T[] result = newArray(array, array.length + 1);
        result[0] = element;
        System.arraycopy(array, 0, result, 1, array.length);
        return result;
    }


    public static <T> T[] concat(T[] array, @Nullable T element) {
        T[] result = arraysCopyOf(array, array.length + 1);
        result[array.length] = element;
        return result;
    }


    static <T> T[] arraysCopyOf(T[] original, int newLength) {
        T[] copy = newArray(original, newLength);
        System.arraycopy(
                original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }


    static <T> T[] toArrayImpl(Collection<?> c, T[] array) {
        int size = c.size();
        if (array.length < size) {
            array = newArray(array, size);
        }
        fillArray(c, array);
        if (array.length > size) {
            array[size] = null;
        }
        return array;
    }


    static <T> T[] toArrayImpl(Object[] src, int offset, int len, T[] dst) {
        checkPositionIndexes(offset, offset + len, src.length);
        if (dst.length < len) {
            dst = newArray(dst, len);
        } else if (dst.length > len) {
            dst[len] = null;
        }
        System.arraycopy(src, offset, dst, 0, len);
        return dst;
    }


    static Object[] toArrayImpl(Collection<?> c) {
        return fillArray(c, new Object[c.size()]);
    }


    static Object[] copyAsObjectArray(Object[] elements, int offset, int length) {
        checkPositionIndexes(offset, offset + length, elements.length);
        if (length == 0) {
            return EMPTY_ARRAY;
        }
        Object[] result = new Object[length];
        System.arraycopy(elements, offset, result, 0, length);
        return result;
    }

    private static Object[] fillArray(Iterable<?> elements, Object[] array) {
        int i = 0;
        for (Object element : elements) {
            array[i++] = element;
        }
        return array;
    }


    static void swap(Object[] array, int i, int j) {
        Object temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    static Object[] checkElementsNotNull(Object... array) {
        return checkElementsNotNull(array, array.length);
    }

    static Object[] checkElementsNotNull(Object[] array, int length) {
        for (int i = 0; i < length; i++) {
            checkElementNotNull(array[i], i);
        }
        return array;
    }

    // We do this instead of Preconditions.checkNotNull to save boxing and array
    // creation cost.
    static Object checkElementNotNull(Object element, int index) {
        if (element == null) {
            throw new NullPointerException("at index " + index);
        }
        return element;
    }
}
