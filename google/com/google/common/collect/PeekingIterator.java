

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;

import java.util.Iterator;


@GwtCompatible
public interface PeekingIterator<E> extends Iterator<E> {

    E peek();


    @Override
    E next();


    @Override
    void remove();
}
