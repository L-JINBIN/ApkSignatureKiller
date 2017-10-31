

package com.google.common.collect;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.Weak;
import com.google.common.annotations.WeakOuter;
import com.google.common.base.Function;
import com.google.common.base.Joiner.MapJoiner;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.primitives.Ints;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.compose;
import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.CollectPreconditions.checkNonnegative;


@GwtCompatible(emulated = true)
public final class Maps {
    private Maps() {
    }

    private enum EntryFunction implements Function<Entry<?, ?>, Object> {
        KEY {
            @Override
            @Nullable
            public Object apply(Entry<?, ?> entry) {
                return entry.getKey();
            }
        },
        VALUE {
            @Override
            @Nullable
            public Object apply(Entry<?, ?> entry) {
                return entry.getValue();
            }
        };
    }

    @SuppressWarnings("unchecked")
    static <K> Function<Entry<K, ?>, K> keyFunction() {
        return (Function) EntryFunction.KEY;
    }

    @SuppressWarnings("unchecked")
    static <V> Function<Entry<?, V>, V> valueFunction() {
        return (Function) EntryFunction.VALUE;
    }

    static <K, V> Iterator<K> keyIterator(Iterator<Entry<K, V>> entryIterator) {
        return Iterators.transform(entryIterator, Maps.<K>keyFunction());
    }

    static <K, V> Iterator<V> valueIterator(Iterator<Entry<K, V>> entryIterator) {
        return Iterators.transform(entryIterator, Maps.<V>valueFunction());
    }


    public static <K, V> HashMap<K, V> newHashMap() {
        return new HashMap<>();
    }


    public static <K, V> HashMap<K, V> newHashMapWithExpectedSize(
            int expectedSize) {
        return new HashMap<>(capacity(expectedSize));
    }


    static int capacity(int expectedSize) {
        if (expectedSize < 3) {
            checkNonnegative(expectedSize, "expectedSize");
            return expectedSize + 1;
        }
        if (expectedSize < Ints.MAX_POWER_OF_TWO) {
            // This is the calculation used in JDK8 to resize when a putAll
            // happens; it seems to be the most conservative calculation we
            // can make.  0.75 is the default load factor.
            return (int) ((float) expectedSize / 0.75F + 1.0F);
        }
        return Integer.MAX_VALUE; // any large value
    }


    public static <K, V> LinkedHashMap<K, V> newLinkedHashMap() {
        return new LinkedHashMap<>();
    }


    public static <K, V> LinkedHashMap<K, V> newLinkedHashMapWithExpectedSize(
            int expectedSize) {
        return new LinkedHashMap<>(capacity(expectedSize));
    }


    public static <K, V> ConcurrentMap<K, V> newConcurrentMap() {
        return new MapMaker().makeMap();
    }


    public static <K extends Comparable, V> TreeMap<K, V> newTreeMap() {
        return new TreeMap<>();
    }


    public static <K, V> IdentityHashMap<K, V> newIdentityHashMap() {
        return new IdentityHashMap<>();
    }


    public static <K, V> SortedMap<K, V> asMap(
            SortedSet<K> set, Function<? super K, V> function) {
        return Platform.mapsAsMapSortedSet(set, function);
    }

    static <K, V> SortedMap<K, V> asMapSortedIgnoreNavigable(SortedSet<K> set,
                                                             Function<? super K, V> function) {
        return new SortedAsMapView<>(set, function);
    }


    @GwtIncompatible("NavigableMap")
    public static <K, V> NavigableMap<K, V> asMap(
            NavigableSet<K> set, Function<? super K, V> function) {
        return new NavigableAsMapView<>(set, function);
    }

    private static class AsMapView<K, V> extends ViewCachingAbstractMap<K, V> {

        private final Set<K> set;
        final Function<? super K, V> function;

        Set<K> backingSet() {
            return set;
        }

        AsMapView(Set<K> set, Function<? super K, V> function) {
            this.set = checkNotNull(set);
            this.function = checkNotNull(function);
        }

        @Override
        public Set<K> createKeySet() {
            return removeOnlySet(backingSet());
        }

        @Override
        Collection<V> createValues() {
            return Collections2.transform(set, function);
        }

        @Override
        public int size() {
            return backingSet().size();
        }

        @Override
        public boolean containsKey(@Nullable Object key) {
            return backingSet().contains(key);
        }

        @Override
        public V get(@Nullable Object key) {
            if (Collections2.safeContains(backingSet(), key)) {
                @SuppressWarnings("unchecked") // unsafe, but Javadoc warns about it
                        K k = (K) key;
                return function.apply(k);
            } else {
                return null;
            }
        }

        @Override
        public V remove(@Nullable Object key) {
            if (backingSet().remove(key)) {
                @SuppressWarnings("unchecked") // unsafe, but Javadoc warns about it
                        K k = (K) key;
                return function.apply(k);
            } else {
                return null;
            }
        }

        @Override
        public void clear() {
            backingSet().clear();
        }

        @Override
        protected Set<Entry<K, V>> createEntrySet() {
            @WeakOuter
            class EntrySetImpl extends EntrySet<K, V> {
                @Override
                Map<K, V> map() {
                    return AsMapView.this;
                }

                @NonNull
                @Override
                public Iterator<Entry<K, V>> iterator() {
                    return asMapEntryIterator(backingSet(), function);
                }
            }
            return new EntrySetImpl();
        }
    }

    static <K, V> Iterator<Entry<K, V>> asMapEntryIterator(
            Set<K> set, final Function<? super K, V> function) {
        return new TransformedIterator<K, Entry<K, V>>(set.iterator()) {
            @Override
            Entry<K, V> transform(final K key) {
                return immutableEntry(key, function.apply(key));
            }
        };
    }

    private static class SortedAsMapView<K, V> extends AsMapView<K, V>
            implements SortedMap<K, V> {

        SortedAsMapView(SortedSet<K> set, Function<? super K, V> function) {
            super(set, function);
        }

        @Override
        SortedSet<K> backingSet() {
            return (SortedSet<K>) super.backingSet();
        }

        @Override
        public Comparator<? super K> comparator() {
            return backingSet().comparator();
        }

        @NonNull
        @Override
        public Set<K> keySet() {
            return removeOnlySortedSet(backingSet());
        }

        @NonNull
        @Override
        public SortedMap<K, V> subMap(K fromKey, K toKey) {
            return asMap(backingSet().subSet(fromKey, toKey), function);
        }

        @NonNull
        @Override
        public SortedMap<K, V> headMap(K toKey) {
            return asMap(backingSet().headSet(toKey), function);
        }

        @NonNull
        @Override
        public SortedMap<K, V> tailMap(K fromKey) {
            return asMap(backingSet().tailSet(fromKey), function);
        }

        @Override
        public K firstKey() {
            return backingSet().first();
        }

        @Override
        public K lastKey() {
            return backingSet().last();
        }
    }

    @GwtIncompatible("NavigableMap")
    private static final class NavigableAsMapView<K, V>
            extends AbstractNavigableMap<K, V> {


        private final NavigableSet<K> set;
        private final Function<? super K, V> function;

        NavigableAsMapView(NavigableSet<K> ks, Function<? super K, V> vFunction) {
            this.set = checkNotNull(ks);
            this.function = checkNotNull(vFunction);
        }

        @Override
        public NavigableMap<K, V> subMap(
                K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
            return asMap(set.subSet(fromKey, fromInclusive, toKey, toInclusive), function);
        }

        @Override
        public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
            return asMap(set.headSet(toKey, inclusive), function);
        }

        @Override
        public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
            return asMap(set.tailSet(fromKey, inclusive), function);
        }

        @Override
        public Comparator<? super K> comparator() {
            return set.comparator();
        }

        @Override
        @Nullable
        public V get(@Nullable Object key) {
            if (Collections2.safeContains(set, key)) {
                @SuppressWarnings("unchecked") // unsafe, but Javadoc warns about it
                        K k = (K) key;
                return function.apply(k);
            } else {
                return null;
            }
        }

        @Override
        public void clear() {
            set.clear();
        }

        @Override
        Iterator<Entry<K, V>> entryIterator() {
            return asMapEntryIterator(set, function);
        }

        @Override
        Iterator<Entry<K, V>> descendingEntryIterator() {
            return descendingMap().entrySet().iterator();
        }

        @Override
        public NavigableSet<K> navigableKeySet() {
            return removeOnlyNavigableSet(set);
        }

        @Override
        public int size() {
            return set.size();
        }

        @Override
        public NavigableMap<K, V> descendingMap() {
            return asMap(set.descendingSet(), function);
        }
    }

    private static <E> Set<E> removeOnlySet(final Set<E> set) {
        return new ForwardingSet<E>() {
            @Override
            protected Set<E> delegate() {
                return set;
            }

            @Override
            public boolean add(E element) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean addAll(Collection<? extends E> es) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static <E> SortedSet<E> removeOnlySortedSet(final SortedSet<E> set) {
        return new ForwardingSortedSet<E>() {
            @Override
            protected SortedSet<E> delegate() {
                return set;
            }

            @Override
            public boolean add(E element) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean addAll(Collection<? extends E> es) {
                throw new UnsupportedOperationException();
            }

            @NonNull
            @Override
            public SortedSet<E> headSet(E toElement) {
                return removeOnlySortedSet(super.headSet(toElement));
            }

            @NonNull
            @Override
            public SortedSet<E> subSet(E fromElement, E toElement) {
                return removeOnlySortedSet(super.subSet(fromElement, toElement));
            }

            @NonNull
            @Override
            public SortedSet<E> tailSet(E fromElement) {
                return removeOnlySortedSet(super.tailSet(fromElement));
            }
        };
    }

    @GwtIncompatible("NavigableSet")
    private static <E> NavigableSet<E> removeOnlyNavigableSet(final NavigableSet<E> set) {
        return new ForwardingNavigableSet<E>() {
            @Override
            protected NavigableSet<E> delegate() {
                return set;
            }

            @Override
            public boolean add(E element) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean addAll(Collection<? extends E> es) {
                throw new UnsupportedOperationException();
            }

            @NonNull
            @Override
            public SortedSet<E> headSet(E toElement) {
                return removeOnlySortedSet(super.headSet(toElement));
            }

            @NonNull
            @Override
            public SortedSet<E> subSet(E fromElement, E toElement) {
                return removeOnlySortedSet(
                        super.subSet(fromElement, toElement));
            }

            @NonNull
            @Override
            public SortedSet<E> tailSet(E fromElement) {
                return removeOnlySortedSet(super.tailSet(fromElement));
            }

            @NonNull
            @Override
            public NavigableSet<E> headSet(E toElement, boolean inclusive) {
                return removeOnlyNavigableSet(super.headSet(toElement, inclusive));
            }

            @NonNull
            @Override
            public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
                return removeOnlyNavigableSet(super.tailSet(fromElement, inclusive));
            }

            @NonNull
            @Override
            public NavigableSet<E> subSet(E fromElement, boolean fromInclusive,
                                          E toElement, boolean toInclusive) {
                return removeOnlyNavigableSet(super.subSet(
                        fromElement, fromInclusive, toElement, toInclusive));
            }

            @NonNull
            @Override
            public NavigableSet<E> descendingSet() {
                return removeOnlyNavigableSet(super.descendingSet());
            }
        };
    }


    public static <K, V> ImmutableMap<K, V> toMap(Iterable<K> keys,
                                                  Function<? super K, V> valueFunction) {
        return toMap(keys.iterator(), valueFunction);
    }


    public static <K, V> ImmutableMap<K, V> toMap(Iterator<K> keys,
                                                  Function<? super K, V> valueFunction) {
        checkNotNull(valueFunction);
        // Using LHM instead of a builder so as not to fail on duplicate keys
        Map<K, V> builder = newLinkedHashMap();
        while (keys.hasNext()) {
            K key = keys.next();
            builder.put(key, valueFunction.apply(key));
        }
        return ImmutableMap.copyOf(builder);
    }


    public static <K, V> ImmutableMap<K, V> uniqueIndex(
            Iterable<V> values, Function<? super V, K> keyFunction) {
        // TODO(lowasser): consider presizing the builder if values is a Collection
        return uniqueIndex(values.iterator(), keyFunction);
    }


    public static <K, V> ImmutableMap<K, V> uniqueIndex(
            Iterator<V> values, Function<? super V, K> keyFunction) {
        checkNotNull(keyFunction);
        ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
        while (values.hasNext()) {
            V value = values.next();
            builder.put(keyFunction.apply(value), value);
        }
        try {
            return builder.build();
        } catch (IllegalArgumentException duplicateKeys) {
            throw new IllegalArgumentException(duplicateKeys.getMessage()
                    + ". To index multiple values under a key, use Multimaps.index.");
        }
    }


    @GwtCompatible(serializable = true)
    public static <K, V> Entry<K, V> immutableEntry(
            @Nullable K key, @Nullable V value) {
        return new ImmutableEntry<>(key, value);
    }


    static <K, V> Entry<K, V> unmodifiableEntry(final Entry<? extends K, ? extends V> entry) {
        checkNotNull(entry);
        return new AbstractMapEntry<K, V>() {
            @Override
            public K getKey() {
                return entry.getKey();
            }

            @Override
            public V getValue() {
                return entry.getValue();
            }
        };
    }

    static <K, V> UnmodifiableIterator<Entry<K, V>> unmodifiableEntryIterator(
            final Iterator<Entry<K, V>> entryIterator) {
        return new UnmodifiableIterator<Entry<K, V>>() {
            @Override
            public boolean hasNext() {
                return entryIterator.hasNext();
            }

            @Override
            public Entry<K, V> next() {
                return unmodifiableEntry(entryIterator.next());
            }
        };
    }


    public static <K, V1, V2> Map<K, V2> transformValues(
            Map<K, V1> fromMap, Function<? super V1, V2> function) {
        return transformEntries(fromMap, asEntryTransformer(function));
    }


    public static <K, V1, V2> SortedMap<K, V2> transformValues(
            SortedMap<K, V1> fromMap, Function<? super V1, V2> function) {
        return transformEntries(fromMap, asEntryTransformer(function));
    }


    public static <K, V1, V2> Map<K, V2> transformEntries(
            Map<K, V1> fromMap,
            EntryTransformer<? super K, ? super V1, V2> transformer) {
        if (fromMap instanceof SortedMap) {
            return transformEntries((SortedMap<K, V1>) fromMap, transformer);
        }
        return new TransformedEntriesMap<>(fromMap, transformer);
    }


    public static <K, V1, V2> SortedMap<K, V2> transformEntries(
            SortedMap<K, V1> fromMap,
            EntryTransformer<? super K, ? super V1, V2> transformer) {
        return Platform.mapsTransformEntriesSortedMap(fromMap, transformer);
    }


    @GwtIncompatible("NavigableMap")
    public static <K, V1, V2> NavigableMap<K, V2> transformEntries(
            final NavigableMap<K, V1> fromMap,
            EntryTransformer<? super K, ? super V1, V2> transformer) {
        return new TransformedEntriesNavigableMap<>(fromMap, transformer);
    }

    static <K, V1, V2> SortedMap<K, V2> transformEntriesIgnoreNavigable(
            SortedMap<K, V1> fromMap,
            EntryTransformer<? super K, ? super V1, V2> transformer) {
        return new TransformedEntriesSortedMap<>(fromMap, transformer);
    }


    public interface EntryTransformer<K, V1, V2> {

        V2 transformEntry(@Nullable K key, @Nullable V1 value);
    }


    static <K, V1, V2> EntryTransformer<K, V1, V2>
    asEntryTransformer(final Function<? super V1, V2> function) {
        checkNotNull(function);
        return new EntryTransformer<K, V1, V2>() {
            @Override
            public V2 transformEntry(K key, V1 value) {
                return function.apply(value);
            }
        };
    }

    static <K, V1, V2> Function<V1, V2> asValueToValueFunction(
            final EntryTransformer<? super K, V1, V2> transformer, final K key) {
        checkNotNull(transformer);
        return new Function<V1, V2>() {
            @Override
            public V2 apply(@Nullable V1 v1) {
                return transformer.transformEntry(key, v1);
            }
        };
    }


    static <K, V1, V2> Function<Entry<K, V1>, V2> asEntryToValueFunction(
            final EntryTransformer<? super K, ? super V1, V2> transformer) {
        checkNotNull(transformer);
        return new Function<Entry<K, V1>, V2>() {
            @Override
            public V2 apply(Entry<K, V1> entry) {
                return transformer.transformEntry(entry.getKey(), entry.getValue());
            }
        };
    }


    static <V2, K, V1> Entry<K, V2> transformEntry(
            final EntryTransformer<? super K, ? super V1, V2> transformer, final Entry<K, V1> entry) {
        checkNotNull(transformer);
        checkNotNull(entry);
        return new AbstractMapEntry<K, V2>() {
            @Override
            public K getKey() {
                return entry.getKey();
            }

            @Override
            public V2 getValue() {
                return transformer.transformEntry(entry.getKey(), entry.getValue());
            }
        };
    }


    static <K, V1, V2> Function<Entry<K, V1>, Entry<K, V2>> asEntryToEntryFunction(
            final EntryTransformer<? super K, ? super V1, V2> transformer) {
        checkNotNull(transformer);
        return new Function<Entry<K, V1>, Entry<K, V2>>() {
            @Override
            public Entry<K, V2> apply(final Entry<K, V1> entry) {
                return transformEntry(transformer, entry);
            }
        };
    }

    static class TransformedEntriesMap<K, V1, V2>
            extends IteratorBasedAbstractMap<K, V2> {
        final Map<K, V1> fromMap;
        final EntryTransformer<? super K, ? super V1, V2> transformer;

        TransformedEntriesMap(
                Map<K, V1> fromMap,
                EntryTransformer<? super K, ? super V1, V2> transformer) {
            this.fromMap = checkNotNull(fromMap);
            this.transformer = checkNotNull(transformer);
        }

        @Override
        public int size() {
            return fromMap.size();
        }

        @Override
        public boolean containsKey(Object key) {
            return fromMap.containsKey(key);
        }

        // safe as long as the user followed the <b>Warning</b> in the javadoc
        @SuppressWarnings("unchecked")
        @Override
        public V2 get(Object key) {
            V1 value = fromMap.get(key);
            return (value != null || fromMap.containsKey(key))
                    ? transformer.transformEntry((K) key, value)
                    : null;
        }

        // safe as long as the user followed the <b>Warning</b> in the javadoc
        @SuppressWarnings("unchecked")
        @Override
        public V2 remove(Object key) {
            return fromMap.containsKey(key)
                    ? transformer.transformEntry((K) key, fromMap.remove(key))
                    : null;
        }

        @Override
        public void clear() {
            fromMap.clear();
        }

        @NonNull
        @Override
        public Set<K> keySet() {
            return fromMap.keySet();
        }

        @Override
        Iterator<Entry<K, V2>> entryIterator() {
            return Iterators.transform(fromMap.entrySet().iterator(),
                    Maps.asEntryToEntryFunction(transformer));
        }

        @NonNull
        @Override
        public Collection<V2> values() {
            return new Values<>(this);
        }
    }

    static class TransformedEntriesSortedMap<K, V1, V2>
            extends TransformedEntriesMap<K, V1, V2> implements SortedMap<K, V2> {

        protected SortedMap<K, V1> fromMap() {
            return (SortedMap<K, V1>) fromMap;
        }

        TransformedEntriesSortedMap(SortedMap<K, V1> fromMap,
                                    EntryTransformer<? super K, ? super V1, V2> transformer) {
            super(fromMap, transformer);
        }

        @Override
        public Comparator<? super K> comparator() {
            return fromMap().comparator();
        }

        @Override
        public K firstKey() {
            return fromMap().firstKey();
        }

        @NonNull
        @Override
        public SortedMap<K, V2> headMap(K toKey) {
            return transformEntries(fromMap().headMap(toKey), transformer);
        }

        @Override
        public K lastKey() {
            return fromMap().lastKey();
        }

        @NonNull
        @Override
        public SortedMap<K, V2> subMap(K fromKey, K toKey) {
            return transformEntries(
                    fromMap().subMap(fromKey, toKey), transformer);
        }

        @NonNull
        @Override
        public SortedMap<K, V2> tailMap(K fromKey) {
            return transformEntries(fromMap().tailMap(fromKey), transformer);
        }
    }

    @GwtIncompatible("NavigableMap")
    private static class TransformedEntriesNavigableMap<K, V1, V2>
            extends TransformedEntriesSortedMap<K, V1, V2>
            implements NavigableMap<K, V2> {

        TransformedEntriesNavigableMap(NavigableMap<K, V1> fromMap,
                                       EntryTransformer<? super K, ? super V1, V2> transformer) {
            super(fromMap, transformer);
        }

        @Override
        public Entry<K, V2> ceilingEntry(K key) {
            return transformEntry(fromMap().ceilingEntry(key));
        }

        @Override
        public K ceilingKey(K key) {
            return fromMap().ceilingKey(key);
        }

        @Override
        public NavigableSet<K> descendingKeySet() {
            return fromMap().descendingKeySet();
        }

        @Override
        public NavigableMap<K, V2> descendingMap() {
            return transformEntries(fromMap().descendingMap(), transformer);
        }

        @Override
        public Entry<K, V2> firstEntry() {
            return transformEntry(fromMap().firstEntry());
        }

        @Override
        public Entry<K, V2> floorEntry(K key) {
            return transformEntry(fromMap().floorEntry(key));
        }

        @Override
        public K floorKey(K key) {
            return fromMap().floorKey(key);
        }

        @NonNull
        @Override
        public NavigableMap<K, V2> headMap(K toKey) {
            return headMap(toKey, false);
        }

        @Override
        public NavigableMap<K, V2> headMap(K toKey, boolean inclusive) {
            return transformEntries(
                    fromMap().headMap(toKey, inclusive), transformer);
        }

        @Override
        public Entry<K, V2> higherEntry(K key) {
            return transformEntry(fromMap().higherEntry(key));
        }

        @Override
        public K higherKey(K key) {
            return fromMap().higherKey(key);
        }

        @Override
        public Entry<K, V2> lastEntry() {
            return transformEntry(fromMap().lastEntry());
        }

        @Override
        public Entry<K, V2> lowerEntry(K key) {
            return transformEntry(fromMap().lowerEntry(key));
        }

        @Override
        public K lowerKey(K key) {
            return fromMap().lowerKey(key);
        }

        @Override
        public NavigableSet<K> navigableKeySet() {
            return fromMap().navigableKeySet();
        }

        @Override
        public Entry<K, V2> pollFirstEntry() {
            return transformEntry(fromMap().pollFirstEntry());
        }

        @Override
        public Entry<K, V2> pollLastEntry() {
            return transformEntry(fromMap().pollLastEntry());
        }

        @Override
        public NavigableMap<K, V2> subMap(
                K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
            return transformEntries(
                    fromMap().subMap(fromKey, fromInclusive, toKey, toInclusive),
                    transformer);
        }

        @NonNull
        @Override
        public NavigableMap<K, V2> subMap(K fromKey, K toKey) {
            return subMap(fromKey, true, toKey, false);
        }

        @NonNull
        @Override
        public NavigableMap<K, V2> tailMap(K fromKey) {
            return tailMap(fromKey, true);
        }

        @Override
        public NavigableMap<K, V2> tailMap(K fromKey, boolean inclusive) {
            return transformEntries(
                    fromMap().tailMap(fromKey, inclusive), transformer);
        }

        @Nullable
        private Entry<K, V2> transformEntry(@Nullable Entry<K, V1> entry) {
            return (entry == null) ? null : Maps.transformEntry(transformer, entry);
        }

        @Override
        protected NavigableMap<K, V1> fromMap() {
            return (NavigableMap<K, V1>) super.fromMap();
        }
    }

    static <K> Predicate<Entry<K, ?>> keyPredicateOnEntries(Predicate<? super K> keyPredicate) {
        return compose(keyPredicate, Maps.<K>keyFunction());
    }

    static <V> Predicate<Entry<?, V>> valuePredicateOnEntries(Predicate<? super V> valuePredicate) {
        return compose(valuePredicate, Maps.<V>valueFunction());
    }


    static <K, V> SortedMap<K, V> filterSortedIgnoreNavigable(
            SortedMap<K, V> unfiltered,
            Predicate<? super Entry<K, V>> entryPredicate) {
        checkNotNull(entryPredicate);
        return (unfiltered instanceof FilteredEntrySortedMap)
                ? filterFiltered((FilteredEntrySortedMap<K, V>) unfiltered, entryPredicate)
                : new FilteredEntrySortedMap<>(checkNotNull(unfiltered), entryPredicate);
    }


    @GwtIncompatible("NavigableMap")

    public static <K, V> NavigableMap<K, V> filterEntries(
            NavigableMap<K, V> unfiltered,
            Predicate<? super Entry<K, V>> entryPredicate) {
        checkNotNull(entryPredicate);
        return (unfiltered instanceof FilteredEntryNavigableMap)
                ? filterFiltered((FilteredEntryNavigableMap<K, V>) unfiltered, entryPredicate)
                : new FilteredEntryNavigableMap<>(checkNotNull(unfiltered), entryPredicate);
    }


    private abstract static class AbstractFilteredMap<K, V>
            extends ViewCachingAbstractMap<K, V> {
        final Map<K, V> unfiltered;
        final Predicate<? super Entry<K, V>> predicate;

        AbstractFilteredMap(
                Map<K, V> unfiltered, Predicate<? super Entry<K, V>> predicate) {
            this.unfiltered = unfiltered;
            this.predicate = predicate;
        }

        boolean apply(@Nullable Object key, @Nullable V value) {
            // This method is called only when the key is in the map, implying that
            // key is a K.
            @SuppressWarnings("unchecked")
            K k = (K) key;
            return predicate.apply(Maps.immutableEntry(k, value));
        }

        @Override
        public V put(K key, V value) {
            checkArgument(apply(key, value));
            return unfiltered.put(key, value);
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> map) {
            for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
                checkArgument(apply(entry.getKey(), entry.getValue()));
            }
            unfiltered.putAll(map);
        }

        @Override
        public boolean containsKey(Object key) {
            return unfiltered.containsKey(key) && apply(key, unfiltered.get(key));
        }

        @Override
        public V get(Object key) {
            V value = unfiltered.get(key);
            return ((value != null) && apply(key, value)) ? value : null;
        }

        @Override
        public boolean isEmpty() {
            return entrySet().isEmpty();
        }

        @Override
        public V remove(Object key) {
            return containsKey(key) ? unfiltered.remove(key) : null;
        }

        @Override
        Collection<V> createValues() {
            return new FilteredMapValues<>(this, unfiltered, predicate);
        }
    }

    private static final class FilteredMapValues<K, V> extends Maps.Values<K, V> {
        Map<K, V> unfiltered;
        Predicate<? super Entry<K, V>> predicate;

        FilteredMapValues(Map<K, V> filteredMap, Map<K, V> unfiltered,
                          Predicate<? super Entry<K, V>> predicate) {
            super(filteredMap);
            this.unfiltered = unfiltered;
            this.predicate = predicate;
        }

        @Override
        public boolean remove(Object o) {
            return Iterables.removeFirstMatching(unfiltered.entrySet(),
                    Predicates.and(predicate, Maps.<V>valuePredicateOnEntries(equalTo(o))))
                    != null;
        }

        private boolean removeIf(Predicate<? super V> valuePredicate) {
            return Iterables.removeIf(unfiltered.entrySet(), Predicates.and(
                    predicate, Maps.valuePredicateOnEntries(valuePredicate)));
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            return removeIf(in(collection));
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            return removeIf(not(in(collection)));
        }

        @Override
        public Object[] toArray() {
            // creating an ArrayList so filtering happens once
            return Lists.newArrayList(iterator()).toArray();
        }

        @Override
        public <T> T[] toArray(T[] array) {
            return Lists.newArrayList(iterator()).toArray(array);
        }
    }

    static class FilteredEntryMap<K, V> extends AbstractFilteredMap<K, V> {

        final Set<Entry<K, V>> filteredEntrySet;

        FilteredEntryMap(
                Map<K, V> unfiltered, Predicate<? super Entry<K, V>> entryPredicate) {
            super(unfiltered, entryPredicate);
            filteredEntrySet = Sets.filter(unfiltered.entrySet(), predicate);
        }

        @Override
        protected Set<Entry<K, V>> createEntrySet() {
            return new EntrySet();
        }

        @WeakOuter
        private class EntrySet extends ForwardingSet<Entry<K, V>> {
            @Override
            protected Set<Entry<K, V>> delegate() {
                return filteredEntrySet;
            }

            @NonNull
            @Override
            public Iterator<Entry<K, V>> iterator() {
                return new TransformedIterator<Entry<K, V>, Entry<K, V>>(filteredEntrySet.iterator()) {
                    @Override
                    Entry<K, V> transform(final Entry<K, V> entry) {
                        return new ForwardingMapEntry<K, V>() {
                            @Override
                            protected Entry<K, V> delegate() {
                                return entry;
                            }

                            @Override
                            public V setValue(V newValue) {
                                checkArgument(apply(getKey(), newValue));
                                return super.setValue(newValue);
                            }
                        };
                    }
                };
            }
        }

        @Override
        Set<K> createKeySet() {
            return new KeySet();
        }

        @WeakOuter
        class KeySet extends Maps.KeySet<K, V> {
            KeySet() {
                super(FilteredEntryMap.this);
            }

            @Override
            public boolean remove(Object o) {
                if (containsKey(o)) {
                    unfiltered.remove(o);
                    return true;
                }
                return false;
            }

            private boolean removeIf(Predicate<? super K> keyPredicate) {
                return Iterables.removeIf(unfiltered.entrySet(), Predicates.and(
                        predicate, Maps.keyPredicateOnEntries(keyPredicate)));
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                return removeIf(in(c));
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                return removeIf(not(in(c)));
            }

            @NonNull
            @Override
            public Object[] toArray() {
                // creating an ArrayList so filtering happens once
                return Lists.newArrayList(iterator()).toArray();
            }

            @NonNull
            @Override
            public <T> T[] toArray(T[] array) {
                return Lists.newArrayList(iterator()).toArray(array);
            }
        }
    }


    private static <K, V> SortedMap<K, V> filterFiltered(
            FilteredEntrySortedMap<K, V> map,
            Predicate<? super Entry<K, V>> entryPredicate) {
        Predicate<Entry<K, V>> predicate
                = Predicates.and(map.predicate, entryPredicate);
        return new FilteredEntrySortedMap<>(map.sortedMap(), predicate);
    }

    private static class FilteredEntrySortedMap<K, V>
            extends FilteredEntryMap<K, V> implements SortedMap<K, V> {

        FilteredEntrySortedMap(SortedMap<K, V> unfiltered,
                               Predicate<? super Entry<K, V>> entryPredicate) {
            super(unfiltered, entryPredicate);
        }

        SortedMap<K, V> sortedMap() {
            return (SortedMap<K, V>) unfiltered;
        }

        @NonNull
        @Override
        public SortedSet<K> keySet() {
            return (SortedSet<K>) super.keySet();
        }

        @Override
        SortedSet<K> createKeySet() {
            return new SortedKeySet();
        }

        @WeakOuter
        class SortedKeySet extends KeySet implements SortedSet<K> {
            @Override
            public Comparator<? super K> comparator() {
                return sortedMap().comparator();
            }

            @NonNull
            @Override
            public SortedSet<K> subSet(K fromElement, K toElement) {
                return (SortedSet<K>) subMap(fromElement, toElement).keySet();
            }

            @NonNull
            @Override
            public SortedSet<K> headSet(K toElement) {
                return (SortedSet<K>) headMap(toElement).keySet();
            }

            @NonNull
            @Override
            public SortedSet<K> tailSet(K fromElement) {
                return (SortedSet<K>) tailMap(fromElement).keySet();
            }

            @Override
            public K first() {
                return firstKey();
            }

            @Override
            public K last() {
                return lastKey();
            }
        }

        @Override
        public Comparator<? super K> comparator() {
            return sortedMap().comparator();
        }

        @Override
        public K firstKey() {
            // correctly throws NoSuchElementException when filtered map is empty.
            return keySet().iterator().next();
        }

        @Override
        public K lastKey() {
            SortedMap<K, V> headMap = sortedMap();
            while (true) {
                // correctly throws NoSuchElementException when filtered map is empty.
                K key = headMap.lastKey();
                if (apply(key, unfiltered.get(key))) {
                    return key;
                }
                headMap = sortedMap().headMap(key);
            }
        }

        @NonNull
        @Override
        public SortedMap<K, V> headMap(K toKey) {
            return new FilteredEntrySortedMap<>(sortedMap().headMap(toKey), predicate);
        }

        @NonNull
        @Override
        public SortedMap<K, V> subMap(K fromKey, K toKey) {
            return new FilteredEntrySortedMap<>(
                    sortedMap().subMap(fromKey, toKey), predicate);
        }

        @NonNull
        @Override
        public SortedMap<K, V> tailMap(K fromKey) {
            return new FilteredEntrySortedMap<>(
                    sortedMap().tailMap(fromKey), predicate);
        }
    }


    @GwtIncompatible("NavigableMap")
    private static <K, V> NavigableMap<K, V> filterFiltered(
            FilteredEntryNavigableMap<K, V> map,
            Predicate<? super Entry<K, V>> entryPredicate) {
        Predicate<Entry<K, V>> predicate
                = Predicates.and(map.entryPredicate, entryPredicate);
        return new FilteredEntryNavigableMap<>(map.unfiltered, predicate);
    }

    @GwtIncompatible("NavigableMap")
    private static class FilteredEntryNavigableMap<K, V> extends AbstractNavigableMap<K, V> {


        private final NavigableMap<K, V> unfiltered;
        private final Predicate<? super Entry<K, V>> entryPredicate;
        private final Map<K, V> filteredDelegate;

        FilteredEntryNavigableMap(
                NavigableMap<K, V> unfiltered, Predicate<? super Entry<K, V>> entryPredicate) {
            this.unfiltered = checkNotNull(unfiltered);
            this.entryPredicate = entryPredicate;
            this.filteredDelegate = new FilteredEntryMap<>(unfiltered, entryPredicate);
        }

        @Override
        public Comparator<? super K> comparator() {
            return unfiltered.comparator();
        }

        @Override
        public NavigableSet<K> navigableKeySet() {
            return new Maps.NavigableKeySet<K, V>(this) {
                @Override
                public boolean removeAll(Collection<?> c) {
                    return Iterators.removeIf(unfiltered.entrySet().iterator(),
                            Predicates.and(entryPredicate, Maps.<K>keyPredicateOnEntries(in(c))));
                }

                @Override
                public boolean retainAll(Collection<?> c) {
                    return Iterators.removeIf(unfiltered.entrySet().iterator(), Predicates.and(
                            entryPredicate, Maps.<K>keyPredicateOnEntries(not(in(c)))));
                }
            };
        }

        @NonNull
        @Override
        public Collection<V> values() {
            return new FilteredMapValues<>(this, unfiltered, entryPredicate);
        }

        @Override
        Iterator<Entry<K, V>> entryIterator() {
            return Iterators.filter(unfiltered.entrySet().iterator(), entryPredicate);
        }

        @Override
        Iterator<Entry<K, V>> descendingEntryIterator() {
            return Iterators.filter(unfiltered.descendingMap().entrySet().iterator(), entryPredicate);
        }

        @Override
        public int size() {
            return filteredDelegate.size();
        }

        @Override
        public boolean isEmpty() {
            return !Iterables.any(unfiltered.entrySet(), entryPredicate);
        }

        @Override
        @Nullable
        public V get(@Nullable Object key) {
            return filteredDelegate.get(key);
        }

        @Override
        public boolean containsKey(@Nullable Object key) {
            return filteredDelegate.containsKey(key);
        }

        @Override
        public V put(K key, V value) {
            return filteredDelegate.put(key, value);
        }

        @Override
        public V remove(@Nullable Object key) {
            return filteredDelegate.remove(key);
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            filteredDelegate.putAll(m);
        }

        @Override
        public void clear() {
            filteredDelegate.clear();
        }

        @NonNull
        @Override
        public Set<Entry<K, V>> entrySet() {
            return filteredDelegate.entrySet();
        }

        @Override
        public Entry<K, V> pollFirstEntry() {
            return Iterables.removeFirstMatching(unfiltered.entrySet(), entryPredicate);
        }

        @Override
        public Entry<K, V> pollLastEntry() {
            return Iterables.removeFirstMatching(unfiltered.descendingMap().entrySet(), entryPredicate);
        }

        @Override
        public NavigableMap<K, V> descendingMap() {
            return filterEntries(unfiltered.descendingMap(), entryPredicate);
        }

        @Override
        public NavigableMap<K, V> subMap(
                K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
            return filterEntries(
                    unfiltered.subMap(fromKey, fromInclusive, toKey, toInclusive), entryPredicate);
        }

        @Override
        public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
            return filterEntries(unfiltered.headMap(toKey, inclusive), entryPredicate);
        }

        @Override
        public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
            return filterEntries(unfiltered.tailMap(fromKey, inclusive), entryPredicate);
        }
    }


    @GwtIncompatible("NavigableMap")
    public static <K, V> NavigableMap<K, V> unmodifiableNavigableMap(NavigableMap<K, V> map) {
        checkNotNull(map);
        if (map instanceof UnmodifiableNavigableMap) {
            return map;
        } else {
            return new UnmodifiableNavigableMap<>(map);
        }
    }

    @Nullable
    private static <K, V> Entry<K, V> unmodifiableOrNull(@Nullable Entry<K, V> entry) {
        return (entry == null) ? null : Maps.unmodifiableEntry(entry);
    }

    @GwtIncompatible("NavigableMap")
    static class UnmodifiableNavigableMap<K, V>
            extends ForwardingSortedMap<K, V> implements NavigableMap<K, V>, Serializable {
        private final NavigableMap<K, V> delegate;

        UnmodifiableNavigableMap(NavigableMap<K, V> delegate) {
            this.delegate = delegate;
        }

        UnmodifiableNavigableMap(
                NavigableMap<K, V> delegate, UnmodifiableNavigableMap<K, V> descendingMap) {
            this.delegate = delegate;
            this.descendingMap = descendingMap;
        }

        @Override
        protected SortedMap<K, V> delegate() {
            return Collections.unmodifiableSortedMap(delegate);
        }

        @Override
        public Entry<K, V> lowerEntry(K key) {
            return unmodifiableOrNull(delegate.lowerEntry(key));
        }

        @Override
        public K lowerKey(K key) {
            return delegate.lowerKey(key);
        }

        @Override
        public Entry<K, V> floorEntry(K key) {
            return unmodifiableOrNull(delegate.floorEntry(key));
        }

        @Override
        public K floorKey(K key) {
            return delegate.floorKey(key);
        }

        @Override
        public Entry<K, V> ceilingEntry(K key) {
            return unmodifiableOrNull(delegate.ceilingEntry(key));
        }

        @Override
        public K ceilingKey(K key) {
            return delegate.ceilingKey(key);
        }

        @Override
        public Entry<K, V> higherEntry(K key) {
            return unmodifiableOrNull(delegate.higherEntry(key));
        }

        @Override
        public K higherKey(K key) {
            return delegate.higherKey(key);
        }

        @Override
        public Entry<K, V> firstEntry() {
            return unmodifiableOrNull(delegate.firstEntry());
        }

        @Override
        public Entry<K, V> lastEntry() {
            return unmodifiableOrNull(delegate.lastEntry());
        }

        @Override
        public final Entry<K, V> pollFirstEntry() {
            throw new UnsupportedOperationException();
        }

        @Override
        public final Entry<K, V> pollLastEntry() {
            throw new UnsupportedOperationException();
        }

        private transient UnmodifiableNavigableMap<K, V> descendingMap;

        @Override
        public NavigableMap<K, V> descendingMap() {
            UnmodifiableNavigableMap<K, V> result = descendingMap;
            return (result == null)
                    ? descendingMap = new UnmodifiableNavigableMap<>(delegate.descendingMap(), this)
                    : result;
        }

        @NonNull
        @Override
        public Set<K> keySet() {
            return navigableKeySet();
        }

        @Override
        public NavigableSet<K> navigableKeySet() {
            return Sets.unmodifiableNavigableSet(delegate.navigableKeySet());
        }

        @Override
        public NavigableSet<K> descendingKeySet() {
            return Sets.unmodifiableNavigableSet(delegate.descendingKeySet());
        }

        @NonNull
        @Override
        public SortedMap<K, V> subMap(K fromKey, K toKey) {
            return subMap(fromKey, true, toKey, false);
        }

        @NonNull
        @Override
        public SortedMap<K, V> headMap(K toKey) {
            return headMap(toKey, false);
        }

        @NonNull
        @Override
        public SortedMap<K, V> tailMap(K fromKey) {
            return tailMap(fromKey, true);
        }

        @Override
        public NavigableMap<K, V>
        subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
            return Maps.unmodifiableNavigableMap(delegate.subMap(
                    fromKey,
                    fromInclusive,
                    toKey,
                    toInclusive));
        }

        @Override
        public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
            return Maps.unmodifiableNavigableMap(delegate.headMap(toKey, inclusive));
        }

        @Override
        public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
            return Maps.unmodifiableNavigableMap(delegate.tailMap(fromKey, inclusive));
        }
    }


    @GwtCompatible
    abstract static class ViewCachingAbstractMap<K, V> extends AbstractMap<K, V> {

        abstract Set<Entry<K, V>> createEntrySet();

        private transient Set<Entry<K, V>> entrySet;

        @NonNull
        @Override
        public Set<Entry<K, V>> entrySet() {
            Set<Entry<K, V>> result = entrySet;
            return (result == null) ? entrySet = createEntrySet() : result;
        }

        private transient Set<K> keySet;

        @NonNull
        @Override
        public Set<K> keySet() {
            Set<K> result = keySet;
            return (result == null) ? keySet = createKeySet() : result;
        }

        Set<K> createKeySet() {
            return new KeySet<>(this);
        }

        private transient Collection<V> values;

        @NonNull
        @Override
        public Collection<V> values() {
            Collection<V> result = values;
            return (result == null) ? values = createValues() : result;
        }

        Collection<V> createValues() {
            return new Values<>(this);
        }
    }

    abstract static class IteratorBasedAbstractMap<K, V> extends AbstractMap<K, V> {
        @Override
        public abstract int size();

        abstract Iterator<Entry<K, V>> entryIterator();

        @NonNull
        @Override
        public Set<Entry<K, V>> entrySet() {
            return new EntrySet<K, V>() {
                @Override
                Map<K, V> map() {
                    return IteratorBasedAbstractMap.this;
                }

                @NonNull
                @Override
                public Iterator<Entry<K, V>> iterator() {
                    return entryIterator();
                }
            };
        }

        @Override
        public void clear() {
            Iterators.clear(entryIterator());
        }
    }


    static <V> V safeGet(Map<?, V> map, @Nullable Object key) {
        checkNotNull(map);
        try {
            return map.get(key);
        } catch (ClassCastException e) {
            return null;
        } catch (NullPointerException e) {
            return null;
        }
    }


    static boolean safeContainsKey(Map<?, ?> map, Object key) {
        checkNotNull(map);
        try {
            return map.containsKey(key);
        } catch (ClassCastException e) {
            return false;
        } catch (NullPointerException e) {
            return false;
        }
    }


    static <V> V safeRemove(Map<?, V> map, Object key) {
        checkNotNull(map);
        try {
            return map.remove(key);
        } catch (ClassCastException e) {
            return null;
        } catch (NullPointerException e) {
            return null;
        }
    }


    static boolean containsKeyImpl(Map<?, ?> map, @Nullable Object key) {
        return Iterators.contains(keyIterator(map.entrySet().iterator()), key);
    }


    static boolean containsValueImpl(Map<?, ?> map, @Nullable Object value) {
        return Iterators.contains(valueIterator(map.entrySet().iterator()), value);
    }


    static <K, V> boolean containsEntryImpl(Collection<Entry<K, V>> c, Object o) {
        if (!(o instanceof Entry)) {
            return false;
        }
        return c.contains(unmodifiableEntry((Entry<?, ?>) o));
    }


    static <K, V> boolean removeEntryImpl(Collection<Entry<K, V>> c, Object o) {
        if (!(o instanceof Entry)) {
            return false;
        }
        return c.remove(unmodifiableEntry((Entry<?, ?>) o));
    }


    static boolean equalsImpl(Map<?, ?> map, Object object) {
        if (map == object) {
            return true;
        } else if (object instanceof Map) {
            Map<?, ?> o = (Map<?, ?>) object;
            return map.entrySet().equals(o.entrySet());
        }
        return false;
    }

    static final MapJoiner STANDARD_JOINER =
            Collections2.STANDARD_JOINER.withKeyValueSeparator("=");


    static String toStringImpl(Map<?, ?> map) {
        StringBuilder sb
                = Collections2.newStringBuilderForCollection(map.size()).append('{');
        STANDARD_JOINER.appendTo(sb, map);
        return sb.append('}').toString();
    }


    static <K, V> void putAllImpl(
            Map<K, V> self, Map<? extends K, ? extends V> map) {
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            self.put(entry.getKey(), entry.getValue());
        }
    }

    static class KeySet<K, V> extends Sets.ImprovedAbstractSet<K> {
        @Weak
        final Map<K, V> map;

        KeySet(Map<K, V> map) {
            this.map = checkNotNull(map);
        }

        Map<K, V> map() {
            return map;
        }

        @NonNull
        @Override
        public Iterator<K> iterator() {
            return keyIterator(map().entrySet().iterator());
        }

        @Override
        public int size() {
            return map().size();
        }

        @Override
        public boolean isEmpty() {
            return map().isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return map().containsKey(o);
        }

        @Override
        public boolean remove(Object o) {
            if (contains(o)) {
                map().remove(o);
                return true;
            }
            return false;
        }

        @Override
        public void clear() {
            map().clear();
        }
    }

    @Nullable
    static <K> K keyOrNull(@Nullable Entry<K, ?> entry) {
        return (entry == null) ? null : entry.getKey();
    }

    @Nullable
    static <V> V valueOrNull(@Nullable Entry<?, V> entry) {
        return (entry == null) ? null : entry.getValue();
    }

    static class SortedKeySet<K, V> extends KeySet<K, V> implements SortedSet<K> {
        SortedKeySet(SortedMap<K, V> map) {
            super(map);
        }

        @Override
        SortedMap<K, V> map() {
            return (SortedMap<K, V>) super.map();
        }

        @Override
        public Comparator<? super K> comparator() {
            return map().comparator();
        }

        @NonNull
        @Override
        public SortedSet<K> subSet(K fromElement, K toElement) {
            return new SortedKeySet<>(map().subMap(fromElement, toElement));
        }

        @NonNull
        @Override
        public SortedSet<K> headSet(K toElement) {
            return new SortedKeySet<>(map().headMap(toElement));
        }

        @NonNull
        @Override
        public SortedSet<K> tailSet(K fromElement) {
            return new SortedKeySet<>(map().tailMap(fromElement));
        }

        @Override
        public K first() {
            return map().firstKey();
        }

        @Override
        public K last() {
            return map().lastKey();
        }
    }

    @GwtIncompatible("NavigableMap")
    static class NavigableKeySet<K, V> extends SortedKeySet<K, V> implements NavigableSet<K> {
        NavigableKeySet(NavigableMap<K, V> map) {
            super(map);
        }

        @Override
        NavigableMap<K, V> map() {
            return (NavigableMap<K, V>) map;
        }

        @Override
        public K lower(K e) {
            return map().lowerKey(e);
        }

        @Override
        public K floor(K e) {
            return map().floorKey(e);
        }

        @Override
        public K ceiling(K e) {
            return map().ceilingKey(e);
        }

        @Override
        public K higher(K e) {
            return map().higherKey(e);
        }

        @Override
        public K pollFirst() {
            return keyOrNull(map().pollFirstEntry());
        }

        @Override
        public K pollLast() {
            return keyOrNull(map().pollLastEntry());
        }

        @NonNull
        @Override
        public NavigableSet<K> descendingSet() {
            return map().descendingKeySet();
        }

        @NonNull
        @Override
        public Iterator<K> descendingIterator() {
            return descendingSet().iterator();
        }

        @NonNull
        @Override
        public NavigableSet<K> subSet(
                K fromElement,
                boolean fromInclusive,
                K toElement,
                boolean toInclusive) {
            return map().subMap(fromElement, fromInclusive, toElement, toInclusive).navigableKeySet();
        }

        @NonNull
        @Override
        public NavigableSet<K> headSet(K toElement, boolean inclusive) {
            return map().headMap(toElement, inclusive).navigableKeySet();
        }

        @NonNull
        @Override
        public NavigableSet<K> tailSet(K fromElement, boolean inclusive) {
            return map().tailMap(fromElement, inclusive).navigableKeySet();
        }

        @NonNull
        @Override
        public SortedSet<K> subSet(K fromElement, K toElement) {
            return subSet(fromElement, true, toElement, false);
        }

        @NonNull
        @Override
        public SortedSet<K> headSet(K toElement) {
            return headSet(toElement, false);
        }

        @NonNull
        @Override
        public SortedSet<K> tailSet(K fromElement) {
            return tailSet(fromElement, true);
        }
    }

    static class Values<K, V> extends AbstractCollection<V> {
        @Weak
        final Map<K, V> map;

        Values(Map<K, V> map) {
            this.map = checkNotNull(map);
        }

        final Map<K, V> map() {
            return map;
        }

        @NonNull
        @Override
        public Iterator<V> iterator() {
            return valueIterator(map().entrySet().iterator());
        }

        @Override
        public boolean remove(Object o) {
            try {
                return super.remove(o);
            } catch (UnsupportedOperationException e) {
                for (Entry<K, V> entry : map().entrySet()) {
                    if (Objects.equal(o, entry.getValue())) {
                        map().remove(entry.getKey());
                        return true;
                    }
                }
                return false;
            }
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            try {
                return super.removeAll(checkNotNull(c));
            } catch (UnsupportedOperationException e) {
                Set<K> toRemove = Sets.newHashSet();
                for (Entry<K, V> entry : map().entrySet()) {
                    if (c.contains(entry.getValue())) {
                        toRemove.add(entry.getKey());
                    }
                }
                return map().keySet().removeAll(toRemove);
            }
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            try {
                return super.retainAll(checkNotNull(c));
            } catch (UnsupportedOperationException e) {
                Set<K> toRetain = Sets.newHashSet();
                for (Entry<K, V> entry : map().entrySet()) {
                    if (c.contains(entry.getValue())) {
                        toRetain.add(entry.getKey());
                    }
                }
                return map().keySet().retainAll(toRetain);
            }
        }

        @Override
        public int size() {
            return map().size();
        }

        @Override
        public boolean isEmpty() {
            return map().isEmpty();
        }

        @Override
        public boolean contains(@Nullable Object o) {
            return map().containsValue(o);
        }

        @Override
        public void clear() {
            map().clear();
        }
    }

    abstract static class EntrySet<K, V>
            extends Sets.ImprovedAbstractSet<Entry<K, V>> {
        abstract Map<K, V> map();

        @Override
        public int size() {
            return map().size();
        }

        @Override
        public void clear() {
            map().clear();
        }

        @Override
        public boolean contains(Object o) {
            if (o instanceof Entry) {
                Entry<?, ?> entry = (Entry<?, ?>) o;
                Object key = entry.getKey();
                V value = Maps.safeGet(map(), key);
                return Objects.equal(value, entry.getValue())
                        && (value != null || map().containsKey(key));
            }
            return false;
        }

        @Override
        public boolean isEmpty() {
            return map().isEmpty();
        }

        @Override
        public boolean remove(Object o) {
            if (contains(o)) {
                Entry<?, ?> entry = (Entry<?, ?>) o;
                return map().keySet().remove(entry.getKey());
            }
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            try {
                return super.removeAll(checkNotNull(c));
            } catch (UnsupportedOperationException e) {
                // if the iterators don't support remove
                return Sets.removeAllImpl(this, c.iterator());
            }
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            try {
                return super.retainAll(checkNotNull(c));
            } catch (UnsupportedOperationException e) {
                // if the iterators don't support remove
                Set<Object> keys = Sets.newHashSetWithExpectedSize(c.size());
                for (Object o : c) {
                    if (contains(o)) {
                        Entry<?, ?> entry = (Entry<?, ?>) o;
                        keys.add(entry.getKey());
                    }
                }
                return map().keySet().retainAll(keys);
            }
        }
    }

    @GwtIncompatible("NavigableMap")
    abstract static class DescendingMap<K, V> extends ForwardingMap<K, V>
            implements NavigableMap<K, V> {

        abstract NavigableMap<K, V> forward();

        @Override
        protected final Map<K, V> delegate() {
            return forward();
        }

        private transient Comparator<? super K> comparator;

        @SuppressWarnings("unchecked")
        @Override
        public Comparator<? super K> comparator() {
            Comparator<? super K> result = comparator;
            if (result == null) {
                Comparator<? super K> forwardCmp = forward().comparator();
                if (forwardCmp == null) {
                    forwardCmp = (Comparator) Ordering.natural();
                }
                result = comparator = reverse(forwardCmp);
            }
            return result;
        }

        // If we inline this, we get a javac error.
        private static <T> Ordering<T> reverse(Comparator<T> forward) {
            return Ordering.from(forward).reverse();
        }

        @Override
        public K firstKey() {
            return forward().lastKey();
        }

        @Override
        public K lastKey() {
            return forward().firstKey();
        }

        @Override
        public Entry<K, V> lowerEntry(K key) {
            return forward().higherEntry(key);
        }

        @Override
        public K lowerKey(K key) {
            return forward().higherKey(key);
        }

        @Override
        public Entry<K, V> floorEntry(K key) {
            return forward().ceilingEntry(key);
        }

        @Override
        public K floorKey(K key) {
            return forward().ceilingKey(key);
        }

        @Override
        public Entry<K, V> ceilingEntry(K key) {
            return forward().floorEntry(key);
        }

        @Override
        public K ceilingKey(K key) {
            return forward().floorKey(key);
        }

        @Override
        public Entry<K, V> higherEntry(K key) {
            return forward().lowerEntry(key);
        }

        @Override
        public K higherKey(K key) {
            return forward().lowerKey(key);
        }

        @Override
        public Entry<K, V> firstEntry() {
            return forward().lastEntry();
        }

        @Override
        public Entry<K, V> lastEntry() {
            return forward().firstEntry();
        }

        @Override
        public Entry<K, V> pollFirstEntry() {
            return forward().pollLastEntry();
        }

        @Override
        public Entry<K, V> pollLastEntry() {
            return forward().pollFirstEntry();
        }

        @Override
        public NavigableMap<K, V> descendingMap() {
            return forward();
        }

        private transient Set<Entry<K, V>> entrySet;

        @NonNull
        @Override
        public Set<Entry<K, V>> entrySet() {
            Set<Entry<K, V>> result = entrySet;
            return (result == null) ? entrySet = createEntrySet() : result;
        }

        abstract Iterator<Entry<K, V>> entryIterator();

        Set<Entry<K, V>> createEntrySet() {
            @WeakOuter
            class EntrySetImpl extends EntrySet<K, V> {
                @Override
                Map<K, V> map() {
                    return DescendingMap.this;
                }

                @NonNull
                @Override
                public Iterator<Entry<K, V>> iterator() {
                    return entryIterator();
                }
            }
            return new EntrySetImpl();
        }

        @NonNull
        @Override
        public Set<K> keySet() {
            return navigableKeySet();
        }

        private transient NavigableSet<K> navigableKeySet;

        @Override
        public NavigableSet<K> navigableKeySet() {
            NavigableSet<K> result = navigableKeySet;
            return (result == null) ? navigableKeySet = new NavigableKeySet<>(this) : result;
        }

        @Override
        public NavigableSet<K> descendingKeySet() {
            return forward().navigableKeySet();
        }

        @Override
        public NavigableMap<K, V>
        subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
            return forward().subMap(toKey, toInclusive, fromKey, fromInclusive).descendingMap();
        }

        @Override
        public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
            return forward().tailMap(toKey, inclusive).descendingMap();
        }

        @Override
        public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
            return forward().headMap(fromKey, inclusive).descendingMap();
        }

        @NonNull
        @Override
        public SortedMap<K, V> subMap(K fromKey, K toKey) {
            return subMap(fromKey, true, toKey, false);
        }

        @NonNull
        @Override
        public SortedMap<K, V> headMap(K toKey) {
            return headMap(toKey, false);
        }

        @NonNull
        @Override
        public SortedMap<K, V> tailMap(K fromKey) {
            return tailMap(fromKey, true);
        }

        @NonNull
        @Override
        public Collection<V> values() {
            return new Values<>(this);
        }

        @Override
        public String toString() {
            return standardToString();
        }
    }


    static <E> ImmutableMap<E, Integer> indexMap(Collection<E> list) {
        ImmutableMap.Builder<E, Integer> builder =
                new ImmutableMap.Builder<>(list.size());
        int i = 0;
        for (E e : list) {
            builder.put(e, i++);
        }
        return builder.build();
    }
}
