

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.primitives.Booleans;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

import java.util.Comparator;


@GwtCompatible
public abstract class ComparisonChain {
    private ComparisonChain() {
    }


    public static ComparisonChain start() {
        return ACTIVE;
    }

    private static final ComparisonChain ACTIVE =
            new ComparisonChain() {
                @SuppressWarnings("unchecked")
                @Override
                public ComparisonChain compare(Comparable left, Comparable right) {
                    return classify(left.compareTo(right));
                }

                @Override
                public <T> ComparisonChain compare(
                        @Nullable T left, @Nullable T right, Comparator<T> comparator) {
                    return classify(comparator.compare(left, right));
                }

                @Override
                public ComparisonChain compare(int left, int right) {
                    return classify(Ints.compare(left, right));
                }

                @Override
                public ComparisonChain compare(long left, long right) {
                    return classify(Longs.compare(left, right));
                }

                @Override
                public ComparisonChain compare(float left, float right) {
                    return classify(Float.compare(left, right));
                }

                @Override
                public ComparisonChain compare(double left, double right) {
                    return classify(Double.compare(left, right));
                }

                @Override
                public ComparisonChain compareTrueFirst(boolean left, boolean right) {
                    return classify(Booleans.compare(right, left)); // reversed
                }

                @Override
                public ComparisonChain compareFalseFirst(boolean left, boolean right) {
                    return classify(Booleans.compare(left, right));
                }

                ComparisonChain classify(int result) {
                    return (result < 0) ? LESS : (result > 0) ? GREATER : ACTIVE;
                }

                @Override
                public int result() {
                    return 0;
                }
            };

    private static final ComparisonChain LESS = new InactiveComparisonChain(-1);

    private static final ComparisonChain GREATER = new InactiveComparisonChain(1);

    private static final class InactiveComparisonChain extends ComparisonChain {
        final int result;

        InactiveComparisonChain(int result) {
            this.result = result;
        }

        @Override
        public ComparisonChain compare(@Nullable Comparable left, @Nullable Comparable right) {
            return this;
        }

        @Override
        public <T> ComparisonChain compare(
                @Nullable T left, @Nullable T right, @Nullable Comparator<T> comparator) {
            return this;
        }

        @Override
        public ComparisonChain compare(int left, int right) {
            return this;
        }

        @Override
        public ComparisonChain compare(long left, long right) {
            return this;
        }

        @Override
        public ComparisonChain compare(float left, float right) {
            return this;
        }

        @Override
        public ComparisonChain compare(double left, double right) {
            return this;
        }

        @Override
        public ComparisonChain compareTrueFirst(boolean left, boolean right) {
            return this;
        }

        @Override
        public ComparisonChain compareFalseFirst(boolean left, boolean right) {
            return this;
        }

        @Override
        public int result() {
            return result;
        }
    }


    public abstract ComparisonChain compare(Comparable<?> left, Comparable<?> right);


    public abstract <T> ComparisonChain compare(
            @Nullable T left, @Nullable T right, Comparator<T> comparator);


    public abstract ComparisonChain compare(int left, int right);


    public abstract ComparisonChain compare(long left, long right);


    public abstract ComparisonChain compare(float left, float right);


    public abstract ComparisonChain compare(double left, double right);


    @Deprecated
    public final ComparisonChain compare(Boolean left, Boolean right) {
        return compareFalseFirst(left, right);
    }


    public abstract ComparisonChain compareTrueFirst(boolean left, boolean right);


    public abstract ComparisonChain compareFalseFirst(boolean left, boolean right);


    public abstract int result();
}
