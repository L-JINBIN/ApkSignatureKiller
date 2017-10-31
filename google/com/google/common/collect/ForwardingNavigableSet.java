

package com.google.common.collect;

import android.support.annotation.NonNull;

import com.google.common.annotations.Beta;

import java.util.Iterator;
import java.util.NavigableSet;
import java.util.SortedSet;


public abstract class ForwardingNavigableSet<E> extends ForwardingSortedSet<E>
        implements NavigableSet<E> {


    protected ForwardingNavigableSet() {
    }

    @Override
    protected abstract NavigableSet<E> delegate();

    @Override
    public E lower(E e) {
        return delegate().lower(e);
    }


    protected E standardLower(E e) {
        return Iterators.getNext(headSet(e, false).descendingIterator(), null);
    }

    @Override
    public E floor(E e) {
        return delegate().floor(e);
    }


    protected E standardFloor(E e) {
        return Iterators.getNext(headSet(e, true).descendingIterator(), null);
    }

    @Override
    public E ceiling(E e) {
        return delegate().ceiling(e);
    }


    protected E standardCeiling(E e) {
        return Iterators.getNext(tailSet(e, true).iterator(), null);
    }

    @Override
    public E higher(E e) {
        return delegate().higher(e);
    }


    protected E standardHigher(E e) {
        return Iterators.getNext(tailSet(e, false).iterator(), null);
    }

    @Override
    public E pollFirst() {
        return delegate().pollFirst();
    }


    protected E standardPollFirst() {
        return Iterators.pollNext(iterator());
    }

    @Override
    public E pollLast() {
        return delegate().pollLast();
    }


    protected E standardPollLast() {
        return Iterators.pollNext(descendingIterator());
    }

    protected E standardFirst() {
        return iterator().next();
    }

    protected E standardLast() {
        return descendingIterator().next();
    }

    @NonNull
    @Override
    public NavigableSet<E> descendingSet() {
        return delegate().descendingSet();
    }


    @Beta
    protected class StandardDescendingSet extends Sets.DescendingSet<E> {

        public StandardDescendingSet() {
            super(ForwardingNavigableSet.this);
        }
    }

    @Override
    public Iterator<E> descendingIterator() {
        return delegate().descendingIterator();
    }

    @NonNull
    @Override
    public NavigableSet<E> subSet(
            E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        return delegate().subSet(fromElement, fromInclusive, toElement, toInclusive);
    }


    @Beta
    protected NavigableSet<E> standardSubSet(
            E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        return tailSet(fromElement, fromInclusive).headSet(toElement, toInclusive);
    }


    @Override
    protected SortedSet<E> standardSubSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @NonNull
    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        return delegate().headSet(toElement, inclusive);
    }


    protected SortedSet<E> standardHeadSet(E toElement) {
        return headSet(toElement, false);
    }

    @NonNull
    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return delegate().tailSet(fromElement, inclusive);
    }


    protected SortedSet<E> standardTailSet(E fromElement) {
        return tailSet(fromElement, true);
    }
}
