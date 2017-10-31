

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.Beta;

import java.util.Map;


@Beta
public interface RangeMap<K extends Comparable, V> {

    @Nullable
    V get(K key);


    @Nullable
    Map.Entry<Range<K>, V> getEntry(K key);


    Range<K> span();


    void put(Range<K> range, V value);


    void putAll(RangeMap<K, V> rangeMap);


    void clear();


    void remove(Range<K> range);


    Map<Range<K>, V> asMapOfRanges();


    Map<Range<K>, V> asDescendingMapOfRanges();


    RangeMap<K, V> subRangeMap(Range<K> range);


    @Override
    boolean equals(@Nullable Object o);


    @Override
    int hashCode();


    @Override
    String toString();
}
