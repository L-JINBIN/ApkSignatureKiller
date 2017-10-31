

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.WeakOuter;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.CollectPreconditions.checkEntryNotNull;


@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial") // we're overriding default serialization
public abstract class ImmutableMap<K, V> implements Map<K, V>, Serializable {


    public static <K, V> ImmutableMap<K, V> of() {
        return ImmutableBiMap.of();
    }


    public static <K, V> ImmutableMap<K, V> of(K k1, V v1) {
        return ImmutableBiMap.of(k1, v1);
    }


    // looking for of() with > 5 entries? Use the builder instead.


    static <K, V> ImmutableMapEntry<K, V> entryOf(K key, V value) {
        return new ImmutableMapEntry<>(key, value);
    }


    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    static void checkNoConflict(
            boolean safe, String conflictDescription, Entry<?, ?> entry1, Entry<?, ?> entry2) {
        if (!safe) {
            throw new IllegalArgumentException(
                    "Multiple entries with same " + conflictDescription + ": " + entry1 + " and " + entry2);
        }
    }


    public static class Builder<K, V> {
        Comparator<? super V> valueComparator;
        ImmutableMapEntry<K, V>[] entries;
        int size;
        boolean entriesUsed;


        public Builder() {
            this(ImmutableCollection.Builder.DEFAULT_INITIAL_CAPACITY);
        }

        @SuppressWarnings("unchecked")
        Builder(int initialCapacity) {
            this.entries = new ImmutableMapEntry[initialCapacity];
            this.size = 0;
            this.entriesUsed = false;
        }

        private void ensureCapacity(int minCapacity) {
            if (minCapacity > entries.length) {
                entries =
                        ObjectArrays.arraysCopyOf(
                                entries, ImmutableCollection.Builder.expandedCapacity(entries.length, minCapacity));
                entriesUsed = false;
            }
        }


        public Builder<K, V> put(K key, V value) {
            ensureCapacity(size + 1);
            ImmutableMapEntry<K, V> entry = entryOf(key, value);
            // don't inline this: we want to fail atomically if key or value is null
            entries[size++] = entry;
            return this;
        }


        public Builder<K, V> put(Entry<? extends K, ? extends V> entry) {
            return put(entry.getKey(), entry.getValue());
        }


        public Builder<K, V> putAll(Map<? extends K, ? extends V> map) {
            ensureCapacity(size + map.size());
            return putAll(map.entrySet());
        }


        @Beta
        public Builder<K, V> putAll(Iterable<? extends Entry<? extends K, ? extends V>> entries) {
            for (Entry<? extends K, ? extends V> entry : entries) {
                put(entry);
            }
            return this;
        }


        @Beta
        public Builder<K, V> orderEntriesByValue(Comparator<? super V> valueComparator) {
            checkState(this.valueComparator == null, "valueComparator was already set");
            this.valueComparator = checkNotNull(valueComparator, "valueComparator");
            return this;
        }


        public ImmutableMap<K, V> build() {
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
                    return RegularImmutableMap.fromEntryArray(size, entries);
            }
        }
    }


    public static <K, V> ImmutableMap<K, V> copyOf(Map<? extends K, ? extends V> map) {
        if ((map instanceof ImmutableMap) && !(map instanceof ImmutableSortedMap)) {
            // TODO(lowasser): Make ImmutableMap.copyOf(immutableBiMap) call copyOf()
            // on the ImmutableMap delegate(), rather than the bimap itself

            @SuppressWarnings("unchecked") // safe since map is not writable
                    ImmutableMap<K, V> kvMap = (ImmutableMap<K, V>) map;
            if (!kvMap.isPartialView()) {
                return kvMap;
            }
        } else if (map instanceof EnumMap) {
            @SuppressWarnings("unchecked") // safe since map is not writable
                    ImmutableMap<K, V> kvMap = (ImmutableMap<K, V>) copyOfEnumMap((EnumMap<?, ?>) map);
            return kvMap;
        }
        return copyOf(map.entrySet());
    }


    @Beta
    public static <K, V> ImmutableMap<K, V> copyOf(
            Iterable<? extends Entry<? extends K, ? extends V>> entries) {
        @SuppressWarnings("unchecked") // we'll only be using getKey and getValue, which are covariant
                Entry<K, V>[] entryArray = (Entry<K, V>[]) Iterables.toArray(entries, EMPTY_ENTRY_ARRAY);
        switch (entryArray.length) {
            case 0:
                return of();
            case 1:
                Entry<K, V> onlyEntry = entryArray[0];
                return of(onlyEntry.getKey(), onlyEntry.getValue());
            default:

                return RegularImmutableMap.fromEntries(entryArray);
        }
    }

    private static <K extends Enum<K>, V> ImmutableMap<K, V> copyOfEnumMap(
            EnumMap<K, ? extends V> original) {
        EnumMap<K, V> copy = new EnumMap<>(original);
        for (Map.Entry<?, ?> entry : copy.entrySet()) {
            checkEntryNotNull(entry.getKey(), entry.getValue());
        }
        return ImmutableEnumMap.asImmutable(copy);
    }

    static final Entry<?, ?>[] EMPTY_ENTRY_ARRAY = new Entry<?, ?>[0];

    abstract static class IteratorBasedImmutableMap<K, V> extends ImmutableMap<K, V> {
        abstract UnmodifiableIterator<Entry<K, V>> entryIterator();

        @Override
        ImmutableSet<Entry<K, V>> createEntrySet() {
            @WeakOuter
            class EntrySetImpl extends ImmutableMapEntrySet<K, V> {
                @Override
                ImmutableMap<K, V> map() {
                    return IteratorBasedImmutableMap.this;
                }

                @Override
                public UnmodifiableIterator<Entry<K, V>> iterator() {
                    return entryIterator();
                }
            }
            return new EntrySetImpl();
        }
    }

    ImmutableMap() {
    }


    @Deprecated
    @Override
    public final V put(K k, V v) {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    @Override
    public final V remove(Object o) {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    @Override
    public final void putAll(Map<? extends K, ? extends V> map) {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    @Override
    public final void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(@Nullable Object key) {
        return get(key) != null;
    }

    @Override
    public boolean containsValue(@Nullable Object value) {
        return values().contains(value);
    }

    // Overriding to mark it Nullable
    @Override
    public abstract V get(@Nullable Object key);

    private transient ImmutableSet<Entry<K, V>> entrySet;


    @Override
    public ImmutableSet<Entry<K, V>> entrySet() {
        ImmutableSet<Entry<K, V>> result = entrySet;
        return (result == null) ? entrySet = createEntrySet() : result;
    }

    abstract ImmutableSet<Entry<K, V>> createEntrySet();

    private transient ImmutableSet<K> keySet;


    @Override
    public ImmutableSet<K> keySet() {
        ImmutableSet<K> result = keySet;
        return (result == null) ? keySet = createKeySet() : result;
    }

    ImmutableSet<K> createKeySet() {
        return isEmpty() ? ImmutableSet.<K>of() : new ImmutableMapKeySet<>(this);
    }

    UnmodifiableIterator<K> keyIterator() {
        final UnmodifiableIterator<Entry<K, V>> entryIterator = entrySet().iterator();
        return new UnmodifiableIterator<K>() {
            @Override
            public boolean hasNext() {
                return entryIterator.hasNext();
            }

            @Override
            public K next() {
                return entryIterator.next().getKey();
            }
        };
    }

    private transient ImmutableCollection<V> values;


    @Override
    public ImmutableCollection<V> values() {
        ImmutableCollection<V> result = values;
        return (result == null) ? values = new ImmutableMapValues<>(this) : result;
    }

    // cached so that this.multimapView().inverse() only computes inverse once
    private transient ImmutableSetMultimap<K, V> multimapView;


    @WeakOuter
    private final class MapViewOfValuesAsSingletonSets
            extends IteratorBasedImmutableMap<K, ImmutableSet<V>> {

        @Override
        public int size() {
            return ImmutableMap.this.size();
        }

        @Override
        public ImmutableSet<K> keySet() {
            return ImmutableMap.this.keySet();
        }

        @Override
        public boolean containsKey(@Nullable Object key) {
            return ImmutableMap.this.containsKey(key);
        }

        @Override
        public ImmutableSet<V> get(@Nullable Object key) {
            V outerValue = ImmutableMap.this.get(key);
            return (outerValue == null) ? null : ImmutableSet.of(outerValue);
        }

        @Override
        boolean isPartialView() {
            return ImmutableMap.this.isPartialView();
        }

        @Override
        public int hashCode() {
            // ImmutableSet.of(value).hashCode() == value.hashCode(), so the hashes are the same
            return ImmutableMap.this.hashCode();
        }

        @Override
        boolean isHashCodeFast() {
            return ImmutableMap.this.isHashCodeFast();
        }

        @Override
        UnmodifiableIterator<Entry<K, ImmutableSet<V>>> entryIterator() {
            final Iterator<Entry<K, V>> backingIterator = ImmutableMap.this.entrySet().iterator();
            return new UnmodifiableIterator<Entry<K, ImmutableSet<V>>>() {
                @Override
                public boolean hasNext() {
                    return backingIterator.hasNext();
                }

                @Override
                public Entry<K, ImmutableSet<V>> next() {
                    final Entry<K, V> backingEntry = backingIterator.next();
                    return new AbstractMapEntry<K, ImmutableSet<V>>() {
                        @Override
                        public K getKey() {
                            return backingEntry.getKey();
                        }

                        @Override
                        public ImmutableSet<V> getValue() {
                            return ImmutableSet.of(backingEntry.getValue());
                        }
                    };
                }
            };
        }
    }

    @Override
    public boolean equals(@Nullable Object object) {
        return Maps.equalsImpl(this, object);
    }

    abstract boolean isPartialView();

    @Override
    public int hashCode() {
        return Sets.hashCodeImpl(entrySet());
    }

    boolean isHashCodeFast() {
        return false;
    }

    @Override
    public String toString() {
        return Maps.toStringImpl(this);
    }


    static class SerializedForm implements Serializable {
        private final Object[] keys;
        private final Object[] values;

        SerializedForm(ImmutableMap<?, ?> map) {
            keys = new Object[map.size()];
            values = new Object[map.size()];
            int i = 0;
            for (Entry<?, ?> entry : map.entrySet()) {
                keys[i] = entry.getKey();
                values[i] = entry.getValue();
                i++;
            }
        }

        Object readResolve() {
            Builder<Object, Object> builder = new Builder<>(keys.length);
            return createMap(builder);
        }

        Object createMap(Builder<Object, Object> builder) {
            for (int i = 0; i < keys.length; i++) {
                builder.put(keys[i], values[i]);
            }
            return builder.build();
        }

        private static final long serialVersionUID = 0;
    }

    Object writeReplace() {
        return new SerializedForm(this);
    }
}
