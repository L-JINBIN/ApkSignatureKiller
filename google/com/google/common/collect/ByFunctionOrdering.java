

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Function;
import com.google.common.base.Objects;

import java.io.Serializable;

import static com.google.common.base.Preconditions.checkNotNull;


@GwtCompatible(serializable = true)
final class ByFunctionOrdering<F, T> extends Ordering<F> implements Serializable {
    final Function<F, ? extends T> function;
    final Ordering<T> ordering;

    ByFunctionOrdering(Function<F, ? extends T> function, Ordering<T> ordering) {
        this.function = checkNotNull(function);
        this.ordering = checkNotNull(ordering);
    }

    @Override
    public int compare(F left, F right) {
        return ordering.compare(function.apply(left), function.apply(right));
    }

    @Override
    public boolean equals(@Nullable Object object) {
        if (object == this) {
            return true;
        }
        if (object instanceof ByFunctionOrdering) {
            ByFunctionOrdering<?, ?> that = (ByFunctionOrdering<?, ?>) object;
            return this.function.equals(that.function) && this.ordering.equals(that.ordering);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(function, ordering);
    }

    @Override
    public String toString() {
        return ordering + ".onResultOf(" + function + ")";
    }

    private static final long serialVersionUID = 0;
}
