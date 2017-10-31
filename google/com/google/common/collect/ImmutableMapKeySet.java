

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.Weak;

import java.io.Serializable;


@GwtCompatible(emulated = true)
final class ImmutableMapKeySet<K, V> extends ImmutableSet.Indexed<K> {
    @Weak
    private final ImmutableMap<K, V> map;

    ImmutableMapKeySet(ImmutableMap<K, V> map) {
        this.map = map;
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public UnmodifiableIterator<K> iterator() {
        return map.keyIterator();
    }

    @Override
    public boolean contains(@Nullable Object object) {
        return map.containsKey(object);
    }

    @Override
    K get(int index) {
        return map.entrySet().asList().get(index).getKey();
    }

    @Override
    boolean isPartialView() {
        return true;
    }

    @GwtIncompatible("serialization")
    @Override
    Object writeReplace() {
        return new KeySetSerializedForm<>(map);
    }

    @GwtIncompatible("serialization")
    private static class KeySetSerializedForm<K> implements Serializable {
        final ImmutableMap<K, ?> map;

        KeySetSerializedForm(ImmutableMap<K, ?> map) {
            this.map = map;
        }

        Object readResolve() {
            return map.keySet();
        }

        private static final long serialVersionUID = 0;
    }
}
