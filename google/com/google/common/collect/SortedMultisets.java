

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.Weak;
import com.google.common.collect.Multiset.Entry;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.SortedSet;

import static com.google.common.collect.BoundType.CLOSED;
import static com.google.common.collect.BoundType.OPEN;


@GwtCompatible(emulated = true)
final class SortedMultisets {
    private SortedMultisets() {
    }


    static class ElementSet<E> extends Multisets.ElementSet<E> implements
            SortedSet<E> {
        @Weak
        private final SortedMultiset<E> multiset;

        ElementSet(SortedMultiset<E> multiset) {
            this.multiset = multiset;
        }

        @Override
        final SortedMultiset<E> multiset() {
            return multiset;
        }

        @Override
        public Comparator<? super E> comparator() {
            return multiset().comparator();
        }

        @Override
        public SortedSet<E> subSet(E fromElement, E toElement) {
            return multiset().subMultiset(fromElement, CLOSED, toElement, OPEN).elementSet();
        }

        @Override
        public SortedSet<E> headSet(E toElement) {
            return multiset().headMultiset(toElement, OPEN).elementSet();
        }

        @Override
        public SortedSet<E> tailSet(E fromElement) {
            return multiset().tailMultiset(fromElement, CLOSED).elementSet();
        }

        @Override
        public E first() {
            return getElementOrThrow(multiset().firstEntry());
        }

        @Override
        public E last() {
            return getElementOrThrow(multiset().lastEntry());
        }
    }


    @GwtIncompatible("Navigable")
    static class NavigableElementSet<E> extends ElementSet<E> implements NavigableSet<E> {
        NavigableElementSet(SortedMultiset<E> multiset) {
            super(multiset);
        }

        @Override
        public E lower(E e) {
            return getElementOrNull(multiset().headMultiset(e, OPEN).lastEntry());
        }

        @Override
        public E floor(E e) {
            return getElementOrNull(multiset().headMultiset(e, CLOSED).lastEntry());
        }

        @Override
        public E ceiling(E e) {
            return getElementOrNull(multiset().tailMultiset(e, CLOSED).firstEntry());
        }

        @Override
        public E higher(E e) {
            return getElementOrNull(multiset().tailMultiset(e, OPEN).firstEntry());
        }

        @Override
        public NavigableSet<E> descendingSet() {
            return new NavigableElementSet<>(multiset().descendingMultiset());
        }

        @Override
        public Iterator<E> descendingIterator() {
            return descendingSet().iterator();
        }

        @Override
        public E pollFirst() {
            return getElementOrNull(multiset().pollFirstEntry());
        }

        @Override
        public E pollLast() {
            return getElementOrNull(multiset().pollLastEntry());
        }

        @Override
        public NavigableSet<E> subSet(
                E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
            return new NavigableElementSet<>(multiset().subMultiset(
                    fromElement, BoundType.forBoolean(fromInclusive),
                    toElement, BoundType.forBoolean(toInclusive)));
        }

        @Override
        public NavigableSet<E> headSet(E toElement, boolean inclusive) {
            return new NavigableElementSet<>(
                    multiset().headMultiset(toElement, BoundType.forBoolean(inclusive)));
        }

        @Override
        public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
            return new NavigableElementSet<>(
                    multiset().tailMultiset(fromElement, BoundType.forBoolean(inclusive)));
        }
    }

    private static <E> E getElementOrThrow(Entry<E> entry) {
        if (entry == null) {
            throw new NoSuchElementException();
        }
        return entry.getElement();
    }

    private static <E> E getElementOrNull(@Nullable Entry<E> entry) {
        return (entry == null) ? null : entry.getElement();
    }
}
