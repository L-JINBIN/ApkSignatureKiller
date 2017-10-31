

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;


@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial")
abstract class ImmutableAsList<E> extends ImmutableList<E> {
    abstract ImmutableCollection<E> delegateCollection();

    @Override
    public boolean contains(Object target) {
        // The collection's contains() is at least as fast as ImmutableList's
        // and is often faster.
        return delegateCollection().contains(target);
    }

    @Override
    public int size() {
        return delegateCollection().size();
    }

    @Override
    public boolean isEmpty() {
        return delegateCollection().isEmpty();
    }

    @Override
    boolean isPartialView() {
        return delegateCollection().isPartialView();
    }


    @GwtIncompatible("serialization")
    static class SerializedForm implements Serializable {
        final ImmutableCollection<?> collection;

        SerializedForm(ImmutableCollection<?> collection) {
            this.collection = collection;
        }

        Object readResolve() {
            return collection.asList();
        }

        private static final long serialVersionUID = 0;
    }

    @GwtIncompatible("serialization")
    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Use SerializedForm");
    }

    @GwtIncompatible("serialization")
    @Override
    Object writeReplace() {
        return new SerializedForm(delegateCollection());
    }
}
