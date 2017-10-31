

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2.FilteredCollection;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;


@GwtCompatible(emulated = true)
public final class Sets {
    private Sets() {
    }


    abstract static class ImprovedAbstractSet<E> extends AbstractSet<E> {
        @Override
        public boolean removeAll(Collection<?> c) {
            return removeAllImpl(this, c);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return super.retainAll(checkNotNull(c)); // GWT compatibility
        }
    }


    // HashSet


    public static <E> HashSet<E> newHashSet() {
        return new HashSet<>();
    }


    public static <E> HashSet<E> newHashSetWithExpectedSize(int expectedSize) {
        return new HashSet<>(Maps.capacity(expectedSize));
    }


    public static <E> HashSet<E> newHashSet(Iterable<? extends E> elements) {
        return (elements instanceof Collection)
                ? new HashSet<>(Collections2.cast(elements))
                : newHashSet(elements.iterator());
    }


    public static <E> HashSet<E> newHashSet(Iterator<? extends E> elements) {
        HashSet<E> set = newHashSet();
        Iterators.addAll(set, elements);
        return set;
    }


    // LinkedHashSet


    public static <E> LinkedHashSet<E> newLinkedHashSet() {
        return new LinkedHashSet<>();
    }


    // TreeSet


    public abstract static class SetView<E> extends AbstractSet<E> {
        private SetView() {
        } // no subclasses but our own


        public ImmutableSet<E> immutableCopy() {
            return ImmutableSet.copyOf(this);
        }


        // Note: S should logically extend Set<? super E> but can't due to either
        // some javac bug or some weirdness in the spec, not sure which.
        public <S extends Set<E>> S copyInto(S set) {
            set.addAll(this);
            return set;
        }
    }


    // TODO(kevinb): how to omit that last sentence when building GWT javadoc?

    public static <E> Set<E> filter(
            Set<E> unfiltered, Predicate<? super E> predicate) {
        if (unfiltered instanceof SortedSet) {
            return filter((SortedSet<E>) unfiltered, predicate);
        }
        if (unfiltered instanceof FilteredSet) {
            // Support clear(), removeAll(), and retainAll() when filtering a filtered
            // collection.
            FilteredSet<E> filtered = (FilteredSet<E>) unfiltered;
            Predicate<E> combinedPredicate
                    = Predicates.and(filtered.predicate, predicate);
            return new FilteredSet<>(
                    (Set<E>) filtered.unfiltered, combinedPredicate);
        }

        return new FilteredSet<>(
                checkNotNull(unfiltered), checkNotNull(predicate));
    }

    private static class FilteredSet<E> extends FilteredCollection<E>
            implements Set<E> {
        FilteredSet(Set<E> unfiltered, Predicate<? super E> predicate) {
            super(unfiltered, predicate);
        }

        @Override
        public boolean equals(@Nullable Object object) {
            return equalsImpl(this, object);
        }

        @Override
        public int hashCode() {
            return hashCodeImpl(this);
        }
    }


    public static <E> SortedSet<E> filter(
            SortedSet<E> unfiltered, Predicate<? super E> predicate) {
        return Platform.setsFilterSortedSet(unfiltered, predicate);
    }

    static <E> SortedSet<E> filterSortedIgnoreNavigable(
            SortedSet<E> unfiltered, Predicate<? super E> predicate) {
        if (unfiltered instanceof FilteredSet) {
            // Support clear(), removeAll(), and retainAll() when filtering a filtered
            // collection.
            FilteredSet<E> filtered = (FilteredSet<E>) unfiltered;
            Predicate<E> combinedPredicate
                    = Predicates.and(filtered.predicate, predicate);
            return new FilteredSortedSet<>(
                    (SortedSet<E>) filtered.unfiltered, combinedPredicate);
        }

        return new FilteredSortedSet<>(
                checkNotNull(unfiltered), checkNotNull(predicate));
    }

    private static class FilteredSortedSet<E> extends FilteredSet<E>
            implements SortedSet<E> {

        FilteredSortedSet(SortedSet<E> unfiltered, Predicate<? super E> predicate) {
            super(unfiltered, predicate);
        }

        @Override
        public Comparator<? super E> comparator() {
            return ((SortedSet<E>) unfiltered).comparator();
        }

        @Override
        public SortedSet<E> subSet(E fromElement, E toElement) {
            return new FilteredSortedSet<>(((SortedSet<E>) unfiltered).subSet(fromElement, toElement),
                    predicate);
        }

        @Override
        public SortedSet<E> headSet(E toElement) {
            return new FilteredSortedSet<>(((SortedSet<E>) unfiltered).headSet(toElement), predicate);
        }

        @Override
        public SortedSet<E> tailSet(E fromElement) {
            return new FilteredSortedSet<>(((SortedSet<E>) unfiltered).tailSet(fromElement), predicate);
        }

        @Override
        public E first() {
            return iterator().next();
        }

        @Override
        public E last() {
            SortedSet<E> sortedUnfiltered = (SortedSet<E>) unfiltered;
            while (true) {
                E element = sortedUnfiltered.last();
                if (predicate.apply(element)) {
                    return element;
                }
                sortedUnfiltered = sortedUnfiltered.headSet(element);
            }
        }
    }


    @GwtIncompatible("NavigableSet")
    @SuppressWarnings("unchecked")

    public static <E> NavigableSet<E> filter(
            NavigableSet<E> unfiltered, Predicate<? super E> predicate) {
        if (unfiltered instanceof FilteredSet) {
            // Support clear(), removeAll(), and retainAll() when filtering a filtered
            // collection.
            FilteredSet<E> filtered = (FilteredSet<E>) unfiltered;
            Predicate<E> combinedPredicate
                    = Predicates.and(filtered.predicate, predicate);
            return new FilteredNavigableSet<>(
                    (NavigableSet<E>) filtered.unfiltered, combinedPredicate);
        }

        return new FilteredNavigableSet<>(
                checkNotNull(unfiltered), checkNotNull(predicate));
    }

    @GwtIncompatible("NavigableSet")
    private static class FilteredNavigableSet<E> extends FilteredSortedSet<E>
            implements NavigableSet<E> {
        FilteredNavigableSet(NavigableSet<E> unfiltered, Predicate<? super E> predicate) {
            super(unfiltered, predicate);
        }

        NavigableSet<E> unfiltered() {
            return (NavigableSet<E>) unfiltered;
        }

        @Override
        @Nullable
        public E lower(E e) {
            return Iterators.getNext(headSet(e, false).descendingIterator(), null);
        }

        @Override
        @Nullable
        public E floor(E e) {
            return Iterators.getNext(headSet(e, true).descendingIterator(), null);
        }

        @Override
        public E ceiling(E e) {
            return Iterables.getFirst(tailSet(e, true), null);
        }

        @Override
        public E higher(E e) {
            return Iterables.getFirst(tailSet(e, false), null);
        }

        @Override
        public E pollFirst() {
            return Iterables.removeFirstMatching(unfiltered(), predicate);
        }

        @Override
        public E pollLast() {
            return Iterables.removeFirstMatching(unfiltered().descendingSet(), predicate);
        }

        @Override
        public NavigableSet<E> descendingSet() {
            return Sets.filter(unfiltered().descendingSet(), predicate);
        }

        @Override
        public Iterator<E> descendingIterator() {
            return Iterators.filter(unfiltered().descendingIterator(), predicate);
        }

        @Override
        public E last() {
            return descendingIterator().next();
        }

        @Override
        public NavigableSet<E> subSet(
                E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
            return filter(
                    unfiltered().subSet(fromElement, fromInclusive, toElement, toInclusive), predicate);
        }

        @Override
        public NavigableSet<E> headSet(E toElement, boolean inclusive) {
            return filter(unfiltered().headSet(toElement, inclusive), predicate);
        }

        @Override
        public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
            return filter(unfiltered().tailSet(fromElement, inclusive), predicate);
        }
    }


    private static final class CartesianSet<E>
            extends ForwardingCollection<List<E>> implements Set<List<E>> {
        private transient final ImmutableList<ImmutableSet<E>> axes;
        private transient final CartesianList<E> delegate;

        static <E> Set<List<E>> create(List<? extends Set<? extends E>> sets) {
            ImmutableList.Builder<ImmutableSet<E>> axesBuilder =
                    new ImmutableList.Builder<>(sets.size());
            for (Set<? extends E> set : sets) {
                ImmutableSet<E> copy = ImmutableSet.copyOf(set);
                if (copy.isEmpty()) {
                    return ImmutableSet.of();
                }
                axesBuilder.add(copy);
            }
            final ImmutableList<ImmutableSet<E>> axes = axesBuilder.build();
            ImmutableList<List<E>> listAxes = new ImmutableList<List<E>>() {

                @Override
                public int size() {
                    return axes.size();
                }

                @Override
                public List<E> get(int index) {
                    return axes.get(index).asList();
                }

                @Override
                boolean isPartialView() {
                    return true;
                }
            };
            return new CartesianSet<>(axes, new CartesianList<>(listAxes));
        }

        private CartesianSet(
                ImmutableList<ImmutableSet<E>> axes, CartesianList<E> delegate) {
            this.axes = axes;
            this.delegate = delegate;
        }

        @Override
        protected Collection<List<E>> delegate() {
            return delegate;
        }

        @Override
        public boolean equals(@Nullable Object object) {
            // Warning: this is broken if size() == 0, so it is critical that we
            // substitute an empty ImmutableSet to the user in place of this
            if (object instanceof CartesianSet) {
                CartesianSet<?> that = (CartesianSet<?>) object;
                return this.axes.equals(that.axes);
            }
            return super.equals(object);
        }

        @Override
        public int hashCode() {
            // Warning: this is broken if size() == 0, so it is critical that we
            // substitute an empty ImmutableSet to the user in place of this

            // It's a weird formula, but tests prove it works.
            int adjust = size() - 1;
            for (int i = 0; i < axes.size(); i++) {
                adjust *= 31;
                adjust = ~~adjust;
                // in GWT, we have to deal with integer overflow carefully
            }
            int hash = 1;
            for (Set<E> axis : axes) {
                hash = 31 * hash + (size() / axis.size() * axis.hashCode());

                hash = ~~hash;
            }
            hash += adjust;
            return ~~hash;
        }
    }


    private static final class SubSet<E> extends AbstractSet<E> {
        private final ImmutableMap<E, Integer> inputSet;
        private final int mask;

        SubSet(ImmutableMap<E, Integer> inputSet, int mask) {
            this.inputSet = inputSet;
            this.mask = mask;
        }

        @Override
        public Iterator<E> iterator() {
            return new UnmodifiableIterator<E>() {
                final ImmutableList<E> elements = inputSet.keySet().asList();
                int remainingSetBits = mask;

                @Override
                public boolean hasNext() {
                    return remainingSetBits != 0;
                }

                @Override
                public E next() {
                    int index = Integer.numberOfTrailingZeros(remainingSetBits);
                    if (index == 32) {
                        throw new NoSuchElementException();
                    }
                    remainingSetBits &= ~(1 << index);
                    return elements.get(index);
                }
            };
        }

        @Override
        public int size() {
            return Integer.bitCount(mask);
        }

        @Override
        public boolean contains(@Nullable Object o) {
            Integer index = inputSet.get(o);
            return index != null && (mask & (1 << index)) != 0;
        }
    }

    private static final class PowerSet<E> extends AbstractSet<Set<E>> {
        final ImmutableMap<E, Integer> inputSet;

        PowerSet(Set<E> input) {
            this.inputSet = Maps.indexMap(input);
            checkArgument(inputSet.size() <= 30,
                    "Too many elements to create power set: %s > 30", inputSet.size());
        }

        @Override
        public int size() {
            return 1 << inputSet.size();
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Iterator<Set<E>> iterator() {
            return new AbstractIndexedListIterator<Set<E>>(size()) {
                @Override
                protected Set<E> get(final int setBits) {
                    return new SubSet<>(inputSet, setBits);
                }
            };
        }

        @Override
        public boolean contains(@Nullable Object obj) {
            if (obj instanceof Set) {
                Set<?> set = (Set<?>) obj;
                return inputSet.keySet().containsAll(set);
            }
            return false;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof PowerSet) {
                PowerSet<?> that = (PowerSet<?>) obj;
                return inputSet.equals(that.inputSet);
            }
            return super.equals(obj);
        }

        @Override
        public int hashCode() {

            return inputSet.keySet().hashCode() << (inputSet.size() - 1);
        }

        @Override
        public String toString() {
            return "powerSet(" + inputSet + ")";
        }
    }


    static int hashCodeImpl(Set<?> s) {
        int hashCode = 0;
        for (Object o : s) {
            hashCode += o != null ? o.hashCode() : 0;

            hashCode = ~~hashCode;
            // Needed to deal with unusual integer overflow in GWT.
        }
        return hashCode;
    }


    static boolean equalsImpl(Set<?> s, @Nullable Object object) {
        if (s == object) {
            return true;
        }
        if (object instanceof Set) {
            Set<?> o = (Set<?>) object;

            try {
                return s.size() == o.size() && s.containsAll(o);
            } catch (NullPointerException ignored) {
                return false;
            } catch (ClassCastException ignored) {
                return false;
            }
        }
        return false;
    }


    @GwtIncompatible("NavigableSet")
    public static <E> NavigableSet<E> unmodifiableNavigableSet(
            NavigableSet<E> set) {
        if (set instanceof ImmutableSortedSet
                || set instanceof UnmodifiableNavigableSet) {
            return set;
        }
        return new UnmodifiableNavigableSet<>(set);
    }

    @GwtIncompatible("NavigableSet")
    static final class UnmodifiableNavigableSet<E>
            extends ForwardingSortedSet<E> implements NavigableSet<E>, Serializable {
        private final NavigableSet<E> delegate;

        UnmodifiableNavigableSet(NavigableSet<E> delegate) {
            this.delegate = checkNotNull(delegate);
        }

        @Override
        protected SortedSet<E> delegate() {
            return Collections.unmodifiableSortedSet(delegate);
        }

        @Override
        public E lower(E e) {
            return delegate.lower(e);
        }

        @Override
        public E floor(E e) {
            return delegate.floor(e);
        }

        @Override
        public E ceiling(E e) {
            return delegate.ceiling(e);
        }

        @Override
        public E higher(E e) {
            return delegate.higher(e);
        }

        @Override
        public E pollFirst() {
            throw new UnsupportedOperationException();
        }

        @Override
        public E pollLast() {
            throw new UnsupportedOperationException();
        }

        private transient UnmodifiableNavigableSet<E> descendingSet;

        @Override
        public NavigableSet<E> descendingSet() {
            UnmodifiableNavigableSet<E> result = descendingSet;
            if (result == null) {
                result = descendingSet = new UnmodifiableNavigableSet<>(
                        delegate.descendingSet());
                result.descendingSet = this;
            }
            return result;
        }

        @Override
        public Iterator<E> descendingIterator() {
            return Iterators.unmodifiableIterator(delegate.descendingIterator());
        }

        @Override
        public NavigableSet<E> subSet(
                E fromElement,
                boolean fromInclusive,
                E toElement,
                boolean toInclusive) {
            return unmodifiableNavigableSet(delegate.subSet(
                    fromElement,
                    fromInclusive,
                    toElement,
                    toInclusive));
        }

        @Override
        public NavigableSet<E> headSet(E toElement, boolean inclusive) {
            return unmodifiableNavigableSet(delegate.headSet(toElement, inclusive));
        }

        @Override
        public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
            return unmodifiableNavigableSet(
                    delegate.tailSet(fromElement, inclusive));
        }

        private static final long serialVersionUID = 0;
    }


    static boolean removeAllImpl(Set<?> set, Iterator<?> iterator) {
        boolean changed = false;
        while (iterator.hasNext()) {
            changed |= set.remove(iterator.next());
        }
        return changed;
    }

    static boolean removeAllImpl(Set<?> set, Collection<?> collection) {
        checkNotNull(collection); // for GWT
        if (collection instanceof Multiset) {
            collection = ((Multiset<?>) collection).elementSet();
        }

        if (collection instanceof Set && collection.size() > set.size()) {
            return Iterators.removeAll(set.iterator(), collection);
        } else {
            return removeAllImpl(set, collection.iterator());
        }
    }

    @GwtIncompatible("NavigableSet")
    static class DescendingSet<E> extends ForwardingNavigableSet<E> {
        private final NavigableSet<E> forward;

        DescendingSet(NavigableSet<E> forward) {
            this.forward = forward;
        }

        @Override
        protected NavigableSet<E> delegate() {
            return forward;
        }

        @Override
        public E lower(E e) {
            return forward.higher(e);
        }

        @Override
        public E floor(E e) {
            return forward.ceiling(e);
        }

        @Override
        public E ceiling(E e) {
            return forward.floor(e);
        }

        @Override
        public E higher(E e) {
            return forward.lower(e);
        }

        @Override
        public E pollFirst() {
            return forward.pollLast();
        }

        @Override
        public E pollLast() {
            return forward.pollFirst();
        }

        @Override
        public NavigableSet<E> descendingSet() {
            return forward;
        }

        @Override
        public Iterator<E> descendingIterator() {
            return forward.iterator();
        }

        @Override
        public NavigableSet<E> subSet(
                E fromElement,
                boolean fromInclusive,
                E toElement,
                boolean toInclusive) {
            return forward.subSet(toElement, toInclusive, fromElement, fromInclusive).descendingSet();
        }

        @Override
        public NavigableSet<E> headSet(E toElement, boolean inclusive) {
            return forward.tailSet(toElement, inclusive).descendingSet();
        }

        @Override
        public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
            return forward.headSet(fromElement, inclusive).descendingSet();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Comparator<? super E> comparator() {
            Comparator<? super E> forwardComparator = forward.comparator();
            if (forwardComparator == null) {
                return (Comparator) Ordering.natural().reverse();
            } else {
                return reverse(forwardComparator);
            }
        }

        // If we inline this, we get a javac error.
        private static <T> Ordering<T> reverse(Comparator<T> forward) {
            return Ordering.from(forward).reverse();
        }

        @Override
        public E first() {
            return forward.last();
        }

        @Override
        public SortedSet<E> headSet(E toElement) {
            return standardHeadSet(toElement);
        }

        @Override
        public E last() {
            return forward.first();
        }

        @Override
        public SortedSet<E> subSet(E fromElement, E toElement) {
            return standardSubSet(fromElement, toElement);
        }

        @Override
        public SortedSet<E> tailSet(E fromElement) {
            return standardTailSet(fromElement);
        }

        @Override
        public Iterator<E> iterator() {
            return forward.descendingIterator();
        }

        @Override
        public Object[] toArray() {
            return standardToArray();
        }

        @Override
        public <T> T[] toArray(T[] array) {
            return standardToArray(array);
        }

        @Override
        public String toString() {
            return standardToString();
        }
    }
}
