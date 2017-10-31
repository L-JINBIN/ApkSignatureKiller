

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.Weak;
import com.google.common.annotations.WeakOuter;
import com.google.common.base.Function;
import com.google.common.collect.Maps.EntryTransformer;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.CollectPreconditions.checkNonnegative;


@GwtCompatible(emulated = true)
public final class Multimaps {
    private Multimaps() {
    }


    private static class TransformedEntriesMultimap<K, V1, V2>
            extends AbstractMultimap<K, V2> {
        final Multimap<K, V1> fromMultimap;
        final EntryTransformer<? super K, ? super V1, V2> transformer;

        TransformedEntriesMultimap(Multimap<K, V1> fromMultimap,
                                   final EntryTransformer<? super K, ? super V1, V2> transformer) {
            this.fromMultimap = checkNotNull(fromMultimap);
            this.transformer = checkNotNull(transformer);
        }

        Collection<V2> transform(K key, Collection<V1> values) {
            Function<? super V1, V2> function =
                    Maps.asValueToValueFunction(transformer, key);
            if (values instanceof List) {
                return Lists.transform((List<V1>) values, function);
            } else {
                return Collections2.transform(values, function);
            }
        }

        @Override
        Map<K, Collection<V2>> createAsMap() {
            return Maps.transformEntries(fromMultimap.asMap(),
                    new EntryTransformer<K, Collection<V1>, Collection<V2>>() {
                        @Override
                        public Collection<V2> transformEntry(
                                K key, Collection<V1> value) {
                            return transform(key, value);
                        }
                    });
        }

        @Override
        public void clear() {
            fromMultimap.clear();
        }

        @Override
        public boolean containsKey(Object key) {
            return fromMultimap.containsKey(key);
        }

        @Override
        Iterator<Entry<K, V2>> entryIterator() {
            return Iterators.transform(fromMultimap.entries().iterator(),
                    Maps.asEntryToEntryFunction(transformer));
        }

        @Override
        public Collection<V2> get(final K key) {
            return transform(key, fromMultimap.get(key));
        }

        @Override
        public boolean isEmpty() {
            return fromMultimap.isEmpty();
        }

        @Override
        public Set<K> keySet() {
            return fromMultimap.keySet();
        }

        @Override
        public Multiset<K> keys() {
            return fromMultimap.keys();
        }

        @Override
        public boolean put(K key, V2 value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean putAll(K key, Iterable<? extends V2> values) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean putAll(
                Multimap<? extends K, ? extends V2> multimap) {
            throw new UnsupportedOperationException();
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean remove(Object key, Object value) {
            return get((K) key).remove(value);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Collection<V2> removeAll(Object key) {
            return transform((K) key, fromMultimap.removeAll(key));
        }

        @Override
        public Collection<V2> replaceValues(
                K key, Iterable<? extends V2> values) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return fromMultimap.size();
        }

        @Override
        Collection<V2> createValues() {
            return Collections2.transform(
                    fromMultimap.entries(), Maps.asEntryToValueFunction(transformer));
        }
    }


    private static final class TransformedEntriesListMultimap<K, V1, V2>
            extends TransformedEntriesMultimap<K, V1, V2>
            implements ListMultimap<K, V2> {

        TransformedEntriesListMultimap(ListMultimap<K, V1> fromMultimap,
                                       EntryTransformer<? super K, ? super V1, V2> transformer) {
            super(fromMultimap, transformer);
        }

        @Override
        List<V2> transform(K key, Collection<V1> values) {
            return Lists.transform((List<V1>) values, Maps.asValueToValueFunction(transformer, key));
        }

        @Override
        public List<V2> get(K key) {
            return transform(key, fromMultimap.get(key));
        }

        @SuppressWarnings("unchecked")
        @Override
        public List<V2> removeAll(Object key) {
            return transform((K) key, fromMultimap.removeAll(key));
        }

        @Override
        public List<V2> replaceValues(
                K key, Iterable<? extends V2> values) {
            throw new UnsupportedOperationException();
        }
    }


    static class Keys<K, V> extends AbstractMultiset<K> {
        @Weak
        final Multimap<K, V> multimap;

        Keys(Multimap<K, V> multimap) {
            this.multimap = multimap;
        }

        @Override
        Iterator<Multiset.Entry<K>> entryIterator() {
            return new TransformedIterator<Map.Entry<K, Collection<V>>, Multiset.Entry<K>>(
                    multimap.asMap().entrySet().iterator()) {
                @Override
                Multiset.Entry<K> transform(
                        final Map.Entry<K, Collection<V>> backingEntry) {
                    return new Multisets.AbstractEntry<K>() {
                        @Override
                        public K getElement() {
                            return backingEntry.getKey();
                        }

                        @Override
                        public int getCount() {
                            return backingEntry.getValue().size();
                        }
                    };
                }
            };
        }

        @Override
        int distinctElements() {
            return multimap.asMap().size();
        }

        @Override
        Set<Multiset.Entry<K>> createEntrySet() {
            return new KeysEntrySet();
        }

        @WeakOuter
        class KeysEntrySet extends Multisets.EntrySet<K> {
            @Override
            Multiset<K> multiset() {
                return Keys.this;
            }

            @Override
            public Iterator<Multiset.Entry<K>> iterator() {
                return entryIterator();
            }

            @Override
            public int size() {
                return distinctElements();
            }

            @Override
            public boolean isEmpty() {
                return multimap.isEmpty();
            }

            @Override
            public boolean contains(@Nullable Object o) {
                if (o instanceof Multiset.Entry) {
                    Multiset.Entry<?> entry = (Multiset.Entry<?>) o;
                    Collection<V> collection = multimap.asMap().get(entry.getElement());
                    return collection != null && collection.size() == entry.getCount();
                }
                return false;
            }

            @Override
            public boolean remove(@Nullable Object o) {
                if (o instanceof Multiset.Entry) {
                    Multiset.Entry<?> entry = (Multiset.Entry<?>) o;
                    Collection<V> collection = multimap.asMap().get(entry.getElement());
                    if (collection != null && collection.size() == entry.getCount()) {
                        collection.clear();
                        return true;
                    }
                }
                return false;
            }
        }

        @Override
        public boolean contains(@Nullable Object element) {
            return multimap.containsKey(element);
        }

        @Override
        public Iterator<K> iterator() {
            return Maps.keyIterator(multimap.entries().iterator());
        }

        @Override
        public int count(@Nullable Object element) {
            Collection<V> values = Maps.safeGet(multimap.asMap(), element);
            return (values == null) ? 0 : values.size();
        }

        @Override
        public int remove(@Nullable Object element, int occurrences) {
            checkNonnegative(occurrences, "occurrences");
            if (occurrences == 0) {
                return count(element);
            }

            Collection<V> values = Maps.safeGet(multimap.asMap(), element);

            if (values == null) {
                return 0;
            }

            int oldCount = values.size();
            if (occurrences >= oldCount) {
                values.clear();
            } else {
                Iterator<V> iterator = values.iterator();
                for (int i = 0; i < occurrences; i++) {
                    iterator.next();
                    iterator.remove();
                }
            }
            return oldCount;
        }

        @Override
        public void clear() {
            multimap.clear();
        }

        @Override
        public Set<K> elementSet() {
            return multimap.keySet();
        }
    }


    abstract static class Entries<K, V> extends
            AbstractCollection<Map.Entry<K, V>> {
        abstract Multimap<K, V> multimap();

        @Override
        public int size() {
            return multimap().size();
        }

        @Override
        public boolean contains(@Nullable Object o) {
            if (o instanceof Map.Entry) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
                return multimap().containsEntry(entry.getKey(), entry.getValue());
            }
            return false;
        }

        @Override
        public boolean remove(@Nullable Object o) {
            if (o instanceof Map.Entry) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
                return multimap().remove(entry.getKey(), entry.getValue());
            }
            return false;
        }

        @Override
        public void clear() {
            multimap().clear();
        }
    }


    static boolean equalsImpl(Multimap<?, ?> multimap, @Nullable Object object) {
        if (object == multimap) {
            return true;
        }
        if (object instanceof Multimap) {
            Multimap<?, ?> that = (Multimap<?, ?>) object;
            return multimap.asMap().equals(that.asMap());
        }
        return false;
    }

    // TODO(jlevy): Create methods that filter a SortedSetMultimap.
}
