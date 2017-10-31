

package com.google.common.collect;

import android.support.annotation.Nullable;


abstract class AbstractRangeSet<C extends Comparable> implements RangeSet<C> {
    AbstractRangeSet() {
    }

    @Override
    public boolean contains(C value) {
        return rangeContaining(value) != null;
    }

    @Override
    public abstract Range<C> rangeContaining(C value);

    @Override
    public boolean isEmpty() {
        return asRanges().isEmpty();
    }

    @Override
    public void add(Range<C> range) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(Range<C> range) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        remove(Range.<C>all());
    }

    @Override
    public boolean enclosesAll(RangeSet<C> other) {
        for (Range<C> range : other.asRanges()) {
            if (!encloses(range)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void addAll(RangeSet<C> other) {
        for (Range<C> range : other.asRanges()) {
            add(range);
        }
    }

    @Override
    public void removeAll(RangeSet<C> other) {
        for (Range<C> range : other.asRanges()) {
            remove(range);
        }
    }

    @Override
    public abstract boolean encloses(Range<C> otherRange);

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof RangeSet) {
            RangeSet<?> other = (RangeSet<?>) obj;
            return this.asRanges().equals(other.asRanges());
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return asRanges().hashCode();
    }

    @Override
    public final String toString() {
        return asRanges().toString();
    }
}
