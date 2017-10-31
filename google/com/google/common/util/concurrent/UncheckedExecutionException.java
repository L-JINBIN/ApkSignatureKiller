

package com.google.common.util.concurrent;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;


@GwtCompatible
public class UncheckedExecutionException extends RuntimeException {


    public UncheckedExecutionException(@Nullable Throwable cause) {
        super(cause);
    }

    private static final long serialVersionUID = 0;
}
