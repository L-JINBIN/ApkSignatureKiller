

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;

import java.util.NoSuchElementException;
import java.util.Queue;


@GwtCompatible
public abstract class ForwardingQueue<E> extends ForwardingCollection<E> implements Queue<E> {


    protected ForwardingQueue() {
    }

    @Override
    protected abstract Queue<E> delegate();

    @Override
    public boolean offer(E o) {
        return delegate().offer(o);
    }

    @Override
    public E poll() {
        return delegate().poll();
    }

    @Override
    public E remove() {
        return delegate().remove();
    }

    @Override
    public E peek() {
        return delegate().peek();
    }

    @Override
    public E element() {
        return delegate().element();
    }


    protected boolean standardOffer(E e) {
        try {
            return add(e);
        } catch (IllegalStateException caught) {
            return false;
        }
    }


    protected E standardPeek() {
        try {
            return element();
        } catch (NoSuchElementException caught) {
            return null;
        }
    }


    protected E standardPoll() {
        try {
            return remove();
        } catch (NoSuchElementException caught) {
            return null;
        }
    }
}
