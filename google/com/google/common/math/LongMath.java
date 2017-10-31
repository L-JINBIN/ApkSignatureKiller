

package com.google.common.math;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.VisibleForTesting;

import java.math.RoundingMode;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.math.MathPreconditions.checkNonNegative;
import static com.google.common.math.MathPreconditions.checkPositive;
import static com.google.common.math.MathPreconditions.checkRoundingUnnecessary;
import static java.lang.Math.min;


@GwtCompatible(emulated = true)
public final class LongMath {
    // NOTE: Whenever both tests are cheap and functional, it's faster to use &, | instead of &&, ||


    public static boolean isPowerOfTwo(long x) {
        return x > 0 & (x & (x - 1)) == 0;
    }


    @VisibleForTesting
    static int lessThanBranchFree(long x, long y) {
        // Returns the sign bit of x - y.
        return (int) (~~(x - y) >>> (Long.SIZE - 1));
    }


    @SuppressWarnings("fallthrough")
    // TODO(kevinb): remove after this warning is disabled globally
    public static int log2(long x, RoundingMode mode) {
        checkPositive("x", x);
        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(isPowerOfTwo(x));
                // fall through
            case DOWN:
            case FLOOR:
                return (Long.SIZE - 1) - Long.numberOfLeadingZeros(x);

            case UP:
            case CEILING:
                return Long.SIZE - Long.numberOfLeadingZeros(x - 1);

            case HALF_DOWN:
            case HALF_UP:
            case HALF_EVEN:
                // Since sqrt(2) is irrational, log2(x) - logFloor cannot be exactly 0.5
                int leadingZeros = Long.numberOfLeadingZeros(x);
                long cmp = MAX_POWER_OF_SQRT2_UNSIGNED >>> leadingZeros;
                // floor(2^(logFloor + 0.5))
                int logFloor = (Long.SIZE - 1) - leadingZeros;
                return logFloor + lessThanBranchFree(cmp, x);

            default:
                throw new AssertionError("impossible");
        }
    }


    @VisibleForTesting
    static final long MAX_POWER_OF_SQRT2_UNSIGNED = 0xB504F333F9DE6484L;


    public static long gcd(long a, long b) {

        checkNonNegative("a", a);
        checkNonNegative("b", b);
        if (a == 0) {
            // 0 % b == 0, so b divides a, but the converse doesn't hold.
            // BigInteger.gcd is consistent with this decision.
            return b;
        } else if (b == 0) {
            return a; // similar logic
        }

        int aTwos = Long.numberOfTrailingZeros(a);
        a >>= aTwos; // divide out all 2s
        int bTwos = Long.numberOfTrailingZeros(b);
        b >>= bTwos; // divide out all 2s
        while (a != b) { // both a, b are odd
            // The key to the binary GCD algorithm is as follows:
            // Both a and b are odd.  Assume a > b; then gcd(a - b, b) = gcd(a, b).
            // But in gcd(a - b, b), a - b is even and b is odd, so we can divide out powers of two.

            // We bend over backwards to avoid branching, adapting a technique from
            // http://graphics.stanford.edu/~seander/bithacks.html#IntegerMinOrMax

            long delta = a - b; // can't overflow, since a and b are nonnegative

            long minDeltaOrZero = delta & (delta >> (Long.SIZE - 1));
            // equivalent to Math.min(delta, 0)

            a = delta - minDeltaOrZero - minDeltaOrZero; // sets a to Math.abs(a - b)
            // a is now nonnegative and even

            b += minDeltaOrZero; // sets b to min(old a, b)
            a >>= Long.numberOfTrailingZeros(a); // divide out all 2s, since 2 doesn't divide b
        }
        return a << min(aTwos, bTwos);
    }


    static final long[] factorials = {
            1L,
            1L,
            1L * 2,
            1L * 2 * 3,
            1L * 2 * 3 * 4,
            1L * 2 * 3 * 4 * 5,
            1L * 2 * 3 * 4 * 5 * 6,
            1L * 2 * 3 * 4 * 5 * 6 * 7,
            1L * 2 * 3 * 4 * 5 * 6 * 7 * 8,
            1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9,
            1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10,
            1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11,
            1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12,
            1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13,
            1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14,
            1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14 * 15,
            1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14 * 15 * 16,
            1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14 * 15 * 16 * 17,
            1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14 * 15 * 16 * 17 * 18,
            1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14 * 15 * 16 * 17 * 18 * 19,
            1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14 * 15 * 16 * 17 * 18 * 19 * 20
    };


    public static long binomial(int n, int k) {
        checkNonNegative("n", n);
        checkNonNegative("k", k);
        checkArgument(k <= n, "k (%s) > n (%s)", k, n);
        if (k > (n >> 1)) {
            k = n - k;
        }
        switch (k) {
            case 0:
                return 1;
            case 1:
                return n;
            default:
                if (n < factorials.length) {
                    return factorials[n] / (factorials[k] * factorials[n - k]);
                } else if (k >= biggestBinomials.length || n > biggestBinomials[k]) {
                    return Long.MAX_VALUE;
                } else if (k < biggestSimpleBinomials.length && n <= biggestSimpleBinomials[k]) {
                    // guaranteed not to overflow
                    long result = n--;
                    for (int i = 2; i <= k; n--, i++) {
                        result *= n;
                        result /= i;
                    }
                    return result;
                } else {
                    int nBits = LongMath.log2(n, RoundingMode.CEILING);

                    long result = 1;
                    long numerator = n--;
                    long denominator = 1;

                    int numeratorBits = nBits;
                    // This is an upper bound on log2(numerator, ceiling).


                    for (int i = 2; i <= k; i++, n--) {
                        if (numeratorBits + nBits < Long.SIZE - 1) {
                            // It's definitely safe to multiply into numerator and denominator.
                            numerator *= n;
                            denominator *= i;
                            numeratorBits += nBits;
                        } else {
                            // It might not be safe to multiply into numerator and denominator,
                            // so multiply (numerator / denominator) into result.
                            result = multiplyFraction(result, numerator, denominator);
                            numerator = n;
                            denominator = i;
                            numeratorBits = nBits;
                        }
                    }
                    return multiplyFraction(result, numerator, denominator);
                }
        }
    }


    static long multiplyFraction(long x, long numerator, long denominator) {
        if (x == 1) {
            return numerator / denominator;
        }
        long commonDivisor = gcd(x, denominator);
        x /= commonDivisor;
        denominator /= commonDivisor;
        // We know gcd(x, denominator) = 1, and x * numerator / denominator is exact,
        // so denominator must be a divisor of numerator.
        return x * (numerator / denominator);
    }


    static final int[] biggestBinomials =
            {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 3810779, 121977, 16175, 4337, 1733,
                    887, 534, 361, 265, 206, 169, 143, 125, 111, 101, 94, 88, 83, 79, 76, 74, 72, 70, 69, 68,
                    67, 67, 66, 66, 66, 66};


    @VisibleForTesting
    static final int[] biggestSimpleBinomials =
            {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 2642246, 86251, 11724, 3218, 1313,
                    684, 419, 287, 214, 169, 139, 119, 105, 95, 87, 81, 76, 73, 70, 68, 66, 64, 63, 62, 62,
                    61, 61, 61};
    // These values were generated by using checkedMultiply to see when the simple multiply/divide
    // algorithm would lead to an overflow.


    private LongMath() {
    }
}
