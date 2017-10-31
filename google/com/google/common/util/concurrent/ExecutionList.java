

package com.google.common.util.concurrent;

import android.support.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;

import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;


public final class ExecutionList {
    // Logger to log exceptions caught when running runnables.
    @VisibleForTesting
    static final Logger log = Logger.getLogger(ExecutionList.class.getName());


    private RunnableExecutorPair runnables;

    private boolean executed;


    public ExecutionList() {
    }


    public void add(Runnable runnable, Executor executor) {
        // Fail fast on a null.  We throw NPE here because the contract of Executor states that it
        // throws NPE on null listener, so we propagate that contract up into the add method as well.
        checkNotNull(runnable, "Runnable was null.");
        checkNotNull(executor, "Executor was null.");

        // Lock while we check state.  We must maintain the lock while adding the new pair so that
        // another thread can't run the list out from under us. We only add to the list if we have not
        // yet started execution.
        synchronized (this) {
            if (!executed) {
                runnables = new RunnableExecutorPair(runnable, executor, runnables);
                return;
            }
        }
        // Execute the runnable immediately. Because of scheduling this may end up getting called before
        // some of the previously added runnables, but we're OK with that.  If we want to change the
        // contract to guarantee ordering among runnables we'd have to modify the logic here to allow
        // it.
        executeListener(runnable, executor);
    }


    public void execute() {
        // Lock while we update our state so the add method above will finish adding any listeners
        // before we start to run them.
        RunnableExecutorPair list;
        synchronized (this) {
            if (executed) {
                return;
            }
            executed = true;
            list = runnables;
            runnables = null;  // allow GC to free listeners even if this stays around for a while.
        }
        // If we succeeded then list holds all the runnables we to execute.  The pairs in the stack are
        // in the opposite order from how they were added so we need to reverse the list to fulfill our
        // contract.
        // This is somewhat annoying, but turns out to be very fast in practice.  Alternatively, we
        // could drop the contract on the method that enforces this queue like behavior since depending
        // on it is likely to be a bug anyway.

        // N.B. All writes to the list and the next pointers must have happened before the above
        // synchronized block, so we can iterate the list without the lock held here.
        RunnableExecutorPair reversedList = null;
        while (list != null) {
            RunnableExecutorPair tmp = list;
            list = list.next;
            tmp.next = reversedList;
            reversedList = tmp;
        }
        while (reversedList != null) {
            executeListener(reversedList.runnable, reversedList.executor);
            reversedList = reversedList.next;
        }
    }


    private static void executeListener(Runnable runnable, Executor executor) {
        try {
            executor.execute(runnable);
        } catch (RuntimeException e) {
            // Log it and keep going, bad runnable and/or executor.  Don't punish the other runnables if
            // we're given a bad one.  We only catch RuntimeException because we want Errors to propagate
            // up.
            log.log(Level.SEVERE, "RuntimeException while executing runnable "
                    + runnable + " with executor " + executor, e);
        }
    }

    private static final class RunnableExecutorPair {
        final Runnable runnable;
        final Executor executor;
        @Nullable
        RunnableExecutorPair next;

        RunnableExecutorPair(Runnable runnable, Executor executor, RunnableExecutorPair next) {
            this.runnable = runnable;
            this.executor = executor;
            this.next = next;
        }
    }
}
