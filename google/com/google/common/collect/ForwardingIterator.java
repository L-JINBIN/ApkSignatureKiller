

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;

import java.util.Iterator;


@GwtCompatible
public abstract class ForwardingIterator<T> extends ForwardingObject implements Iterator<T> {


    protected ForwardingIterator() {
    }

    @Override
    protected abstract Iterator<T> delegate();

    @Override
    public boolean hasNext() {
        return delegate().hasNext();
    }

    @Override
    public T next() {
        return delegate().next();
    }

    @Override
    public void remove() {
        delegate().remove();
    }
}
