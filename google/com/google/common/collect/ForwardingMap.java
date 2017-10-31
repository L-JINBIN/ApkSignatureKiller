

package com.google.common.collect;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


@GwtCompatible
public abstract class ForwardingMap<K, V> extends ForwardingObject implements Map<K, V> {
    // TODO(lowasser): identify places where thread safety is actually lost


    protected ForwardingMap() {
    }

    @Override
    protected abstract Map<K, V> delegate();

    @Override
    public int size() {
        return delegate().size();
    }

    @Override
    public boolean isEmpty() {
        return delegate().isEmpty();
    }

    @Override
    public V remove(Object object) {
        return delegate().remove(object);
    }

    @Override
    public void clear() {
        delegate().clear();
    }

    @Override
    public boolean containsKey(@Nullable Object key) {
        return delegate().containsKey(key);
    }

    @Override
    public boolean containsValue(@Nullable Object value) {
        return delegate().containsValue(value);
    }

    @Override
    public V get(@Nullable Object key) {
        return delegate().get(key);
    }

    @Override
    public V put(K key, V value) {
        return delegate().put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        delegate().putAll(map);
    }

    @NonNull
    @Override
    public Set<K> keySet() {
        return delegate().keySet();
    }

    @NonNull
    @Override
    public Collection<V> values() {
        return delegate().values();
    }

    @NonNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return delegate().entrySet();
    }

    @Override
    public boolean equals(@Nullable Object object) {
        return object == this || delegate().equals(object);
    }

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }


    protected void standardPutAll(Map<? extends K, ? extends V> map) {
        Maps.putAllImpl(this, map);
    }


    @Beta
    protected V standardRemove(@Nullable Object key) {
        Iterator<Entry<K, V>> entryIterator = entrySet().iterator();
        while (entryIterator.hasNext()) {
            Entry<K, V> entry = entryIterator.next();
            if (Objects.equal(entry.getKey(), key)) {
                V value = entry.getValue();
                entryIterator.remove();
                return value;
            }
        }
        return null;
    }


    protected void standardClear() {
        Iterators.clear(entrySet().iterator());
    }


    @Beta
    protected class StandardKeySet extends Maps.KeySet<K, V> {

        public StandardKeySet() {
            super(ForwardingMap.this);
        }
    }


    @Beta
    protected boolean standardContainsKey(@Nullable Object key) {
        return Maps.containsKeyImpl(this, key);
    }


    @Beta
    protected class StandardValues extends Maps.Values<K, V> {

        public StandardValues() {
            super(ForwardingMap.this);
        }
    }


    protected boolean standardContainsValue(@Nullable Object value) {
        return Maps.containsValueImpl(this, value);
    }


    @Beta
    protected abstract class StandardEntrySet extends Maps.EntrySet<K, V> {

        public StandardEntrySet() {
        }

        @Override
        Map<K, V> map() {
            return ForwardingMap.this;
        }
    }


    protected boolean standardIsEmpty() {
        return !entrySet().iterator().hasNext();
    }


    protected boolean standardEquals(@Nullable Object object) {
        return Maps.equalsImpl(this, object);
    }


    protected int standardHashCode() {
        return Sets.hashCodeImpl(entrySet());
    }


    protected String standardToString() {
        return Maps.toStringImpl(this);
    }
}
