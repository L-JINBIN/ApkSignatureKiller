

package com.google.common.primitives;

import com.google.common.annotations.GwtCompatible;


@GwtCompatible(emulated = true)
public final class Ints {
    private Ints() {
    }


    public static final int MAX_POWER_OF_TWO = 1 << (Integer.SIZE - 2);


    public static int saturatedCast(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) value;
    }


    public static int compare(int a, int b) {
        return (a < b) ? -1 : ((a > b) ? 1 : 0);
    }


}
