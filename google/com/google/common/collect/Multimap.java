

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;

import java.util.Collection;
import java.util.Map;
import java.util.Set;


@GwtCompatible
public interface Multimap<K, V> {
    // Query Operations


    int size();


    boolean isEmpty();


    boolean containsKey(@Nullable Object key);


    boolean containsValue(@Nullable Object value);


    boolean containsEntry(@Nullable Object key, @Nullable Object value);

    // Modification Operations


    boolean put(@Nullable K key, @Nullable V value);


    boolean remove(@Nullable Object key, @Nullable Object value);

    // Bulk Operations


    boolean putAll(@Nullable K key, Iterable<? extends V> values);


    boolean putAll(Multimap<? extends K, ? extends V> multimap);


    Collection<V> replaceValues(@Nullable K key, Iterable<? extends V> values);


    Collection<V> removeAll(@Nullable Object key);


    void clear();

    // Views


    Collection<V> get(@Nullable K key);


    Set<K> keySet();


    Multiset<K> keys();


    Collection<V> values();


    Collection<Map.Entry<K, V>> entries();


    Map<K, Collection<V>> asMap();

    // Comparison and hashing


    @Override
    boolean equals(@Nullable Object obj);


    @Override
    int hashCode();
}
