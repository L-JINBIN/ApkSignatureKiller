

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;

import java.util.Map;
import java.util.Map.Entry;


@GwtCompatible
public abstract class ForwardingMapEntry<K, V> extends ForwardingObject implements Map.Entry<K, V> {
    // TODO(lowasser): identify places where thread safety is actually lost


    protected ForwardingMapEntry() {
    }

    @Override
    protected abstract Map.Entry<K, V> delegate();

    @Override
    public K getKey() {
        return delegate().getKey();
    }

    @Override
    public V getValue() {
        return delegate().getValue();
    }

    @Override
    public V setValue(V value) {
        return delegate().setValue(value);
    }

    @Override
    public boolean equals(@Nullable Object object) {
        return delegate().equals(object);
    }

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }


    protected boolean standardEquals(@Nullable Object object) {
        if (object instanceof Entry) {
            Entry<?, ?> that = (Entry<?, ?>) object;
            return Objects.equal(this.getKey(), that.getKey())
                    && Objects.equal(this.getValue(), that.getValue());
        }
        return false;
    }


    protected int standardHashCode() {
        K k = getKey();
        V v = getValue();
        return ((k == null) ? 0 : k.hashCode()) ^ ((v == null) ? 0 : v.hashCode());
    }


    @Beta
    protected String standardToString() {
        return getKey() + "=" + getValue();
    }
}
