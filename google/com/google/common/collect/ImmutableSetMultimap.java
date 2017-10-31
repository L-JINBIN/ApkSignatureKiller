

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.Weak;
import com.google.common.base.MoreObjects;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static com.google.common.base.Preconditions.checkNotNull;


@GwtCompatible(serializable = true, emulated = true)
public class ImmutableSetMultimap<K, V> extends ImmutableMultimap<K, V>
        implements SetMultimap<K, V> {


    // Casting is safe because the multimap will never hold any elements.
    @SuppressWarnings("unchecked")
    public static <K, V> ImmutableSetMultimap<K, V> of() {
        return (ImmutableSetMultimap<K, V>) EmptyImmutableSetMultimap.INSTANCE;
    }


    // looking for of() with > 5 entries? Use the builder instead.


    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }


    private static class BuilderMultimap<K, V> extends AbstractMapBasedMultimap<K, V> {
        BuilderMultimap() {
            super(new LinkedHashMap<K, Collection<V>>());
        }

        @Override
        Collection<V> createCollection() {
            return Sets.newLinkedHashSet();
        }

        private static final long serialVersionUID = 0;
    }


    public static final class Builder<K, V> extends ImmutableMultimap.Builder<K, V> {

        public Builder() {
            builderMultimap = new BuilderMultimap<>();
        }


        @Override
        public Builder<K, V> put(K key, V value) {
            builderMultimap.put(checkNotNull(key), checkNotNull(value));
            return this;
        }


        @Override
        public Builder<K, V> put(Entry<? extends K, ? extends V> entry) {
            builderMultimap.put(entry.getKey(), entry.getValue());
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
            Collection<V> collection = builderMultimap.get(checkNotNull(key));
            for (V value : values) {
                collection.add(checkNotNull(value));
            }
            return this;
        }

        @Override
        public Builder<K, V> putAll(K key, V... values) {
            return putAll(key, Arrays.asList(values));
        }

        @Override
        public Builder<K, V> putAll(Multimap<? extends K, ? extends V> multimap) {
            for (Entry<? extends K, ? extends Collection<? extends V>> entry :
                    multimap.asMap().entrySet()) {
                putAll(entry.getKey(), entry.getValue());
            }
            return this;
        }


        @Override
        public Builder<K, V> orderKeysBy(Comparator<? super K> keyComparator) {
            this.keyComparator = checkNotNull(keyComparator);
            return this;
        }


        // TODO: Make serialization behavior consistent.
        @Override
        public Builder<K, V> orderValuesBy(Comparator<? super V> valueComparator) {
            super.orderValuesBy(valueComparator);
            return this;
        }


        @Override
        public ImmutableSetMultimap<K, V> build() {
            if (keyComparator != null) {
                Multimap<K, V> sortedCopy = new BuilderMultimap<>();
                List<Map.Entry<K, Collection<V>>> entries =
                        Ordering.from(keyComparator)
                                .<K>onKeys()
                                .immutableSortedCopy(builderMultimap.asMap().entrySet());
                for (Map.Entry<K, Collection<V>> entry : entries) {
                    sortedCopy.putAll(entry.getKey(), entry.getValue());
                }
                builderMultimap = sortedCopy;
            }
            return copyOf(builderMultimap, valueComparator);
        }
    }


    private static <K, V> ImmutableSetMultimap<K, V> copyOf(
            Multimap<? extends K, ? extends V> multimap, Comparator<? super V> valueComparator) {
        checkNotNull(multimap); // eager for GWT
        if (multimap.isEmpty() && valueComparator == null) {
            return of();
        }

        if (multimap instanceof ImmutableSetMultimap) {
            @SuppressWarnings("unchecked") // safe since multimap is not writable
                    ImmutableSetMultimap<K, V> kvMultimap = (ImmutableSetMultimap<K, V>) multimap;
            if (!kvMultimap.isPartialView()) {
                return kvMultimap;
            }
        }

        ImmutableMap.Builder<K, ImmutableSet<V>> builder =
                new ImmutableMap.Builder<>(multimap.asMap().size());
        int size = 0;

        for (Entry<? extends K, ? extends Collection<? extends V>> entry :
                multimap.asMap().entrySet()) {
            K key = entry.getKey();
            Collection<? extends V> values = entry.getValue();
            ImmutableSet<V> set = valueSet(valueComparator, values);
            if (!set.isEmpty()) {
                builder.put(key, set);
                size += set.size();
            }
        }

        return new ImmutableSetMultimap<>(builder.build(), size, valueComparator);
    }


    private final transient ImmutableSet<V> emptySet;

    ImmutableSetMultimap(
            ImmutableMap<K, ImmutableSet<V>> map,
            int size,
            @Nullable Comparator<? super V> valueComparator) {
        super(map, size);
        this.emptySet = emptySet(valueComparator);
    }

    // views


    @Override
    public ImmutableSet<V> get(@Nullable K key) {
        // This cast is safe as its type is known in constructor.
        ImmutableSet<V> set = (ImmutableSet<V>) map.get(key);
        return MoreObjects.firstNonNull(set, emptySet);
    }

    private transient ImmutableSetMultimap<V, K> inverse;


    private ImmutableSetMultimap<V, K> invert() {
        Builder<V, K> builder = builder();
        for (Entry<K, V> entry : entries()) {
            builder.put(entry.getValue(), entry.getKey());
        }
        ImmutableSetMultimap<V, K> invertedMultimap = builder.build();
        invertedMultimap.inverse = this;
        return invertedMultimap;
    }


    @Deprecated
    @Override
    public ImmutableSet<V> removeAll(Object key) {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    @Override
    public ImmutableSet<V> replaceValues(K key, Iterable<? extends V> values) {
        throw new UnsupportedOperationException();
    }

    private transient ImmutableSet<Entry<K, V>> entries;


    @Override
    public ImmutableSet<Entry<K, V>> entries() {
        ImmutableSet<Entry<K, V>> result = entries;
        return result == null
                ? (entries = new EntrySet<>(this))
                : result;
    }

    private static final class EntrySet<K, V> extends ImmutableSet<Entry<K, V>> {
        @Weak
        private final transient ImmutableSetMultimap<K, V> multimap;

        EntrySet(ImmutableSetMultimap<K, V> multimap) {
            this.multimap = multimap;
        }

        @Override
        public boolean contains(@Nullable Object object) {
            if (object instanceof Entry) {
                Entry<?, ?> entry = (Entry<?, ?>) object;
                return multimap.containsEntry(entry.getKey(), entry.getValue());
            }
            return false;
        }

        @Override
        public int size() {
            return multimap.size();
        }

        @Override
        public UnmodifiableIterator<Entry<K, V>> iterator() {
            return multimap.entryIterator();
        }

        @Override
        boolean isPartialView() {
            return false;
        }
    }

    private static <V> ImmutableSet<V> valueSet(
            @Nullable Comparator<? super V> valueComparator, Collection<? extends V> values) {
        return (valueComparator == null)
                ? ImmutableSet.copyOf(values)
                : ImmutableSortedSet.copyOf(valueComparator, values);
    }

    private static <V> ImmutableSet<V> emptySet(@Nullable Comparator<? super V> valueComparator) {
        return (valueComparator == null)
                ? ImmutableSet.<V>of()
                : ImmutableSortedSet.emptySet(valueComparator);
    }

    private static <V> ImmutableSet.Builder<V> valuesBuilder(
            @Nullable Comparator<? super V> valueComparator) {
        return (valueComparator == null)
                ? new ImmutableSet.Builder<V>()
                : new ImmutableSortedSet.Builder<>(valueComparator);
    }


    @GwtIncompatible("java.io.ObjectOutputStream")
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeObject(valueComparator());
        Serialization.writeMultimap(this, stream);
    }

    @Nullable
    Comparator<? super V> valueComparator() {
        return emptySet instanceof ImmutableSortedSet
                ? ((ImmutableSortedSet<V>) emptySet).comparator()
                : null;
    }

    @GwtIncompatible("java.io.ObjectInputStream")
    // Serialization type safety is at the caller's mercy.
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        Comparator<Object> valueComparator = (Comparator<Object>) stream.readObject();
        int keyCount = stream.readInt();
        if (keyCount < 0) {
            throw new InvalidObjectException("Invalid key count " + keyCount);
        }
        ImmutableMap.Builder<Object, ImmutableSet<Object>> builder = ImmutableMap.builder();
        int tmpSize = 0;

        for (int i = 0; i < keyCount; i++) {
            Object key = stream.readObject();
            int valueCount = stream.readInt();
            if (valueCount <= 0) {
                throw new InvalidObjectException("Invalid value count " + valueCount);
            }

            ImmutableSet.Builder<Object> valuesBuilder = valuesBuilder(valueComparator);
            for (int j = 0; j < valueCount; j++) {
                valuesBuilder.add(stream.readObject());
            }
            ImmutableSet<Object> valueSet = valuesBuilder.build();
            if (valueSet.size() != valueCount) {
                throw new InvalidObjectException("Duplicate key-value pairs exist for key " + key);
            }
            builder.put(key, valueSet);
            tmpSize += valueCount;
        }

        ImmutableMap<Object, ImmutableSet<Object>> tmpMap;
        try {
            tmpMap = builder.build();
        } catch (IllegalArgumentException e) {
            throw (InvalidObjectException) new InvalidObjectException(e.getMessage()).initCause(e);
        }

        FieldSettersHolder.MAP_FIELD_SETTER.set(this, tmpMap);
        FieldSettersHolder.SIZE_FIELD_SETTER.set(this, tmpSize);
        FieldSettersHolder.EMPTY_SET_FIELD_SETTER.set(this, emptySet(valueComparator));
    }

    @GwtIncompatible("not needed in emulated source.")
    private static final long serialVersionUID = 0;
}
