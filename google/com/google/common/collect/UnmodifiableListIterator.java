

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;

import java.util.ListIterator;


@GwtCompatible
public abstract class UnmodifiableListIterator<E>
        extends UnmodifiableIterator<E> implements ListIterator<E> {

    protected UnmodifiableListIterator() {
    }


    @Deprecated
    @Override
    public final void add(E e) {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    @Override
    public final void set(E e) {
        throw new UnsupportedOperationException();
    }
}
