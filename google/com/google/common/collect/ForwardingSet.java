

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;

import java.util.Collection;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;


@GwtCompatible
public abstract class ForwardingSet<E> extends ForwardingCollection<E> implements Set<E> {
    // TODO(lowasser): identify places where thread safety is actually lost


    protected ForwardingSet() {
    }

    @Override
    protected abstract Set<E> delegate();

    @Override
    public boolean equals(@Nullable Object object) {
        return object == this || delegate().equals(object);
    }

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }


    @Override
    protected boolean standardRemoveAll(Collection<?> collection) {
        return Sets.removeAllImpl(this, checkNotNull(collection)); // for GWT
    }


    protected boolean standardEquals(@Nullable Object object) {
        return Sets.equalsImpl(this, object);
    }


    protected int standardHashCode() {
        return Sets.hashCodeImpl(this);
    }
}
