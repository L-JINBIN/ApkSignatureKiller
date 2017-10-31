

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;

import java.util.Collection;
import java.util.Map;
import java.util.Set;


@GwtCompatible
public interface SetMultimap<K, V> extends Multimap<K, V> {

    @Override
    Set<V> get(@Nullable K key);


    @Override
    Set<V> removeAll(@Nullable Object key);


    @Override
    Set<V> replaceValues(K key, Iterable<? extends V> values);


    @Override
    Set<Map.Entry<K, V>> entries();


    @Override
    Map<K, Collection<V>> asMap();


    @Override
    boolean equals(@Nullable Object obj);
}
