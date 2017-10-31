

package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;

import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;


@Beta
@GwtIncompatible("hasn't been tested yet")
public abstract class ImmutableSortedMultiset<E> extends ImmutableMultiset<E>
        implements SortedMultiset<E> {
    // TODO(lowasser): GWT compatibility

    private static final Comparator<Comparable> NATURAL_ORDER = Ordering.natural();

    private static final ImmutableSortedMultiset<Comparable> NATURAL_EMPTY_MULTISET =
            new RegularImmutableSortedMultiset<>(NATURAL_ORDER);


    public static <E> ImmutableSortedMultiset<E> copyOf(
            Comparator<? super E> comparator, Iterable<? extends E> elements) {
        if (elements instanceof ImmutableSortedMultiset) {
            @SuppressWarnings("unchecked") // immutable collections are always safe for covariant casts
                    ImmutableSortedMultiset<E> multiset = (ImmutableSortedMultiset<E>) elements;
            if (comparator.equals(multiset.comparator())) {
                if (multiset.isPartialView()) {
                    return copyOfSortedEntries(comparator, multiset.entrySet().asList());
                } else {
                    return multiset;
                }
            }
        }
        elements = Lists.newArrayList(elements); // defensive copy
        TreeMultiset<E> sortedCopy = TreeMultiset.create(checkNotNull(comparator));
        Iterables.addAll(sortedCopy, elements);
        return copyOfSortedEntries(comparator, sortedCopy.entrySet());
    }


    public static <E> ImmutableSortedMultiset<E> copyOfSorted(SortedMultiset<E> sortedMultiset) {
        return copyOfSortedEntries(
                sortedMultiset.comparator(), Lists.newArrayList(sortedMultiset.entrySet()));
    }

    private static <E> ImmutableSortedMultiset<E> copyOfSortedEntries(
            Comparator<? super E> comparator, Collection<Entry<E>> entries) {
        if (entries.isEmpty()) {
            return emptyMultiset(comparator);
        }
        ImmutableList.Builder<E> elementsBuilder = new ImmutableList.Builder<>(entries.size());
        long[] cumulativeCounts = new long[entries.size() + 1];
        int i = 0;
        for (Entry<E> entry : entries) {
            elementsBuilder.add(entry.getElement());
            cumulativeCounts[i + 1] = cumulativeCounts[i] + entry.getCount();
            i++;
        }
        return new RegularImmutableSortedMultiset<>(
                new RegularImmutableSortedSet<>(elementsBuilder.build(), comparator),
                cumulativeCounts,
                0,
                entries.size());
    }

    @SuppressWarnings("unchecked")
    static <E> ImmutableSortedMultiset<E> emptyMultiset(Comparator<? super E> comparator) {
        if (NATURAL_ORDER.equals(comparator)) {
            return (ImmutableSortedMultiset<E>) NATURAL_EMPTY_MULTISET;
        } else {
            return new RegularImmutableSortedMultiset<>(comparator);
        }
    }

    @Override
    public final Comparator<? super E> comparator() {
        return elementSet().comparator();
    }

    @Override
    public abstract ImmutableSortedSet<E> elementSet();

    transient ImmutableSortedMultiset<E> descendingMultiset;

    @Override
    public ImmutableSortedMultiset<E> descendingMultiset() {
        ImmutableSortedMultiset<E> result = descendingMultiset;
        if (result == null) {
            return descendingMultiset =
                    this.isEmpty()
                            ? emptyMultiset(Ordering.from(comparator()).reverse())
                            : new DescendingImmutableSortedMultiset<>(this);
        }
        return result;
    }


    @Deprecated
    @Override
    public final Entry<E> pollFirstEntry() {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    @Override
    public final Entry<E> pollLastEntry() {
        throw new UnsupportedOperationException();
    }

    @Override
    public abstract ImmutableSortedMultiset<E> headMultiset(E upperBound, BoundType boundType);

    @Override
    public ImmutableSortedMultiset<E> subMultiset(
            E lowerBound, BoundType lowerBoundType, E upperBound, BoundType upperBoundType) {
        checkArgument(
                comparator().compare(lowerBound, upperBound) <= 0,
                "Expected lowerBound <= upperBound but %s > %s",
                lowerBound,
                upperBound);
        return tailMultiset(lowerBound, lowerBoundType).headMultiset(upperBound, upperBoundType);
    }

    @Override
    public abstract ImmutableSortedMultiset<E> tailMultiset(E lowerBound, BoundType boundType);


    public static class Builder<E> extends ImmutableMultiset.Builder<E> {

        public Builder(Comparator<? super E> comparator) {
            super(TreeMultiset.create(checkNotNull(comparator)));
        }


        @Override
        public Builder<E> add(E element) {
            super.add(element);
            return this;
        }


        @Override
        public Builder<E> addCopies(E element, int occurrences) {
            super.addCopies(element, occurrences);
            return this;
        }


        @Override
        public Builder<E> setCount(E element, int count) {
            super.setCount(element, count);
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
        public ImmutableSortedMultiset<E> build() {
            return copyOfSorted((SortedMultiset<E>) contents);
        }
    }

    private static final class SerializedForm<E> implements Serializable {
        Comparator<? super E> comparator;
        E[] elements;
        int[] counts;

        @SuppressWarnings("unchecked")
        SerializedForm(SortedMultiset<E> multiset) {
            this.comparator = multiset.comparator();
            int n = multiset.entrySet().size();
            elements = (E[]) new Object[n];
            counts = new int[n];
            int i = 0;
            for (Entry<E> entry : multiset.entrySet()) {
                elements[i] = entry.getElement();
                counts[i] = entry.getCount();
                i++;
            }
        }

        Object readResolve() {
            int n = elements.length;
            Builder<E> builder = new Builder<>(comparator);
            for (int i = 0; i < n; i++) {
                builder.addCopies(elements[i], counts[i]);
            }
            return builder.build();
        }
    }

    @Override
    Object writeReplace() {
        return new SerializedForm<>(this);
    }
}
