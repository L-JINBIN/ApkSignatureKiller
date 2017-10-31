

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;

import java.io.Serializable;
import java.util.Comparator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.BoundType.CLOSED;
import static com.google.common.collect.BoundType.OPEN;


@GwtCompatible(serializable = true)
final class GeneralRange<T> implements Serializable {


    static <T> GeneralRange<T> all(Comparator<? super T> comparator) {
        return new GeneralRange<>(comparator, false, null, OPEN, false, null, OPEN);
    }


    static <T> GeneralRange<T> downTo(
            Comparator<? super T> comparator, @Nullable T endpoint, BoundType boundType) {
        return new GeneralRange<>(comparator, true, endpoint, boundType, false, null, OPEN);
    }


    static <T> GeneralRange<T> upTo(
            Comparator<? super T> comparator, @Nullable T endpoint, BoundType boundType) {
        return new GeneralRange<>(comparator, false, null, OPEN, true, endpoint, boundType);
    }


    private final Comparator<? super T> comparator;
    private final boolean hasLowerBound;
    @Nullable
    private final T lowerEndpoint;
    private final BoundType lowerBoundType;
    private final boolean hasUpperBound;
    @Nullable
    private final T upperEndpoint;
    private final BoundType upperBoundType;

    private GeneralRange(
            Comparator<? super T> comparator,
            boolean hasLowerBound,
            @Nullable T lowerEndpoint,
            BoundType lowerBoundType,
            boolean hasUpperBound,
            @Nullable T upperEndpoint,
            BoundType upperBoundType) {
        this.comparator = checkNotNull(comparator);
        this.hasLowerBound = hasLowerBound;
        this.hasUpperBound = hasUpperBound;
        this.lowerEndpoint = lowerEndpoint;
        this.lowerBoundType = checkNotNull(lowerBoundType);
        this.upperEndpoint = upperEndpoint;
        this.upperBoundType = checkNotNull(upperBoundType);

        if (hasLowerBound) {
            comparator.compare(lowerEndpoint, lowerEndpoint);
        }
        if (hasUpperBound) {
            comparator.compare(upperEndpoint, upperEndpoint);
        }
        if (hasLowerBound && hasUpperBound) {
            int cmp = comparator.compare(lowerEndpoint, upperEndpoint);
            // be consistent with Range
            checkArgument(
                    cmp <= 0, "lowerEndpoint (%s) > upperEndpoint (%s)", lowerEndpoint, upperEndpoint);
            if (cmp == 0) {
                checkArgument(lowerBoundType != OPEN | upperBoundType != OPEN);
            }
        }
    }

    Comparator<? super T> comparator() {
        return comparator;
    }

    boolean hasLowerBound() {
        return hasLowerBound;
    }

    boolean hasUpperBound() {
        return hasUpperBound;
    }

    boolean isEmpty() {
        return (hasUpperBound() && tooLow(getUpperEndpoint()))
                || (hasLowerBound() && tooHigh(getLowerEndpoint()));
    }

    boolean tooLow(@Nullable T t) {
        if (!hasLowerBound()) {
            return false;
        }
        T lbound = getLowerEndpoint();
        int cmp = comparator.compare(t, lbound);
        return cmp < 0 | (cmp == 0 & getLowerBoundType() == OPEN);
    }

    boolean tooHigh(@Nullable T t) {
        if (!hasUpperBound()) {
            return false;
        }
        T ubound = getUpperEndpoint();
        int cmp = comparator.compare(t, ubound);
        return cmp > 0 | (cmp == 0 & getUpperBoundType() == OPEN);
    }

    boolean contains(@Nullable T t) {
        return !tooLow(t) && !tooHigh(t);
    }


    GeneralRange<T> intersect(GeneralRange<T> other) {
        checkNotNull(other);
        checkArgument(comparator.equals(other.comparator));

        boolean hasLowBound = this.hasLowerBound;
        T lowEnd = getLowerEndpoint();
        BoundType lowType = getLowerBoundType();
        if (!hasLowerBound()) {
            hasLowBound = other.hasLowerBound;
            lowEnd = other.getLowerEndpoint();
            lowType = other.getLowerBoundType();
        } else if (other.hasLowerBound()) {
            int cmp = comparator.compare(getLowerEndpoint(), other.getLowerEndpoint());
            if (cmp < 0 || (cmp == 0 && other.getLowerBoundType() == OPEN)) {
                lowEnd = other.getLowerEndpoint();
                lowType = other.getLowerBoundType();
            }
        }

        boolean hasUpBound = this.hasUpperBound;
        T upEnd = getUpperEndpoint();
        BoundType upType = getUpperBoundType();
        if (!hasUpperBound()) {
            hasUpBound = other.hasUpperBound;
            upEnd = other.getUpperEndpoint();
            upType = other.getUpperBoundType();
        } else if (other.hasUpperBound()) {
            int cmp = comparator.compare(getUpperEndpoint(), other.getUpperEndpoint());
            if (cmp > 0 || (cmp == 0 && other.getUpperBoundType() == OPEN)) {
                upEnd = other.getUpperEndpoint();
                upType = other.getUpperBoundType();
            }
        }

        if (hasLowBound && hasUpBound) {
            int cmp = comparator.compare(lowEnd, upEnd);
            if (cmp > 0 || (cmp == 0 && lowType == OPEN && upType == OPEN)) {
                // force allowed empty range
                lowEnd = upEnd;
                lowType = OPEN;
                upType = CLOSED;
            }
        }

        return new GeneralRange<>(comparator, hasLowBound, lowEnd, lowType, hasUpBound, upEnd, upType);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof GeneralRange) {
            GeneralRange<?> r = (GeneralRange<?>) obj;
            return comparator.equals(r.comparator)
                    && hasLowerBound == r.hasLowerBound
                    && hasUpperBound == r.hasUpperBound
                    && getLowerBoundType().equals(r.getLowerBoundType())
                    && getUpperBoundType().equals(r.getUpperBoundType())
                    && Objects.equal(getLowerEndpoint(), r.getLowerEndpoint())
                    && Objects.equal(getUpperEndpoint(), r.getUpperEndpoint());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(
                comparator,
                getLowerEndpoint(),
                getLowerBoundType(),
                getUpperEndpoint(),
                getUpperBoundType());
    }

    private transient GeneralRange<T> reverse;


    GeneralRange<T> reverse() {
        GeneralRange<T> result = reverse;
        if (result == null) {
            result = new GeneralRange<>(
                    Ordering.from(comparator).reverse(),
                    hasUpperBound,
                    getUpperEndpoint(),
                    getUpperBoundType(),
                    hasLowerBound,
                    getLowerEndpoint(),
                    getLowerBoundType());
            result.reverse = this;
            return this.reverse = result;
        }
        return result;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append(comparator)
                .append(":")
                .append(lowerBoundType == CLOSED ? '[' : '(')
                .append(hasLowerBound ? lowerEndpoint : "-\u221e")
                .append(',')
                .append(hasUpperBound ? upperEndpoint : "\u221e")
                .append(upperBoundType == CLOSED ? ']' : ')')
                .toString();
    }

    T getLowerEndpoint() {
        return lowerEndpoint;
    }

    BoundType getLowerBoundType() {
        return lowerBoundType;
    }

    T getUpperEndpoint() {
        return upperEndpoint;
    }

    BoundType getUpperBoundType() {
        return upperBoundType;
    }
}
