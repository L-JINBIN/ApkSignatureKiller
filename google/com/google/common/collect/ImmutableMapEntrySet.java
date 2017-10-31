

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.Weak;

import java.io.Serializable;
import java.util.Map.Entry;


@GwtCompatible(emulated = true)
abstract class ImmutableMapEntrySet<K, V> extends ImmutableSet<Entry<K, V>> {
    static final class RegularEntrySet<K, V> extends ImmutableMapEntrySet<K, V> {
        @Weak
        private final transient ImmutableMap<K, V> map;
        private final transient Entry<K, V>[] entries;

        RegularEntrySet(ImmutableMap<K, V> map, Entry<K, V>[] entries) {
            this.map = map;
            this.entries = entries;
        }

        @Override
        ImmutableMap<K, V> map() {
            return map;
        }

        @Override
        public UnmodifiableIterator<Entry<K, V>> iterator() {
            return asList().iterator();
        }

        @Override
        ImmutableList<Entry<K, V>> createAsList() {
            return new RegularImmutableAsList<>(this, entries);
        }
    }

    ImmutableMapEntrySet() {
    }

    abstract ImmutableMap<K, V> map();

    @Override
    public int size() {
        return map().size();
    }

    @Override
    public boolean contains(@Nullable Object object) {
        if (object instanceof Entry) {
            Entry<?, ?> entry = (Entry<?, ?>) object;
            V value = map().get(entry.getKey());
            return value != null && value.equals(entry.getValue());
        }
        return false;
    }

    @Override
    boolean isPartialView() {
        return map().isPartialView();
    }

    @Override
    @GwtIncompatible("not used in GWT")
    boolean isHashCodeFast() {
        return map().isHashCodeFast();
    }

    @Override
    public int hashCode() {
        return map().hashCode();
    }

    @GwtIncompatible("serialization")
    @Override
    Object writeReplace() {
        return new EntrySetSerializedForm<>(map());
    }

    @GwtIncompatible("serialization")
    private static class EntrySetSerializedForm<K, V> implements Serializable {
        final ImmutableMap<K, V> map;

        EntrySetSerializedForm(ImmutableMap<K, V> map) {
            this.map = map;
        }

        Object readResolve() {
            return map.entrySet();
        }

        private static final long serialVersionUID = 0;
    }
}
