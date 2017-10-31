

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;
import com.google.common.collect.Multiset.Entry;
import com.google.common.primitives.Ints;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.CollectPreconditions.checkNonnegative;
import static com.google.common.collect.CollectPreconditions.checkRemove;


@GwtCompatible
public final class Multisets {
    private Multisets() {
    }


    static class UnmodifiableMultiset<E>
            extends ForwardingMultiset<E> implements Serializable {
        final Multiset<? extends E> delegate;

        UnmodifiableMultiset(Multiset<? extends E> delegate) {
            this.delegate = delegate;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected Multiset<E> delegate() {
            // This is safe because all non-covariant methods are overriden
            return (Multiset<E>) delegate;
        }

        transient Set<E> elementSet;

        Set<E> createElementSet() {
            return Collections.unmodifiableSet(delegate.elementSet());
        }

        @Override
        public Set<E> elementSet() {
            Set<E> es = elementSet;
            return (es == null) ? elementSet = createElementSet() : es;
        }

        transient Set<Multiset.Entry<E>> entrySet;

        @SuppressWarnings("unchecked")
        @Override
        public Set<Multiset.Entry<E>> entrySet() {
            Set<Multiset.Entry<E>> es = entrySet;
            return (es == null)
                    // Safe because the returned set is made unmodifiable and Entry
                    // itself is readonly
                    ? entrySet = (Set) Collections.unmodifiableSet(delegate.entrySet())
                    : es;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Iterator<E> iterator() {
            // Safe because the returned Iterator is made unmodifiable
            return (Iterator<E>) Iterators.unmodifiableIterator(delegate.iterator());
        }

        @Override
        public boolean add(E element) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int add(E element, int occurences) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends E> elementsToAdd) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object element) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int remove(Object element, int occurrences) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> elementsToRemove) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> elementsToRetain) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int setCount(E element, int count) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean setCount(E element, int oldCount, int newCount) {
            throw new UnsupportedOperationException();
        }

        private static final long serialVersionUID = 0;
    }


    @Beta
    public static <E> SortedMultiset<E> unmodifiableSortedMultiset(
            SortedMultiset<E> sortedMultiset) {
        // it's in its own file so it can be emulated for GWT
        return new UnmodifiableSortedMultiset<>(checkNotNull(sortedMultiset));
    }


    public static <E> Multiset.Entry<E> immutableEntry(@Nullable E e, int n) {
        return new ImmutableEntry<>(e, n);
    }

    static class ImmutableEntry<E> extends AbstractEntry<E> implements
            Serializable {
        @Nullable
        private final E element;
        private final int count;

        ImmutableEntry(@Nullable E element, int count) {
            this.element = element;
            this.count = count;
            checkNonnegative(count, "count");
        }

        @Override
        @Nullable
        public final E getElement() {
            return element;
        }

        @Override
        public final int getCount() {
            return count;
        }

        public ImmutableEntry<E> nextInBucket() {
            return null;
        }

        private static final long serialVersionUID = 0;
    }


    static int inferDistinctElements(Iterable<?> elements) {
        if (elements instanceof Multiset) {
            return ((Multiset<?>) elements).elementSet().size();
        }
        return 11; // initial capacity will be rounded up to 16
    }


    abstract static class AbstractEntry<E> implements Multiset.Entry<E> {

        @Override
        public boolean equals(@Nullable Object object) {
            if (object instanceof Multiset.Entry) {
                Multiset.Entry<?> that = (Multiset.Entry<?>) object;
                return this.getCount() == that.getCount()
                        && Objects.equal(this.getElement(), that.getElement());
            }
            return false;
        }


        @Override
        public int hashCode() {
            E e = getElement();
            return ((e == null) ? 0 : e.hashCode()) ^ getCount();
        }


        @Override
        public String toString() {
            String text = String.valueOf(getElement());
            int n = getCount();
            return (n == 1) ? text : (text + " x " + n);
        }
    }


    static boolean equalsImpl(Multiset<?> multiset, @Nullable Object object) {
        if (object == multiset) {
            return true;
        }
        if (object instanceof Multiset) {
            Multiset<?> that = (Multiset<?>) object;


            if (multiset.size() != that.size()
                    || multiset.entrySet().size() != that.entrySet().size()) {
                return false;
            }
            for (Entry<?> entry : that.entrySet()) {
                if (multiset.count(entry.getElement()) != entry.getCount()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }


    static <E> boolean addAllImpl(
            Multiset<E> self, Collection<? extends E> elements) {
        if (elements.isEmpty()) {
            return false;
        }
        if (elements instanceof Multiset) {
            Multiset<? extends E> that = cast(elements);
            for (Entry<? extends E> entry : that.entrySet()) {
                self.add(entry.getElement(), entry.getCount());
            }
        } else {
            Iterators.addAll(self, elements.iterator());
        }
        return true;
    }


    static boolean removeAllImpl(
            Multiset<?> self, Collection<?> elementsToRemove) {
        Collection<?> collection = (elementsToRemove instanceof Multiset)
                ? ((Multiset<?>) elementsToRemove).elementSet() : elementsToRemove;

        return self.elementSet().removeAll(collection);
    }


    static boolean retainAllImpl(
            Multiset<?> self, Collection<?> elementsToRetain) {
        checkNotNull(elementsToRetain);
        Collection<?> collection = (elementsToRetain instanceof Multiset)
                ? ((Multiset<?>) elementsToRetain).elementSet() : elementsToRetain;

        return self.elementSet().retainAll(collection);
    }


    static <E> int setCountImpl(Multiset<E> self, E element, int count) {
        checkNonnegative(count, "count");

        int oldCount = self.count(element);

        int delta = count - oldCount;
        if (delta > 0) {
            self.add(element, delta);
        } else if (delta < 0) {
            self.remove(element, -delta);
        }

        return oldCount;
    }


    static <E> boolean setCountImpl(
            Multiset<E> self, E element, int oldCount, int newCount) {
        checkNonnegative(oldCount, "oldCount");
        checkNonnegative(newCount, "newCount");

        if (self.count(element) == oldCount) {
            self.setCount(element, newCount);
            return true;
        } else {
            return false;
        }
    }

    abstract static class ElementSet<E> extends Sets.ImprovedAbstractSet<E> {
        abstract Multiset<E> multiset();

        @Override
        public void clear() {
            multiset().clear();
        }

        @Override
        public boolean contains(Object o) {
            return multiset().contains(o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return multiset().containsAll(c);
        }

        @Override
        public boolean isEmpty() {
            return multiset().isEmpty();
        }

        @Override
        public Iterator<E> iterator() {
            return new TransformedIterator<Entry<E>, E>(multiset().entrySet().iterator()) {
                @Override
                E transform(Entry<E> entry) {
                    return entry.getElement();
                }
            };
        }

        @Override
        public boolean remove(Object o) {
            return multiset().remove(o, Integer.MAX_VALUE) > 0;
        }

        @Override
        public int size() {
            return multiset().entrySet().size();
        }
    }

    abstract static class EntrySet<E> extends Sets.ImprovedAbstractSet<Entry<E>> {
        abstract Multiset<E> multiset();

        @Override
        public boolean contains(@Nullable Object o) {
            if (o instanceof Entry) {

                @SuppressWarnings("cast")
                Entry<?> entry = (Entry<?>) o;
                if (entry.getCount() <= 0) {
                    return false;
                }
                int count = multiset().count(entry.getElement());
                return count == entry.getCount();

            }
            return false;
        }

        // GWT compiler warning; see contains().
        @SuppressWarnings("cast")
        @Override
        public boolean remove(Object object) {
            if (object instanceof Multiset.Entry) {
                Entry<?> entry = (Entry<?>) object;
                Object element = entry.getElement();
                int entryCount = entry.getCount();
                if (entryCount != 0) {
                    // Safe as long as we never add a new entry, which we won't.
                    @SuppressWarnings("unchecked")
                    Multiset<Object> multiset = (Multiset) multiset();
                    return multiset.setCount(element, entryCount, 0);
                }
            }
            return false;
        }

        @Override
        public void clear() {
            multiset().clear();
        }
    }


    static <E> Iterator<E> iteratorImpl(Multiset<E> multiset) {
        return new MultisetIteratorImpl<>(
                multiset, multiset.entrySet().iterator());
    }

    static final class MultisetIteratorImpl<E> implements Iterator<E> {
        private final Multiset<E> multiset;
        private final Iterator<Entry<E>> entryIterator;
        private Entry<E> currentEntry;

        private int laterCount;

        private int totalCount;
        private boolean canRemove;

        MultisetIteratorImpl(
                Multiset<E> multiset, Iterator<Entry<E>> entryIterator) {
            this.multiset = multiset;
            this.entryIterator = entryIterator;
        }

        @Override
        public boolean hasNext() {
            return laterCount > 0 || entryIterator.hasNext();
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            if (laterCount == 0) {
                currentEntry = entryIterator.next();
                totalCount = laterCount = currentEntry.getCount();
            }
            laterCount--;
            canRemove = true;
            return currentEntry.getElement();
        }

        @Override
        public void remove() {
            checkRemove(canRemove);
            if (totalCount == 1) {
                entryIterator.remove();
            } else {
                multiset.remove(currentEntry.getElement());
            }
            totalCount--;
            canRemove = false;
        }
    }


    static int sizeImpl(Multiset<?> multiset) {
        long size = 0;
        for (Entry<?> entry : multiset.entrySet()) {
            size += entry.getCount();
        }
        return Ints.saturatedCast(size);
    }


    static <T> Multiset<T> cast(Iterable<T> iterable) {
        return (Multiset<T>) iterable;
    }

    private static final Ordering<Entry<?>> DECREASING_COUNT_ORDERING = new Ordering<Entry<?>>() {
        @Override
        public int compare(Entry<?> entry1, Entry<?> entry2) {
            return Ints.compare(entry2.getCount(), entry1.getCount());
        }
    };


}
