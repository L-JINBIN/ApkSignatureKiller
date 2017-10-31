

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.Beta;

import java.util.Set;


@Beta
public interface RangeSet<C extends Comparable> {

    // Query methods


    boolean contains(C value);


    Range<C> rangeContaining(C value);


    boolean encloses(Range<C> otherRange);


    boolean enclosesAll(RangeSet<C> other);


    boolean isEmpty();


    Range<C> span();

    // Views


    Set<Range<C>> asRanges();


    Set<Range<C>> asDescendingSetOfRanges();


    RangeSet<C> complement();


    RangeSet<C> subRangeSet(Range<C> view);

    // Modification


    void add(Range<C> range);


    void remove(Range<C> range);


    void clear();


    void addAll(RangeSet<C> other);


    void removeAll(RangeSet<C> other);

    // Object methods


    @Override
    boolean equals(@Nullable Object obj);


    @Override
    int hashCode();


    @Override
    String toString();
}
