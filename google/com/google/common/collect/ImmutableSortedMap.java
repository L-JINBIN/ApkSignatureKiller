

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.WeakOuter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.CollectPreconditions.checkEntryNotNull;
import static com.google.common.collect.Maps.keyOrNull;


@GwtCompatible(serializable = true, emulated = true)
public final class ImmutableSortedMap<K, V> extends ImmutableSortedMapFauxverideShim<K, V>
        implements NavigableMap<K, V> {


    private static final Comparator<Comparable> NATURAL_ORDER = Ordering.natural();

    private static final ImmutableSortedMap<Comparable, Object> NATURAL_EMPTY_MAP =
            new ImmutableSortedMap<>(
                    ImmutableSortedSet.emptySet(Ordering.natural()), ImmutableList.of());

    static <K, V> ImmutableSortedMap<K, V> emptyMap(Comparator<? super K> comparator) {
        if (Ordering.natural().equals(comparator)) {
            return of();
        } else {
            return new ImmutableSortedMap<>(
                    ImmutableSortedSet.emptySet(comparator), ImmutableList.<V>of());
        }
    }


    @SuppressWarnings("unchecked")
    // unsafe, comparator() returns a comparator on the specified type
    // TODO(kevinb): evaluate whether or not of().comparator() should return null
    public static <K, V> ImmutableSortedMap<K, V> of() {
        return (ImmutableSortedMap<K, V>) NATURAL_EMPTY_MAP;
    }


    private static <K, V> ImmutableSortedMap<K, V> of(Comparator<? super K> comparator, K k1, V v1) {
        return new ImmutableSortedMap<>(
                new RegularImmutableSortedSet<>(ImmutableList.of(k1), checkNotNull(comparator)),
                ImmutableList.of(v1));
    }

    private static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> ofEntries(
            ImmutableMapEntry<K, V>... entries) {
        return fromEntries(Ordering.natural(), false, entries, entries.length);
    }


    private static <K, V> ImmutableSortedMap<K, V> copyOfInternal(
            Map<? extends K, ? extends V> map, Comparator<? super K> comparator) {
        boolean sameComparator = false;
        if (map instanceof SortedMap) {
            SortedMap<?, ?> sortedMap = (SortedMap<?, ?>) map;
            Comparator<?> comparator2 = sortedMap.comparator();
            sameComparator =
                    (comparator2 == null)
                            ? comparator == NATURAL_ORDER
                            : comparator.equals(comparator2);
        }

        if (sameComparator && (map instanceof ImmutableSortedMap)) {
            // TODO(kevinb): Prove that this cast is safe, even though
            // Collections.unmodifiableSortedMap requires the same key type.
            @SuppressWarnings("unchecked")
            ImmutableSortedMap<K, V> kvMap = (ImmutableSortedMap<K, V>) map;
            if (!kvMap.isPartialView()) {
                return kvMap;
            }
        }
        return fromEntries(comparator, sameComparator, map.entrySet());
    }


    private static <K, V> ImmutableSortedMap<K, V> fromEntries(
            Comparator<? super K> comparator,
            boolean sameComparator,
            Iterable<? extends Entry<? extends K, ? extends V>> entries) {
        // "adding" type params to an array of a raw type should be safe as
        // long as no one can ever cast that same array instance back to a
        // raw type.
        @SuppressWarnings("unchecked")
        Entry<K, V>[] entryArray = (Entry[]) Iterables.toArray(entries, EMPTY_ENTRY_ARRAY);
        return fromEntries(comparator, sameComparator, entryArray, entryArray.length);
    }

    private static <K, V> ImmutableSortedMap<K, V> fromEntries(
            Comparator<? super K> comparator,
            boolean sameComparator,
            Entry<K, V>[] entryArray,
            int size) {
        switch (size) {
            case 0:
                return emptyMap(comparator);
            case 1:
                return ImmutableSortedMap.of(
                        comparator, entryArray[0].getKey(), entryArray[0].getValue());
            default:
                Object[] keys = new Object[size];
                Object[] values = new Object[size];
                if (sameComparator) {
                    // Need to check for nulls, but don't need to sort or validate.
                    for (int i = 0; i < size; i++) {
                        Object key = entryArray[i].getKey();
                        Object value = entryArray[i].getValue();
                        checkEntryNotNull(key, value);
                        keys[i] = key;
                        values[i] = value;
                    }
                } else {
                    // Need to sort and check for nulls and dupes.
                    Arrays.sort(entryArray, 0, size, Ordering.from(comparator).<K>onKeys());
                    K prevKey = entryArray[0].getKey();
                    keys[0] = prevKey;
                    values[0] = entryArray[0].getValue();
                    for (int i = 1; i < size; i++) {
                        K key = entryArray[i].getKey();
                        V value = entryArray[i].getValue();
                        checkEntryNotNull(key, value);
                        keys[i] = key;
                        values[i] = value;
                        checkNoConflict(
                                comparator.compare(prevKey, key) != 0, "key", entryArray[i - 1], entryArray[i]);
                        prevKey = key;
                    }
                }
                return new ImmutableSortedMap<>(
                        new RegularImmutableSortedSet<>(new RegularImmutableList<K>(keys), comparator),
                        new RegularImmutableList<V>(values));
        }
    }


    public static class Builder<K, V> extends ImmutableMap.Builder<K, V> {
        private final Comparator<? super K> comparator;


        @SuppressWarnings("unchecked")
        public Builder(Comparator<? super K> comparator) {
            this.comparator = checkNotNull(comparator);
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
        @Deprecated
        public Builder<K, V> orderEntriesByValue(Comparator<? super V> valueComparator) {
            throw new UnsupportedOperationException("Not available on ImmutableSortedMap.Builder");
        }


        @Override
        public ImmutableSortedMap<K, V> build() {
            switch (size) {
                case 0:
                    return emptyMap(comparator);
                case 1:
                    return of(comparator, entries[0].getKey(), entries[0].getValue());
                default:
                    return fromEntries(comparator, false, entries, size);
            }
        }
    }

    private final transient RegularImmutableSortedSet<K> keySet;
    private final transient ImmutableList<V> valueList;
    private transient ImmutableSortedMap<K, V> descendingMap;

    ImmutableSortedMap(RegularImmutableSortedSet<K> keySet, ImmutableList<V> valueList) {
        this(keySet, valueList, null);
    }

    ImmutableSortedMap(
            RegularImmutableSortedSet<K> keySet,
            ImmutableList<V> valueList,
            ImmutableSortedMap<K, V> descendingMap) {
        this.keySet = keySet;
        this.valueList = valueList;
        this.descendingMap = descendingMap;
    }

    @Override
    public int size() {
        return valueList.size();
    }

    @Override
    public V get(@Nullable Object key) {
        int index = keySet.indexOf(key);
        return (index == -1) ? null : valueList.get(index);
    }

    @Override
    boolean isPartialView() {
        return keySet.isPartialView() || valueList.isPartialView();
    }


    @Override
    public ImmutableSet<Entry<K, V>> entrySet() {
        return super.entrySet();
    }

    @Override
    ImmutableSet<Entry<K, V>> createEntrySet() {
        @WeakOuter
        class EntrySet extends ImmutableMapEntrySet<K, V> {
            @Override
            public UnmodifiableIterator<Entry<K, V>> iterator() {
                return asList().iterator();
            }

            @Override
            ImmutableList<Entry<K, V>> createAsList() {
                return new ImmutableAsList<Entry<K, V>>() {
                    @Override
                    public Entry<K, V> get(int index) {
                        return Maps.immutableEntry(keySet.asList().get(index), valueList.get(index));
                    }

                    @Override
                    ImmutableCollection<Entry<K, V>> delegateCollection() {
                        return EntrySet.this;
                    }
                };
            }

            @Override
            ImmutableMap<K, V> map() {
                return ImmutableSortedMap.this;
            }
        }
        return isEmpty() ? ImmutableSet.<Entry<K, V>>of() : new EntrySet();
    }


    @Override
    public ImmutableSortedSet<K> keySet() {
        return keySet;
    }


    @Override
    public ImmutableCollection<V> values() {
        return valueList;
    }


    @Override
    public Comparator<? super K> comparator() {
        return keySet().comparator();
    }

    @Override
    public K firstKey() {
        return keySet().first();
    }

    @Override
    public K lastKey() {
        return keySet().last();
    }

    private ImmutableSortedMap<K, V> getSubMap(int fromIndex, int toIndex) {
        if (fromIndex == 0 && toIndex == size()) {
            return this;
        } else if (fromIndex == toIndex) {
            return emptyMap(comparator());
        } else {
            return new ImmutableSortedMap<>(
                    keySet.getSubSet(fromIndex, toIndex), valueList.subList(fromIndex, toIndex));
        }
    }


    @Override
    public ImmutableSortedMap<K, V> headMap(K toKey) {
        return headMap(toKey, false);
    }


    @Override
    public ImmutableSortedMap<K, V> headMap(K toKey, boolean inclusive) {
        return getSubMap(0, keySet.headIndex(checkNotNull(toKey), inclusive));
    }


    @Override
    public ImmutableSortedMap<K, V> subMap(K fromKey, K toKey) {
        return subMap(fromKey, true, toKey, false);
    }


    @Override
    public ImmutableSortedMap<K, V> subMap(
            K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        checkNotNull(fromKey);
        checkNotNull(toKey);
        checkArgument(
                comparator().compare(fromKey, toKey) <= 0,
                "expected fromKey <= toKey but %s > %s",
                fromKey,
                toKey);
        return headMap(toKey, toInclusive).tailMap(fromKey, fromInclusive);
    }


    @Override
    public ImmutableSortedMap<K, V> tailMap(K fromKey) {
        return tailMap(fromKey, true);
    }


    @Override
    public ImmutableSortedMap<K, V> tailMap(K fromKey, boolean inclusive) {
        return getSubMap(keySet.tailIndex(checkNotNull(fromKey), inclusive), size());
    }

    @Override
    public Entry<K, V> lowerEntry(K key) {
        return headMap(key, false).lastEntry();
    }

    @Override
    public K lowerKey(K key) {
        return keyOrNull(lowerEntry(key));
    }

    @Override
    public Entry<K, V> floorEntry(K key) {
        return headMap(key, true).lastEntry();
    }

    @Override
    public K floorKey(K key) {
        return keyOrNull(floorEntry(key));
    }

    @Override
    public Entry<K, V> ceilingEntry(K key) {
        return tailMap(key, true).firstEntry();
    }

    @Override
    public K ceilingKey(K key) {
        return keyOrNull(ceilingEntry(key));
    }

    @Override
    public Entry<K, V> higherEntry(K key) {
        return tailMap(key, false).firstEntry();
    }

    @Override
    public K higherKey(K key) {
        return keyOrNull(higherEntry(key));
    }

    @Override
    public Entry<K, V> firstEntry() {
        return isEmpty() ? null : entrySet().asList().get(0);
    }

    @Override
    public Entry<K, V> lastEntry() {
        return isEmpty() ? null : entrySet().asList().get(size() - 1);
    }


    @Deprecated
    @Override
    public final Entry<K, V> pollFirstEntry() {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    @Override
    public final Entry<K, V> pollLastEntry() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ImmutableSortedMap<K, V> descendingMap() {
        ImmutableSortedMap<K, V> result = descendingMap;
        if (result == null) {
            if (isEmpty()) {
                return result = emptyMap(Ordering.from(comparator()).reverse());
            } else {
                return result = new ImmutableSortedMap<>(
                        (RegularImmutableSortedSet<K>) keySet.descendingSet(), valueList.reverse(), this);
            }
        }
        return result;
    }

    @Override
    public ImmutableSortedSet<K> navigableKeySet() {
        return keySet;
    }

    @Override
    public ImmutableSortedSet<K> descendingKeySet() {
        return keySet.descendingSet();
    }


    private static class SerializedForm extends ImmutableMap.SerializedForm {
        private final Comparator<Object> comparator;

        @SuppressWarnings("unchecked")
        SerializedForm(ImmutableSortedMap<?, ?> sortedMap) {
            super(sortedMap);
            comparator = (Comparator<Object>) sortedMap.comparator();
        }

        @Override
        Object readResolve() {
            Builder<Object, Object> builder = new Builder<>(comparator);
            return createMap(builder);
        }

        private static final long serialVersionUID = 0;
    }

    @Override
    Object writeReplace() {
        return new SerializedForm(this);
    }

    // This class is never actually serialized directly, but we have to make the
    // warning go away (and suppressing would suppress for all nested classes too)
    private static final long serialVersionUID = 0;
}
