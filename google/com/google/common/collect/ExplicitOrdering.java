

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;

import java.io.Serializable;
import java.util.List;


@GwtCompatible(serializable = true)
final class ExplicitOrdering<T> extends Ordering<T> implements Serializable {
    final ImmutableMap<T, Integer> rankMap;

    ExplicitOrdering(List<T> valuesInOrder) {
        this(Maps.indexMap(valuesInOrder));
    }

    ExplicitOrdering(ImmutableMap<T, Integer> rankMap) {
        this.rankMap = rankMap;
    }

    @Override
    public int compare(T left, T right) {
        return rank(left) - rank(right); // safe because both are nonnegative
    }

    private int rank(T value) {
        Integer rank = rankMap.get(value);
        if (rank == null) {
            throw new IncomparableValueException(value);
        }
        return rank;
    }

    @Override
    public boolean equals(@Nullable Object object) {
        if (object instanceof ExplicitOrdering) {
            ExplicitOrdering<?> that = (ExplicitOrdering<?>) object;
            return this.rankMap.equals(that.rankMap);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return rankMap.hashCode();
    }

    @Override
    public String toString() {
        return "Ordering.explicit(" + rankMap.keySet() + ")";
    }

    private static final long serialVersionUID = 0;
}
