

package com.google.common.util.concurrent;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;


@GwtCompatible
public class ExecutionError extends Error {


    public ExecutionError(@Nullable Error cause) {
        super(cause);
    }

    private static final long serialVersionUID = 0;
}
