
package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.Set;


@GwtCompatible(emulated = true)
@SuppressWarnings("unchecked") // allow ungenerified Comparable types
final class EmptyContiguousSet<C extends Comparable> extends ContiguousSet<C> {
    EmptyContiguousSet(DiscreteDomain<C> domain) {
        super(domain);
    }

    @Override
    public C first() {
        throw new NoSuchElementException();
    }

    @Override
    public C last() {
        throw new NoSuchElementException();
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public Range<C> range() {
        throw new NoSuchElementException();
    }

    @Override
    ContiguousSet<C> headSetImpl(C toElement, boolean inclusive) {
        return this;
    }

    @Override
    ContiguousSet<C> subSetImpl(
            C fromElement, boolean fromInclusive, C toElement, boolean toInclusive) {
        return this;
    }

    @Override
    ContiguousSet<C> tailSetImpl(C fromElement, boolean fromInclusive) {
        return this;
    }

    @Override
    public boolean contains(Object object) {
        return false;
    }

    @GwtIncompatible("not used by GWT emulation")
    @Override
    int indexOf(Object target) {
        return -1;
    }

    @Override
    public UnmodifiableIterator<C> iterator() {
        return Iterators.emptyIterator();
    }

    @GwtIncompatible("NavigableSet")
    @Override
    public UnmodifiableIterator<C> descendingIterator() {
        return Iterators.emptyIterator();
    }

    @Override
    boolean isPartialView() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public ImmutableList<C> asList() {
        return ImmutableList.of();
    }

    @Override
    public String toString() {
        return "[]";
    }

    @Override
    public boolean equals(@Nullable Object object) {
        if (object instanceof Set) {
            Set<?> that = (Set<?>) object;
            return that.isEmpty();
        }
        return false;
    }

    @GwtIncompatible("not used in GWT")
    @Override
    boolean isHashCodeFast() {
        return true;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @GwtIncompatible("serialization")
    private static final class SerializedForm<C extends Comparable> implements Serializable {
        private final DiscreteDomain<C> domain;

        private SerializedForm(DiscreteDomain<C> domain) {
            this.domain = domain;
        }

        private Object readResolve() {
            return new EmptyContiguousSet<>(domain);
        }

        private static final long serialVersionUID = 0;
    }

    @GwtIncompatible("serialization")
    @Override
    Object writeReplace() {
        return new SerializedForm<>(domain);
    }

    @GwtIncompatible("NavigableSet")
    ImmutableSortedSet<C> createDescendingSet() {
        return ImmutableSortedSet.emptySet(Ordering.natural().reverse());
    }
}
