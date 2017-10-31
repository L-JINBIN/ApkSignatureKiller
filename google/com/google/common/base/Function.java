

package com.google.common.base;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;


@GwtCompatible
public interface Function<F, T> {

    @Nullable
    T apply(@Nullable F input);


    @Override
    boolean equals(@Nullable Object object);
}
