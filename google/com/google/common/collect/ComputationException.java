

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;


@GwtCompatible
public class ComputationException extends RuntimeException {

    public ComputationException(@Nullable Throwable cause) {
        super(cause);
    }

    private static final long serialVersionUID = 0;
}
