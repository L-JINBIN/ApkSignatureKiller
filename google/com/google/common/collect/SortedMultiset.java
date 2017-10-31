

package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Set;


@Beta
@GwtCompatible(emulated = true)
public interface SortedMultiset<E> extends SortedMultisetBridge<E>, SortedIterable<E> {

    Comparator<? super E> comparator();


    Entry<E> firstEntry();


    Entry<E> lastEntry();


    Entry<E> pollFirstEntry();


    Entry<E> pollLastEntry();


    @Override
    NavigableSet<E> elementSet();


    @Override
    Set<Entry<E>> entrySet();


    @Override
    Iterator<E> iterator();


    SortedMultiset<E> descendingMultiset();


    SortedMultiset<E> headMultiset(E upperBound, BoundType boundType);


    SortedMultiset<E> subMultiset(E lowerBound, BoundType lowerBoundType,
                                  E upperBound, BoundType upperBoundType);


    SortedMultiset<E> tailMultiset(E lowerBound, BoundType boundType);
}
