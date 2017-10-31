

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.CollectPreconditions.checkNonnegative;
import static com.google.common.collect.ObjectArrays.checkElementsNotNull;


@GwtCompatible(emulated = true)
@SuppressWarnings("serial") // we're overriding default serialization
// TODO(kevinb): I think we should push everything down to "BaseImmutableCollection" or something,
// just to do everything we can to emphasize the "practically an interface" nature of this class.
public abstract class ImmutableCollection<E> extends AbstractCollection<E> implements Serializable {

    ImmutableCollection() {
    }


    @Override
    public abstract UnmodifiableIterator<E> iterator();

    @Override
    public final Object[] toArray() {
        int size = size();
        if (size == 0) {
            return ObjectArrays.EMPTY_ARRAY;
        }
        Object[] result = new Object[size];
        copyIntoArray(result, 0);
        return result;
    }

    @Override
    public final <T> T[] toArray(T[] other) {
        checkNotNull(other);
        int size = size();
        if (other.length < size) {
            other = ObjectArrays.newArray(other, size);
        } else if (other.length > size) {
            other[size] = null;
        }
        copyIntoArray(other, 0);
        return other;
    }

    @Override
    public abstract boolean contains(@Nullable Object object);


    @Deprecated
    @Override
    public final boolean add(E e) {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    @Override
    public final boolean remove(Object object) {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    @Override
    public final boolean addAll(Collection<? extends E> newElements) {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    @Override
    public final boolean removeAll(Collection<?> oldElements) {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    @Override
    public final boolean retainAll(Collection<?> elementsToKeep) {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    @Override
    public final void clear() {
        throw new UnsupportedOperationException();
    }


    private transient ImmutableList<E> asList;


    public ImmutableList<E> asList() {
        ImmutableList<E> list = asList;
        return (list == null) ? (asList = createAsList()) : list;
    }

    ImmutableList<E> createAsList() {
        switch (size()) {
            case 0:
                return ImmutableList.of();
            case 1:
                return ImmutableList.of(iterator().next());
            default:
                return new RegularImmutableAsList<>(this, toArray());
        }
    }


    abstract boolean isPartialView();


    int copyIntoArray(Object[] dst, int offset) {
        for (E e : this) {
            dst[offset++] = e;
        }
        return offset;
    }

    Object writeReplace() {
        // We serialize by default to ImmutableList, the simplest thing that works.
        return new ImmutableList.SerializedForm(toArray());
    }


    public abstract static class Builder<E> {
        static final int DEFAULT_INITIAL_CAPACITY = 4;

        static int expandedCapacity(int oldCapacity, int minCapacity) {
            if (minCapacity < 0) {
                throw new AssertionError("cannot store more than MAX_VALUE elements");
            }
            // careful of overflow!
            int newCapacity = oldCapacity + (oldCapacity >> 1) + 1;
            if (newCapacity < minCapacity) {
                newCapacity = Integer.highestOneBit(minCapacity - 1) << 1;
            }
            if (newCapacity < 0) {
                newCapacity = Integer.MAX_VALUE;
                // guaranteed to be >= newCapacity
            }
            return newCapacity;
        }

        Builder() {
        }


        public abstract Builder<E> add(E element);


        public Builder<E> add(E... elements) {
            for (E element : elements) {
                add(element);
            }
            return this;
        }


        public Builder<E> addAll(Iterable<? extends E> elements) {
            for (E element : elements) {
                add(element);
            }
            return this;
        }


        public Builder<E> addAll(Iterator<? extends E> elements) {
            while (elements.hasNext()) {
                add(elements.next());
            }
            return this;
        }


        public abstract ImmutableCollection<E> build();
    }

    abstract static class ArrayBasedBuilder<E> extends ImmutableCollection.Builder<E> {
        Object[] contents;
        int size;

        ArrayBasedBuilder(int initialCapacity) {
            checkNonnegative(initialCapacity, "initialCapacity");
            this.contents = new Object[initialCapacity];
            this.size = 0;
        }


        private void ensureCapacity(int minCapacity) {
            if (contents.length < minCapacity) {
                this.contents =
                        ObjectArrays.arraysCopyOf(
                                this.contents, expandedCapacity(contents.length, minCapacity));
            }
        }

        @Override
        public ArrayBasedBuilder<E> add(E element) {
            checkNotNull(element);
            ensureCapacity(size + 1);
            contents[size++] = element;
            return this;
        }

        @Override
        public Builder<E> add(E... elements) {
            checkElementsNotNull(elements);
            ensureCapacity(size + elements.length);
            System.arraycopy(elements, 0, contents, size, elements.length);
            size += elements.length;
            return this;
        }

        @Override
        public Builder<E> addAll(Iterable<? extends E> elements) {
            if (elements instanceof Collection) {
                Collection<?> collection = (Collection<?>) elements;
                ensureCapacity(size + collection.size());
            }
            super.addAll(elements);
            return this;
        }
    }
}
