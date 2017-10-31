

package com.google.common.base;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;

import java.io.Serializable;

import static com.google.common.base.Preconditions.checkNotNull;


@GwtCompatible(serializable = true)
public abstract class Optional<T> implements Serializable {

    public static <T> Optional<T> absent() {
        return Absent.withType();
    }


    public static <T> Optional<T> of(T reference) {
        return new Present<>(checkNotNull(reference));
    }


    Optional() {
    }


    public abstract boolean isPresent();


    public abstract T get();


    @Override
    public abstract boolean equals(@Nullable Object object);


    @Override
    public abstract int hashCode();


    @Override
    public abstract String toString();


    private static final long serialVersionUID = 0;
}
