

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;

import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;


@GwtCompatible
abstract class TransformedIterator<F, T> implements Iterator<T> {
    final Iterator<? extends F> backingIterator;

    TransformedIterator(Iterator<? extends F> backingIterator) {
        this.backingIterator = checkNotNull(backingIterator);
    }

    abstract T transform(F from);

    @Override
    public final boolean hasNext() {
        return backingIterator.hasNext();
    }

    @Override
    public final T next() {
        return transform(backingIterator.next());
    }

    @Override
    public final void remove() {
        backingIterator.remove();
    }
}
