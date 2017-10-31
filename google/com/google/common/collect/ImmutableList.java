

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;

import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkPositionIndexes;
import static com.google.common.collect.ObjectArrays.arraysCopyOf;
import static com.google.common.collect.ObjectArrays.checkElementsNotNull;
import static com.google.common.collect.RegularImmutableList.EMPTY;


@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial") // we're overriding default serialization
public abstract class ImmutableList<E> extends ImmutableCollection<E>
        implements List<E>, RandomAccess {

    // Casting to any type is safe because the list will never hold any elements.
    @SuppressWarnings("unchecked")
    public static <E> ImmutableList<E> of() {
        return (ImmutableList<E>) EMPTY;
    }


    public static <E> ImmutableList<E> of(E element) {
        return new SingletonImmutableList<>(element);
    }


    public static <E> ImmutableList<E> of(E e1, E e2) {
        return construct(e1, e2);
    }


    public static <E> ImmutableList<E> of(E e1, E e2, E e3) {
        return construct(e1, e2, e3);
    }


    public static <E> ImmutableList<E> of(E e1, E e2, E e3, E e4) {
        return construct(e1, e2, e3, e4);
    }


    // These go up to eleven. After that, you just get the varargs form, and
    // whatever warnings might come along with it. :(


    public static <E> ImmutableList<E> copyOf(Iterable<? extends E> elements) {
        checkNotNull(elements); // TODO(kevinb): is this here only for GWT?
        return (elements instanceof Collection)
                ? copyOf((Collection<? extends E>) elements)
                : copyOf(elements.iterator());
    }


    public static <E> ImmutableList<E> copyOf(Collection<? extends E> elements) {
        if (elements instanceof ImmutableCollection) {
            @SuppressWarnings("unchecked") // all supported methods are covariant
                    ImmutableList<E> list = ((ImmutableCollection<E>) elements).asList();
            return list.isPartialView()
                    ? ImmutableList.<E>asImmutableList(list.toArray())
                    : list;
        }
        return construct(elements.toArray());
    }


    public static <E> ImmutableList<E> copyOf(Iterator<? extends E> elements) {
        // We special-case for 0 or 1 elements, but going further is madness.
        if (!elements.hasNext()) {
            return of();
        }
        E first = elements.next();
        if (!elements.hasNext()) {
            return of(first);
        } else {
            return new ImmutableList.Builder<E>()
                    .add(first)
                    .addAll(elements)
                    .build();
        }
    }


    public static <E> ImmutableList<E> copyOf(E[] elements) {
        switch (elements.length) {
            case 0:
                return ImmutableList.of();
            case 1:
                return new SingletonImmutableList<>(elements[0]);
            default:
                return new RegularImmutableList<>(checkElementsNotNull(elements.clone()));
        }
    }


    private static <E> ImmutableList<E> construct(Object... elements) {
        return asImmutableList(checkElementsNotNull(elements));
    }


    static <E> ImmutableList<E> asImmutableList(Object[] elements) {
        return asImmutableList(elements, elements.length);
    }


    static <E> ImmutableList<E> asImmutableList(Object[] elements, int length) {
        switch (length) {
            case 0:
                return of();
            case 1:
                @SuppressWarnings("unchecked") // collection had only Es in it
                        ImmutableList<E> list = new SingletonImmutableList<>((E) elements[0]);
                return list;
            default:
                if (length < elements.length) {
                    elements = arraysCopyOf(elements, length);
                }
                return new RegularImmutableList<>(elements);
        }
    }

    ImmutableList() {
    }

    // This declaration is needed to make List.iterator() and
    // ImmutableCollection.iterator() consistent.
    @Override
    public UnmodifiableIterator<E> iterator() {
        return listIterator();
    }

    @Override
    public UnmodifiableListIterator<E> listIterator() {
        return listIterator(0);
    }

    @Override
    public UnmodifiableListIterator<E> listIterator(int index) {
        return new AbstractIndexedListIterator<E>(size(), index) {
            @Override
            protected E get(int index) {
                return ImmutableList.this.get(index);
            }
        };
    }

    @Override
    public int indexOf(@Nullable Object object) {
        return (object == null) ? -1 : Lists.indexOfImpl(this, object);
    }

    @Override
    public int lastIndexOf(@Nullable Object object) {
        return (object == null) ? -1 : Lists.lastIndexOfImpl(this, object);
    }

    @Override
    public boolean contains(@Nullable Object object) {
        return indexOf(object) >= 0;
    }

    // constrain the return type to ImmutableList<E>


    @Override
    public ImmutableList<E> subList(int fromIndex, int toIndex) {
        checkPositionIndexes(fromIndex, toIndex, size());
        int length = toIndex - fromIndex;
        if (length == size()) {
            return this;
        }
        switch (length) {
            case 0:
                return of();
            case 1:
                return of(get(fromIndex));
            default:
                return subListUnchecked(fromIndex, toIndex);
        }
    }


    ImmutableList<E> subListUnchecked(int fromIndex, int toIndex) {
        return new SubList(fromIndex, toIndex - fromIndex);
    }

    class SubList extends ImmutableList<E> {
        final transient int offset;
        final transient int length;

        SubList(int offset, int length) {
            this.offset = offset;
            this.length = length;
        }

        @Override
        public int size() {
            return length;
        }

        @Override
        public E get(int index) {
            checkElementIndex(index, length);
            return ImmutableList.this.get(index + offset);
        }

        @Override
        public ImmutableList<E> subList(int fromIndex, int toIndex) {
            checkPositionIndexes(fromIndex, toIndex, length);
            return ImmutableList.this.subList(fromIndex + offset, toIndex + offset);
        }

        @Override
        boolean isPartialView() {
            return true;
        }
    }


    @Deprecated
    @Override
    public final boolean addAll(int index, Collection<? extends E> newElements) {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    @Override
    public final E set(int index, E element) {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    @Override
    public final void add(int index, E element) {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    @Override
    public final E remove(int index) {
        throw new UnsupportedOperationException();
    }


    @Override
    public final ImmutableList<E> asList() {
        return this;
    }

    @Override
    int copyIntoArray(Object[] dst, int offset) {
        // this loop is faster for RandomAccess instances, which ImmutableLists are
        int size = size();
        for (int i = 0; i < size; i++) {
            dst[offset + i] = get(i);
        }
        return offset + size;
    }


    public ImmutableList<E> reverse() {
        return (size() <= 1) ? this : new ReverseImmutableList<>(this);
    }

    private static class ReverseImmutableList<E> extends ImmutableList<E> {
        private final transient ImmutableList<E> forwardList;

        ReverseImmutableList(ImmutableList<E> backingList) {
            this.forwardList = backingList;
        }

        private int reverseIndex(int index) {
            return (size() - 1) - index;
        }

        private int reversePosition(int index) {
            return size() - index;
        }

        @Override
        public ImmutableList<E> reverse() {
            return forwardList;
        }

        @Override
        public boolean contains(@Nullable Object object) {
            return forwardList.contains(object);
        }

        @Override
        public int indexOf(@Nullable Object object) {
            int index = forwardList.lastIndexOf(object);
            return (index >= 0) ? reverseIndex(index) : -1;
        }

        @Override
        public int lastIndexOf(@Nullable Object object) {
            int index = forwardList.indexOf(object);
            return (index >= 0) ? reverseIndex(index) : -1;
        }

        @Override
        public ImmutableList<E> subList(int fromIndex, int toIndex) {
            checkPositionIndexes(fromIndex, toIndex, size());
            return forwardList
                    .subList(reversePosition(toIndex), reversePosition(fromIndex))
                    .reverse();
        }

        @Override
        public E get(int index) {
            checkElementIndex(index, size());
            return forwardList.get(reverseIndex(index));
        }

        @Override
        public int size() {
            return forwardList.size();
        }

        @Override
        boolean isPartialView() {
            return forwardList.isPartialView();
        }
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return Lists.equalsImpl(this, obj);
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        int n = size();
        for (int i = 0; i < n; i++) {
            hashCode = 31 * hashCode + get(i).hashCode();

            hashCode = ~~hashCode;
            // needed to deal with GWT integer overflow
        }
        return hashCode;
    }


    static class SerializedForm implements Serializable {
        final Object[] elements;

        SerializedForm(Object[] elements) {
            this.elements = elements;
        }

        Object readResolve() {
            return copyOf(elements);
        }

        private static final long serialVersionUID = 0;
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Use SerializedForm");
    }

    @Override
    Object writeReplace() {
        return new SerializedForm(toArray());
    }


    public static <E> Builder<E> builder() {
        return new Builder<>();
    }


    public static final class Builder<E> extends ImmutableCollection.ArrayBasedBuilder<E> {

        public Builder() {
            this(DEFAULT_INITIAL_CAPACITY);
        }

        // TODO(lowasser): consider exposing this
        Builder(int capacity) {
            super(capacity);
        }


        @Override
        public Builder<E> add(E element) {
            super.add(element);
            return this;
        }


        @Override
        public Builder<E> addAll(Iterable<? extends E> elements) {
            super.addAll(elements);
            return this;
        }


        @Override
        public Builder<E> add(E... elements) {
            super.add(elements);
            return this;
        }


        @Override
        public Builder<E> addAll(Iterator<? extends E> elements) {
            super.addAll(elements);
            return this;
        }


        @Override
        public ImmutableList<E> build() {
            return asImmutableList(contents, size);
        }
    }
}
