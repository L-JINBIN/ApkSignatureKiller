

package com.google.common.math;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.VisibleForTesting;

import java.math.RoundingMode;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.math.MathPreconditions.checkNoOverflow;
import static com.google.common.math.MathPreconditions.checkNonNegative;
import static com.google.common.math.MathPreconditions.checkPositive;
import static com.google.common.math.MathPreconditions.checkRoundingUnnecessary;
import static java.lang.Math.abs;
import static java.math.RoundingMode.HALF_EVEN;
import static java.math.RoundingMode.HALF_UP;


@GwtCompatible(emulated = true)
public final class IntMath {
    // NOTE: Whenever both tests are cheap and functional, it's faster to use &, | instead of &&, ||


    public static boolean isPowerOfTwo(int x) {
        return x > 0 & (x & (x - 1)) == 0;
    }


    @VisibleForTesting
    static int lessThanBranchFree(int x, int y) {
        // The double negation is optimized away by normal Java, but is necessary for GWT
        // to make sure bit twiddling works as expected.
        return ~~(x - y) >>> (Integer.SIZE - 1);
    }


    @SuppressWarnings("fallthrough")
    // TODO(kevinb): remove after this warning is disabled globally
    public static int log2(int x, RoundingMode mode) {
        checkPositive("x", x);
        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(isPowerOfTwo(x));
                // fall through
            case DOWN:
            case FLOOR:
                return (Integer.SIZE - 1) - Integer.numberOfLeadingZeros(x);

            case UP:
            case CEILING:
                return Integer.SIZE - Integer.numberOfLeadingZeros(x - 1);

            case HALF_DOWN:
            case HALF_UP:
            case HALF_EVEN:
                // Since sqrt(2) is irrational, log2(x) - logFloor cannot be exactly 0.5
                int leadingZeros = Integer.numberOfLeadingZeros(x);
                int cmp = MAX_POWER_OF_SQRT2_UNSIGNED >>> leadingZeros;
                // floor(2^(logFloor + 0.5))
                int logFloor = (Integer.SIZE - 1) - leadingZeros;
                return logFloor + lessThanBranchFree(cmp, x);

            default:
                throw new AssertionError();
        }
    }


    @VisibleForTesting
    static final int MAX_POWER_OF_SQRT2_UNSIGNED = 0xB504F333;


    @SuppressWarnings("fallthrough")
    public static int divide(int p, int q, RoundingMode mode) {
        checkNotNull(mode);
        if (q == 0) {
            throw new ArithmeticException("/ by zero"); // for GWT
        }
        int div = p / q;
        int rem = p - q * div; // equal to p % q

        if (rem == 0) {
            return div;
        }


        int signum = 1 | ((p ^ q) >> (Integer.SIZE - 1));
        boolean increment;
        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(rem == 0);
                // fall through
            case DOWN:
                increment = false;
                break;
            case UP:
                increment = true;
                break;
            case CEILING:
                increment = signum > 0;
                break;
            case FLOOR:
                increment = signum < 0;
                break;
            case HALF_EVEN:
            case HALF_DOWN:
            case HALF_UP:
                int absRem = abs(rem);
                int cmpRemToHalfDivisor = absRem - (abs(q) - absRem);
                // subtracting two nonnegative ints can't overflow
                // cmpRemToHalfDivisor has the same sign as compare(abs(rem), abs(q) / 2).
                if (cmpRemToHalfDivisor == 0) { // exactly on the half mark
                    increment = (mode == HALF_UP || (mode == HALF_EVEN & (div & 1) != 0));
                } else {
                    increment = cmpRemToHalfDivisor > 0; // closer to the UP value
                }
                break;
            default:
                throw new AssertionError();
        }
        return increment ? div + signum : div;
    }


    public static int checkedMultiply(int a, int b) {
        long result = (long) a * b;
        checkNoOverflow(result == (int) result);
        return (int) result;
    }


    public static int factorial(int n) {
        checkNonNegative("n", n);
        return (n < factorials.length) ? factorials[n] : Integer.MAX_VALUE;
    }

    private static final int[] factorials = {
            1,
            1,
            1 * 2,
            1 * 2 * 3,
            1 * 2 * 3 * 4,
            1 * 2 * 3 * 4 * 5,
            1 * 2 * 3 * 4 * 5 * 6,
            1 * 2 * 3 * 4 * 5 * 6 * 7,
            1 * 2 * 3 * 4 * 5 * 6 * 7 * 8,
            1 * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9,
            1 * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10,
            1 * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11,
            1 * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12};


    private IntMath() {
    }
}
