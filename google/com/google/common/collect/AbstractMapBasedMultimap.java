

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.WeakOuter;
import com.google.common.collect.Maps.ViewCachingAbstractMap;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.CollectPreconditions.checkRemove;


@GwtCompatible(emulated = true)
abstract class AbstractMapBasedMultimap<K, V> extends AbstractMultimap<K, V>
        implements Serializable {


    private transient Map<K, Collection<V>> map;
    private transient int totalSize;


    protected AbstractMapBasedMultimap(Map<K, Collection<V>> map) {
        checkArgument(map.isEmpty());
        this.map = map;
    }


    final void setMap(Map<K, Collection<V>> map) {
        this.map = map;
        totalSize = 0;
        for (Collection<V> values : map.values()) {
            checkArgument(!values.isEmpty());
            totalSize += values.size();
        }
    }


    Collection<V> createUnmodifiableEmptyCollection() {
        return unmodifiableCollectionSubclass(createCollection());
    }


    abstract Collection<V> createCollection();


    Collection<V> createCollection(@Nullable K key) {
        return createCollection();
    }

    Map<K, Collection<V>> backingMap() {
        return map;
    }

    // Query Operations

    @Override
    public int size() {
        return totalSize;
    }

    @Override
    public boolean containsKey(@Nullable Object key) {
        return map.containsKey(key);
    }

    // Modification Operations

    @Override
    public boolean put(@Nullable K key, @Nullable V value) {
        Collection<V> collection = map.get(key);
        if (collection == null) {
            collection = createCollection(key);
            if (collection.add(value)) {
                totalSize++;
                map.put(key, collection);
                return true;
            } else {
                throw new AssertionError("New Collection violated the Collection spec");
            }
        } else if (collection.add(value)) {
            totalSize++;
            return true;
        } else {
            return false;
        }
    }

    private Collection<V> getOrCreateCollection(@Nullable K key) {
        Collection<V> collection = map.get(key);
        if (collection == null) {
            collection = createCollection(key);
            map.put(key, collection);
        }
        return collection;
    }

    // Bulk Operations


    @Override
    public Collection<V> replaceValues(@Nullable K key, Iterable<? extends V> values) {
        Iterator<? extends V> iterator = values.iterator();
        if (!iterator.hasNext()) {
            return removeAll(key);
        }

        // TODO(lowasser): investigate atomic failure?
        Collection<V> collection = getOrCreateCollection(key);
        Collection<V> oldValues = createCollection();
        oldValues.addAll(collection);

        totalSize -= collection.size();
        collection.clear();

        while (iterator.hasNext()) {
            if (collection.add(iterator.next())) {
                totalSize++;
            }
        }

        return unmodifiableCollectionSubclass(oldValues);
    }


    @Override
    public Collection<V> removeAll(@Nullable Object key) {
        Collection<V> collection = map.remove(key);

        if (collection == null) {
            return createUnmodifiableEmptyCollection();
        }

        Collection<V> output = createCollection();
        output.addAll(collection);
        totalSize -= collection.size();
        collection.clear();

        return unmodifiableCollectionSubclass(output);
    }

    Collection<V> unmodifiableCollectionSubclass(Collection<V> collection) {
        // We don't deal with NavigableSet here yet for GWT reasons -- instead,
        // non-GWT TreeMultimap explicitly overrides this and uses NavigableSet.
        if (collection instanceof SortedSet) {
            return Collections.unmodifiableSortedSet((SortedSet<V>) collection);
        } else if (collection instanceof Set) {
            return Collections.unmodifiableSet((Set<V>) collection);
        } else if (collection instanceof List) {
            return Collections.unmodifiableList((List<V>) collection);
        } else {
            return Collections.unmodifiableCollection(collection);
        }
    }

    @Override
    public void clear() {
        // Clear each collection, to make previously returned collections empty.
        for (Collection<V> collection : map.values()) {
            collection.clear();
        }
        map.clear();
        totalSize = 0;
    }

    // Views


    @Override
    public Collection<V> get(@Nullable K key) {
        Collection<V> collection = map.get(key);
        if (collection == null) {
            collection = createCollection(key);
        }
        return wrapCollection(key, collection);
    }


    Collection<V> wrapCollection(@Nullable K key, Collection<V> collection) {
        // We don't deal with NavigableSet here yet for GWT reasons -- instead,
        // non-GWT TreeMultimap explicitly overrides this and uses NavigableSet.
        if (collection instanceof SortedSet) {
            return new WrappedSortedSet(key, (SortedSet<V>) collection, null);
        } else if (collection instanceof Set) {
            return new WrappedSet(key, (Set<V>) collection);
        } else if (collection instanceof List) {
            return wrapList(key, (List<V>) collection, null);
        } else {
            return new WrappedCollection(key, collection, null);
        }
    }

    private List<V> wrapList(@Nullable K key, List<V> list, @Nullable WrappedCollection ancestor) {
        return (list instanceof RandomAccess)
                ? new RandomAccessWrappedList(key, list, ancestor)
                : new WrappedList(key, list, ancestor);
    }


    @WeakOuter
    private class WrappedCollection extends AbstractCollection<V> {
        final K key;
        Collection<V> delegate;
        final WrappedCollection ancestor;
        final Collection<V> ancestorDelegate;

        WrappedCollection(
                @Nullable K key, Collection<V> delegate, @Nullable WrappedCollection ancestor) {
            this.key = key;
            this.delegate = delegate;
            this.ancestor = ancestor;
            this.ancestorDelegate = (ancestor == null) ? null : ancestor.getDelegate();
        }


        void refreshIfEmpty() {
            if (ancestor != null) {
                ancestor.refreshIfEmpty();
                if (ancestor.getDelegate() != ancestorDelegate) {
                    throw new ConcurrentModificationException();
                }
            } else if (delegate.isEmpty()) {
                Collection<V> newDelegate = map.get(key);
                if (newDelegate != null) {
                    delegate = newDelegate;
                }
            }
        }


        void removeIfEmpty() {
            if (ancestor != null) {
                ancestor.removeIfEmpty();
            } else if (delegate.isEmpty()) {
                map.remove(key);
            }
        }

        K getKey() {
            return key;
        }


        void addToMap() {
            if (ancestor != null) {
                ancestor.addToMap();
            } else {
                map.put(key, delegate);
            }
        }

        @Override
        public int size() {
            refreshIfEmpty();
            return delegate.size();
        }

        @Override
        public boolean equals(@Nullable Object object) {
            if (object == this) {
                return true;
            }
            refreshIfEmpty();
            return delegate.equals(object);
        }

        @Override
        public int hashCode() {
            refreshIfEmpty();
            return delegate.hashCode();
        }

        @Override
        public String toString() {
            refreshIfEmpty();
            return delegate.toString();
        }

        Collection<V> getDelegate() {
            return delegate;
        }

        @Override
        public Iterator<V> iterator() {
            refreshIfEmpty();
            return new WrappedIterator();
        }


        class WrappedIterator implements Iterator<V> {
            final Iterator<V> delegateIterator;
            final Collection<V> originalDelegate = delegate;

            WrappedIterator() {
                delegateIterator = iteratorOrListIterator(delegate);
            }

            WrappedIterator(Iterator<V> delegateIterator) {
                this.delegateIterator = delegateIterator;
            }


            void validateIterator() {
                refreshIfEmpty();
                if (delegate != originalDelegate) {
                    throw new ConcurrentModificationException();
                }
            }

            @Override
            public boolean hasNext() {
                validateIterator();
                return delegateIterator.hasNext();
            }

            @Override
            public V next() {
                validateIterator();
                return delegateIterator.next();
            }

            @Override
            public void remove() {
                delegateIterator.remove();
                totalSize--;
                removeIfEmpty();
            }

            Iterator<V> getDelegateIterator() {
                validateIterator();
                return delegateIterator;
            }
        }

        @Override
        public boolean add(V value) {
            refreshIfEmpty();
            boolean wasEmpty = delegate.isEmpty();
            boolean changed = delegate.add(value);
            if (changed) {
                totalSize++;
                if (wasEmpty) {
                    addToMap();
                }
            }
            return changed;
        }

        WrappedCollection getAncestor() {
            return ancestor;
        }

        // The following methods are provided for better performance.

        @Override
        public boolean addAll(Collection<? extends V> collection) {
            if (collection.isEmpty()) {
                return false;
            }
            int oldSize = size(); // calls refreshIfEmpty
            boolean changed = delegate.addAll(collection);
            if (changed) {
                int newSize = delegate.size();
                totalSize += (newSize - oldSize);
                if (oldSize == 0) {
                    addToMap();
                }
            }
            return changed;
        }

        @Override
        public boolean contains(Object o) {
            refreshIfEmpty();
            return delegate.contains(o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            refreshIfEmpty();
            return delegate.containsAll(c);
        }

        @Override
        public void clear() {
            int oldSize = size(); // calls refreshIfEmpty
            if (oldSize == 0) {
                return;
            }
            delegate.clear();
            totalSize -= oldSize;
            removeIfEmpty(); // maybe shouldn't be removed if this is a sublist
        }

        @Override
        public boolean remove(Object o) {
            refreshIfEmpty();
            boolean changed = delegate.remove(o);
            if (changed) {
                totalSize--;
                removeIfEmpty();
            }
            return changed;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            if (c.isEmpty()) {
                return false;
            }
            int oldSize = size(); // calls refreshIfEmpty
            boolean changed = delegate.removeAll(c);
            if (changed) {
                int newSize = delegate.size();
                totalSize += (newSize - oldSize);
                removeIfEmpty();
            }
            return changed;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            checkNotNull(c);
            int oldSize = size(); // calls refreshIfEmpty
            boolean changed = delegate.retainAll(c);
            if (changed) {
                int newSize = delegate.size();
                totalSize += (newSize - oldSize);
                removeIfEmpty();
            }
            return changed;
        }
    }

    private Iterator<V> iteratorOrListIterator(Collection<V> collection) {
        return (collection instanceof List)
                ? ((List<V>) collection).listIterator()
                : collection.iterator();
    }


    @WeakOuter
    private class WrappedSet extends WrappedCollection implements Set<V> {
        WrappedSet(@Nullable K key, Set<V> delegate) {
            super(key, delegate, null);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            if (c.isEmpty()) {
                return false;
            }
            int oldSize = size(); // calls refreshIfEmpty

            // Guava issue 1013: AbstractSet and most JDK set implementations are
            // susceptible to quadratic removeAll performance on lists;
            // use a slightly smarter implementation here
            boolean changed = Sets.removeAllImpl((Set<V>) delegate, c);
            if (changed) {
                int newSize = delegate.size();
                totalSize += (newSize - oldSize);
                removeIfEmpty();
            }
            return changed;
        }
    }


    @WeakOuter
    private class WrappedSortedSet extends WrappedCollection implements SortedSet<V> {
        WrappedSortedSet(@Nullable K key, SortedSet<V> delegate, @Nullable WrappedCollection ancestor) {
            super(key, delegate, ancestor);
        }

        SortedSet<V> getSortedSetDelegate() {
            return (SortedSet<V>) getDelegate();
        }

        @Override
        public Comparator<? super V> comparator() {
            return getSortedSetDelegate().comparator();
        }

        @Override
        public V first() {
            refreshIfEmpty();
            return getSortedSetDelegate().first();
        }

        @Override
        public V last() {
            refreshIfEmpty();
            return getSortedSetDelegate().last();
        }

        @Override
        public SortedSet<V> headSet(V toElement) {
            refreshIfEmpty();
            return new WrappedSortedSet(
                    getKey(),
                    getSortedSetDelegate().headSet(toElement),
                    (getAncestor() == null) ? this : getAncestor());
        }

        @Override
        public SortedSet<V> subSet(V fromElement, V toElement) {
            refreshIfEmpty();
            return new WrappedSortedSet(
                    getKey(),
                    getSortedSetDelegate().subSet(fromElement, toElement),
                    (getAncestor() == null) ? this : getAncestor());
        }

        @Override
        public SortedSet<V> tailSet(V fromElement) {
            refreshIfEmpty();
            return new WrappedSortedSet(
                    getKey(),
                    getSortedSetDelegate().tailSet(fromElement),
                    (getAncestor() == null) ? this : getAncestor());
        }
    }

    @GwtIncompatible("NavigableSet")
    @WeakOuter
    class WrappedNavigableSet extends WrappedSortedSet implements NavigableSet<V> {
        WrappedNavigableSet(
                @Nullable K key, NavigableSet<V> delegate, @Nullable WrappedCollection ancestor) {
            super(key, delegate, ancestor);
        }

        @Override
        NavigableSet<V> getSortedSetDelegate() {
            return (NavigableSet<V>) super.getSortedSetDelegate();
        }

        @Override
        public V lower(V v) {
            return getSortedSetDelegate().lower(v);
        }

        @Override
        public V floor(V v) {
            return getSortedSetDelegate().floor(v);
        }

        @Override
        public V ceiling(V v) {
            return getSortedSetDelegate().ceiling(v);
        }

        @Override
        public V higher(V v) {
            return getSortedSetDelegate().higher(v);
        }

        @Override
        public V pollFirst() {
            return Iterators.pollNext(iterator());
        }

        @Override
        public V pollLast() {
            return Iterators.pollNext(descendingIterator());
        }

        private NavigableSet<V> wrap(NavigableSet<V> wrapped) {
            return new WrappedNavigableSet(key, wrapped, (getAncestor() == null) ? this : getAncestor());
        }

        @Override
        public NavigableSet<V> descendingSet() {
            return wrap(getSortedSetDelegate().descendingSet());
        }

        @Override
        public Iterator<V> descendingIterator() {
            return new WrappedIterator(getSortedSetDelegate().descendingIterator());
        }

        @Override
        public NavigableSet<V> subSet(
                V fromElement, boolean fromInclusive, V toElement, boolean toInclusive) {
            return wrap(
                    getSortedSetDelegate().subSet(fromElement, fromInclusive, toElement, toInclusive));
        }

        @Override
        public NavigableSet<V> headSet(V toElement, boolean inclusive) {
            return wrap(getSortedSetDelegate().headSet(toElement, inclusive));
        }

        @Override
        public NavigableSet<V> tailSet(V fromElement, boolean inclusive) {
            return wrap(getSortedSetDelegate().tailSet(fromElement, inclusive));
        }
    }


    @WeakOuter
    private class WrappedList extends WrappedCollection implements List<V> {
        WrappedList(@Nullable K key, List<V> delegate, @Nullable WrappedCollection ancestor) {
            super(key, delegate, ancestor);
        }

        List<V> getListDelegate() {
            return (List<V>) getDelegate();
        }

        @Override
        public boolean addAll(int index, Collection<? extends V> c) {
            if (c.isEmpty()) {
                return false;
            }
            int oldSize = size(); // calls refreshIfEmpty
            boolean changed = getListDelegate().addAll(index, c);
            if (changed) {
                int newSize = getDelegate().size();
                totalSize += (newSize - oldSize);
                if (oldSize == 0) {
                    addToMap();
                }
            }
            return changed;
        }

        @Override
        public V get(int index) {
            refreshIfEmpty();
            return getListDelegate().get(index);
        }

        @Override
        public V set(int index, V element) {
            refreshIfEmpty();
            return getListDelegate().set(index, element);
        }

        @Override
        public void add(int index, V element) {
            refreshIfEmpty();
            boolean wasEmpty = getDelegate().isEmpty();
            getListDelegate().add(index, element);
            totalSize++;
            if (wasEmpty) {
                addToMap();
            }
        }

        @Override
        public V remove(int index) {
            refreshIfEmpty();
            V value = getListDelegate().remove(index);
            totalSize--;
            removeIfEmpty();
            return value;
        }

        @Override
        public int indexOf(Object o) {
            refreshIfEmpty();
            return getListDelegate().indexOf(o);
        }

        @Override
        public int lastIndexOf(Object o) {
            refreshIfEmpty();
            return getListDelegate().lastIndexOf(o);
        }

        @Override
        public ListIterator<V> listIterator() {
            refreshIfEmpty();
            return new WrappedListIterator();
        }

        @Override
        public ListIterator<V> listIterator(int index) {
            refreshIfEmpty();
            return new WrappedListIterator(index);
        }

        @Override
        public List<V> subList(int fromIndex, int toIndex) {
            refreshIfEmpty();
            return wrapList(
                    getKey(),
                    getListDelegate().subList(fromIndex, toIndex),
                    (getAncestor() == null) ? this : getAncestor());
        }


        private class WrappedListIterator extends WrappedIterator implements ListIterator<V> {
            WrappedListIterator() {
            }

            public WrappedListIterator(int index) {
                super(getListDelegate().listIterator(index));
            }

            private ListIterator<V> getDelegateListIterator() {
                return (ListIterator<V>) getDelegateIterator();
            }

            @Override
            public boolean hasPrevious() {
                return getDelegateListIterator().hasPrevious();
            }

            @Override
            public V previous() {
                return getDelegateListIterator().previous();
            }

            @Override
            public int nextIndex() {
                return getDelegateListIterator().nextIndex();
            }

            @Override
            public int previousIndex() {
                return getDelegateListIterator().previousIndex();
            }

            @Override
            public void set(V value) {
                getDelegateListIterator().set(value);
            }

            @Override
            public void add(V value) {
                boolean wasEmpty = isEmpty();
                getDelegateListIterator().add(value);
                totalSize++;
                if (wasEmpty) {
                    addToMap();
                }
            }
        }
    }


    private class RandomAccessWrappedList extends WrappedList implements RandomAccess {
        RandomAccessWrappedList(
                @Nullable K key, List<V> delegate, @Nullable WrappedCollection ancestor) {
            super(key, delegate, ancestor);
        }
    }

    @Override
    Set<K> createKeySet() {
        // TreeMultimap uses NavigableKeySet explicitly, but we don't handle that here for GWT
        // compatibility reasons
        return (map instanceof SortedMap)
                ? new SortedKeySet((SortedMap<K, Collection<V>>) map)
                : new KeySet(map);
    }

    @WeakOuter
    private class KeySet extends Maps.KeySet<K, Collection<V>> {
        KeySet(final Map<K, Collection<V>> subMap) {
            super(subMap);
        }

        @Override
        public Iterator<K> iterator() {
            final Iterator<Map.Entry<K, Collection<V>>> entryIterator = map().entrySet().iterator();
            return new Iterator<K>() {
                Map.Entry<K, Collection<V>> entry;

                @Override
                public boolean hasNext() {
                    return entryIterator.hasNext();
                }

                @Override
                public K next() {
                    entry = entryIterator.next();
                    return entry.getKey();
                }

                @Override
                public void remove() {
                    checkRemove(entry != null);
                    Collection<V> collection = entry.getValue();
                    entryIterator.remove();
                    totalSize -= collection.size();
                    collection.clear();
                }
            };
        }

        // The following methods are included for better performance.

        @Override
        public boolean remove(Object key) {
            int count = 0;
            Collection<V> collection = map().remove(key);
            if (collection != null) {
                count = collection.size();
                collection.clear();
                totalSize -= count;
            }
            return count > 0;
        }

        @Override
        public void clear() {
            Iterators.clear(iterator());
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return map().keySet().containsAll(c);
        }

        @Override
        public boolean equals(@Nullable Object object) {
            return this == object || this.map().keySet().equals(object);
        }

        @Override
        public int hashCode() {
            return map().keySet().hashCode();
        }
    }

    @WeakOuter
    private class SortedKeySet extends KeySet implements SortedSet<K> {

        SortedKeySet(SortedMap<K, Collection<V>> subMap) {
            super(subMap);
        }

        SortedMap<K, Collection<V>> sortedMap() {
            return (SortedMap<K, Collection<V>>) super.map();
        }

        @Override
        public Comparator<? super K> comparator() {
            return sortedMap().comparator();
        }

        @Override
        public K first() {
            return sortedMap().firstKey();
        }

        @Override
        public SortedSet<K> headSet(K toElement) {
            return new SortedKeySet(sortedMap().headMap(toElement));
        }

        @Override
        public K last() {
            return sortedMap().lastKey();
        }

        @Override
        public SortedSet<K> subSet(K fromElement, K toElement) {
            return new SortedKeySet(sortedMap().subMap(fromElement, toElement));
        }

        @Override
        public SortedSet<K> tailSet(K fromElement) {
            return new SortedKeySet(sortedMap().tailMap(fromElement));
        }
    }

    @GwtIncompatible("NavigableSet")
    @WeakOuter
    class NavigableKeySet extends SortedKeySet implements NavigableSet<K> {
        NavigableKeySet(NavigableMap<K, Collection<V>> subMap) {
            super(subMap);
        }

        @Override
        NavigableMap<K, Collection<V>> sortedMap() {
            return (NavigableMap<K, Collection<V>>) super.sortedMap();
        }

        @Override
        public K lower(K k) {
            return sortedMap().lowerKey(k);
        }

        @Override
        public K floor(K k) {
            return sortedMap().floorKey(k);
        }

        @Override
        public K ceiling(K k) {
            return sortedMap().ceilingKey(k);
        }

        @Override
        public K higher(K k) {
            return sortedMap().higherKey(k);
        }

        @Override
        public K pollFirst() {
            return Iterators.pollNext(iterator());
        }

        @Override
        public K pollLast() {
            return Iterators.pollNext(descendingIterator());
        }

        @Override
        public NavigableSet<K> descendingSet() {
            return new NavigableKeySet(sortedMap().descendingMap());
        }

        @Override
        public Iterator<K> descendingIterator() {
            return descendingSet().iterator();
        }

        @Override
        public NavigableSet<K> headSet(K toElement) {
            return headSet(toElement, false);
        }

        @Override
        public NavigableSet<K> headSet(K toElement, boolean inclusive) {
            return new NavigableKeySet(sortedMap().headMap(toElement, inclusive));
        }

        @Override
        public NavigableSet<K> subSet(K fromElement, K toElement) {
            return subSet(fromElement, true, toElement, false);
        }

        @Override
        public NavigableSet<K> subSet(
                K fromElement, boolean fromInclusive, K toElement, boolean toInclusive) {
            return new NavigableKeySet(
                    sortedMap().subMap(fromElement, fromInclusive, toElement, toInclusive));
        }

        @Override
        public NavigableSet<K> tailSet(K fromElement) {
            return tailSet(fromElement, true);
        }

        @Override
        public NavigableSet<K> tailSet(K fromElement, boolean inclusive) {
            return new NavigableKeySet(sortedMap().tailMap(fromElement, inclusive));
        }
    }


    private int removeValuesForKey(Object key) {
        Collection<V> collection = Maps.safeRemove(map, key);

        int count = 0;
        if (collection != null) {
            count = collection.size();
            collection.clear();
            totalSize -= count;
        }
        return count;
    }

    private abstract class Itr<T> implements Iterator<T> {
        final Iterator<Map.Entry<K, Collection<V>>> keyIterator;
        K key;
        Collection<V> collection;
        Iterator<V> valueIterator;

        Itr() {
            keyIterator = map.entrySet().iterator();
            key = null;
            collection = null;
            valueIterator = Iterators.emptyModifiableIterator();
        }

        abstract T output(K key, V value);

        @Override
        public boolean hasNext() {
            return keyIterator.hasNext() || valueIterator.hasNext();
        }

        @Override
        public T next() {
            if (!valueIterator.hasNext()) {
                Map.Entry<K, Collection<V>> mapEntry = keyIterator.next();
                key = mapEntry.getKey();
                collection = mapEntry.getValue();
                valueIterator = collection.iterator();
            }
            return output(key, valueIterator.next());
        }

        @Override
        public void remove() {
            valueIterator.remove();
            if (collection.isEmpty()) {
                keyIterator.remove();
            }
            totalSize--;
        }
    }


    @Override
    public Collection<V> values() {
        return super.values();
    }

    @Override
    Iterator<V> valueIterator() {
        return new Itr<V>() {
            @Override
            V output(K key, V value) {
                return value;
            }
        };
    }


    @Override
    public Collection<Map.Entry<K, V>> entries() {
        return super.entries();
    }


    @Override
    Iterator<Map.Entry<K, V>> entryIterator() {
        return new Itr<Map.Entry<K, V>>() {
            @Override
            Entry<K, V> output(K key, V value) {
                return Maps.immutableEntry(key, value);
            }
        };
    }

    @Override
    Map<K, Collection<V>> createAsMap() {
        // TreeMultimap uses NavigableAsMap explicitly, but we don't handle that here for GWT
        // compatibility reasons
        return (map instanceof SortedMap)
                ? new SortedAsMap((SortedMap<K, Collection<V>>) map)
                : new AsMap(map);
    }

    @WeakOuter
    private class AsMap extends ViewCachingAbstractMap<K, Collection<V>> {

        final transient Map<K, Collection<V>> submap;

        AsMap(Map<K, Collection<V>> submap) {
            this.submap = submap;
        }

        @Override
        protected Set<Entry<K, Collection<V>>> createEntrySet() {
            return new AsMapEntries();
        }

        // The following methods are included for performance.

        @Override
        public boolean containsKey(Object key) {
            return Maps.safeContainsKey(submap, key);
        }

        @Override
        public Collection<V> get(Object key) {
            Collection<V> collection = Maps.safeGet(submap, key);
            if (collection == null) {
                return null;
            }
            @SuppressWarnings("unchecked")
            K k = (K) key;
            return wrapCollection(k, collection);
        }

        @Override
        public Set<K> keySet() {
            return AbstractMapBasedMultimap.this.keySet();
        }

        @Override
        public int size() {
            return submap.size();
        }

        @Override
        public Collection<V> remove(Object key) {
            Collection<V> collection = submap.remove(key);
            if (collection == null) {
                return null;
            }

            Collection<V> output = createCollection();
            output.addAll(collection);
            totalSize -= collection.size();
            collection.clear();
            return output;
        }

        @Override
        public boolean equals(@Nullable Object object) {
            return this == object || submap.equals(object);
        }

        @Override
        public int hashCode() {
            return submap.hashCode();
        }

        @Override
        public String toString() {
            return submap.toString();
        }

        @Override
        public void clear() {
            if (submap == map) {
                AbstractMapBasedMultimap.this.clear();
            } else {
                Iterators.clear(new AsMapIterator());
            }
        }

        Entry<K, Collection<V>> wrapEntry(Entry<K, Collection<V>> entry) {
            K key = entry.getKey();
            return Maps.immutableEntry(key, wrapCollection(key, entry.getValue()));
        }

        @WeakOuter
        class AsMapEntries extends Maps.EntrySet<K, Collection<V>> {
            @Override
            Map<K, Collection<V>> map() {
                return AsMap.this;
            }

            @Override
            public Iterator<Map.Entry<K, Collection<V>>> iterator() {
                return new AsMapIterator();
            }

            // The following methods are included for performance.

            @Override
            public boolean contains(Object o) {
                return Collections2.safeContains(submap.entrySet(), o);
            }

            @Override
            public boolean remove(Object o) {
                if (!contains(o)) {
                    return false;
                }
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
                removeValuesForKey(entry.getKey());
                return true;
            }
        }


        class AsMapIterator implements Iterator<Map.Entry<K, Collection<V>>> {
            final Iterator<Map.Entry<K, Collection<V>>> delegateIterator = submap.entrySet().iterator();
            Collection<V> collection;

            @Override
            public boolean hasNext() {
                return delegateIterator.hasNext();
            }

            @Override
            public Map.Entry<K, Collection<V>> next() {
                Map.Entry<K, Collection<V>> entry = delegateIterator.next();
                collection = entry.getValue();
                return wrapEntry(entry);
            }

            @Override
            public void remove() {
                delegateIterator.remove();
                totalSize -= collection.size();
                collection.clear();
            }
        }
    }

    @WeakOuter
    private class SortedAsMap extends AsMap implements SortedMap<K, Collection<V>> {
        SortedAsMap(SortedMap<K, Collection<V>> submap) {
            super(submap);
        }

        SortedMap<K, Collection<V>> sortedMap() {
            return (SortedMap<K, Collection<V>>) submap;
        }

        @Override
        public Comparator<? super K> comparator() {
            return sortedMap().comparator();
        }

        @Override
        public K firstKey() {
            return sortedMap().firstKey();
        }

        @Override
        public K lastKey() {
            return sortedMap().lastKey();
        }

        @Override
        public SortedMap<K, Collection<V>> headMap(K toKey) {
            return new SortedAsMap(sortedMap().headMap(toKey));
        }

        @Override
        public SortedMap<K, Collection<V>> subMap(K fromKey, K toKey) {
            return new SortedAsMap(sortedMap().subMap(fromKey, toKey));
        }

        @Override
        public SortedMap<K, Collection<V>> tailMap(K fromKey) {
            return new SortedAsMap(sortedMap().tailMap(fromKey));
        }

        SortedSet<K> sortedKeySet;

        // returns a SortedSet, even though returning a Set would be sufficient to
        // satisfy the SortedMap.keySet() interface
        @Override
        public SortedSet<K> keySet() {
            SortedSet<K> result = sortedKeySet;
            return (result == null) ? sortedKeySet = createKeySet() : result;
        }

        @Override
        SortedSet<K> createKeySet() {
            return new SortedKeySet(sortedMap());
        }
    }

    @GwtIncompatible("NavigableAsMap")
    class NavigableAsMap extends SortedAsMap implements NavigableMap<K, Collection<V>> {

        NavigableAsMap(NavigableMap<K, Collection<V>> submap) {
            super(submap);
        }

        @Override
        NavigableMap<K, Collection<V>> sortedMap() {
            return (NavigableMap<K, Collection<V>>) super.sortedMap();
        }

        @Override
        public Entry<K, Collection<V>> lowerEntry(K key) {
            Entry<K, Collection<V>> entry = sortedMap().lowerEntry(key);
            return (entry == null) ? null : wrapEntry(entry);
        }

        @Override
        public K lowerKey(K key) {
            return sortedMap().lowerKey(key);
        }

        @Override
        public Entry<K, Collection<V>> floorEntry(K key) {
            Entry<K, Collection<V>> entry = sortedMap().floorEntry(key);
            return (entry == null) ? null : wrapEntry(entry);
        }

        @Override
        public K floorKey(K key) {
            return sortedMap().floorKey(key);
        }

        @Override
        public Entry<K, Collection<V>> ceilingEntry(K key) {
            Entry<K, Collection<V>> entry = sortedMap().ceilingEntry(key);
            return (entry == null) ? null : wrapEntry(entry);
        }

        @Override
        public K ceilingKey(K key) {
            return sortedMap().ceilingKey(key);
        }

        @Override
        public Entry<K, Collection<V>> higherEntry(K key) {
            Entry<K, Collection<V>> entry = sortedMap().higherEntry(key);
            return (entry == null) ? null : wrapEntry(entry);
        }

        @Override
        public K higherKey(K key) {
            return sortedMap().higherKey(key);
        }

        @Override
        public Entry<K, Collection<V>> firstEntry() {
            Entry<K, Collection<V>> entry = sortedMap().firstEntry();
            return (entry == null) ? null : wrapEntry(entry);
        }

        @Override
        public Entry<K, Collection<V>> lastEntry() {
            Entry<K, Collection<V>> entry = sortedMap().lastEntry();
            return (entry == null) ? null : wrapEntry(entry);
        }

        @Override
        public Entry<K, Collection<V>> pollFirstEntry() {
            return pollAsMapEntry(entrySet().iterator());
        }

        @Override
        public Entry<K, Collection<V>> pollLastEntry() {
            return pollAsMapEntry(descendingMap().entrySet().iterator());
        }

        Map.Entry<K, Collection<V>> pollAsMapEntry(Iterator<Entry<K, Collection<V>>> entryIterator) {
            if (!entryIterator.hasNext()) {
                return null;
            }
            Entry<K, Collection<V>> entry = entryIterator.next();
            Collection<V> output = createCollection();
            output.addAll(entry.getValue());
            entryIterator.remove();
            return Maps.immutableEntry(entry.getKey(), unmodifiableCollectionSubclass(output));
        }

        @Override
        public NavigableMap<K, Collection<V>> descendingMap() {
            return new NavigableAsMap(sortedMap().descendingMap());
        }

        @Override
        public NavigableSet<K> keySet() {
            return (NavigableSet<K>) super.keySet();
        }

        @Override
        NavigableSet<K> createKeySet() {
            return new NavigableKeySet(sortedMap());
        }

        @Override
        public NavigableSet<K> navigableKeySet() {
            return keySet();
        }

        @Override
        public NavigableSet<K> descendingKeySet() {
            return descendingMap().navigableKeySet();
        }

        @Override
        public NavigableMap<K, Collection<V>> subMap(K fromKey, K toKey) {
            return subMap(fromKey, true, toKey, false);
        }

        @Override
        public NavigableMap<K, Collection<V>> subMap(
                K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
            return new NavigableAsMap(sortedMap().subMap(fromKey, fromInclusive, toKey, toInclusive));
        }

        @Override
        public NavigableMap<K, Collection<V>> headMap(K toKey) {
            return headMap(toKey, false);
        }

        @Override
        public NavigableMap<K, Collection<V>> headMap(K toKey, boolean inclusive) {
            return new NavigableAsMap(sortedMap().headMap(toKey, inclusive));
        }

        @Override
        public NavigableMap<K, Collection<V>> tailMap(K fromKey) {
            return tailMap(fromKey, true);
        }

        @Override
        public NavigableMap<K, Collection<V>> tailMap(K fromKey, boolean inclusive) {
            return new NavigableAsMap(sortedMap().tailMap(fromKey, inclusive));
        }
    }

    private static final long serialVersionUID = 2447537837011683357L;
}
