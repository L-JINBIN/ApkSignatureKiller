

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ObjectArrays.checkElementsNotNull;


// TODO(benyu): benchmark and optimize all creation paths, which are a mess now
@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial") // we're overriding default serialization
public abstract class ImmutableSortedSet<E> extends ImmutableSortedSetFauxverideShim<E>
        implements NavigableSet<E>, SortedIterable<E> {

    private static final Comparator<Comparable> NATURAL_ORDER = Ordering.natural();

    private static final RegularImmutableSortedSet<Comparable> NATURAL_EMPTY_SET =
            new RegularImmutableSortedSet<>(ImmutableList.<Comparable>of(), NATURAL_ORDER);

    static <E> RegularImmutableSortedSet<E> emptySet(Comparator<? super E> comparator) {
        if (NATURAL_ORDER.equals(comparator)) {
            return (RegularImmutableSortedSet<E>) NATURAL_EMPTY_SET;
        } else {
            return new RegularImmutableSortedSet<>(ImmutableList.<E>of(), comparator);
        }
    }


    public static <E> ImmutableSortedSet<E> of() {
        return (ImmutableSortedSet<E>) NATURAL_EMPTY_SET;
    }


    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(E element) {
        return new RegularImmutableSortedSet<>(ImmutableList.of(element), Ordering.natural());
    }


    // TODO(kevinb): Consider factory methods that reject duplicates


    public static <E> ImmutableSortedSet<E> copyOf(Iterable<? extends E> elements) {
        // Hack around E not being a subtype of Comparable.
        // Unsafe, see ImmutableSortedSetFauxverideShim.
        @SuppressWarnings("unchecked")
        Ordering<E> naturalOrder = (Ordering<E>) Ordering.natural();
        return copyOf(naturalOrder, elements);
    }


    public static <E> ImmutableSortedSet<E> copyOf(Collection<? extends E> elements) {
        // Hack around E not being a subtype of Comparable.
        // Unsafe, see ImmutableSortedSetFauxverideShim.
        @SuppressWarnings("unchecked")
        Ordering<E> naturalOrder = (Ordering<E>) Ordering.natural();
        return copyOf(naturalOrder, elements);
    }


    public static <E> ImmutableSortedSet<E> copyOf(
            Comparator<? super E> comparator, Iterator<? extends E> elements) {
        return new Builder<>(comparator).addAll(elements).build();
    }


    public static <E> ImmutableSortedSet<E> copyOf(
            Comparator<? super E> comparator, Iterable<? extends E> elements) {
        checkNotNull(comparator);
        boolean hasSameComparator = SortedIterables.hasSameComparator(comparator, elements);

        if (hasSameComparator && (elements instanceof ImmutableSortedSet)) {
            @SuppressWarnings("unchecked")
            ImmutableSortedSet<E> original = (ImmutableSortedSet<E>) elements;
            if (!original.isPartialView()) {
                return original;
            }
        }
        @SuppressWarnings("unchecked") // elements only contains E's; it's safe.
                E[] array = (E[]) Iterables.toArray(elements);
        return construct(comparator, array.length, array);
    }


    public static <E> ImmutableSortedSet<E> copyOf(
            Comparator<? super E> comparator, Collection<? extends E> elements) {
        return copyOf(comparator, (Iterable<? extends E>) elements);
    }


    static <E> ImmutableSortedSet<E> construct(
            Comparator<? super E> comparator, int n, E... contents) {
        if (n == 0) {
            return emptySet(comparator);
        }
        checkElementsNotNull(contents, n);
        Arrays.sort(contents, 0, n, comparator);
        int uniques = 1;
        for (int i = 1; i < n; i++) {
            E cur = contents[i];
            E prev = contents[uniques - 1];
            if (comparator.compare(cur, prev) != 0) {
                contents[uniques++] = cur;
            }
        }
        Arrays.fill(contents, uniques, n, null);
        return new RegularImmutableSortedSet<>(
                ImmutableList.<E>asImmutableList(contents, uniques), comparator);
    }


    public static final class Builder<E> extends ImmutableSet.Builder<E> {
        private final Comparator<? super E> comparator;


        public Builder(Comparator<? super E> comparator) {
            this.comparator = checkNotNull(comparator);
        }


        @Override
        public Builder<E> add(E element) {
            super.add(element);
            return this;
        }


        @Override
        public Builder<E> add(E... elements) {
            super.add(elements);
            return this;
        }


        @Override
        public Builder<E> addAll(Iterable<? extends E> elements) {
            super.addAll(elements);
            return this;
        }


        @Override
        public Builder<E> addAll(Iterator<? extends E> elements) {
            super.addAll(elements);
            return this;
        }


        @Override
        public ImmutableSortedSet<E> build() {
            @SuppressWarnings("unchecked") // we're careful to put only E's in here
                    E[] contentsArray = (E[]) contents;
            ImmutableSortedSet<E> result = construct(comparator, size, contentsArray);
            this.size = result.size(); // we eliminated duplicates in-place in contentsArray
            return result;
        }
    }

    int unsafeCompare(Object a, Object b) {
        return unsafeCompare(comparator, a, b);
    }

    static int unsafeCompare(Comparator<?> comparator, Object a, Object b) {
        // Pretend the comparator can compare anything. If it turns out it can't
        // compare a and b, we should get a CCE on the subsequent line. Only methods
        // that are spec'd to throw CCE should call this.
        @SuppressWarnings("unchecked")
        Comparator<Object> unsafeComparator = (Comparator<Object>) comparator;
        return unsafeComparator.compare(a, b);
    }

    final transient Comparator<? super E> comparator;

    ImmutableSortedSet(Comparator<? super E> comparator) {
        this.comparator = comparator;
    }


    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override // needed to unify the iterator() methods in Collection and SortedIterable
    public abstract UnmodifiableIterator<E> iterator();


    @Override
    public ImmutableSortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }


    @GwtIncompatible("NavigableSet")
    @Override
    public ImmutableSortedSet<E> headSet(E toElement, boolean inclusive) {
        return headSetImpl(checkNotNull(toElement), inclusive);
    }


    @Override
    public ImmutableSortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }


    @GwtIncompatible("NavigableSet")
    @Override
    public ImmutableSortedSet<E> subSet(
            E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        checkNotNull(fromElement);
        checkNotNull(toElement);
        checkArgument(comparator.compare(fromElement, toElement) <= 0);
        return subSetImpl(fromElement, fromInclusive, toElement, toInclusive);
    }


    @Override
    public ImmutableSortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }


    @GwtIncompatible("NavigableSet")
    @Override
    public ImmutableSortedSet<E> tailSet(E fromElement, boolean inclusive) {
        return tailSetImpl(checkNotNull(fromElement), inclusive);
    }


    abstract ImmutableSortedSet<E> headSetImpl(E toElement, boolean inclusive);

    abstract ImmutableSortedSet<E> subSetImpl(
            E fromElement, boolean fromInclusive, E toElement, boolean toInclusive);

    abstract ImmutableSortedSet<E> tailSetImpl(E fromElement, boolean inclusive);


    @GwtIncompatible("NavigableSet")
    @Override
    public E lower(E e) {
        return Iterators.getNext(headSet(e, false).descendingIterator(), null);
    }


    @GwtIncompatible("NavigableSet")
    @Override
    public E floor(E e) {
        return Iterators.getNext(headSet(e, true).descendingIterator(), null);
    }


    @GwtIncompatible("NavigableSet")
    @Override
    public E ceiling(E e) {
        return Iterables.getFirst(tailSet(e, true), null);
    }


    @GwtIncompatible("NavigableSet")
    @Override
    public E higher(E e) {
        return Iterables.getFirst(tailSet(e, false), null);
    }

    @Override
    public E first() {
        return iterator().next();
    }

    @Override
    public E last() {
        return descendingIterator().next();
    }


    @Deprecated
    @GwtIncompatible("NavigableSet")
    @Override
    public final E pollFirst() {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    @GwtIncompatible("NavigableSet")
    @Override
    public final E pollLast() {
        throw new UnsupportedOperationException();
    }

    @GwtIncompatible("NavigableSet")
    transient ImmutableSortedSet<E> descendingSet;


    @GwtIncompatible("NavigableSet")
    @Override
    public ImmutableSortedSet<E> descendingSet() {
        // racy single-check idiom
        ImmutableSortedSet<E> result = descendingSet;
        if (result == null) {
            result = descendingSet = createDescendingSet();
            result.descendingSet = this;
        }
        return result;
    }

    @GwtIncompatible("NavigableSet")
    ImmutableSortedSet<E> createDescendingSet() {
        return new DescendingImmutableSortedSet<>(this);
    }


    @GwtIncompatible("NavigableSet")
    @Override
    public abstract UnmodifiableIterator<E> descendingIterator();


    abstract int indexOf(@Nullable Object target);


    private static class SerializedForm<E> implements Serializable {
        final Comparator<? super E> comparator;
        final Object[] elements;

        public SerializedForm(Comparator<? super E> comparator, Object[] elements) {
            this.comparator = comparator;
            this.elements = elements;
        }

        @SuppressWarnings("unchecked")
        Object readResolve() {
            return new Builder<>(comparator).add((E[]) elements).build();
        }

        private static final long serialVersionUID = 0;
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Use SerializedForm");
    }

    @Override
    Object writeReplace() {
        return new SerializedForm<>(comparator, toArray());
    }
}
