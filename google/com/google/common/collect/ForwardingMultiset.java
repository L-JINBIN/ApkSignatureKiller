

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;


@GwtCompatible
public abstract class ForwardingMultiset<E> extends ForwardingCollection<E> implements Multiset<E> {


    protected ForwardingMultiset() {
    }

    @Override
    protected abstract Multiset<E> delegate();

    @Override
    public int count(Object element) {
        return delegate().count(element);
    }

    @Override
    public int add(E element, int occurrences) {
        return delegate().add(element, occurrences);
    }

    @Override
    public int remove(Object element, int occurrences) {
        return delegate().remove(element, occurrences);
    }

    @Override
    public Set<E> elementSet() {
        return delegate().elementSet();
    }

    @Override
    public Set<Entry<E>> entrySet() {
        return delegate().entrySet();
    }

    @Override
    public boolean equals(@Nullable Object object) {
        return object == this || delegate().equals(object);
    }

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }

    @Override
    public int setCount(E element, int count) {
        return delegate().setCount(element, count);
    }

    @Override
    public boolean setCount(E element, int oldCount, int newCount) {
        return delegate().setCount(element, oldCount, newCount);
    }


    @Override
    protected boolean standardContains(@Nullable Object object) {
        return count(object) > 0;
    }


    @Override
    protected void standardClear() {
        Iterators.clear(entrySet().iterator());
    }


    @Beta
    protected int standardCount(@Nullable Object object) {
        for (Entry<?> entry : this.entrySet()) {
            if (Objects.equal(entry.getElement(), object)) {
                return entry.getCount();
            }
        }
        return 0;
    }


    protected boolean standardAdd(E element) {
        add(element, 1);
        return true;
    }


    @Beta
    @Override
    protected boolean standardAddAll(Collection<? extends E> elementsToAdd) {
        return Multisets.addAllImpl(this, elementsToAdd);
    }


    @Override
    protected boolean standardRemove(Object element) {
        return remove(element, 1) > 0;
    }


    @Override
    protected boolean standardRemoveAll(Collection<?> elementsToRemove) {
        return Multisets.removeAllImpl(this, elementsToRemove);
    }


    @Override
    protected boolean standardRetainAll(Collection<?> elementsToRetain) {
        return Multisets.retainAllImpl(this, elementsToRetain);
    }


    protected int standardSetCount(E element, int count) {
        return Multisets.setCountImpl(this, element, count);
    }


    protected boolean standardSetCount(E element, int oldCount, int newCount) {
        return Multisets.setCountImpl(this, element, oldCount, newCount);
    }


    @Beta
    protected class StandardElementSet extends Multisets.ElementSet<E> {

        public StandardElementSet() {
        }

        @Override
        Multiset<E> multiset() {
            return ForwardingMultiset.this;
        }
    }


    protected Iterator<E> standardIterator() {
        return Multisets.iteratorImpl(this);
    }


    protected int standardSize() {
        return Multisets.sizeImpl(this);
    }


    protected boolean standardEquals(@Nullable Object object) {
        return Multisets.equalsImpl(this, object);
    }


    protected int standardHashCode() {
        return entrySet().hashCode();
    }


    @Override
    protected String standardToString() {
        return entrySet().toString();
    }
}
