

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;


@GwtCompatible
public interface Multiset<E> extends Collection<E> {
    // Query Operations


    int count(@Nullable Object element);

    // Bulk Operations


    int add(@Nullable E element, int occurrences);


    int remove(@Nullable Object element, int occurrences);


    int setCount(E element, int count);


    boolean setCount(E element, int oldCount, int newCount);

    // Views


    Set<E> elementSet();


    Set<Entry<E>> entrySet();


    interface Entry<E> {


        E getElement();


        int getCount();


        @Override
            // TODO(kevinb): check this wrt TreeMultiset?
        boolean equals(Object o);


        @Override
        int hashCode();


        @Override
        String toString();
    }

    // Comparison and hashing


    @Override
        // TODO(kevinb): caveats about equivalence-relation?
    boolean equals(@Nullable Object object);


    @Override
    int hashCode();


    @Override
    String toString();

    // Refined Collection Methods


    @Override
    Iterator<E> iterator();


    @Override
    boolean contains(@Nullable Object element);


    @Override
    boolean containsAll(Collection<?> elements);


    @Override
    boolean add(E element);


    @Override
    boolean remove(@Nullable Object element);


    @Override
    boolean removeAll(Collection<?> c);


    @Override
    boolean retainAll(Collection<?> c);
}
