

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.WeakOuter;
import com.google.common.base.Objects;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import static com.google.common.collect.Multisets.setCountImpl;


@GwtCompatible
abstract class AbstractMultiset<E> extends AbstractCollection<E> implements Multiset<E> {
    // Query Operations

    @Override
    public int size() {
        return Multisets.sizeImpl(this);
    }

    @Override
    public boolean isEmpty() {
        return entrySet().isEmpty();
    }

    @Override
    public boolean contains(@Nullable Object element) {
        return count(element) > 0;
    }

    @Override
    public Iterator<E> iterator() {
        return Multisets.iteratorImpl(this);
    }

    @Override
    public int count(@Nullable Object element) {
        for (Entry<E> entry : entrySet()) {
            if (Objects.equal(entry.getElement(), element)) {
                return entry.getCount();
            }
        }
        return 0;
    }

    // Modification Operations

    @Override
    public boolean add(@Nullable E element) {
        add(element, 1);
        return true;
    }

    @Override
    public int add(@Nullable E element, int occurrences) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(@Nullable Object element) {
        return remove(element, 1) > 0;
    }

    @Override
    public int remove(@Nullable Object element, int occurrences) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int setCount(@Nullable E element, int count) {
        return setCountImpl(this, element, count);
    }

    @Override
    public boolean setCount(@Nullable E element, int oldCount, int newCount) {
        return setCountImpl(this, element, oldCount, newCount);
    }

    // Bulk Operations


    @Override
    public boolean addAll(Collection<? extends E> elementsToAdd) {
        return Multisets.addAllImpl(this, elementsToAdd);
    }

    @Override
    public boolean removeAll(Collection<?> elementsToRemove) {
        return Multisets.removeAllImpl(this, elementsToRemove);
    }

    @Override
    public boolean retainAll(Collection<?> elementsToRetain) {
        return Multisets.retainAllImpl(this, elementsToRetain);
    }

    @Override
    public void clear() {
        Iterators.clear(entryIterator());
    }

    // Views

    private transient Set<E> elementSet;

    @Override
    public Set<E> elementSet() {
        Set<E> result = elementSet;
        if (result == null) {
            elementSet = result = createElementSet();
        }
        return result;
    }


    Set<E> createElementSet() {
        return new ElementSet();
    }

    @WeakOuter
    class ElementSet extends Multisets.ElementSet<E> {
        @Override
        Multiset<E> multiset() {
            return AbstractMultiset.this;
        }
    }

    abstract Iterator<Entry<E>> entryIterator();

    abstract int distinctElements();

    private transient Set<Entry<E>> entrySet;

    @Override
    public Set<Entry<E>> entrySet() {
        Set<Entry<E>> result = entrySet;
        if (result == null) {
            entrySet = result = createEntrySet();
        }
        return result;
    }

    @WeakOuter
    class EntrySet extends Multisets.EntrySet<E> {
        @Override
        Multiset<E> multiset() {
            return AbstractMultiset.this;
        }

        @Override
        public Iterator<Entry<E>> iterator() {
            return entryIterator();
        }

        @Override
        public int size() {
            return distinctElements();
        }
    }

    Set<Entry<E>> createEntrySet() {
        return new EntrySet();
    }

    // Object methods


    @Override
    public boolean equals(@Nullable Object object) {
        return Multisets.equalsImpl(this, object);
    }


    @Override
    public int hashCode() {
        return entrySet().hashCode();
    }


    @Override
    public String toString() {
        return entrySet().toString();
    }
}
