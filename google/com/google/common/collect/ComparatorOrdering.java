

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;

import java.io.Serializable;
import java.util.Comparator;

import static com.google.common.base.Preconditions.checkNotNull;


@GwtCompatible(serializable = true)
final class ComparatorOrdering<T> extends Ordering<T> implements Serializable {
    final Comparator<T> comparator;

    ComparatorOrdering(Comparator<T> comparator) {
        this.comparator = checkNotNull(comparator);
    }

    @Override
    public int compare(T a, T b) {
        return comparator.compare(a, b);
    }

    @Override
    public boolean equals(@Nullable Object object) {
        if (object == this) {
            return true;
        }
        if (object instanceof ComparatorOrdering) {
            ComparatorOrdering<?> that = (ComparatorOrdering<?>) object;
            return this.comparator.equals(that.comparator);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return comparator.hashCode();
    }

    @Override
    public String toString() {
        return comparator.toString();
    }

    private static final long serialVersionUID = 0;
}
