

package com.google.common.base;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;


@GwtCompatible
public interface Predicate<T> {

    boolean apply(@Nullable T input);


    @Override
    boolean equals(@Nullable Object object);
}
