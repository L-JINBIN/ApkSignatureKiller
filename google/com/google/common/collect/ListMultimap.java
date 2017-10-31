

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;

import java.util.Collection;
import java.util.List;
import java.util.Map;


@GwtCompatible
public interface ListMultimap<K, V> extends Multimap<K, V> {

    @Override
    List<V> get(@Nullable K key);


    @Override
    List<V> removeAll(@Nullable Object key);


    @Override
    List<V> replaceValues(K key, Iterable<? extends V> values);


    @Override
    Map<K, Collection<V>> asMap();


    @Override
    boolean equals(@Nullable Object obj);
}
