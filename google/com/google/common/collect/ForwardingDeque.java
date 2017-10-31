

package com.google.common.collect;

import java.util.Deque;
import java.util.Iterator;


public abstract class ForwardingDeque<E> extends ForwardingQueue<E> implements Deque<E> {


    protected ForwardingDeque() {
    }

    @Override
    protected abstract Deque<E> delegate();

    @Override
    public void addFirst(E e) {
        delegate().addFirst(e);
    }

    @Override
    public void addLast(E e) {
        delegate().addLast(e);
    }

    @Override
    public Iterator<E> descendingIterator() {
        return delegate().descendingIterator();
    }

    @Override
    public E getFirst() {
        return delegate().getFirst();
    }

    @Override
    public E getLast() {
        return delegate().getLast();
    }

    @Override
    public boolean offerFirst(E e) {
        return delegate().offerFirst(e);
    }

    @Override
    public boolean offerLast(E e) {
        return delegate().offerLast(e);
    }

    @Override
    public E peekFirst() {
        return delegate().peekFirst();
    }

    @Override
    public E peekLast() {
        return delegate().peekLast();
    }

    @Override
    public E pollFirst() {
        return delegate().pollFirst();
    }

    @Override
    public E pollLast() {
        return delegate().pollLast();
    }

    @Override
    public E pop() {
        return delegate().pop();
    }

    @Override
    public void push(E e) {
        delegate().push(e);
    }

    @Override
    public E removeFirst() {
        return delegate().removeFirst();
    }

    @Override
    public E removeLast() {
        return delegate().removeLast();
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        return delegate().removeFirstOccurrence(o);
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        return delegate().removeLastOccurrence(o);
    }
}
