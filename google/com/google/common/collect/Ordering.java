

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.CollectPreconditions.checkNonnegative;


@GwtCompatible
public abstract class Ordering<T> implements Comparator<T> {
    // Natural order


    @GwtCompatible(serializable = true)
    @SuppressWarnings("unchecked") // TODO(kevinb): right way to explain this??
    public static <C extends Comparable> Ordering<C> natural() {
        return (Ordering<C>) NaturalOrdering.INSTANCE;
    }

    // Static factories


    @GwtCompatible(serializable = true)
    public static <T> Ordering<T> from(Comparator<T> comparator) {
        return (comparator instanceof Ordering)
                ? (Ordering<T>) comparator
                : new ComparatorOrdering<>(comparator);
    }


    @GwtCompatible(serializable = true)
    @Deprecated
    public static <T> Ordering<T> from(Ordering<T> ordering) {
        return checkNotNull(ordering);
    }


    @GwtCompatible(serializable = true)
    public static <T> Ordering<T> explicit(List<T> valuesInOrder) {
        return new ExplicitOrdering<>(valuesInOrder);
    }


    @GwtCompatible(serializable = true)
    public static <T> Ordering<T> explicit(
            T leastValue, T... remainingValuesInOrder) {
        return explicit(Lists.asList(leastValue, remainingValuesInOrder));
    }

    // Ordering<Object> singletons


    @GwtCompatible(serializable = true)
    @SuppressWarnings("unchecked")
    public static Ordering<Object> allEqual() {
        return AllEqualOrdering.INSTANCE;
    }


    @GwtCompatible(serializable = true)
    public static Ordering<Object> usingToString() {
        return UsingToStringOrdering.INSTANCE;
    }


    public static Ordering<Object> arbitrary() {
        return ArbitraryOrderingHolder.ARBITRARY_ORDERING;
    }

    private static class ArbitraryOrderingHolder {
        static final Ordering<Object> ARBITRARY_ORDERING = new ArbitraryOrdering();
    }

    @VisibleForTesting
    static class ArbitraryOrdering extends Ordering<Object> {
        @SuppressWarnings("deprecation") // TODO(kevinb): ?
        private Map<Object, Integer> uids =
                Platform.tryWeakKeys(new MapMaker()).makeComputingMap(
                        new Function<Object, Integer>() {
                            final AtomicInteger counter = new AtomicInteger(0);

                            @Override
                            public Integer apply(Object from) {
                                return counter.getAndIncrement();
                            }
                        });

        @Override
        public int compare(Object left, Object right) {
            if (left == right) {
                return 0;
            } else if (left == null) {
                return -1;
            } else if (right == null) {
                return 1;
            }
            int leftCode = identityHashCode(left);
            int rightCode = identityHashCode(right);
            if (leftCode != rightCode) {
                return leftCode < rightCode ? -1 : 1;
            }

            // identityHashCode collision (rare, but not as rare as you'd think)
            int result = uids.get(left).compareTo(uids.get(right));
            if (result == 0) {
                throw new AssertionError(); // extremely, extremely unlikely.
            }
            return result;
        }

        @Override
        public String toString() {
            return "Ordering.arbitrary()";
        }


        int identityHashCode(Object object) {
            return System.identityHashCode(object);
        }
    }

    // Constructor


    protected Ordering() {
    }

    // Instance-based factories (and any static equivalents)


    // type parameter <S> lets us avoid the extra <String> in statements like:
    // Ordering<String> o = Ordering.<String>natural().reverse();
    @GwtCompatible(serializable = true)
    public <S extends T> Ordering<S> reverse() {
        return new ReverseOrdering<>(this);
    }


    // type parameter <S> lets us avoid the extra <String> in statements like:
    // Ordering<String> o = Ordering.<String>natural().nullsFirst();
    @GwtCompatible(serializable = true)
    public <S extends T> Ordering<S> nullsFirst() {
        return new NullsFirstOrdering<>(this);
    }


    // type parameter <S> lets us avoid the extra <String> in statements like:
    // Ordering<String> o = Ordering.<String>natural().nullsLast();
    @GwtCompatible(serializable = true)
    public <S extends T> Ordering<S> nullsLast() {
        return new NullsLastOrdering<>(this);
    }


    @GwtCompatible(serializable = true)
    public <F> Ordering<F> onResultOf(Function<F, ? extends T> function) {
        return new ByFunctionOrdering<>(function, this);
    }

    <T2 extends T> Ordering<Map.Entry<T2, ?>> onKeys() {
        return onResultOf(Maps.<T2>keyFunction());
    }


    @GwtCompatible(serializable = true)
    public <U extends T> Ordering<U> compound(
            Comparator<? super U> secondaryComparator) {
        return new CompoundOrdering<>(this, checkNotNull(secondaryComparator));
    }


    @GwtCompatible(serializable = true)
    public static <T> Ordering<T> compound(
            Iterable<? extends Comparator<? super T>> comparators) {
        return new CompoundOrdering<>(comparators);
    }


    @GwtCompatible(serializable = true)
    // type parameter <S> lets us avoid the extra <String> in statements like:
    // Ordering<Iterable<String>> o =
    //     Ordering.<String>natural().lexicographical();
    public <S extends T> Ordering<Iterable<S>> lexicographical() {

        return new LexicographicalOrdering<>(this);
    }

    // Regular instance methods

    // Override to add @Nullable
    @Override
    public abstract int compare(@Nullable T left, @Nullable T right);


    public <E extends T> E min(Iterator<E> iterator) {
        // let this throw NoSuchElementException as necessary
        E minSoFar = iterator.next();

        while (iterator.hasNext()) {
            minSoFar = min(minSoFar, iterator.next());
        }

        return minSoFar;
    }


    public <E extends T> E min(Iterable<E> iterable) {
        return min(iterable.iterator());
    }


    public <E extends T> E min(@Nullable E a, @Nullable E b) {
        return (compare(a, b) <= 0) ? a : b;
    }


    public <E extends T> E min(
            @Nullable E a, @Nullable E b, @Nullable E c, E... rest) {
        E minSoFar = min(min(a, b), c);

        for (E r : rest) {
            minSoFar = min(minSoFar, r);
        }

        return minSoFar;
    }


    public <E extends T> E max(Iterator<E> iterator) {
        // let this throw NoSuchElementException as necessary
        E maxSoFar = iterator.next();

        while (iterator.hasNext()) {
            maxSoFar = max(maxSoFar, iterator.next());
        }

        return maxSoFar;
    }


    public <E extends T> E max(Iterable<E> iterable) {
        return max(iterable.iterator());
    }


    public <E extends T> E max(@Nullable E a, @Nullable E b) {
        return (compare(a, b) >= 0) ? a : b;
    }


    public <E extends T> E max(
            @Nullable E a, @Nullable E b, @Nullable E c, E... rest) {
        E maxSoFar = max(max(a, b), c);

        for (E r : rest) {
            maxSoFar = max(maxSoFar, r);
        }

        return maxSoFar;
    }


    public <E extends T> List<E> leastOf(Iterable<E> iterable, int k) {
        if (iterable instanceof Collection) {
            Collection<E> collection = (Collection<E>) iterable;
            if (collection.size() <= 2L * k) {
                // In this case, just dumping the collection to an array and sorting is
                // faster than using the implementation for Iterator, which is
                // specialized for k much smaller than n.

                @SuppressWarnings("unchecked") // c only contains E's and doesn't escape
                        E[] array = (E[]) collection.toArray();
                Arrays.sort(array, this);
                if (array.length > k) {
                    array = ObjectArrays.arraysCopyOf(array, k);
                }
                return Collections.unmodifiableList(Arrays.asList(array));
            }
        }
        return leastOf(iterable.iterator(), k);
    }


    public <E extends T> List<E> leastOf(Iterator<E> elements, int k) {
        checkNotNull(elements);
        checkNonnegative(k, "k");

        if (k == 0 || !elements.hasNext()) {
            return ImmutableList.of();
        } else if (k >= Integer.MAX_VALUE / 2) {
            // k is really large; just do a straightforward sorted-copy-and-sublist
            ArrayList<E> list = Lists.newArrayList(elements);
            Collections.sort(list, this);
            if (list.size() > k) {
                list.subList(k, list.size()).clear();
            }
            list.trimToSize();
            return Collections.unmodifiableList(list);
        }


        int bufferCap = k * 2;
        @SuppressWarnings("unchecked") // we'll only put E's in
                E[] buffer = (E[]) new Object[bufferCap];
        E threshold = elements.next();
        buffer[0] = threshold;
        int bufferSize = 1;
        // threshold is the kth smallest element seen so far.  Once bufferSize >= k,
        // anything larger than threshold can be ignored immediately.

        while (bufferSize < k && elements.hasNext()) {
            E e = elements.next();
            buffer[bufferSize++] = e;
            threshold = max(threshold, e);
        }

        while (elements.hasNext()) {
            E e = elements.next();
            if (compare(e, threshold) >= 0) {
                continue;
            }

            buffer[bufferSize++] = e;
            if (bufferSize == bufferCap) {
                // We apply the quickselect algorithm to partition about the median,
                // and then ignore the last k elements.
                int left = 0;
                int right = bufferCap - 1;

                int minThresholdPosition = 0;
                // The leftmost position at which the greatest of the k lower elements
                // -- the new value of threshold -- might be found.

                while (left < right) {
                    int pivotIndex = (left + right + 1) >>> 1;
                    int pivotNewIndex = partition(buffer, left, right, pivotIndex);
                    if (pivotNewIndex > k) {
                        right = pivotNewIndex - 1;
                    } else if (pivotNewIndex < k) {
                        left = Math.max(pivotNewIndex, left + 1);
                        minThresholdPosition = pivotNewIndex;
                    } else {
                        break;
                    }
                }
                bufferSize = k;

                threshold = buffer[minThresholdPosition];
                for (int i = minThresholdPosition + 1; i < bufferSize; i++) {
                    threshold = max(threshold, buffer[i]);
                }
            }
        }

        Arrays.sort(buffer, 0, bufferSize, this);

        bufferSize = Math.min(bufferSize, k);
        return Collections.unmodifiableList(
                Arrays.asList(ObjectArrays.arraysCopyOf(buffer, bufferSize)));
        // We can't use ImmutableList; we have to be null-friendly!
    }

    private <E extends T> int partition(
            E[] values, int left, int right, int pivotIndex) {
        E pivotValue = values[pivotIndex];

        values[pivotIndex] = values[right];
        values[right] = pivotValue;

        int storeIndex = left;
        for (int i = left; i < right; i++) {
            if (compare(values[i], pivotValue) < 0) {
                ObjectArrays.swap(values, storeIndex, i);
                storeIndex++;
            }
        }
        ObjectArrays.swap(values, right, storeIndex);
        return storeIndex;
    }


    public <E extends T> List<E> greatestOf(Iterable<E> iterable, int k) {
        // TODO(kevinb): see if delegation is hurting performance noticeably
        // TODO(kevinb): if we change this implementation, add full unit tests.
        return reverse().leastOf(iterable, k);
    }


    public <E extends T> List<E> greatestOf(Iterator<E> iterator, int k) {
        return reverse().leastOf(iterator, k);
    }


    public <E extends T> List<E> sortedCopy(Iterable<E> elements) {
        @SuppressWarnings("unchecked") // does not escape, and contains only E's
                E[] array = (E[]) Iterables.toArray(elements);
        Arrays.sort(array, this);
        return Lists.newArrayList(Arrays.asList(array));
    }


    public <E extends T> ImmutableList<E> immutableSortedCopy(
            Iterable<E> elements) {
        @SuppressWarnings("unchecked") // we'll only ever have E's in here
                E[] array = (E[]) Iterables.toArray(elements);
        for (E e : array) {
            checkNotNull(e);
        }
        Arrays.sort(array, this);
        return ImmutableList.asImmutableList(array);
    }


    public boolean isOrdered(Iterable<? extends T> iterable) {
        Iterator<? extends T> it = iterable.iterator();
        if (it.hasNext()) {
            T prev = it.next();
            while (it.hasNext()) {
                T next = it.next();
                if (compare(prev, next) > 0) {
                    return false;
                }
                prev = next;
            }
        }
        return true;
    }


    public boolean isStrictlyOrdered(Iterable<? extends T> iterable) {
        Iterator<? extends T> it = iterable.iterator();
        if (it.hasNext()) {
            T prev = it.next();
            while (it.hasNext()) {
                T next = it.next();
                if (compare(prev, next) >= 0) {
                    return false;
                }
                prev = next;
            }
        }
        return true;
    }


    public int binarySearch(List<? extends T> sortedList, @Nullable T key) {
        return Collections.binarySearch(sortedList, key, this);
    }


    // TODO(kevinb): make this public, document it right
    @VisibleForTesting
    static class IncomparableValueException extends ClassCastException {
        final Object value;

        IncomparableValueException(Object value) {
            super("Cannot compare value: " + value);
            this.value = value;
        }

        private static final long serialVersionUID = 0;
    }

    // Never make these public
    static final int LEFT_IS_GREATER = 1;
    static final int RIGHT_IS_GREATER = -1;
}
