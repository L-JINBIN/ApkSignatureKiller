

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;

import java.util.ListIterator;


@GwtCompatible
abstract class TransformedListIterator<F, T> extends TransformedIterator<F, T>
        implements ListIterator<T> {
    TransformedListIterator(ListIterator<? extends F> backingIterator) {
        super(backingIterator);
    }

    private ListIterator<? extends F> backingIterator() {
        return Iterators.cast(backingIterator);
    }

    @Override
    public final boolean hasPrevious() {
        return backingIterator().hasPrevious();
    }

    @Override
    public final T previous() {
        return transform(backingIterator().previous());
    }

    @Override
    public final int nextIndex() {
        return backingIterator().nextIndex();
    }

    @Override
    public final int previousIndex() {
        return backingIterator().previousIndex();
    }

    @Override
    public void set(T element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(T element) {
        throw new UnsupportedOperationException();
    }
}
