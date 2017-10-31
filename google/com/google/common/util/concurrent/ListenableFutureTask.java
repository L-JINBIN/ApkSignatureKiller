

package com.google.common.util.concurrent;

import android.support.annotation.Nullable;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;


public class ListenableFutureTask<V> extends FutureTask<V>
        implements ListenableFuture<V> {
    // TODO(cpovirk): explore ways of making ListenableFutureTask final. There are
    // some valid reasons such as BoundedQueueExecutorService to allow extends but it
    // would be nice to make it final to avoid unintended usage.

    // The execution list to hold our listeners.
    private final ExecutionList executionList = new ExecutionList();


    public static <V> ListenableFutureTask<V> create(Callable<V> callable) {
        return new ListenableFutureTask<>(callable);
    }


    public static <V> ListenableFutureTask<V> create(
            Runnable runnable, @Nullable V result) {
        return new ListenableFutureTask<>(runnable, result);
    }

    ListenableFutureTask(Callable<V> callable) {
        super(callable);
    }

    ListenableFutureTask(Runnable runnable, @Nullable V result) {
        super(runnable, result);
    }

    @Override
    public void addListener(Runnable listener, Executor exec) {
        executionList.add(listener, exec);
    }


    @Override
    protected void done() {
        executionList.execute();
    }
}
