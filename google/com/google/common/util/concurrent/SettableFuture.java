

package com.google.common.util.concurrent;

import android.support.annotation.Nullable;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;


@GwtCompatible
public final class SettableFuture<V> extends AbstractFuture.TrustedFuture<V> {


    public static <V> SettableFuture<V> create() {
        return new SettableFuture<>();
    }


    private SettableFuture() {
    }

    @Override
    public boolean set(@Nullable V value) {
        return super.set(value);
    }

    @Override
    public boolean setException(Throwable throwable) {
        return super.setException(throwable);
    }

    @Beta
    @Override
    public boolean setFuture(ListenableFuture<? extends V> future) {
        return super.setFuture(future);
    }
}
