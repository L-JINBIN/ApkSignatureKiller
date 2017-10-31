

package com.google.common.util.concurrent;

import com.google.common.annotations.GwtCompatible;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;


@GwtCompatible
public interface ListenableFuture<V> extends Future<V> {

    void addListener(Runnable listener, Executor executor);
}
