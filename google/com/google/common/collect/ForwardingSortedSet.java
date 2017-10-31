

package com.google.common.collect;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;


@GwtCompatible
public abstract class ForwardingSortedSet<E> extends ForwardingSet<E> implements SortedSet<E> {


    protected ForwardingSortedSet() {
    }

    @Override
    protected abstract SortedSet<E> delegate();

    @Override
    public Comparator<? super E> comparator() {
        return delegate().comparator();
    }

    @Override
    public E first() {
        return delegate().first();
    }

    @NonNull
    @Override
    public SortedSet<E> headSet(E toElement) {
        return delegate().headSet(toElement);
    }

    @Override
    public E last() {
        return delegate().last();
    }

    @NonNull
    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return delegate().subSet(fromElement, toElement);
    }

    @NonNull
    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return delegate().tailSet(fromElement);
    }

    // unsafe, but worst case is a CCE is thrown, which callers will be expecting
    @SuppressWarnings("unchecked")
    private int unsafeCompare(Object o1, Object o2) {
        Comparator<? super E> comparator = comparator();
        return (comparator == null)
                ? ((Comparable<Object>) o1).compareTo(o2)
                : ((Comparator<Object>) comparator).compare(o1, o2);
    }


    @Override
    @Beta
    protected boolean standardContains(@Nullable Object object) {
        try {
            // any ClassCastExceptions are caught
            @SuppressWarnings("unchecked")
            SortedSet<Object> self = (SortedSet<Object>) this;
            Object ceiling = self.tailSet(object).first();
            return unsafeCompare(ceiling, object) == 0;
        } catch (ClassCastException e) {
            return false;
        } catch (NoSuchElementException e) {
            return false;
        } catch (NullPointerException e) {
            return false;
        }
    }


    @Override
    @Beta
    protected boolean standardRemove(@Nullable Object object) {
        try {
            // any ClassCastExceptions are caught
            @SuppressWarnings("unchecked")
            SortedSet<Object> self = (SortedSet<Object>) this;
            Iterator<Object> iterator = self.tailSet(object).iterator();
            if (iterator.hasNext()) {
                Object ceiling = iterator.next();
                if (unsafeCompare(ceiling, object) == 0) {
                    iterator.remove();
                    return true;
                }
            }
        } catch (ClassCastException e) {
            return false;
        } catch (NullPointerException e) {
            return false;
        }
        return false;
    }


    @Beta
    protected SortedSet<E> standardSubSet(E fromElement, E toElement) {
        return tailSet(fromElement).headSet(toElement);
    }
}
