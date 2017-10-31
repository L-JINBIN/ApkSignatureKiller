

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;

import java.util.ListIterator;


@GwtCompatible
public abstract class ForwardingListIterator<E> extends ForwardingIterator<E>
        implements ListIterator<E> {


    protected ForwardingListIterator() {
    }

    @Override
    protected abstract ListIterator<E> delegate();

    @Override
    public void add(E element) {
        delegate().add(element);
    }

    @Override
    public boolean hasPrevious() {
        return delegate().hasPrevious();
    }

    @Override
    public int nextIndex() {
        return delegate().nextIndex();
    }

    @Override
    public E previous() {
        return delegate().previous();
    }

    @Override
    public int previousIndex() {
        return delegate().previousIndex();
    }

    @Override
    public void set(E element) {
        delegate().set(element);
    }
}
