

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;

import java.util.concurrent.ConcurrentMap;


@GwtCompatible
public abstract class ForwardingConcurrentMap<K, V> extends ForwardingMap<K, V>
        implements ConcurrentMap<K, V> {


    protected ForwardingConcurrentMap() {
    }

    @Override
    protected abstract ConcurrentMap<K, V> delegate();

    @Override
    public V putIfAbsent(K key, V value) {
        return delegate().putIfAbsent(key, value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return delegate().remove(key, value);
    }

    @Override
    public V replace(K key, V value) {
        return delegate().replace(key, value);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return delegate().replace(key, oldValue, newValue);
    }
}
