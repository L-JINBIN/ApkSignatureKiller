

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;

import java.io.Serializable;

import static com.google.common.base.Preconditions.checkNotNull;


@GwtCompatible(serializable = true)
@SuppressWarnings("unchecked") // TODO(kevinb): the right way to explain this??
final class NaturalOrdering
        extends Ordering<Comparable> implements Serializable {
    static final NaturalOrdering INSTANCE = new NaturalOrdering();

    @Override
    public int compare(Comparable left, Comparable right) {
        checkNotNull(left); // for GWT
        checkNotNull(right);
        return left.compareTo(right);
    }

    @Override
    public <S extends Comparable> Ordering<S> reverse() {
        return (Ordering<S>) ReverseNaturalOrdering.INSTANCE;
    }

    // preserving singleton-ness gives equals()/hashCode() for free
    private Object readResolve() {
        return INSTANCE;
    }

    @Override
    public String toString() {
        return "Ordering.natural()";
    }

    private NaturalOrdering() {
    }

    private static final long serialVersionUID = 0;
}
