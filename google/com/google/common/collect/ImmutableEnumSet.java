

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;

import java.io.Serializable;
import java.util.Collection;
import java.util.EnumSet;


@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial") // we're overriding default serialization
final class ImmutableEnumSet<E extends Enum<E>> extends ImmutableSet<E> {
    @SuppressWarnings("rawtypes") // necessary to compile against Java 8
    static ImmutableSet asImmutable(EnumSet set) {
        switch (set.size()) {
            case 0:
                return ImmutableSet.of();
            case 1:
                return ImmutableSet.of(Iterables.getOnlyElement(set));
            default:
                return new ImmutableEnumSet(set);
        }
    }


    private final transient EnumSet<E> delegate;

    private ImmutableEnumSet(EnumSet<E> delegate) {
        this.delegate = delegate;
    }

    @Override
    boolean isPartialView() {
        return false;
    }

    @Override
    public UnmodifiableIterator<E> iterator() {
        return Iterators.unmodifiableIterator(delegate.iterator());
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean contains(Object object) {
        return delegate.contains(object);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        if (collection instanceof ImmutableEnumSet<?>) {
            collection = ((ImmutableEnumSet<?>) collection).delegate;
        }
        return delegate.containsAll(collection);
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (object instanceof ImmutableEnumSet) {
            object = ((ImmutableEnumSet<?>) object).delegate;
        }
        return delegate.equals(object);
    }

    @Override
    boolean isHashCodeFast() {
        return true;
    }

    private transient int hashCode;

    @Override
    public int hashCode() {
        int result = hashCode;
        return (result == 0) ? hashCode = delegate.hashCode() : result;
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    // All callers of the constructor are restricted to <E extends Enum<E>>.
    @Override
    Object writeReplace() {
        return new EnumSerializedForm<>(delegate);
    }


    private static class EnumSerializedForm<E extends Enum<E>> implements Serializable {
        final EnumSet<E> delegate;

        EnumSerializedForm(EnumSet<E> delegate) {
            this.delegate = delegate;
        }

        Object readResolve() {
            // EJ2 #76: Write readObject() methods defensively.
            return new ImmutableEnumSet<>(delegate.clone());
        }

        private static final long serialVersionUID = 0;
    }
}
