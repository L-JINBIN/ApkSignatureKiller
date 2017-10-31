

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;

import java.util.Collection;
import java.util.List;
import java.util.Map;


@GwtCompatible
abstract class AbstractListMultimap<K, V> extends AbstractMapBasedMultimap<K, V>
        implements ListMultimap<K, V> {

    protected AbstractListMultimap(Map<K, Collection<V>> map) {
        super(map);
    }

    @Override
    abstract List<V> createCollection();

    @Override
    List<V> createUnmodifiableEmptyCollection() {
        return ImmutableList.of();
    }

    // Following Javadoc copied from ListMultimap.


    @Override
    public List<V> get(@Nullable K key) {
        return (List<V>) super.get(key);
    }


    @Override
    public List<V> removeAll(@Nullable Object key) {
        return (List<V>) super.removeAll(key);
    }


    @Override
    public List<V> replaceValues(@Nullable K key, Iterable<? extends V> values) {
        return (List<V>) super.replaceValues(key, values);
    }


    @Override
    public boolean put(@Nullable K key, @Nullable V value) {
        return super.put(key, value);
    }


    @Override
    public Map<K, Collection<V>> asMap() {
        return super.asMap();
    }


    @Override
    public boolean equals(@Nullable Object object) {
        return super.equals(object);
    }

    private static final long serialVersionUID = 6588350623831699109L;
}
