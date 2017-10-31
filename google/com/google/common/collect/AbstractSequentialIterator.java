

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;

import java.util.NoSuchElementException;


@GwtCompatible
public abstract class AbstractSequentialIterator<T> extends UnmodifiableIterator<T> {
    private T nextOrNull;


    protected AbstractSequentialIterator(@Nullable T firstOrNull) {
        this.nextOrNull = firstOrNull;
    }


    protected abstract T computeNext(T previous);

    @Override
    public final boolean hasNext() {
        return nextOrNull != null;
    }

    @Override
    public final T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        try {
            return nextOrNull;
        } finally {
            nextOrNull = computeNext(nextOrNull);
        }
    }
}
