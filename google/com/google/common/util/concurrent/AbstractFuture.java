

package com.google.common.util.concurrent;

import android.support.annotation.Nullable;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static java.util.concurrent.atomic.AtomicReferenceFieldUpdater.newUpdater;


@GwtCompatible(emulated = true)
public abstract class AbstractFuture<V> implements ListenableFuture<V> {


    abstract static class TrustedFuture<V> extends AbstractFuture<V> {
        // N.B. cancel is not overridden to be final, because many future utilities need to override
        // cancel in order to propagate cancellation to other futures.

        @Override
        public final V get() throws InterruptedException, ExecutionException {
            return super.get();
        }

        @Override
        public final V get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return super.get(timeout, unit);
        }

        @Override
        public final boolean isDone() {
            return super.isDone();
        }

        @Override
        public final boolean isCancelled() {
            return super.isCancelled();
        }

        @Override
        public final void addListener(Runnable listener, Executor executor) {
            super.addListener(listener, executor);
        }
    }

    // Logger to log exceptions caught when running listeners.
    private static final Logger log = Logger.getLogger(AbstractFuture.class.getName());

    // A heuristic for timed gets.  If the remaining timeout is less than this, spin instead of
    // blocking.  This value is what AbstractQueuedSynchronizer uses.
    private static final long SPIN_THRESHOLD_NANOS = 1000L;

    private static final AtomicHelper ATOMIC_HELPER;
    private static final AtomicReferenceFieldUpdater<Waiter, Thread> WAITER_THREAD_UPDATER;
    private static final AtomicReferenceFieldUpdater<Waiter, Waiter> WAITER_NEXT_UPDATER;
    private static final AtomicReferenceFieldUpdater<AbstractFuture, Waiter> WAITERS_UPDATER;
    private static final AtomicReferenceFieldUpdater<AbstractFuture, Listener> LISTENERS_UPDATER;
    private static final AtomicReferenceFieldUpdater<AbstractFuture, Object> VALUE_UPDATER;

    static {
        // The access control checks that ARFU does means the caller class has to be AbstractFuture
        // instead of SafeAtomicHelper, so we annoyingly define these here
        WAITER_THREAD_UPDATER = newUpdater(Waiter.class, Thread.class, "thread");
        WAITER_NEXT_UPDATER = newUpdater(Waiter.class, Waiter.class, "next");
        WAITERS_UPDATER = newUpdater(AbstractFuture.class, Waiter.class, "waiters");
        LISTENERS_UPDATER = newUpdater(AbstractFuture.class, Listener.class, "listeners");
        VALUE_UPDATER = newUpdater(AbstractFuture.class, Object.class, "value");
        ATOMIC_HELPER = new SafeAtomicHelper();
        // Prevent rare disastrous classloading in first call to LockSupport.park.
        // See: https://bugs.openjdk.java.net/browse/JDK-8074773
        Class<?> ensureLoaded = LockSupport.class;
    }


    private static final class Waiter {
        static final Waiter TOMBSTONE = new Waiter(false);

        @Nullable
        volatile Thread thread;
        @Nullable
        volatile Waiter next;

        // Constructor for the TOMBSTONE, avoids use of ATOMIC_HELPER in case this class is loaded
        // before the ATOMIC_HELPER.  Apparently this is possible on some android platforms.
        Waiter(@SuppressWarnings("unused") boolean ignored) {
        }

        Waiter() {
            // avoid volatile write, write is made visible by subsequent CAS on waiters field
            ATOMIC_HELPER.putThread(this, Thread.currentThread());
        }

        // non-volatile write to the next field. Should be made visible by subsequent CAS on waiters
        // field.
        void setNext(Waiter next) {
            ATOMIC_HELPER.putNext(this, next);
        }

        void unpark() {
            // This is racy with removeWaiter.  The consequence of the race is that we may spuriously
            // call unpark even though the thread has already removed itself from the list.  But even if
            // we did use a CAS, that race would still exist (it would just be ever so slightly smaller).
            Thread w = thread;
            if (w != null) {
                thread = null;
                LockSupport.unpark(w);
            }
        }
    }


    private void removeWaiter(Waiter node) {
        node.thread = null;  // mark as 'deleted'
        restart:
        while (true) {
            Waiter pred = null;
            Waiter curr = waiters;
            if (curr == Waiter.TOMBSTONE) {
                return;  // give up if someone is calling complete
            }
            Waiter succ;
            while (curr != null) {
                succ = curr.next;
                if (curr.thread != null) {  // we aren't unlinking this node, update pred.
                    pred = curr;
                } else if (pred != null) { // We are unlinking this node and it has a predecessor.
                    pred.next = succ;
                    if (pred.thread == null) {  // We raced with another node that unlinked pred. Restart.
                        continue restart;
                    }
                } else if (!ATOMIC_HELPER.casWaiters(this, curr, succ)) {  // We are unlinking head
                    continue restart;  // We raced with an add or complete
                }
                curr = succ;
            }
            break;
        }
    }


    private static final class Listener {
        static final Listener TOMBSTONE = new Listener(null, null);
        final Runnable task;
        final Executor executor;

        // writes to next are made visible by subsequent CAS's on the listeners field
        @Nullable
        Listener next;

        Listener(Runnable task, Executor executor) {
            this.task = task;
            this.executor = executor;
        }
    }


    private static final Object NULL = new Object();


    private static final class Failure {
        static final Failure FALLBACK_INSTANCE = new Failure(
                new Throwable("Failure occurred while trying to finish a future.") {
                    @Override
                    public synchronized Throwable fillInStackTrace() {
                        return this;  // no stack trace
                    }
                });
        final Throwable exception;

        Failure(Throwable exception) {
            this.exception = checkNotNull(exception);
        }
    }


    private static final class Cancellation {
        final boolean wasInterrupted;
        final Throwable cause;

        Cancellation(boolean wasInterrupted, Throwable cause) {
            this.wasInterrupted = wasInterrupted;
            this.cause = checkNotNull(cause);
        }
    }


    private final class SetFuture implements Runnable {
        final ListenableFuture<? extends V> future;

        SetFuture(ListenableFuture<? extends V> future) {
            this.future = future;
        }

        @Override
        public void run() {
            if (value != this) {
                // nothing to do, we must have been cancelled
                return;
            }
            completeWithFuture(future, this);
        }
    }

    // TODO(lukes): investigate using the @Contended annotation on these fields when jdk8 is
    // available.

    private volatile Object value;


    private volatile Listener listeners;


    private volatile Waiter waiters;


    protected AbstractFuture() {
    }


    // Gets and Timed Gets
    //
    // * Be responsive to interruption
    // * Don't create Waiter nodes if you aren't going to park, this helps reduce contention on the
    //   waiters field.
    // * Future completion is defined by when #value becomes non-null/non SetFuture
    // * Future completion can be observed if the waiters field contains a TOMBSTONE

    // Timed Get
    // There are a few design constraints to consider
    // * We want to be responsive to small timeouts, unpark() has non trivial latency overheads (I
    //   have observed 12 micros on 64 bit linux systems to wake up a parked thread).  So if the
    //   timeout is small we shouldn't park().  This needs to be traded off with the cpu overhead of
    //   spinning, so we use SPIN_THRESHOLD_NANOS which is what AbstractQueuedSynchronizer uses for
    //   similar purposes.
    // * We want to behave reasonably for timeouts of 0
    // * We are more responsive to completion than timeouts.  This is because parkNanos depends on
    //   system scheduling and as such we could either miss our deadline, or unpark() could be delayed
    //   so that it looks like we timed out even though we didn't.  For comparison FutureTask respects
    //   completion preferably and AQS is non-deterministic (depends on where in the queue the waiter
    //   is).  If we wanted to be strict about it, we could store the unpark() time in the Waiter
    //   node and we could use that to make a decision about whether or not we timed out prior to
    //   being unparked.


    @Override
    public V get(long timeout, TimeUnit unit)
            throws InterruptedException, TimeoutException, ExecutionException {
        // NOTE: if timeout < 0, remainingNanos will be < 0 and we will fall into the while(true) loop
        // at the bottom and throw a timeoutexception.
        long remainingNanos = unit.toNanos(timeout); // we rely on the implicit null check on unit.
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        Object localValue = value;
        if (localValue != null & !(localValue instanceof AbstractFuture.SetFuture)) {
            return getDoneValue(localValue);
        }
        // we delay calling nanoTime until we know we will need to either park or spin
        final long endNanos = remainingNanos > 0 ? System.nanoTime() + remainingNanos : 0;
        long_wait_loop:
        if (remainingNanos >= SPIN_THRESHOLD_NANOS) {
            Waiter oldHead = waiters;
            if (oldHead != Waiter.TOMBSTONE) {
                Waiter node = new Waiter();
                do {
                    node.setNext(oldHead);
                    if (ATOMIC_HELPER.casWaiters(this, oldHead, node)) {
                        while (true) {
                            LockSupport.parkNanos(this, remainingNanos);
                            // Check interruption first, if we woke up due to interruption we need to honor that.
                            if (Thread.interrupted()) {
                                removeWaiter(node);
                                throw new InterruptedException();
                            }

                            // Otherwise re-read and check doneness.  If we loop then it must have been a spurious
                            // wakeup
                            localValue = value;
                            if (localValue != null & !(localValue instanceof AbstractFuture.SetFuture)) {
                                return getDoneValue(localValue);
                            }

                            // timed out?
                            remainingNanos = endNanos - System.nanoTime();
                            if (remainingNanos < SPIN_THRESHOLD_NANOS) {
                                // Remove the waiter, one way or another we are done parking this thread.
                                removeWaiter(node);
                                break long_wait_loop;  // jump down to the busy wait loop
                            }
                        }
                    }
                    oldHead = waiters;  // re-read and loop.
                } while (oldHead != Waiter.TOMBSTONE);
            }
            // re-read value, if we get here then we must have observed a TOMBSTONE while trying to add a
            // waiter.
            return getDoneValue(value);
        }
        // If we get here then we have remainingNanos < SPIN_THRESHOLD_NANOS and there is no node on the
        // waiters list
        while (remainingNanos > 0) {
            localValue = value;
            if (localValue != null & !(localValue instanceof AbstractFuture.SetFuture)) {
                return getDoneValue(localValue);
            }
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            remainingNanos = endNanos - System.nanoTime();
        }
        throw new TimeoutException();
    }


    @Override
    public V get() throws InterruptedException, ExecutionException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        Object localValue = value;
        if (localValue != null & !(localValue instanceof AbstractFuture.SetFuture)) {
            return getDoneValue(localValue);
        }
        Waiter oldHead = waiters;
        if (oldHead != Waiter.TOMBSTONE) {
            Waiter node = new Waiter();
            do {
                node.setNext(oldHead);
                if (ATOMIC_HELPER.casWaiters(this, oldHead, node)) {
                    // we are on the stack, now wait for completion.
                    while (true) {
                        LockSupport.park(this);
                        // Check interruption first, if we woke up due to interruption we need to honor that.
                        if (Thread.interrupted()) {
                            removeWaiter(node);
                            throw new InterruptedException();
                        }
                        // Otherwise re-read and check doneness.  If we loop then it must have been a spurious
                        // wakeup
                        localValue = value;
                        if (localValue != null & !(localValue instanceof AbstractFuture.SetFuture)) {
                            return getDoneValue(localValue);
                        }
                    }
                }
                oldHead = waiters;  // re-read and loop.
            } while (oldHead != Waiter.TOMBSTONE);
        }
        // re-read value, if we get here then we must have observed a TOMBSTONE while trying to add a
        // waiter.
        return getDoneValue(value);
    }


    private V getDoneValue(Object obj) throws ExecutionException {
        // While this seems like it might be too branch-y, simple benchmarking proves it to be
        // unmeasurable (comparing done AbstractFutures with immediateFuture)
        if (obj instanceof Cancellation) {
            throw cancellationExceptionWithCause("Task was cancelled.", ((Cancellation) obj).cause);
        } else if (obj instanceof Failure) {
            throw new ExecutionException(((Failure) obj).exception);
        } else if (obj == NULL) {
            return null;
        } else {
            @SuppressWarnings("unchecked")  // this is the only other option
                    V asV = (V) obj;
            return asV;
        }
    }

    @Override
    public boolean isDone() {
        final Object localValue = value;
        return localValue != null & !(localValue instanceof AbstractFuture.SetFuture);
    }

    @Override
    public boolean isCancelled() {
        final Object localValue = value;
        return localValue instanceof Cancellation;
    }


    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        Object localValue = value;
        if (localValue == null | localValue instanceof AbstractFuture.SetFuture) {
            // Try to delay allocating the exception.  At this point we may still lose the CAS, but it is
            // certainly less likely.
            // TODO(lukes): this exception actually makes cancellation significantly more expensive :(
            // I wonder if we should consider removing it or providing a mechanism to not do it.
            Object valueToSet = new Cancellation(mayInterruptIfRunning, newCancellationCause());
            do {
                if (ATOMIC_HELPER.casValue(this, localValue, valueToSet)) {
                    // We call interuptTask before calling complete(), first which is consistent with
                    // FutureTask
                    if (mayInterruptIfRunning) {
                        interruptTask();
                    }
                    complete();
                    if (localValue instanceof AbstractFuture.SetFuture) {
                        // propagate cancellation to the future set in setfuture, this is racy, and we don't
                        // care if we are successful or not.
                        ((AbstractFuture<?>.SetFuture) localValue).future.cancel(mayInterruptIfRunning);
                    }
                    return true;
                }
                // obj changed, reread
                localValue = value;
                // obj cannot be null at this point, because value can only change from null to non-null. So
                // if value changed (and it did since we lost the CAS), then it cannot be null.
            } while (localValue instanceof AbstractFuture.SetFuture);
        }
        return false;
    }


    @Beta
    protected Throwable newCancellationCause() {
        return new CancellationException("Future.cancel() was called.");
    }


    protected void interruptTask() {
    }


    protected final boolean wasInterrupted() {
        final Object localValue = value;
        return (localValue instanceof Cancellation) && ((Cancellation) localValue).wasInterrupted;
    }


    @Override
    public void addListener(Runnable listener, Executor executor) {
        checkNotNull(listener, "Runnable was null.");
        checkNotNull(executor, "Executor was null.");
        Listener oldHead = listeners;
        if (oldHead != Listener.TOMBSTONE) {
            Listener newNode = new Listener(listener, executor);
            do {
                newNode.next = oldHead;
                if (ATOMIC_HELPER.casListeners(this, oldHead, newNode)) {
                    return;
                }
                oldHead = listeners;  // re-read
            } while (oldHead != Listener.TOMBSTONE);
        }
        // If we get here then the Listener TOMBSTONE was set, which means the future is done, call
        // the listener.
        executeListener(listener, executor);
    }


    protected boolean set(@Nullable V value) {
        Object valueToSet = value == null ? NULL : value;
        if (ATOMIC_HELPER.casValue(this, null, valueToSet)) {
            complete();
            return true;
        }
        return false;
    }


    protected boolean setException(Throwable throwable) {
        Object valueToSet = new Failure(checkNotNull(throwable));
        if (ATOMIC_HELPER.casValue(this, null, valueToSet)) {
            complete();
            return true;
        }
        return false;
    }


    @Beta
    protected boolean setFuture(ListenableFuture<? extends V> future) {
        checkNotNull(future);
        Object localValue = value;
        if (localValue == null) {
            if (future.isDone()) {
                return completeWithFuture(future, null);
            }
            SetFuture valueToSet = new SetFuture(future);
            if (ATOMIC_HELPER.casValue(this, null, valueToSet)) {
                // the listener is responsible for calling completeWithFuture, directExecutor is appropriate
                // since all we are doing is unpacking a completed future which should be fast.
                try {
                    future.addListener(valueToSet, directExecutor());
                } catch (Throwable t) {
                    // addListener has thrown an exception!  SetFuture.run can't throw any exceptions so this
                    // must have been caused by addListener itself.  The most likely explanation is a
                    // misconfigured mock.  Try to switch to Failure.
                    Failure failure;
                    try {
                        failure = new Failure(t);
                    } catch (Throwable oomMostLikely) {
                        failure = Failure.FALLBACK_INSTANCE;
                    }
                    // Note: The only way this CAS could fail is if cancel() has raced with us. That is ok.
                    ATOMIC_HELPER.casValue(this, valueToSet, failure);
                }
                return true;
            }
            localValue = value;  // we lost the cas, fall through and maybe cancel
        }
        // The future has already been set to something.  If it is cancellation we should cancel the
        // incoming future.
        if (localValue instanceof Cancellation) {
            // we don't care if it fails, this is best-effort.
            future.cancel(((Cancellation) localValue).wasInterrupted);
        }
        return false;
    }


    private boolean completeWithFuture(ListenableFuture<? extends V> future, Object expected) {
        Object valueToSet;
        if (future instanceof TrustedFuture) {
            // Break encapsulation for TrustedFuture instances since we know that subclasses cannot
            // override .get() (since it is final) and therefore this is equivalent to calling .get()
            // and unpacking the exceptions like we do below (just much faster because it is a single
            // field read instead of a read, several branches and possibly creating exceptions).
            valueToSet = ((AbstractFuture<?>) future).value;
        } else {
            // Otherwise calculate valueToSet by calling .get()
            try {
                V v = Uninterruptibles.getUninterruptibly(future);
                valueToSet = v == null ? NULL : v;
            } catch (ExecutionException exception) {
                valueToSet = new Failure(exception.getCause());
            } catch (CancellationException cancellation) {
                valueToSet = new Cancellation(false, cancellation);
            } catch (Throwable t) {
                valueToSet = new Failure(t);
            }
        }
        // The only way this can fail is if we raced with another thread calling cancel(). If we lost
        // that race then there is nothing to do.
        if (ATOMIC_HELPER.casValue(AbstractFuture.this, expected, valueToSet)) {
            complete();
            return true;
        }
        return false;
    }


    private void complete() {
        for (Waiter currentWaiter = clearWaiters();
             currentWaiter != null;
             currentWaiter = currentWaiter.next) {
            currentWaiter.unpark();
        }
        // We need to reverse the list to handle buggy listeners that depend on ordering.
        Listener currentListener = clearListeners();
        Listener reversedList = null;
        while (currentListener != null) {
            Listener tmp = currentListener;
            currentListener = currentListener.next;
            tmp.next = reversedList;
            reversedList = tmp;
        }
        for (; reversedList != null; reversedList = reversedList.next) {
            executeListener(reversedList.task, reversedList.executor);
        }
        // We call this after the listeners on the theory that done() will only be used for 'cleanup'
        // oriented tasks (e.g. clearing fields) and so can wait behind listeners which may be executing
        // more important work.  A counter argument would be that done() is trusted code and therefore
        // it would be safe to run before potentially slow or poorly behaved listeners.  Reevaluate this
        // once we have more examples of done() implementations.
        done();
    }


    // TODO(cpovirk): @ForOverride if https://github.com/google/error-prone/issues/342 permits
    void done() {
    }


    final Throwable trustedGetException() {
        return ((Failure) value).exception;
    }


    final void maybePropagateCancellation(@Nullable Future<?> related) {
        if (related != null & isCancelled()) {
            related.cancel(wasInterrupted());
        }
    }


    private Waiter clearWaiters() {
        Waiter head;
        do {
            head = waiters;
        } while (!ATOMIC_HELPER.casWaiters(this, head, Waiter.TOMBSTONE));
        return head;
    }


    private Listener clearListeners() {
        Listener head;
        do {
            head = listeners;
        } while (!ATOMIC_HELPER.casListeners(this, head, Listener.TOMBSTONE));
        return head;
    }


    private static void executeListener(Runnable runnable, Executor executor) {
        try {
            executor.execute(runnable);
        } catch (RuntimeException e) {
            // Log it and keep going, bad runnable and/or executor.  Don't
            // punish the other runnables if we're given a bad one.  We only
            // catch RuntimeException because we want Errors to propagate up.
            log.log(Level.SEVERE, "RuntimeException while executing runnable "
                    + runnable + " with executor " + executor, e);
        }
    }

    static final CancellationException cancellationExceptionWithCause(
            @Nullable String message, @Nullable Throwable cause) {
        CancellationException exception = new CancellationException(message);
        exception.initCause(cause);
        return exception;
    }

    private abstract static class AtomicHelper {

        abstract void putThread(Waiter waiter, Thread thread);


        abstract void putNext(Waiter waiter, Waiter next);


        abstract boolean casWaiters(AbstractFuture future, Waiter curr, Waiter next);


        abstract boolean casListeners(AbstractFuture future, Listener curr, Listener next);


        abstract boolean casValue(AbstractFuture future, Object expected, Object v);
    }


    private static final class SafeAtomicHelper extends AtomicHelper {
        @Override
        void putThread(Waiter waiter, Thread thread) {
            WAITER_THREAD_UPDATER.lazySet(waiter, thread);
        }

        @Override
        void putNext(Waiter waiter, Waiter next) {
            WAITER_NEXT_UPDATER.lazySet(waiter, next);
        }

        @Override
        boolean casWaiters(AbstractFuture future, Waiter curr, Waiter next) {
            return WAITERS_UPDATER.compareAndSet(future, curr, next);
        }

        @Override
        boolean casListeners(AbstractFuture future, Listener curr, Listener next) {
            return LISTENERS_UPDATER.compareAndSet(future, curr, next);
        }

        @Override
        boolean casValue(AbstractFuture future, Object expected, Object v) {
            return VALUE_UPDATER.compareAndSet(future, expected, v);
        }
    }
}
