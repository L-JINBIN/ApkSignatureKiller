

package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.NoSuchElementException;


@GwtCompatible
@Beta
public abstract class DiscreteDomain<C extends Comparable> {


    private static final class IntegerDomain extends DiscreteDomain<Integer> implements Serializable {
        private static final IntegerDomain INSTANCE = new IntegerDomain();

        @Override
        public Integer next(Integer value) {
            int i = value;
            return (i == Integer.MAX_VALUE) ? null : i + 1;
        }

        @Override
        public Integer previous(Integer value) {
            int i = value;
            return (i == Integer.MIN_VALUE) ? null : i - 1;
        }

        @Override
        public long distance(Integer start, Integer end) {
            return (long) end - start;
        }

        @Override
        public Integer minValue() {
            return Integer.MIN_VALUE;
        }

        @Override
        public Integer maxValue() {
            return Integer.MAX_VALUE;
        }

        private Object readResolve() {
            return INSTANCE;
        }

        @Override
        public String toString() {
            return "DiscreteDomain.integers()";
        }

        private static final long serialVersionUID = 0;
    }


    private static final class LongDomain extends DiscreteDomain<Long> implements Serializable {
        private static final LongDomain INSTANCE = new LongDomain();

        @Override
        public Long next(Long value) {
            long l = value;
            return (l == Long.MAX_VALUE) ? null : l + 1;
        }

        @Override
        public Long previous(Long value) {
            long l = value;
            return (l == Long.MIN_VALUE) ? null : l - 1;
        }

        @Override
        public long distance(Long start, Long end) {
            long result = end - start;
            if (end > start && result < 0) { // overflow
                return Long.MAX_VALUE;
            }
            if (end < start && result > 0) { // underflow
                return Long.MIN_VALUE;
            }
            return result;
        }

        @Override
        public Long minValue() {
            return Long.MIN_VALUE;
        }

        @Override
        public Long maxValue() {
            return Long.MAX_VALUE;
        }

        private Object readResolve() {
            return INSTANCE;
        }

        @Override
        public String toString() {
            return "DiscreteDomain.longs()";
        }

        private static final long serialVersionUID = 0;
    }


    private static final class BigIntegerDomain extends DiscreteDomain<BigInteger>
            implements Serializable {
        private static final BigIntegerDomain INSTANCE = new BigIntegerDomain();

        private static final BigInteger MIN_LONG = BigInteger.valueOf(Long.MIN_VALUE);
        private static final BigInteger MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);

        @Override
        public BigInteger next(BigInteger value) {
            return value.add(BigInteger.ONE);
        }

        @Override
        public BigInteger previous(BigInteger value) {
            return value.subtract(BigInteger.ONE);
        }

        @Override
        public long distance(BigInteger start, BigInteger end) {
            return end.subtract(start)
                    .max(MIN_LONG)
                    .min(MAX_LONG)
                    .longValue();
        }

        private Object readResolve() {
            return INSTANCE;
        }

        @Override
        public String toString() {
            return "DiscreteDomain.bigIntegers()";
        }

        private static final long serialVersionUID = 0;
    }


    public abstract C next(C value);


    public abstract C previous(C value);


    public abstract long distance(C start, C end);


    public C minValue() {
        throw new NoSuchElementException();
    }


    public C maxValue() {
        throw new NoSuchElementException();
    }
}
