

package com.google.common.collect;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;

import java.util.Collection;
import java.util.Iterator;


@GwtCompatible
public abstract class ForwardingCollection<E> extends ForwardingObject implements Collection<E> {
    // TODO(lowasser): identify places where thread safety is actually lost


    protected ForwardingCollection() {
    }

    @Override
    protected abstract Collection<E> delegate();

    @NonNull
    @Override
    public Iterator<E> iterator() {
        return delegate().iterator();
    }

    @Override
    public int size() {
        return delegate().size();
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        return delegate().removeAll(collection);
    }

    @Override
    public boolean isEmpty() {
        return delegate().isEmpty();
    }

    @Override
    public boolean contains(Object object) {
        return delegate().contains(object);
    }

    @Override
    public boolean add(E element) {
        return delegate().add(element);
    }

    @Override
    public boolean remove(Object object) {
        return delegate().remove(object);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return delegate().containsAll(collection);
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        return delegate().addAll(collection);
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        return delegate().retainAll(collection);
    }

    @Override
    public void clear() {
        delegate().clear();
    }

    @Override
    public Object[] toArray() {
        return delegate().toArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
        return delegate().toArray(array);
    }


    protected boolean standardContains(@Nullable Object object) {
        return Iterators.contains(iterator(), object);
    }


    protected boolean standardContainsAll(Collection<?> collection) {
        return Collections2.containsAllImpl(this, collection);
    }


    protected boolean standardAddAll(Collection<? extends E> collection) {
        return Iterators.addAll(this, collection.iterator());
    }


    protected boolean standardRemove(@Nullable Object object) {
        Iterator<E> iterator = iterator();
        while (iterator.hasNext()) {
            if (Objects.equal(iterator.next(), object)) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }


    protected boolean standardRemoveAll(Collection<?> collection) {
        return Iterators.removeAll(iterator(), collection);
    }


    protected boolean standardRetainAll(Collection<?> collection) {
        return Iterators.retainAll(iterator(), collection);
    }


    protected void standardClear() {
        Iterators.clear(iterator());
    }


    protected boolean standardIsEmpty() {
        return !iterator().hasNext();
    }


    protected String standardToString() {
        return Collections2.toStringImpl(this);
    }


    protected Object[] standardToArray() {
        Object[] newArray = new Object[size()];
        return toArray(newArray);
    }


    protected <T> T[] standardToArray(T[] array) {
        return ObjectArrays.toArrayImpl(this, array);
    }
}
