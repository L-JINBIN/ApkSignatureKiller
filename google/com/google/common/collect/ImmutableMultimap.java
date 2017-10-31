

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.Weak;
import com.google.common.annotations.WeakOuter;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.CollectPreconditions.checkEntryNotNull;


@GwtCompatible(emulated = true)
public abstract class ImmutableMultimap<K, V> extends AbstractMultimap<K, V>
        implements Serializable {


    // looking for of() with > 5 entries? Use the builder instead.


    private static class BuilderMultimap<K, V> extends AbstractMapBasedMultimap<K, V> {
        BuilderMultimap() {
            super(new LinkedHashMap<K, Collection<V>>());
        }

        @Override
        Collection<V> createCollection() {
            return Lists.newArrayList();
        }

        private static final long serialVersionUID = 0;
    }


    public static class Builder<K, V> {
        Multimap<K, V> builderMultimap = new BuilderMultimap<>();
        Comparator<? super K> keyComparator;
        Comparator<? super V> valueComparator;


        public Builder() {
        }


        public Builder<K, V> put(K key, V value) {
            checkEntryNotNull(key, value);
            builderMultimap.put(key, value);
            return this;
        }


        public Builder<K, V> put(Entry<? extends K, ? extends V> entry) {
            return put(entry.getKey(), entry.getValue());
        }


        @Beta
        public Builder<K, V> putAll(Iterable<? extends Entry<? extends K, ? extends V>> entries) {
            for (Entry<? extends K, ? extends V> entry : entries) {
                put(entry);
            }
            return this;
        }


        public Builder<K, V> putAll(K key, Iterable<? extends V> values) {
            if (key == null) {
                throw new NullPointerException("null key in entry: null=" + Iterables.toString(values));
            }
            Collection<V> valueList = builderMultimap.get(key);
            for (V value : values) {
                checkEntryNotNull(key, value);
                valueList.add(value);
            }
            return this;
        }


        public Builder<K, V> putAll(K key, V... values) {
            return putAll(key, Arrays.asList(values));
        }


        public Builder<K, V> putAll(Multimap<? extends K, ? extends V> multimap) {
            for (Entry<? extends K, ? extends Collection<? extends V>> entry :
                    multimap.asMap().entrySet()) {
                putAll(entry.getKey(), entry.getValue());
            }
            return this;
        }


        public Builder<K, V> orderKeysBy(Comparator<? super K> keyComparator) {
            this.keyComparator = checkNotNull(keyComparator);
            return this;
        }


        public Builder<K, V> orderValuesBy(Comparator<? super V> valueComparator) {
            this.valueComparator = checkNotNull(valueComparator);
            return this;
        }


        public ImmutableMultimap<K, V> build() {
            if (valueComparator != null) {
                for (Collection<V> values : builderMultimap.asMap().values()) {
                    List<V> list = (List<V>) values;
                    Collections.sort(list, valueComparator);
                }
            }
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
            return copyOf(builderMultimap);
        }
    }


    public static <K, V> ImmutableMultimap<K, V> copyOf(Multimap<? extends K, ? extends V> multimap) {
        if (multimap instanceof ImmutableMultimap) {
            @SuppressWarnings("unchecked") // safe since multimap is not writable
                    ImmutableMultimap<K, V> kvMultimap = (ImmutableMultimap<K, V>) multimap;
            if (!kvMultimap.isPartialView()) {
                return kvMultimap;
            }
        }
        return ImmutableListMultimap.copyOf(multimap);
    }


    final transient ImmutableMap<K, ? extends ImmutableCollection<V>> map;
    final transient int size;

    // These constants allow the deserialization code to set final fields. This
    // holder class makes sure they are not initialized unless an instance is
    // deserialized.
    @GwtIncompatible("java serialization is not supported")
    static class FieldSettersHolder {
        static final Serialization.FieldSetter<ImmutableMultimap> MAP_FIELD_SETTER =
                Serialization.getFieldSetter(ImmutableMultimap.class, "map");
        static final Serialization.FieldSetter<ImmutableMultimap> SIZE_FIELD_SETTER =
                Serialization.getFieldSetter(ImmutableMultimap.class, "size");
        static final Serialization.FieldSetter<ImmutableSetMultimap> EMPTY_SET_FIELD_SETTER =
                Serialization.getFieldSetter(ImmutableSetMultimap.class, "emptySet");
    }

    ImmutableMultimap(ImmutableMap<K, ? extends ImmutableCollection<V>> map, int size) {
        this.map = map;
        this.size = size;
    }

    // mutators (not supported)


    @Deprecated
    @Override
    public ImmutableCollection<V> removeAll(Object key) {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    @Override
    public ImmutableCollection<V> replaceValues(K key, Iterable<? extends V> values) {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }


    @Override
    public abstract ImmutableCollection<V> get(K key);


    @Deprecated
    @Override
    public boolean put(K key, V value) {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    @Override
    public boolean putAll(K key, Iterable<? extends V> values) {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    @Override
    public boolean putAll(Multimap<? extends K, ? extends V> multimap) {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    @Override
    public boolean remove(Object key, Object value) {
        throw new UnsupportedOperationException();
    }


    boolean isPartialView() {
        return map.isPartialView();
    }

    // accessors

    @Override
    public boolean containsKey(@Nullable Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(@Nullable Object value) {
        return value != null && super.containsValue(value);
    }

    @Override
    public int size() {
        return size;
    }

    // views


    @Override
    public ImmutableSet<K> keySet() {
        return map.keySet();
    }


    @Override
    @SuppressWarnings("unchecked") // a widening cast
    public ImmutableMap<K, Collection<V>> asMap() {
        return (ImmutableMap) map;
    }

    @Override
    Map<K, Collection<V>> createAsMap() {
        throw new AssertionError("should never be called");
    }


    @Override
    public ImmutableCollection<Entry<K, V>> entries() {
        return (ImmutableCollection<Entry<K, V>>) super.entries();
    }

    @Override
    ImmutableCollection<Entry<K, V>> createEntries() {
        return new EntryCollection<>(this);
    }

    private static class EntryCollection<K, V> extends ImmutableCollection<Entry<K, V>> {
        @Weak
        final ImmutableMultimap<K, V> multimap;

        EntryCollection(ImmutableMultimap<K, V> multimap) {
            this.multimap = multimap;
        }

        @Override
        public UnmodifiableIterator<Entry<K, V>> iterator() {
            return multimap.entryIterator();
        }

        @Override
        boolean isPartialView() {
            return multimap.isPartialView();
        }

        @Override
        public int size() {
            return multimap.size();
        }

        @Override
        public boolean contains(Object object) {
            if (object instanceof Entry) {
                Entry<?, ?> entry = (Entry<?, ?>) object;
                return multimap.containsEntry(entry.getKey(), entry.getValue());
            }
            return false;
        }

        private static final long serialVersionUID = 0;
    }

    private abstract class Itr<T> extends UnmodifiableIterator<T> {
        final Iterator<Entry<K, Collection<V>>> mapIterator = asMap().entrySet().iterator();
        K key = null;
        Iterator<V> valueIterator = Iterators.emptyIterator();

        abstract T output(K key, V value);

        @Override
        public boolean hasNext() {
            return mapIterator.hasNext() || valueIterator.hasNext();
        }

        @Override
        public T next() {
            if (!valueIterator.hasNext()) {
                Entry<K, Collection<V>> mapEntry = mapIterator.next();
                key = mapEntry.getKey();
                valueIterator = mapEntry.getValue().iterator();
            }
            return output(key, valueIterator.next());
        }
    }

    @Override
    UnmodifiableIterator<Entry<K, V>> entryIterator() {
        return new Itr<Entry<K, V>>() {
            @Override
            Entry<K, V> output(K key, V value) {
                return Maps.immutableEntry(key, value);
            }
        };
    }


    @Override
    public ImmutableMultiset<K> keys() {
        return (ImmutableMultiset<K>) super.keys();
    }

    @Override
    ImmutableMultiset<K> createKeys() {
        return new Keys();
    }

    @SuppressWarnings("serial") // Uses writeReplace, not default serialization
    @WeakOuter
    class Keys extends ImmutableMultiset<K> {
        @Override
        public boolean contains(@Nullable Object object) {
            return containsKey(object);
        }

        @Override
        public int count(@Nullable Object element) {
            Collection<V> values = map.get(element);
            return (values == null) ? 0 : values.size();
        }

        @Override
        public Set<K> elementSet() {
            return keySet();
        }

        @Override
        public int size() {
            return ImmutableMultimap.this.size();
        }

        @Override
        Multiset.Entry<K> getEntry(int index) {
            Map.Entry<K, ? extends Collection<V>> entry = map.entrySet().asList().get(index);
            return Multisets.immutableEntry(entry.getKey(), entry.getValue().size());
        }

        @Override
        boolean isPartialView() {
            return true;
        }
    }


    @Override
    public ImmutableCollection<V> values() {
        return (ImmutableCollection<V>) super.values();
    }

    @Override
    ImmutableCollection<V> createValues() {
        return new Values<>(this);
    }

    @Override
    UnmodifiableIterator<V> valueIterator() {
        return new Itr<V>() {
            @Override
            V output(K key, V value) {
                return value;
            }
        };
    }

    private static final class Values<K, V> extends ImmutableCollection<V> {
        @Weak
        private final transient ImmutableMultimap<K, V> multimap;

        Values(ImmutableMultimap<K, V> multimap) {
            this.multimap = multimap;
        }

        @Override
        public boolean contains(@Nullable Object object) {
            return multimap.containsValue(object);
        }

        @Override
        public UnmodifiableIterator<V> iterator() {
            return multimap.valueIterator();
        }

        @GwtIncompatible("not present in emulated superclass")
        @Override
        int copyIntoArray(Object[] dst, int offset) {
            for (ImmutableCollection<V> valueCollection : multimap.map.values()) {
                offset = valueCollection.copyIntoArray(dst, offset);
            }
            return offset;
        }

        @Override
        public int size() {
            return multimap.size();
        }

        @Override
        boolean isPartialView() {
            return true;
        }

        private static final long serialVersionUID = 0;
    }

    private static final long serialVersionUID = 0;
}
