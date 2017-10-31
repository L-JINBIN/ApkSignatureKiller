

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Ints;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ObjectArrays.checkElementNotNull;


@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial") // we're overriding default serialization
public abstract class ImmutableSet<E> extends ImmutableCollection<E> implements Set<E> {

    @SuppressWarnings({"unchecked"})
    // fully variant implementation (never actually produces any Es)
    public static <E> ImmutableSet<E> of() {
        return (ImmutableSet<E>) RegularImmutableSet.EMPTY;
    }


    public static <E> ImmutableSet<E> of(E element) {
        return new SingletonImmutableSet<>(element);
    }


    public static <E> ImmutableSet<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E... others) {
        final int paramCount = 6;
        Object[] elements = new Object[paramCount + others.length];
        elements[0] = e1;
        elements[1] = e2;
        elements[2] = e3;
        elements[3] = e4;
        elements[4] = e5;
        elements[5] = e6;
        System.arraycopy(others, 0, elements, paramCount, others.length);
        return construct(elements.length, elements);
    }


    private static <E> ImmutableSet<E> construct(int n, Object... elements) {
        switch (n) {
            case 0:
                return of();
            case 1:
                @SuppressWarnings("unchecked") // safe; elements contains only E's
                        E elem = (E) elements[0];
                return of(elem);
            default:
                // continue below to handle the general case
        }
        int tableSize = chooseTableSize(n);
        Object[] table = new Object[tableSize];
        int mask = tableSize - 1;
        int hashCode = 0;
        int uniques = 0;
        for (int i = 0; i < n; i++) {
            Object element = checkElementNotNull(elements[i], i);
            int hash = element.hashCode();
            for (int j = Hashing.smear(hash); ; j++) {
                int index = j & mask;
                Object value = table[index];
                if (value == null) {
                    // Came to an empty slot. Put the element here.
                    elements[uniques++] = element;
                    table[index] = element;
                    hashCode += hash;
                    break;
                } else if (value.equals(element)) {
                    break;
                }
            }
        }
        Arrays.fill(elements, uniques, n, null);
        if (uniques == 1) {
            // There is only one element or elements are all duplicates
            @SuppressWarnings("unchecked") // we are careful to only pass in E
                    E element = (E) elements[0];
            return new SingletonImmutableSet<>(element, hashCode);
        } else if (tableSize != chooseTableSize(uniques)) {
            // Resize the table when the array includes too many duplicates.
            // when this happens, we have already made a copy
            return construct(uniques, elements);
        } else {
            Object[] uniqueElements =
                    (uniques < elements.length)
                            ? ObjectArrays.arraysCopyOf(elements, uniques)
                            : elements;
            return new RegularImmutableSet<>(uniqueElements, hashCode, table, mask);
        }
    }

    // We use power-of-2 tables, and this is the highest int that's a power of 2
    static final int MAX_TABLE_SIZE = Ints.MAX_POWER_OF_TWO;

    // Represents how tightly we can pack things, as a maximum.
    private static final double DESIRED_LOAD_FACTOR = 0.7;

    // If the set has this many elements, it will "max out" the table size
    private static final int CUTOFF = (int) (MAX_TABLE_SIZE * DESIRED_LOAD_FACTOR);


    @VisibleForTesting
    static int chooseTableSize(int setSize) {
        // Correct the size for open addressing to match desired load factor.
        if (setSize < CUTOFF) {
            // Round up to the next highest power of 2.
            int tableSize = Integer.highestOneBit(setSize - 1) << 1;
            while (tableSize * DESIRED_LOAD_FACTOR < setSize) {
                tableSize <<= 1;
            }
            return tableSize;
        }

        // The table can't be completely full or we'll get infinite reprobes
        checkArgument(setSize < MAX_TABLE_SIZE, "collection too large");
        return MAX_TABLE_SIZE;
    }


    public static <E> ImmutableSet<E> copyOf(Collection<? extends E> elements) {

        if (elements instanceof ImmutableSet && !(elements instanceof ImmutableSortedSet)) {
            @SuppressWarnings("unchecked") // all supported methods are covariant
                    ImmutableSet<E> set = (ImmutableSet<E>) elements;
            if (!set.isPartialView()) {
                return set;
            }
        } else if (elements instanceof EnumSet) {
            return copyOfEnumSet((EnumSet) elements);
        }
        Object[] array = elements.toArray();
        return construct(array.length, array);
    }


    public static <E> ImmutableSet<E> copyOf(Iterable<? extends E> elements) {
        return (elements instanceof Collection)
                ? copyOf((Collection<? extends E>) elements)
                : copyOf(elements.iterator());
    }


    public static <E> ImmutableSet<E> copyOf(Iterator<? extends E> elements) {
        // We special-case for 0 or 1 elements, but anything further is madness.
        if (!elements.hasNext()) {
            return of();
        }
        E first = elements.next();
        if (!elements.hasNext()) {
            return of(first);
        } else {
            return new ImmutableSet.Builder<E>()
                    .add(first)
                    .addAll(elements)
                    .build();
        }
    }


    public static <E> ImmutableSet<E> copyOf(E[] elements) {
        switch (elements.length) {
            case 0:
                return of();
            case 1:
                return of(elements[0]);
            default:
                return construct(elements.length, elements.clone());
        }
    }

    @SuppressWarnings("rawtypes") // necessary to compile against Java 8
    private static ImmutableSet copyOfEnumSet(EnumSet enumSet) {
        return ImmutableEnumSet.asImmutable(EnumSet.copyOf(enumSet));
    }

    ImmutableSet() {
    }


    boolean isHashCodeFast() {
        return false;
    }

    @Override
    public boolean equals(@Nullable Object object) {
        if (object == this) {
            return true;
        } else if (object instanceof ImmutableSet
                && isHashCodeFast()
                && ((ImmutableSet<?>) object).isHashCodeFast()
                && hashCode() != object.hashCode()) {
            return false;
        }
        return Sets.equalsImpl(this, object);
    }

    @Override
    public int hashCode() {
        return Sets.hashCodeImpl(this);
    }

    // This declaration is needed to make Set.iterator() and
    // ImmutableCollection.iterator() consistent.
    @Override
    public abstract UnmodifiableIterator<E> iterator();

    abstract static class Indexed<E> extends ImmutableSet<E> {
        abstract E get(int index);

        @Override
        public UnmodifiableIterator<E> iterator() {
            return asList().iterator();
        }

        @Override
        ImmutableList<E> createAsList() {
            return new ImmutableAsList<E>() {
                @Override
                public E get(int index) {
                    return Indexed.this.get(index);
                }

                @Override
                Indexed<E> delegateCollection() {
                    return Indexed.this;
                }
            };
        }
    }


    private static class SerializedForm implements Serializable {
        final Object[] elements;

        SerializedForm(Object[] elements) {
            this.elements = elements;
        }

        Object readResolve() {
            return copyOf(elements);
        }

        private static final long serialVersionUID = 0;
    }

    @Override
    Object writeReplace() {
        return new SerializedForm(toArray());
    }


    public static class Builder<E> extends ImmutableCollection.ArrayBasedBuilder<E> {


        public Builder() {
            this(DEFAULT_INITIAL_CAPACITY);
        }

        Builder(int capacity) {
            super(capacity);
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
        public ImmutableSet<E> build() {
            ImmutableSet<E> result = construct(size, contents);
            // construct has the side effect of deduping contents, so we update size
            // accordingly.
            size = result.size();
            return result;
        }
    }
}
