

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;

import java.io.Serializable;


@GwtCompatible(serializable = true)
final class NullsLastOrdering<T> extends Ordering<T> implements Serializable {
    final Ordering<? super T> ordering;

    NullsLastOrdering(Ordering<? super T> ordering) {
        this.ordering = ordering;
    }

    @Override
    public int compare(@Nullable T left, @Nullable T right) {
        if (left == right) {
            return 0;
        }
        if (left == null) {
            return LEFT_IS_GREATER;
        }
        if (right == null) {
            return RIGHT_IS_GREATER;
        }
        return ordering.compare(left, right);
    }

    @Override
    public <S extends T> Ordering<S> reverse() {
        // ordering.reverse() might be optimized, so let it do its thing
        return ordering.reverse().nullsFirst();
    }

    @Override
    public <S extends T> Ordering<S> nullsFirst() {
        return ordering.nullsFirst();
    }

    @SuppressWarnings("unchecked") // still need the right way to explain this
    @Override
    public <S extends T> Ordering<S> nullsLast() {
        return (Ordering<S>) this;
    }

    @Override
    public boolean equals(@Nullable Object object) {
        if (object == this) {
            return true;
        }
        if (object instanceof NullsLastOrdering) {
            NullsLastOrdering<?> that = (NullsLastOrdering<?>) object;
            return this.ordering.equals(that.ordering);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return ordering.hashCode() ^ -921210296; // meaningless
    }

    @Override
    public String toString() {
        return ordering + ".nullsLast()";
    }

    private static final long serialVersionUID = 0;
}
