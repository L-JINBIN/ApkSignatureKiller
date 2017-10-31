

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;

import java.io.Serializable;
import java.util.Comparator;


@GwtCompatible(serializable = true)
final class CompoundOrdering<T> extends Ordering<T> implements Serializable {
    final ImmutableList<Comparator<? super T>> comparators;

    CompoundOrdering(Comparator<? super T> primary, Comparator<? super T> secondary) {
        this.comparators = ImmutableList.<Comparator<? super T>>of(primary, secondary);
    }

    CompoundOrdering(Iterable<? extends Comparator<? super T>> comparators) {
        this.comparators = ImmutableList.copyOf(comparators);
    }

    @Override
    public int compare(T left, T right) {
        // Avoid using the Iterator to avoid generating garbage (issue 979).
        int size = comparators.size();
        for (int i = 0; i < size; i++) {
            int result = comparators.get(i).compare(left, right);
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (object instanceof CompoundOrdering) {
            CompoundOrdering<?> that = (CompoundOrdering<?>) object;
            return this.comparators.equals(that.comparators);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return comparators.hashCode();
    }

    @Override
    public String toString() {
        return "Ordering.compound(" + comparators + ")";
    }

    private static final long serialVersionUID = 0;
}
