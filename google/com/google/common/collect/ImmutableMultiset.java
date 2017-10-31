

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.WeakOuter;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;


@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial") // we're overriding default serialization
// TODO(lowasser): write an efficient asList() implementation
public abstract class ImmutableMultiset<E> extends ImmutableCollection<E> implements Multiset<E> {

    @SuppressWarnings("unchecked") // all supported methods are covariant
    public static <E> ImmutableMultiset<E> of() {
        return (ImmutableMultiset<E>) RegularImmutableMultiset.EMPTY;
    }


    @SuppressWarnings("unchecked") // generic array created but never written
    public static <E> ImmutableMultiset<E> of(E element) {
        return copyFromElements(element);
    }


    @SuppressWarnings("unchecked") //
    public static <E> ImmutableMultiset<E> of(E e1, E e2) {
        return copyFromElements(e1, e2);
    }


    @SuppressWarnings("unchecked") //
    public static <E> ImmutableMultiset<E> of(E e1, E e2, E e3) {
        return copyFromElements(e1, e2, e3);
    }


    @SuppressWarnings("unchecked") //
    public static <E> ImmutableMultiset<E> of(E e1, E e2, E e3, E e4) {
        return copyFromElements(e1, e2, e3, e4);
    }


    @SuppressWarnings("unchecked") //
    public static <E> ImmutableMultiset<E> of(E e1, E e2, E e3, E e4, E e5) {
        return copyFromElements(e1, e2, e3, e4, e5);
    }


    @SuppressWarnings("unchecked") //
    public static <E> ImmutableMultiset<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E... others) {
        return new Builder<E>()
                .add(e1)
                .add(e2)
                .add(e3)
                .add(e4)
                .add(e5)
                .add(e6)
                .add(others)
                .build();
    }


    public static <E> ImmutableMultiset<E> copyOf(E[] elements) {
        return copyFromElements(elements);
    }


    public static <E> ImmutableMultiset<E> copyOf(Iterable<? extends E> elements) {
        if (elements instanceof ImmutableMultiset) {
            @SuppressWarnings("unchecked") // all supported methods are covariant
                    ImmutableMultiset<E> result = (ImmutableMultiset<E>) elements;
            if (!result.isPartialView()) {
                return result;
            }
        }

        Multiset<? extends E> multiset =
                (elements instanceof Multiset)
                        ? Multisets.cast(elements)
                        : LinkedHashMultiset.create(elements);

        return copyFromEntries(multiset.entrySet());
    }

    private static <E> ImmutableMultiset<E> copyFromElements(E... elements) {
        Multiset<E> multiset = LinkedHashMultiset.create();
        Collections.addAll(multiset, elements);
        return copyFromEntries(multiset.entrySet());
    }

    static <E> ImmutableMultiset<E> copyFromEntries(
            Collection<? extends Entry<? extends E>> entries) {
        if (entries.isEmpty()) {
            return of();
        } else {
            return new RegularImmutableMultiset<>(entries);
        }
    }


    public static <E> ImmutableMultiset<E> copyOf(Iterator<? extends E> elements) {
        Multiset<E> multiset = LinkedHashMultiset.create();
        Iterators.addAll(multiset, elements);
        return copyFromEntries(multiset.entrySet());
    }

    ImmutableMultiset() {
    }

    @Override
    public UnmodifiableIterator<E> iterator() {
        final Iterator<Entry<E>> entryIterator = entrySet().iterator();
        return new UnmodifiableIterator<E>() {
            int remaining;
            E element;

            @Override
            public boolean hasNext() {
                return (remaining > 0) || entryIterator.hasNext();
            }

            @Override
            public E next() {
                if (remaining <= 0) {
                    Entry<E> entry = entryIterator.next();
                    element = entry.getElement();
                    remaining = entry.getCount();
                }
                remaining--;
                return element;
            }
        };
    }

    @Override
    public boolean contains(@Nullable Object object) {
        return count(object) > 0;
    }


    @Deprecated
    @Override
    public final int add(E element, int occurrences) {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    @Override
    public final int remove(Object element, int occurrences) {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    @Override
    public final int setCount(E element, int count) {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    @Override
    public final boolean setCount(E element, int oldCount, int newCount) {
        throw new UnsupportedOperationException();
    }

    @GwtIncompatible("not present in emulated superclass")
    @Override
    int copyIntoArray(Object[] dst, int offset) {
        for (Multiset.Entry<E> entry : entrySet()) {
            Arrays.fill(dst, offset, offset + entry.getCount(), entry.getElement());
            offset += entry.getCount();
        }
        return offset;
    }

    @Override
    public boolean equals(@Nullable Object object) {
        return Multisets.equalsImpl(this, object);
    }

    @Override
    public int hashCode() {
        return Sets.hashCodeImpl(entrySet());
    }

    @Override
    public String toString() {
        return entrySet().toString();
    }

    private transient ImmutableSet<Entry<E>> entrySet;

    @Override
    public ImmutableSet<Entry<E>> entrySet() {
        ImmutableSet<Entry<E>> es = entrySet;
        return (es == null) ? (entrySet = createEntrySet()) : es;
    }

    private final ImmutableSet<Entry<E>> createEntrySet() {
        return isEmpty() ? ImmutableSet.<Entry<E>>of() : new EntrySet();
    }

    abstract Entry<E> getEntry(int index);

    @WeakOuter
    private final class EntrySet extends ImmutableSet.Indexed<Entry<E>> {
        @Override
        boolean isPartialView() {
            return ImmutableMultiset.this.isPartialView();
        }

        @Override
        Entry<E> get(int index) {
            return getEntry(index);
        }

        @Override
        public int size() {
            return elementSet().size();
        }

        @Override
        public boolean contains(Object o) {
            if (o instanceof Entry) {
                Entry<?> entry = (Entry<?>) o;
                if (entry.getCount() <= 0) {
                    return false;
                }
                int count = count(entry.getElement());
                return count == entry.getCount();
            }
            return false;
        }

        @Override
        public int hashCode() {
            return ImmutableMultiset.this.hashCode();
        }

        // We can't label this with @Override, because it doesn't override anything
        // in the GWT emulated version.
        // TODO(cpovirk): try making all copies of this method @GwtIncompatible instead
        Object writeReplace() {
            return new EntrySetSerializedForm<>(ImmutableMultiset.this);
        }

        private static final long serialVersionUID = 0;
    }

    static class EntrySetSerializedForm<E> implements Serializable {
        final ImmutableMultiset<E> multiset;

        EntrySetSerializedForm(ImmutableMultiset<E> multiset) {
            this.multiset = multiset;
        }

        Object readResolve() {
            return multiset.entrySet();
        }
    }

    private static class SerializedForm implements Serializable {
        final Object[] elements;
        final int[] counts;

        SerializedForm(Multiset<?> multiset) {
            int distinct = multiset.entrySet().size();
            elements = new Object[distinct];
            counts = new int[distinct];
            int i = 0;
            for (Entry<?> entry : multiset.entrySet()) {
                elements[i] = entry.getElement();
                counts[i] = entry.getCount();
                i++;
            }
        }

        Object readResolve() {
            LinkedHashMultiset<Object> multiset = LinkedHashMultiset.create(elements.length);
            for (int i = 0; i < elements.length; i++) {
                multiset.add(elements[i], counts[i]);
            }
            return ImmutableMultiset.copyOf(multiset);
        }

        private static final long serialVersionUID = 0;
    }

    // We can't label this with @Override, because it doesn't override anything
    // in the GWT emulated version.
    Object writeReplace() {
        return new SerializedForm(this);
    }


    public static <E> Builder<E> builder() {
        return new Builder<>();
    }


    public static class Builder<E> extends ImmutableCollection.Builder<E> {
        final Multiset<E> contents;


        public Builder() {
            this(LinkedHashMultiset.<E>create());
        }

        Builder(Multiset<E> contents) {
            this.contents = contents;
        }


        @Override
        public Builder<E> add(E element) {
            contents.add(checkNotNull(element));
            return this;
        }


        public Builder<E> addCopies(E element, int occurrences) {
            contents.add(checkNotNull(element), occurrences);
            return this;
        }


        public Builder<E> setCount(E element, int count) {
            contents.setCount(checkNotNull(element), count);
            return this;
        }


        @Override
        public Builder<E> add(E... elements) {
            super.add(elements);
            return this;
        }


        @Override
        public Builder<E> addAll(Iterable<? extends E> elements) {
            if (elements instanceof Multiset) {
                Multiset<? extends E> multiset = Multisets.cast(elements);
                for (Entry<? extends E> entry : multiset.entrySet()) {
                    addCopies(entry.getElement(), entry.getCount());
                }
            } else {
                super.addAll(elements);
            }
            return this;
        }


        @Override
        public Builder<E> addAll(Iterator<? extends E> elements) {
            super.addAll(elements);
            return this;
        }


        @Override
        public ImmutableMultiset<E> build() {
            return copyOf(contents);
        }
    }
}
