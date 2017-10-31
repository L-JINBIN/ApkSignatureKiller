

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.CollectPreconditions.checkRemove;


@GwtCompatible(emulated = true)
public final class Iterators {
    private Iterators() {
    }

    static final UnmodifiableListIterator<Object> EMPTY_LIST_ITERATOR =
            new UnmodifiableListIterator<Object>() {
                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public Object next() {
                    throw new NoSuchElementException();
                }

                @Override
                public boolean hasPrevious() {
                    return false;
                }

                @Override
                public Object previous() {
                    throw new NoSuchElementException();
                }

                @Override
                public int nextIndex() {
                    return 0;
                }

                @Override
                public int previousIndex() {
                    return -1;
                }
            };


    @Deprecated
    public static <T> UnmodifiableIterator<T> emptyIterator() {
        return emptyListIterator();
    }


    // Casting to any type is safe since there are no actual elements.
    @SuppressWarnings("unchecked")
    static <T> UnmodifiableListIterator<T> emptyListIterator() {
        return (UnmodifiableListIterator<T>) EMPTY_LIST_ITERATOR;
    }

    private static final Iterator<Object> EMPTY_MODIFIABLE_ITERATOR =
            new Iterator<Object>() {
                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public Object next() {
                    throw new NoSuchElementException();
                }

                @Override
                public void remove() {
                    checkRemove(false);
                }
            };


    // Casting to any type is safe since there are no actual elements.
    @SuppressWarnings("unchecked")
    static <T> Iterator<T> emptyModifiableIterator() {
        return (Iterator<T>) EMPTY_MODIFIABLE_ITERATOR;
    }


    public static <T> UnmodifiableIterator<T> unmodifiableIterator(final Iterator<T> iterator) {
        checkNotNull(iterator);
        if (iterator instanceof UnmodifiableIterator) {
            return (UnmodifiableIterator<T>) iterator;
        }
        return new UnmodifiableIterator<T>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public T next() {
                return iterator.next();
            }
        };
    }


    public static int size(Iterator<?> iterator) {
        int count = 0;
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        return count;
    }


    public static boolean contains(Iterator<?> iterator, @Nullable Object element) {
        return any(iterator, equalTo(element));
    }


    public static boolean removeAll(Iterator<?> removeFrom, Collection<?> elementsToRemove) {
        return removeIf(removeFrom, in(elementsToRemove));
    }


    public static <T> boolean removeIf(Iterator<T> removeFrom, Predicate<? super T> predicate) {
        checkNotNull(predicate);
        boolean modified = false;
        while (removeFrom.hasNext()) {
            if (predicate.apply(removeFrom.next())) {
                removeFrom.remove();
                modified = true;
            }
        }
        return modified;
    }


    public static boolean retainAll(Iterator<?> removeFrom, Collection<?> elementsToRetain) {
        return removeIf(removeFrom, not(in(elementsToRetain)));
    }


    public static boolean elementsEqual(Iterator<?> iterator1, Iterator<?> iterator2) {
        while (iterator1.hasNext()) {
            if (!iterator2.hasNext()) {
                return false;
            }
            Object o1 = iterator1.next();
            Object o2 = iterator2.next();
            if (!Objects.equal(o1, o2)) {
                return false;
            }
        }
        return !iterator2.hasNext();
    }


    public static String toString(Iterator<?> iterator) {
        return Collections2.STANDARD_JOINER
                .appendTo(new StringBuilder().append('['), iterator)
                .append(']')
                .toString();
    }


    public static <T> T getOnlyElement(Iterator<T> iterator) {
        T first = iterator.next();
        if (!iterator.hasNext()) {
            return first;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("expected one element but was: <" + first);
        for (int i = 0; i < 4 && iterator.hasNext(); i++) {
            sb.append(", " + iterator.next());
        }
        if (iterator.hasNext()) {
            sb.append(", ...");
        }
        sb.append('>');

        throw new IllegalArgumentException(sb.toString());
    }


    @Nullable
    public static <T> T getOnlyElement(Iterator<? extends T> iterator, @Nullable T defaultValue) {
        return iterator.hasNext() ? getOnlyElement(iterator) : defaultValue;
    }


    public static <T> boolean addAll(Collection<T> addTo, Iterator<? extends T> iterator) {
        checkNotNull(addTo);
        checkNotNull(iterator);
        boolean wasModified = false;
        while (iterator.hasNext()) {
            wasModified |= addTo.add(iterator.next());
        }
        return wasModified;
    }


    public static int frequency(Iterator<?> iterator, @Nullable Object element) {
        return size(filter(iterator, equalTo(element)));
    }


    public static <T> Iterator<T> cycle(final Iterable<T> iterable) {
        checkNotNull(iterable);
        return new Iterator<T>() {
            Iterator<T> iterator = emptyIterator();
            Iterator<T> removeFrom;

            @Override
            public boolean hasNext() {
                if (!iterator.hasNext()) {
                    iterator = iterable.iterator();
                }
                return iterator.hasNext();
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                removeFrom = iterator;
                return iterator.next();
            }

            @Override
            public void remove() {
                checkRemove(removeFrom != null);
                removeFrom.remove();
                removeFrom = null;
            }
        };
    }


    public static <T> Iterator<T> concat(Iterator<? extends T> a, Iterator<? extends T> b) {
        checkNotNull(a);
        checkNotNull(b);
        return concat(new ConsumingQueueIterator<>(a, b));
    }


    public static <T> Iterator<T> concat(final Iterator<? extends Iterator<? extends T>> inputs) {
        checkNotNull(inputs);
        return new Iterator<T>() {
            Iterator<? extends T> current = emptyIterator();
            Iterator<? extends T> removeFrom;

            @Override
            public boolean hasNext() {
                // http://code.google.com/p/google-collections/issues/detail?id=151
                // current.hasNext() might be relatively expensive, worth minimizing.
                boolean currentHasNext;
                // checkNotNull eager for GWT
                // note: it must be here & not where 'current' is assigned,
                // because otherwise we'll have called inputs.next() before throwing
                // the first NPE, and the next time around we'll call inputs.next()
                // again, incorrectly moving beyond the error.
                while (!(currentHasNext = checkNotNull(current).hasNext()) && inputs.hasNext()) {
                    current = inputs.next();
                }
                return currentHasNext;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                removeFrom = current;
                return current.next();
            }

            @Override
            public void remove() {
                checkRemove(removeFrom != null);
                removeFrom.remove();
                removeFrom = null;
            }
        };
    }


    public static <T> UnmodifiableIterator<List<T>> partition(Iterator<T> iterator, int size) {
        return partitionImpl(iterator, size, false);
    }


    public static <T> UnmodifiableIterator<List<T>> paddedPartition(Iterator<T> iterator, int size) {
        return partitionImpl(iterator, size, true);
    }

    private static <T> UnmodifiableIterator<List<T>> partitionImpl(
            final Iterator<T> iterator, final int size, final boolean pad) {
        checkNotNull(iterator);
        checkArgument(size > 0);
        return new UnmodifiableIterator<List<T>>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public List<T> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                Object[] array = new Object[size];
                int count = 0;
                for (; count < size && iterator.hasNext(); count++) {
                    array[count] = iterator.next();
                }
                for (int i = count; i < size; i++) {
                    array[i] = null; // for GWT
                }

                @SuppressWarnings("unchecked") // we only put Ts in it
                        List<T> list = Collections.unmodifiableList((List<T>) Arrays.asList(array));
                return (pad || count == size) ? list : list.subList(0, count);
            }
        };
    }


    public static <T> UnmodifiableIterator<T> filter(
            final Iterator<T> unfiltered, final Predicate<? super T> predicate) {
        checkNotNull(unfiltered);
        checkNotNull(predicate);
        return new AbstractIterator<T>() {
            @Override
            protected T computeNext() {
                while (unfiltered.hasNext()) {
                    T element = unfiltered.next();
                    if (predicate.apply(element)) {
                        return element;
                    }
                }
                return endOfData();
            }
        };
    }


    @SuppressWarnings("unchecked") // can cast to <T> because non-Ts are removed
    @GwtIncompatible("Class.isInstance")

    public static <T> UnmodifiableIterator<T> filter(Iterator<?> unfiltered, Class<T> type) {
        return (UnmodifiableIterator<T>) filter(unfiltered, instanceOf(type));
    }


    public static <T> boolean any(Iterator<T> iterator, Predicate<? super T> predicate) {
        return indexOf(iterator, predicate) != -1;
    }


    public static <T> boolean all(Iterator<T> iterator, Predicate<? super T> predicate) {
        checkNotNull(predicate);
        while (iterator.hasNext()) {
            T element = iterator.next();
            if (!predicate.apply(element)) {
                return false;
            }
        }
        return true;
    }


    public static <T> T find(Iterator<T> iterator, Predicate<? super T> predicate) {
        return filter(iterator, predicate).next();
    }


    @Nullable
    public static <T> T find(
            Iterator<? extends T> iterator, Predicate<? super T> predicate, @Nullable T defaultValue) {
        return getNext(filter(iterator, predicate), defaultValue);
    }


    public static <T> Optional<T> tryFind(Iterator<T> iterator, Predicate<? super T> predicate) {
        UnmodifiableIterator<T> filteredIterator = filter(iterator, predicate);
        return filteredIterator.hasNext()
                ? Optional.of(filteredIterator.next())
                : Optional.<T>absent();
    }


    public static <T> int indexOf(Iterator<T> iterator, Predicate<? super T> predicate) {
        checkNotNull(predicate, "predicate");
        for (int i = 0; iterator.hasNext(); i++) {
            T current = iterator.next();
            if (predicate.apply(current)) {
                return i;
            }
        }
        return -1;
    }


    public static <F, T> Iterator<T> transform(
            final Iterator<F> fromIterator, final Function<? super F, ? extends T> function) {
        checkNotNull(function);
        return new TransformedIterator<F, T>(fromIterator) {
            @Override
            T transform(F from) {
                return function.apply(from);
            }
        };
    }


    public static <T> T get(Iterator<T> iterator, int position) {
        checkNonnegative(position);
        int skipped = advance(iterator, position);
        if (!iterator.hasNext()) {
            throw new IndexOutOfBoundsException(
                    "position ("
                            + position
                            + ") must be less than the number of elements that remained ("
                            + skipped
                            + ")");
        }
        return iterator.next();
    }

    static void checkNonnegative(int position) {
        if (position < 0) {
            throw new IndexOutOfBoundsException("position (" + position + ") must not be negative");
        }
    }


    @Nullable
    public static <T> T getNext(Iterator<? extends T> iterator, @Nullable T defaultValue) {
        return iterator.hasNext() ? iterator.next() : defaultValue;
    }


    public static <T> T getLast(Iterator<T> iterator) {
        while (true) {
            T current = iterator.next();
            if (!iterator.hasNext()) {
                return current;
            }
        }
    }


    @Nullable
    public static <T> T getLast(Iterator<? extends T> iterator, @Nullable T defaultValue) {
        return iterator.hasNext() ? getLast(iterator) : defaultValue;
    }


    public static int advance(Iterator<?> iterator, int numberToAdvance) {
        checkNotNull(iterator);
        checkArgument(numberToAdvance >= 0, "numberToAdvance must be nonnegative");

        int i;
        for (i = 0; i < numberToAdvance && iterator.hasNext(); i++) {
            iterator.next();
        }
        return i;
    }


    public static <T> Iterator<T> limit(final Iterator<T> iterator, final int limitSize) {
        checkNotNull(iterator);
        checkArgument(limitSize >= 0, "limit is negative");
        return new Iterator<T>() {
            private int count;

            @Override
            public boolean hasNext() {
                return count < limitSize && iterator.hasNext();
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                count++;
                return iterator.next();
            }

            @Override
            public void remove() {
                iterator.remove();
            }
        };
    }


    public static <T> Iterator<T> consumingIterator(final Iterator<T> iterator) {
        checkNotNull(iterator);
        return new UnmodifiableIterator<T>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public T next() {
                T next = iterator.next();
                iterator.remove();
                return next;
            }

            @Override
            public String toString() {
                return "Iterators.consumingIterator(...)";
            }
        };
    }


    @Nullable
    static <T> T pollNext(Iterator<T> iterator) {
        if (iterator.hasNext()) {
            T result = iterator.next();
            iterator.remove();
            return result;
        } else {
            return null;
        }
    }

    // Methods only in Iterators, not in Iterables


    static void clear(Iterator<?> iterator) {
        checkNotNull(iterator);
        while (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }


    public static <T> UnmodifiableIterator<T> forArray(final T... array) {
        return forArray(array, 0, array.length, 0);
    }


    static <T> UnmodifiableListIterator<T> forArray(
            final T[] array, final int offset, int length, int index) {
        checkArgument(length >= 0);
        int end = offset + length;

        // Technically we should give a slightly more descriptive error on overflow
        Preconditions.checkPositionIndexes(offset, end, array.length);
        Preconditions.checkPositionIndex(index, length);
        if (length == 0) {
            return emptyListIterator();
        }


        return new AbstractIndexedListIterator<T>(length, index) {
            @Override
            protected T get(int index) {
                return array[offset + index];
            }
        };
    }


    public static <T> UnmodifiableIterator<T> singletonIterator(@Nullable final T value) {
        return new UnmodifiableIterator<T>() {
            boolean done;

            @Override
            public boolean hasNext() {
                return !done;
            }

            @Override
            public T next() {
                if (done) {
                    throw new NoSuchElementException();
                }
                done = true;
                return value;
            }
        };
    }


    private static class PeekingImpl<E> implements PeekingIterator<E> {

        private final Iterator<? extends E> iterator;
        private boolean hasPeeked;
        private E peekedElement;

        public PeekingImpl(Iterator<? extends E> iterator) {
            this.iterator = checkNotNull(iterator);
        }

        @Override
        public boolean hasNext() {
            return hasPeeked || iterator.hasNext();
        }

        @Override
        public E next() {
            if (!hasPeeked) {
                return iterator.next();
            }
            E result = peekedElement;
            hasPeeked = false;
            peekedElement = null;
            return result;
        }

        @Override
        public void remove() {
            checkState(!hasPeeked, "Can't remove after you've peeked at next");
            iterator.remove();
        }

        @Override
        public E peek() {
            if (!hasPeeked) {
                peekedElement = iterator.next();
                hasPeeked = true;
            }
            return peekedElement;
        }
    }


    public static <T> PeekingIterator<T> peekingIterator(Iterator<? extends T> iterator) {
        if (iterator instanceof PeekingImpl) {
            // Safe to cast <? extends T> to <T> because PeekingImpl only uses T
            // covariantly (and cannot be subclassed to add non-covariant uses).
            @SuppressWarnings("unchecked")
            PeekingImpl<T> peeking = (PeekingImpl<T>) iterator;
            return peeking;
        }
        return new PeekingImpl<>(iterator);
    }


    @Beta
    public static <T> UnmodifiableIterator<T> mergeSorted(
            Iterable<? extends Iterator<? extends T>> iterators, Comparator<? super T> comparator) {
        checkNotNull(iterators, "iterators");
        checkNotNull(comparator, "comparator");

        return new MergingIterator<>(iterators, comparator);
    }


    private static class MergingIterator<T> extends UnmodifiableIterator<T> {
        final Queue<PeekingIterator<T>> queue;

        public MergingIterator(
                Iterable<? extends Iterator<? extends T>> iterators,
                final Comparator<? super T> itemComparator) {
            // A comparator that's used by the heap, allowing the heap
            // to be sorted based on the top of each iterator.
            Comparator<PeekingIterator<T>> heapComparator =
                    new Comparator<PeekingIterator<T>>() {
                        @Override
                        public int compare(PeekingIterator<T> o1, PeekingIterator<T> o2) {
                            return itemComparator.compare(o1.peek(), o2.peek());
                        }
                    };

            queue = new PriorityQueue<>(2, heapComparator);

            for (Iterator<? extends T> iterator : iterators) {
                if (iterator.hasNext()) {
                    queue.add(Iterators.peekingIterator(iterator));
                }
            }
        }

        @Override
        public boolean hasNext() {
            return !queue.isEmpty();
        }

        @Override
        public T next() {
            PeekingIterator<T> nextIter = queue.remove();
            T next = nextIter.next();
            if (nextIter.hasNext()) {
                queue.add(nextIter);
            }
            return next;
        }
    }


    static <T> ListIterator<T> cast(Iterator<T> iterator) {
        return (ListIterator<T>) iterator;
    }
}
