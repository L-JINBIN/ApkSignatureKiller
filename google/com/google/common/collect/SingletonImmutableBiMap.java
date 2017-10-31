

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;

import static com.google.common.collect.CollectPreconditions.checkEntryNotNull;


@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial") // uses writeReplace(), not default serialization
final class SingletonImmutableBiMap<K, V> extends ImmutableBiMap<K, V> {

    final transient K singleKey;
    final transient V singleValue;

    SingletonImmutableBiMap(K singleKey, V singleValue) {
        checkEntryNotNull(singleKey, singleValue);
        this.singleKey = singleKey;
        this.singleValue = singleValue;
    }

    private SingletonImmutableBiMap(K singleKey, V singleValue,
                                    ImmutableBiMap<V, K> inverse) {
        this.singleKey = singleKey;
        this.singleValue = singleValue;
        this.inverse = inverse;
    }

    @Override
    public V get(@Nullable Object key) {
        return singleKey.equals(key) ? singleValue : null;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean containsKey(@Nullable Object key) {
        return singleKey.equals(key);
    }

    @Override
    public boolean containsValue(@Nullable Object value) {
        return singleValue.equals(value);
    }

    @Override
    boolean isPartialView() {
        return false;
    }

    @Override
    ImmutableSet<Entry<K, V>> createEntrySet() {
        return ImmutableSet.of(Maps.immutableEntry(singleKey, singleValue));
    }

    @Override
    ImmutableSet<K> createKeySet() {
        return ImmutableSet.of(singleKey);
    }

    transient ImmutableBiMap<V, K> inverse;

    @Override
    public ImmutableBiMap<V, K> inverse() {
        // racy single-check idiom
        ImmutableBiMap<V, K> result = inverse;
        if (result == null) {
            return inverse = new SingletonImmutableBiMap<>(
                    singleValue, singleKey, this);
        } else {
            return result;
        }
    }
}
