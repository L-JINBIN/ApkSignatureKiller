

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.primitives.Ints;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.AbstractSequentialList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;

import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkPositionIndex;
import static com.google.common.base.Preconditions.checkPositionIndexes;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.CollectPreconditions.checkNonnegative;
import static com.google.common.collect.CollectPreconditions.checkRemove;


@GwtCompatible(emulated = true)
public final class Lists {
    private Lists() {
    }

    // ArrayList


    @GwtCompatible(serializable = true)
    public static <E> ArrayList<E> newArrayList() {
        return new ArrayList<>();
    }


    @GwtCompatible(serializable = true)
    public static <E> ArrayList<E> newArrayList(E... elements) {
        checkNotNull(elements); // for GWT
        // Avoid integer overflow when a large array is passed in
        int capacity = computeArrayListCapacity(elements.length);
        ArrayList<E> list = new ArrayList<>(capacity);
        Collections.addAll(list, elements);
        return list;
    }

    @VisibleForTesting
    static int computeArrayListCapacity(int arraySize) {
        checkNonnegative(arraySize, "arraySize");

        // TODO(kevinb): Figure out the right behavior, and document it
        return Ints.saturatedCast(5L + arraySize + (arraySize / 10));
    }


    @GwtCompatible(serializable = true)
    public static <E> ArrayList<E> newArrayList(Iterable<? extends E> elements) {
        checkNotNull(elements); // for GWT
        // Let ArrayList's sizing logic work, if possible
        return (elements instanceof Collection)
                ? new ArrayList<>(Collections2.cast(elements))
                : newArrayList(elements.iterator());
    }


    @GwtCompatible(serializable = true)
    public static <E> ArrayList<E> newArrayList(Iterator<? extends E> elements) {
        ArrayList<E> list = newArrayList();
        Iterators.addAll(list, elements);
        return list;
    }


    @GwtCompatible(serializable = true)
    public static <E> ArrayList<E> newArrayListWithCapacity(
            int initialArraySize) {
        checkNonnegative(initialArraySize, "initialArraySize"); // for GWT.
        return new ArrayList<>(initialArraySize);
    }


    // LinkedList


    @GwtCompatible(serializable = true)
    public static <E> LinkedList<E> newLinkedList() {
        return new LinkedList<>();
    }


    public static <E> List<E> asList(@Nullable E first, E[] rest) {
        return new OnePlusArrayList<>(first, rest);
    }


    private static class OnePlusArrayList<E> extends AbstractList<E>
            implements Serializable, RandomAccess {
        final E first;
        final E[] rest;

        OnePlusArrayList(@Nullable E first, E[] rest) {
            this.first = first;
            this.rest = checkNotNull(rest);
        }

        @Override
        public int size() {
            return rest.length + 1;
        }

        @Override
        public E get(int index) {
            // check explicitly so the IOOBE will have the right message
            checkElementIndex(index, size());
            return (index == 0) ? first : rest[index - 1];
        }

        private static final long serialVersionUID = 0;
    }


    public static <F, T> List<T> transform(
            List<F> fromList, Function<? super F, ? extends T> function) {
        return (fromList instanceof RandomAccess)
                ? new TransformingRandomAccessList<>(fromList, function)
                : new TransformingSequentialList<>(fromList, function);
    }


    private static class TransformingSequentialList<F, T>
            extends AbstractSequentialList<T> implements Serializable {
        final List<F> fromList;
        final Function<? super F, ? extends T> function;

        TransformingSequentialList(
                List<F> fromList, Function<? super F, ? extends T> function) {
            this.fromList = checkNotNull(fromList);
            this.function = checkNotNull(function);
        }

        @Override
        public void clear() {
            fromList.clear();
        }

        @Override
        public int size() {
            return fromList.size();
        }

        @Override
        public ListIterator<T> listIterator(final int index) {
            return new TransformedListIterator<F, T>(fromList.listIterator(index)) {
                @Override
                T transform(F from) {
                    return function.apply(from);
                }
            };
        }

        private static final long serialVersionUID = 0;
    }


    private static class TransformingRandomAccessList<F, T>
            extends AbstractList<T> implements RandomAccess, Serializable {
        final List<F> fromList;
        final Function<? super F, ? extends T> function;

        TransformingRandomAccessList(
                List<F> fromList, Function<? super F, ? extends T> function) {
            this.fromList = checkNotNull(fromList);
            this.function = checkNotNull(function);
        }

        @Override
        public void clear() {
            fromList.clear();
        }

        @Override
        public T get(int index) {
            return function.apply(fromList.get(index));
        }

        @Override
        public Iterator<T> iterator() {
            return listIterator();
        }

        @Override
        public ListIterator<T> listIterator(int index) {
            return new TransformedListIterator<F, T>(fromList.listIterator(index)) {
                @Override
                T transform(F from) {
                    return function.apply(from);
                }
            };
        }

        @Override
        public boolean isEmpty() {
            return fromList.isEmpty();
        }

        @Override
        public T remove(int index) {
            return function.apply(fromList.remove(index));
        }

        @Override
        public int size() {
            return fromList.size();
        }

        private static final long serialVersionUID = 0;
    }


    @Beta
    public static ImmutableList<Character> charactersOf(String string) {
        return new StringAsImmutableList(checkNotNull(string));
    }

    @SuppressWarnings("serial") // serialized using ImmutableList serialization
    private static final class StringAsImmutableList
            extends ImmutableList<Character> {

        private final String string;

        StringAsImmutableList(String string) {
            this.string = string;
        }

        @Override
        public int indexOf(@Nullable Object object) {
            return (object instanceof Character)
                    ? string.indexOf((Character) object) : -1;
        }

        @Override
        public int lastIndexOf(@Nullable Object object) {
            return (object instanceof Character)
                    ? string.lastIndexOf((Character) object) : -1;
        }

        @Override
        public ImmutableList<Character> subList(
                int fromIndex, int toIndex) {
            checkPositionIndexes(fromIndex, toIndex, size()); // for GWT
            return charactersOf(string.substring(fromIndex, toIndex));
        }

        @Override
        boolean isPartialView() {
            return false;
        }

        @Override
        public Character get(int index) {
            checkElementIndex(index, size()); // for GWT
            return string.charAt(index);
        }

        @Override
        public int size() {
            return string.length();
        }
    }


    public static <T> List<T> reverse(List<T> list) {
        if (list instanceof ImmutableList) {
            return ((ImmutableList<T>) list).reverse();
        } else if (list instanceof ReverseList) {
            return ((ReverseList<T>) list).getForwardList();
        } else if (list instanceof RandomAccess) {
            return new RandomAccessReverseList<>(list);
        } else {
            return new ReverseList<>(list);
        }
    }

    private static class ReverseList<T> extends AbstractList<T> {
        private final List<T> forwardList;

        ReverseList(List<T> forwardList) {
            this.forwardList = checkNotNull(forwardList);
        }

        List<T> getForwardList() {
            return forwardList;
        }

        private int reverseIndex(int index) {
            int size = size();
            checkElementIndex(index, size);
            return (size - 1) - index;
        }

        private int reversePosition(int index) {
            int size = size();
            checkPositionIndex(index, size);
            return size - index;
        }

        @Override
        public void add(int index, @Nullable T element) {
            forwardList.add(reversePosition(index), element);
        }

        @Override
        public void clear() {
            forwardList.clear();
        }

        @Override
        public T remove(int index) {
            return forwardList.remove(reverseIndex(index));
        }

        @Override
        protected void removeRange(int fromIndex, int toIndex) {
            subList(fromIndex, toIndex).clear();
        }

        @Override
        public T set(int index, @Nullable T element) {
            return forwardList.set(reverseIndex(index), element);
        }

        @Override
        public T get(int index) {
            return forwardList.get(reverseIndex(index));
        }

        @Override
        public int size() {
            return forwardList.size();
        }

        @Override
        public List<T> subList(int fromIndex, int toIndex) {
            checkPositionIndexes(fromIndex, toIndex, size());
            return reverse(forwardList.subList(
                    reversePosition(toIndex), reversePosition(fromIndex)));
        }

        @Override
        public Iterator<T> iterator() {
            return listIterator();
        }

        @Override
        public ListIterator<T> listIterator(int index) {
            int start = reversePosition(index);
            final ListIterator<T> forwardIterator = forwardList.listIterator(start);
            return new ListIterator<T>() {

                boolean canRemoveOrSet;

                @Override
                public void add(T e) {
                    forwardIterator.add(e);
                    forwardIterator.previous();
                    canRemoveOrSet = false;
                }

                @Override
                public boolean hasNext() {
                    return forwardIterator.hasPrevious();
                }

                @Override
                public boolean hasPrevious() {
                    return forwardIterator.hasNext();
                }

                @Override
                public T next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    canRemoveOrSet = true;
                    return forwardIterator.previous();
                }

                @Override
                public int nextIndex() {
                    return reversePosition(forwardIterator.nextIndex());
                }

                @Override
                public T previous() {
                    if (!hasPrevious()) {
                        throw new NoSuchElementException();
                    }
                    canRemoveOrSet = true;
                    return forwardIterator.next();
                }

                @Override
                public int previousIndex() {
                    return nextIndex() - 1;
                }

                @Override
                public void remove() {
                    checkRemove(canRemoveOrSet);
                    forwardIterator.remove();
                    canRemoveOrSet = false;
                }

                @Override
                public void set(T e) {
                    checkState(canRemoveOrSet);
                    forwardIterator.set(e);
                }
            };
        }
    }

    private static class RandomAccessReverseList<T> extends ReverseList<T>
            implements RandomAccess {
        RandomAccessReverseList(List<T> forwardList) {
            super(forwardList);
        }
    }


    static int hashCodeImpl(List<?> list) {
        // TODO(lowasser): worth optimizing for RandomAccess?
        int hashCode = 1;
        for (Object o : list) {
            hashCode = 31 * hashCode + (o == null ? 0 : o.hashCode());

            hashCode = ~~hashCode;
            // needed to deal with GWT integer overflow
        }
        return hashCode;
    }


    static boolean equalsImpl(List<?> thisList, @Nullable Object other) {
        if (other == checkNotNull(thisList)) {
            return true;
        }
        if (!(other instanceof List)) {
            return false;
        }
        List<?> otherList = (List<?>) other;
        int size = thisList.size();
        if (size != otherList.size()) {
            return false;
        }
        if (thisList instanceof RandomAccess && otherList instanceof RandomAccess) {
            // avoid allocation and use the faster loop
            for (int i = 0; i < size; i++) {
                if (!Objects.equal(thisList.get(i), otherList.get(i))) {
                    return false;
                }
            }
            return true;
        } else {
            return Iterators.elementsEqual(thisList.iterator(), otherList.iterator());
        }
    }


    static <E> boolean addAllImpl(
            List<E> list, int index, Iterable<? extends E> elements) {
        boolean changed = false;
        ListIterator<E> listIterator = list.listIterator(index);
        for (E e : elements) {
            listIterator.add(e);
            changed = true;
        }
        return changed;
    }


    static int indexOfImpl(List<?> list, @Nullable Object element) {
        if (list instanceof RandomAccess) {
            return indexOfRandomAccess(list, element);
        } else {
            ListIterator<?> listIterator = list.listIterator();
            while (listIterator.hasNext()) {
                if (Objects.equal(element, listIterator.next())) {
                    return listIterator.previousIndex();
                }
            }
            return -1;
        }
    }

    private static int indexOfRandomAccess(List<?> list, @Nullable Object element) {
        int size = list.size();
        if (element == null) {
            for (int i = 0; i < size; i++) {
                if (list.get(i) == null) {
                    return i;
                }
            }
        } else {
            for (int i = 0; i < size; i++) {
                if (element.equals(list.get(i))) {
                    return i;
                }
            }
        }
        return -1;
    }


    static int lastIndexOfImpl(List<?> list, @Nullable Object element) {
        if (list instanceof RandomAccess) {
            return lastIndexOfRandomAccess(list, element);
        } else {
            ListIterator<?> listIterator = list.listIterator(list.size());
            while (listIterator.hasPrevious()) {
                if (Objects.equal(element, listIterator.previous())) {
                    return listIterator.nextIndex();
                }
            }
            return -1;
        }
    }

    private static int lastIndexOfRandomAccess(List<?> list, @Nullable Object element) {
        if (element == null) {
            for (int i = list.size() - 1; i >= 0; i--) {
                if (list.get(i) == null) {
                    return i;
                }
            }
        } else {
            for (int i = list.size() - 1; i >= 0; i--) {
                if (element.equals(list.get(i))) {
                    return i;
                }
            }
        }
        return -1;
    }


    static <E> ListIterator<E> listIteratorImpl(List<E> list, int index) {
        return new AbstractListWrapper<>(list).listIterator(index);
    }


    static <E> List<E> subListImpl(
            final List<E> list, int fromIndex, int toIndex) {
        List<E> wrapper;
        if (list instanceof RandomAccess) {
            wrapper = new RandomAccessListWrapper<E>(list) {
                @Override
                public ListIterator<E> listIterator(int index) {
                    return backingList.listIterator(index);
                }

            };
        } else {
            wrapper = new AbstractListWrapper<E>(list) {
                @Override
                public ListIterator<E> listIterator(int index) {
                    return backingList.listIterator(index);
                }

            };
        }
        return wrapper.subList(fromIndex, toIndex);
    }

    private static class AbstractListWrapper<E> extends AbstractList<E> {
        final List<E> backingList;

        AbstractListWrapper(List<E> backingList) {
            this.backingList = checkNotNull(backingList);
        }

        @Override
        public void add(int index, E element) {
            backingList.add(index, element);
        }

        @Override
        public boolean addAll(int index, Collection<? extends E> c) {
            return backingList.addAll(index, c);
        }

        @Override
        public E get(int index) {
            return backingList.get(index);
        }

        @Override
        public E remove(int index) {
            return backingList.remove(index);
        }

        @Override
        public E set(int index, E element) {
            return backingList.set(index, element);
        }

        @Override
        public boolean contains(Object o) {
            return backingList.contains(o);
        }

        @Override
        public int size() {
            return backingList.size();
        }
    }

    private static class RandomAccessListWrapper<E>
            extends AbstractListWrapper<E> implements RandomAccess {
        RandomAccessListWrapper(List<E> backingList) {
            super(backingList);
        }
    }


    static <T> List<T> cast(Iterable<T> iterable) {
        return (List<T>) iterable;
    }
}
