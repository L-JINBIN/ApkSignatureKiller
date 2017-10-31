

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map.Entry;


@GwtCompatible(serializable = true, emulated = true)
public class ImmutableListMultimap<K, V> extends ImmutableMultimap<K, V>
        implements ListMultimap<K, V> {


    // Casting is safe because the multimap will never hold any elements.
    @SuppressWarnings("unchecked")
    public static <K, V> ImmutableListMultimap<K, V> of() {
        return (ImmutableListMultimap<K, V>) EmptyImmutableListMultimap.INSTANCE;
    }


    public static <K, V> ImmutableListMultimap<K, V> of(K k1, V v1) {
        ImmutableListMultimap.Builder<K, V> builder = ImmutableListMultimap.builder();
        builder.put(k1, v1);
        return builder.build();
    }


    public static <K, V> ImmutableListMultimap<K, V> of(K k1, V v1, K k2, V v2) {
        ImmutableListMultimap.Builder<K, V> builder = ImmutableListMultimap.builder();
        builder.put(k1, v1);
        builder.put(k2, v2);
        return builder.build();
    }


    public static <K, V> ImmutableListMultimap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
        ImmutableListMultimap.Builder<K, V> builder = ImmutableListMultimap.builder();
        builder.put(k1, v1);
        builder.put(k2, v2);
        builder.put(k3, v3);
        return builder.build();
    }


    public static <K, V> ImmutableListMultimap<K, V> of(
            K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        ImmutableListMultimap.Builder<K, V> builder = ImmutableListMultimap.builder();
        builder.put(k1, v1);
        builder.put(k2, v2);
        builder.put(k3, v3);
        builder.put(k4, v4);
        return builder.build();
    }


    public static <K, V> ImmutableListMultimap<K, V> of(
            K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
        ImmutableListMultimap.Builder<K, V> builder = ImmutableListMultimap.builder();
        builder.put(k1, v1);
        builder.put(k2, v2);
        builder.put(k3, v3);
        builder.put(k4, v4);
        builder.put(k5, v5);
        return builder.build();
    }

    // looking for of() with > 5 entries? Use the builder instead.


    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }


    public static final class Builder<K, V> extends ImmutableMultimap.Builder<K, V> {

        public Builder() {
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


        @Beta
        @Override
        public Builder<K, V> putAll(Iterable<? extends Entry<? extends K, ? extends V>> entries) {
            super.putAll(entries);
            return this;
        }

        @Override
        public Builder<K, V> putAll(K key, Iterable<? extends V> values) {
            super.putAll(key, values);
            return this;
        }

        @Override
        public Builder<K, V> putAll(K key, V... values) {
            super.putAll(key, values);
            return this;
        }

        @Override
        public Builder<K, V> putAll(Multimap<? extends K, ? extends V> multimap) {
            super.putAll(multimap);
            return this;
        }


        @Override
        public Builder<K, V> orderKeysBy(Comparator<? super K> keyComparator) {
            super.orderKeysBy(keyComparator);
            return this;
        }


        @Override
        public Builder<K, V> orderValuesBy(Comparator<? super V> valueComparator) {
            super.orderValuesBy(valueComparator);
            return this;
        }


        @Override
        public ImmutableListMultimap<K, V> build() {
            return (ImmutableListMultimap<K, V>) super.build();
        }
    }


    public static <K, V> ImmutableListMultimap<K, V> copyOf(
            Multimap<? extends K, ? extends V> multimap) {
        if (multimap.isEmpty()) {
            return of();
        }

        // TODO(lowasser): copy ImmutableSetMultimap by using asList() on the sets
        if (multimap instanceof ImmutableListMultimap) {
            @SuppressWarnings("unchecked") // safe since multimap is not writable
                    ImmutableListMultimap<K, V> kvMultimap = (ImmutableListMultimap<K, V>) multimap;
            if (!kvMultimap.isPartialView()) {
                return kvMultimap;
            }
        }

        ImmutableMap.Builder<K, ImmutableList<V>> builder =
                new ImmutableMap.Builder<>(multimap.asMap().size());
        int size = 0;

        for (Entry<? extends K, ? extends Collection<? extends V>> entry :
                multimap.asMap().entrySet()) {
            ImmutableList<V> list = ImmutableList.copyOf(entry.getValue());
            if (!list.isEmpty()) {
                builder.put(entry.getKey(), list);
                size += list.size();
            }
        }

        return new ImmutableListMultimap<>(builder.build(), size);
    }


    @Beta
    public static <K, V> ImmutableListMultimap<K, V> copyOf(
            Iterable<? extends Entry<? extends K, ? extends V>> entries) {
        return new Builder<K, V>().putAll(entries).build();
    }

    ImmutableListMultimap(ImmutableMap<K, ImmutableList<V>> map, int size) {
        super(map, size);
    }

    // views


    @Override
    public ImmutableList<V> get(@Nullable K key) {
        // This cast is safe as its type is known in constructor.
        ImmutableList<V> list = (ImmutableList<V>) map.get(key);
        return (list == null) ? ImmutableList.<V>of() : list;
    }

    private transient ImmutableListMultimap<V, K> inverse;


    private ImmutableListMultimap<V, K> invert() {
        Builder<V, K> builder = builder();
        for (Entry<K, V> entry : entries()) {
            builder.put(entry.getValue(), entry.getKey());
        }
        ImmutableListMultimap<V, K> invertedMultimap = builder.build();
        invertedMultimap.inverse = this;
        return invertedMultimap;
    }


    @Deprecated
    @Override
    public ImmutableList<V> removeAll(Object key) {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    @Override
    public ImmutableList<V> replaceValues(K key, Iterable<? extends V> values) {
        throw new UnsupportedOperationException();
    }


    @GwtIncompatible("java.io.ObjectOutputStream")
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        Serialization.writeMultimap(this, stream);
    }

    @GwtIncompatible("java.io.ObjectInputStream")
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        int keyCount = stream.readInt();
        if (keyCount < 0) {
            throw new InvalidObjectException("Invalid key count " + keyCount);
        }
        ImmutableMap.Builder<Object, ImmutableList<Object>> builder = ImmutableMap.builder();
        int tmpSize = 0;

        for (int i = 0; i < keyCount; i++) {
            Object key = stream.readObject();
            int valueCount = stream.readInt();
            if (valueCount <= 0) {
                throw new InvalidObjectException("Invalid value count " + valueCount);
            }

            ImmutableList.Builder<Object> valuesBuilder = ImmutableList.builder();
            for (int j = 0; j < valueCount; j++) {
                valuesBuilder.add(stream.readObject());
            }
            builder.put(key, valuesBuilder.build());
            tmpSize += valueCount;
        }

        ImmutableMap<Object, ImmutableList<Object>> tmpMap;
        try {
            tmpMap = builder.build();
        } catch (IllegalArgumentException e) {
            throw (InvalidObjectException) new InvalidObjectException(e.getMessage()).initCause(e);
        }

        FieldSettersHolder.MAP_FIELD_SETTER.set(this, tmpMap);
        FieldSettersHolder.SIZE_FIELD_SETTER.set(this, tmpSize);
    }

    @GwtIncompatible("Not needed in emulated source")
    private static final long serialVersionUID = 0;
}
