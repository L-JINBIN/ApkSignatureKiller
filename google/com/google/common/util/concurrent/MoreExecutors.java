

package com.google.common.util.concurrent;

import com.google.common.annotations.GwtCompatible;

import java.util.concurrent.Executor;


@GwtCompatible(emulated = true)
public final class MoreExecutors {
    private MoreExecutors() {
    }


    public static Executor directExecutor() {
        return DirectExecutor.INSTANCE;
    }


    private enum DirectExecutor implements Executor {
        INSTANCE;

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }

}
