

package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;


@GwtCompatible(serializable = true, emulated = true)
public abstract class ImmutableBiMap<K, V> extends ImmutableMap<K, V> implements BiMap<K, V> {


    // Casting to any type is safe because the set will never hold any elements.
    @SuppressWarnings("unchecked")
    public static <K, V> ImmutableBiMap<K, V> of() {
        return (ImmutableBiMap<K, V>) RegularImmutableBiMap.EMPTY;
    }


    public static <K, V> ImmutableBiMap<K, V> of(K k1, V v1) {
        return new SingletonImmutableBiMap<>(k1, v1);
    }


    public static <K, V> ImmutableBiMap<K, V> of(K k1, V v1, K k2, V v2) {
        return RegularImmutableBiMap.fromEntries(entryOf(k1, v1), entryOf(k2, v2));
    }


    public static <K, V> ImmutableBiMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
        return RegularImmutableBiMap.fromEntries(entryOf(k1, v1), entryOf(k2, v2), entryOf(k3, v3));
    }


    public static <K, V> ImmutableBiMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        return RegularImmutableBiMap.fromEntries(
                entryOf(k1, v1), entryOf(k2, v2), entryOf(k3, v3), entryOf(k4, v4));
    }


    public static <K, V> ImmutableBiMap<K, V> of(
            K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
        return RegularImmutableBiMap.fromEntries(
                entryOf(k1, v1), entryOf(k2, v2), entryOf(k3, v3), entryOf(k4, v4), entryOf(k5, v5));
    }

    // looking for of() with > 5 entries? Use the builder instead.


    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }


    public static final class Builder<K, V> extends ImmutableMap.Builder<K, V> {


        public Builder() {
        }

        Builder(int size) {
            super(size);
        }


        @Override
        public Builder<K, V> put(K key, V value) {
            super.put(key, value);
            return this;
        }


        @Override
        public Builder<K, V> put(Entry<? extends K, ? extends V> entry) {
            super.put(entry);
            return this;
        }


        @Override
        public Builder<K, V> putAll(Map<? extends K, ? extends V> map) {
            super.putAll(map);
            return this;
        }


        @Beta
        @Override
        public Builder<K, V> putAll(Iterable<? extends Entry<? extends K, ? extends V>> entries) {
            super.putAll(entries);
            return this;
        }


        @Beta
        @Override
        public Builder<K, V> orderEntriesByValue(Comparator<? super V> valueComparator) {
            super.orderEntriesByValue(valueComparator);
            return this;
        }


        @Override
        public ImmutableBiMap<K, V> build() {
            switch (size) {
                case 0:
                    return of();
                case 1:
                    return of(entries[0].getKey(), entries[0].getValue());
                default:

                    if (valueComparator != null) {
                        if (entriesUsed) {
                            entries = ObjectArrays.arraysCopyOf(entries, size);
                        }
                        Arrays.sort(entries, 0, size,
                                Ordering.from(valueComparator).onResultOf(Maps.<V>valueFunction()));
                    }
                    entriesUsed = size == entries.length;
                    return RegularImmutableBiMap.fromEntryArray(size, entries);
            }
        }
    }


    public static <K, V> ImmutableBiMap<K, V> copyOf(Map<? extends K, ? extends V> map) {
        if (map instanceof ImmutableBiMap) {
            @SuppressWarnings("unchecked") // safe since map is not writable
                    ImmutableBiMap<K, V> bimap = (ImmutableBiMap<K, V>) map;
            // TODO(lowasser): if we need to make a copy of a BiMap because the
            // forward map is a view, don't make a copy of the non-view delegate map
            if (!bimap.isPartialView()) {
                return bimap;
            }
        }
        return copyOf(map.entrySet());
    }


    @Beta
    public static <K, V> ImmutableBiMap<K, V> copyOf(
            Iterable<? extends Entry<? extends K, ? extends V>> entries) {
        @SuppressWarnings("unchecked") // we'll only be using getKey and getValue, which are covariant
                Entry<K, V>[] entryArray = (Entry<K, V>[]) Iterables.toArray(entries, EMPTY_ENTRY_ARRAY);
        switch (entryArray.length) {
            case 0:
                return of();
            case 1:
                Entry<K, V> entry = entryArray[0];
                return of(entry.getKey(), entry.getValue());
            default:

                return RegularImmutableBiMap.fromEntries(entryArray);
        }
    }

    ImmutableBiMap() {
    }


    @Override
    public abstract ImmutableBiMap<V, K> inverse();


    @Override
    public ImmutableSet<V> values() {
        return inverse().keySet();
    }


    @Deprecated
    @Override
    public V forcePut(K key, V value) {
        throw new UnsupportedOperationException();
    }


    private static class SerializedForm extends ImmutableMap.SerializedForm {
        SerializedForm(ImmutableBiMap<?, ?> bimap) {
            super(bimap);
        }

        @Override
        Object readResolve() {
            Builder<Object, Object> builder = new Builder<>();
            return createMap(builder);
        }

        private static final long serialVersionUID = 0;
    }

    @Override
    Object writeReplace() {
        return new SerializedForm(this);
    }
}
