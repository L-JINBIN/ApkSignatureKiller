

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.RandomAccess;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.CollectPreconditions.checkRemove;


@GwtCompatible(emulated = true)
public final class Iterables {
    private Iterables() {
    }


    private static final class UnmodifiableIterable<T> extends FluentIterable<T> {
        private final Iterable<T> iterable;

        private UnmodifiableIterable(Iterable<T> iterable) {
            this.iterable = iterable;
        }

        @Override
        public Iterator<T> iterator() {
            return Iterators.unmodifiableIterator(iterable.iterator());
        }

        @Override
        public String toString() {
            return iterable.toString();
        }
        // no equals and hashCode; it would break the contract!
    }


    public static int size(Iterable<?> iterable) {
        return (iterable instanceof Collection)
                ? ((Collection<?>) iterable).size()
                : Iterators.size(iterable.iterator());
    }


    public static boolean contains(Iterable<?> iterable, @Nullable Object element) {
        if (iterable instanceof Collection) {
            Collection<?> collection = (Collection<?>) iterable;
            return Collections2.safeContains(collection, element);
        }
        return Iterators.contains(iterable.iterator(), element);
    }


    public static <T> boolean removeIf(Iterable<T> removeFrom, Predicate<? super T> predicate) {
        if (removeFrom instanceof RandomAccess && removeFrom instanceof List) {
            return removeIfFromRandomAccessList((List<T>) removeFrom, checkNotNull(predicate));
        }
        return Iterators.removeIf(removeFrom.iterator(), predicate);
    }

    private static <T> boolean removeIfFromRandomAccessList(
            List<T> list, Predicate<? super T> predicate) {
        // Note: Not all random access lists support set() so we need to deal with
        // those that don't and attempt the slower remove() based solution.
        int from = 0;
        int to = 0;

        for (; from < list.size(); from++) {
            T element = list.get(from);
            if (!predicate.apply(element)) {
                if (from > to) {
                    try {
                        list.set(to, element);
                    } catch (UnsupportedOperationException e) {
                        slowRemoveIfForRemainingElements(list, predicate, to, from);
                        return true;
                    }
                }
                to++;
            }
        }

        // Clear the tail of any remaining items
        list.subList(to, list.size()).clear();
        return from != to;
    }

    private static <T> void slowRemoveIfForRemainingElements(
            List<T> list, Predicate<? super T> predicate, int to, int from) {
        // Here we know that:
        // * (to < from) and that both are valid indices.
        // * Everything with (index < to) should be kept.
        // * Everything with (to <= index < from) should be removed.
        // * The element with (index == from) should be kept.
        // * Everything with (index > from) has not been checked yet.

        // Check from the end of the list backwards (minimize expected cost of
        // moving elements when remove() is called). Stop before 'from' because
        // we already know that should be kept.
        for (int n = list.size() - 1; n > from; n--) {
            if (predicate.apply(list.get(n))) {
                list.remove(n);
            }
        }
        // And now remove everything in the range [to, from) (going backwards).
        for (int n = from - 1; n >= to; n--) {
            list.remove(n);
        }
    }


    @Nullable
    static <T> T removeFirstMatching(Iterable<T> removeFrom, Predicate<? super T> predicate) {
        checkNotNull(predicate);
        Iterator<T> iterator = removeFrom.iterator();
        while (iterator.hasNext()) {
            T next = iterator.next();
            if (predicate.apply(next)) {
                iterator.remove();
                return next;
            }
        }
        return null;
    }


    public static boolean elementsEqual(Iterable<?> iterable1, Iterable<?> iterable2) {
        if (iterable1 instanceof Collection && iterable2 instanceof Collection) {
            Collection<?> collection1 = (Collection<?>) iterable1;
            Collection<?> collection2 = (Collection<?>) iterable2;
            if (collection1.size() != collection2.size()) {
                return false;
            }
        }
        return Iterators.elementsEqual(iterable1.iterator(), iterable2.iterator());
    }


    public static String toString(Iterable<?> iterable) {
        return Iterators.toString(iterable.iterator());
    }


    public static <T> T getOnlyElement(Iterable<T> iterable) {
        return Iterators.getOnlyElement(iterable.iterator());
    }


    @GwtIncompatible("Array.newInstance(Class, int)")
    public static <T> T[] toArray(Iterable<? extends T> iterable, Class<T> type) {
        Collection<? extends T> collection = toCollection(iterable);
        T[] array = ObjectArrays.newArray(type, collection.size());
        return collection.toArray(array);
    }

    static <T> T[] toArray(Iterable<? extends T> iterable, T[] array) {
        Collection<? extends T> collection = toCollection(iterable);
        return collection.toArray(array);
    }


    static Object[] toArray(Iterable<?> iterable) {
        return toCollection(iterable).toArray();
    }


    private static <E> Collection<E> toCollection(Iterable<E> iterable) {
        return (iterable instanceof Collection)
                ? (Collection<E>) iterable
                : Lists.newArrayList(iterable.iterator());
    }


    public static <T> boolean addAll(Collection<T> addTo, Iterable<? extends T> elementsToAdd) {
        if (elementsToAdd instanceof Collection) {
            Collection<? extends T> c = Collections2.cast(elementsToAdd);
            return addTo.addAll(c);
        }
        return Iterators.addAll(addTo, checkNotNull(elementsToAdd).iterator());
    }


    public static <T> Iterable<T> cycle(final Iterable<T> iterable) {
        checkNotNull(iterable);
        return new FluentIterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return Iterators.cycle(iterable);
            }

            @Override
            public String toString() {
                return iterable.toString() + " (cycled)";
            }
        };
    }


    public static <T> Iterable<T> concat(Iterable<? extends T> a, Iterable<? extends T> b) {
        return concat(ImmutableList.of(a, b));
    }


    public static <T> Iterable<T> concat(final Iterable<? extends Iterable<? extends T>> inputs) {
        checkNotNull(inputs);
        return new FluentIterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return Iterators.concat(iterators(inputs));
            }
        };
    }


    private static <T> Iterator<Iterator<? extends T>> iterators(
            Iterable<? extends Iterable<? extends T>> iterables) {
        return new TransformedIterator<Iterable<? extends T>, Iterator<? extends T>>(
                iterables.iterator()) {
            @Override
            Iterator<? extends T> transform(Iterable<? extends T> from) {
                return from.iterator();
            }
        };
    }


    public static <T> Iterable<T> filter(
            final Iterable<T> unfiltered, final Predicate<? super T> predicate) {
        checkNotNull(unfiltered);
        checkNotNull(predicate);
        return new FluentIterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return Iterators.filter(unfiltered.iterator(), predicate);
            }
        };
    }


    @GwtIncompatible("Class.isInstance")

    public static <T> Iterable<T> filter(final Iterable<?> unfiltered, final Class<T> type) {
        checkNotNull(unfiltered);
        checkNotNull(type);
        return new FluentIterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return Iterators.filter(unfiltered.iterator(), type);
            }
        };
    }


    public static <T> boolean any(Iterable<T> iterable, Predicate<? super T> predicate) {
        return Iterators.any(iterable.iterator(), predicate);
    }


    public static <T> boolean all(Iterable<T> iterable, Predicate<? super T> predicate) {
        return Iterators.all(iterable.iterator(), predicate);
    }


    public static <T> Optional<T> tryFind(Iterable<T> iterable, Predicate<? super T> predicate) {
        return Iterators.tryFind(iterable.iterator(), predicate);
    }


    public static <F, T> Iterable<T> transform(
            final Iterable<F> fromIterable, final Function<? super F, ? extends T> function) {
        checkNotNull(fromIterable);
        checkNotNull(function);
        return new FluentIterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return Iterators.transform(fromIterable.iterator(), function);
            }
        };
    }


    public static <T> T get(Iterable<T> iterable, int position) {
        checkNotNull(iterable);
        return (iterable instanceof List)
                ? ((List<T>) iterable).get(position)
                : Iterators.get(iterable.iterator(), position);
    }


    @Nullable
    public static <T> T getFirst(Iterable<? extends T> iterable, @Nullable T defaultValue) {
        return Iterators.getNext(iterable.iterator(), defaultValue);
    }


    public static <T> T getLast(Iterable<T> iterable) {
        // TODO(kevinb): Support a concurrently modified collection?
        if (iterable instanceof List) {
            List<T> list = (List<T>) iterable;
            if (list.isEmpty()) {
                throw new NoSuchElementException();
            }
            return getLastInNonemptyList(list);
        }

        return Iterators.getLast(iterable.iterator());
    }


    private static <T> T getLastInNonemptyList(List<T> list) {
        return list.get(list.size() - 1);
    }


    public static <T> Iterable<T> skip(final Iterable<T> iterable, final int numberToSkip) {
        checkNotNull(iterable);
        checkArgument(numberToSkip >= 0, "number to skip cannot be negative");

        if (iterable instanceof List) {
            final List<T> list = (List<T>) iterable;
            return new FluentIterable<T>() {
                @Override
                public Iterator<T> iterator() {
                    // TODO(kevinb): Support a concurrently modified collection?
                    int toSkip = Math.min(list.size(), numberToSkip);
                    return list.subList(toSkip, list.size()).iterator();
                }
            };
        }

        return new FluentIterable<T>() {
            @Override
            public Iterator<T> iterator() {
                final Iterator<T> iterator = iterable.iterator();

                Iterators.advance(iterator, numberToSkip);


                return new Iterator<T>() {
                    boolean atStart = true;

                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public T next() {
                        T result = iterator.next();
                        atStart = false; // not called if next() fails
                        return result;
                    }

                    @Override
                    public void remove() {
                        checkRemove(!atStart);
                        iterator.remove();
                    }
                };
            }
        };
    }


    public static <T> Iterable<T> limit(final Iterable<T> iterable, final int limitSize) {
        checkNotNull(iterable);
        checkArgument(limitSize >= 0, "limit is negative");
        return new FluentIterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return Iterators.limit(iterable.iterator(), limitSize);
            }
        };
    }


    // Methods only in Iterables, not in Iterators


    public static boolean isEmpty(Iterable<?> iterable) {
        if (iterable instanceof Collection) {
            return ((Collection<?>) iterable).isEmpty();
        }
        return !iterable.iterator().hasNext();
    }


}
