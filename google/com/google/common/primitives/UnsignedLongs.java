

package com.google.common.primitives;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

import java.math.BigInteger;

@Beta
@GwtCompatible
public final class UnsignedLongs {
    private UnsignedLongs() {
    }

    public static final long MAX_VALUE = -1L; // Equivalent to 2^64 - 1


    private static long flip(long a) {
        return a ^ Long.MIN_VALUE;
    }


    public static int compare(long a, long b) {
        return Longs.compare(flip(a), flip(b));
    }


    public static long divide(long dividend, long divisor) {
        if (divisor < 0) { // i.e., divisor >= 2^63:
            if (compare(dividend, divisor) < 0) {
                return 0; // dividend < divisor
            } else {
                return 1; // dividend >= divisor
            }
        }

        // Optimization - use signed division if dividend < 2^63
        if (dividend >= 0) {
            return dividend / divisor;
        }


        long quotient = ((dividend >>> 1) / divisor) << 1;
        long rem = dividend - quotient * divisor;
        return quotient + (compare(rem, divisor) >= 0 ? 1 : 0);
    }


    public static long remainder(long dividend, long divisor) {
        if (divisor < 0) { // i.e., divisor >= 2^63:
            if (compare(dividend, divisor) < 0) {
                return dividend; // dividend < divisor
            } else {
                return dividend - divisor; // dividend >= divisor
            }
        }

        // Optimization - use signed modulus if dividend < 2^63
        if (dividend >= 0) {
            return dividend % divisor;
        }


        long quotient = ((dividend >>> 1) / divisor) << 1;
        long rem = dividend - quotient * divisor;
        return rem - (compare(rem, divisor) >= 0 ? divisor : 0);
    }

    // calculated as 0xffffffffffffffff / radix
    private static final long[] maxValueDivs = new long[Character.MAX_RADIX + 1];
    private static final int[] maxValueMods = new int[Character.MAX_RADIX + 1];
    private static final int[] maxSafeDigits = new int[Character.MAX_RADIX + 1];

    static {
        BigInteger overflow = new BigInteger("10000000000000000", 16);
        for (int i = Character.MIN_RADIX; i <= Character.MAX_RADIX; i++) {
            maxValueDivs[i] = divide(MAX_VALUE, i);
            maxValueMods[i] = (int) remainder(MAX_VALUE, i);
            maxSafeDigits[i] = overflow.toString(i).length() - 1;
        }
    }
}
