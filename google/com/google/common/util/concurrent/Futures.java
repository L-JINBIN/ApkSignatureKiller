

package com.google.common.util.concurrent;

import android.support.annotation.Nullable;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Function;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static com.google.common.util.concurrent.Uninterruptibles.getUninterruptibly;


@Beta
@GwtCompatible(emulated = true)
public final class Futures extends GwtFuturesCatchingSpecialization {

    private Futures() {
    }


    private abstract static class ImmediateFuture<V>
            implements ListenableFuture<V> {

        private static final Logger log =
                Logger.getLogger(ImmediateFuture.class.getName());

        @Override
        public void addListener(Runnable listener, Executor executor) {
            checkNotNull(listener, "Runnable was null.");
            checkNotNull(executor, "Executor was null.");
            try {
                executor.execute(listener);
            } catch (RuntimeException e) {
                // ListenableFuture's contract is that it will not throw unchecked
                // exceptions, so log the bad runnable and/or executor and swallow it.
                log.log(Level.SEVERE, "RuntimeException while executing runnable "
                        + listener + " with executor " + executor, e);
            }
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public abstract V get() throws ExecutionException;

        @Override
        public V get(long timeout, TimeUnit unit) throws ExecutionException {
            checkNotNull(unit);
            return get();
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }
    }

    private static class ImmediateSuccessfulFuture<V> extends ImmediateFuture<V> {
        static final ImmediateSuccessfulFuture<Object> NULL =
                new ImmediateSuccessfulFuture<>(null);

        @Nullable
        private final V value;

        ImmediateSuccessfulFuture(@Nullable V value) {
            this.value = value;
        }

        @Override
        public V get() {
            return value;
        }
    }

    private static class ImmediateFailedFuture<V> extends ImmediateFuture<V> {

        private final Throwable thrown;

        ImmediateFailedFuture(Throwable thrown) {
            this.thrown = thrown;
        }

        @Override
        public V get() throws ExecutionException {
            throw new ExecutionException(thrown);
        }
    }


    public static <V> ListenableFuture<V> immediateFuture(@Nullable V value) {
        if (value == null) {
            // This cast is safe because null is assignable to V for all V (i.e. it is covariant)
            @SuppressWarnings({"unchecked", "rawtypes"})
            ListenableFuture<V> typedNull = (ListenableFuture) ImmediateSuccessfulFuture.NULL;
            return typedNull;
        }
        return new ImmediateSuccessfulFuture<>(value);
    }


    public static <V> ListenableFuture<V> immediateFailedFuture(
            Throwable throwable) {
        checkNotNull(throwable);
        return new ImmediateFailedFuture<>(throwable);
    }


    public static <I, O> ListenableFuture<O> transform(ListenableFuture<I> input,
                                                       final Function<? super I, ? extends O> function) {
        checkNotNull(function);
        ChainingFuture<I, O> output = new ChainingFuture<>(input, function);
        input.addListener(output, directExecutor());
        return output;
    }


    private abstract static class AbstractChainingFuture<I, O, F>
            extends AbstractFuture.TrustedFuture<O> implements Runnable {
        // In theory, this field might not be visible to a cancel() call in certain circumstances. For
        // details, see the comments on the fields of TimeoutFuture.
        @Nullable
        ListenableFuture<? extends I> inputFuture;
        @Nullable
        F function;

        AbstractChainingFuture(ListenableFuture<? extends I> inputFuture, F function) {
            this.inputFuture = checkNotNull(inputFuture);
            this.function = checkNotNull(function);
        }

        @Override
        public final void run() {
            try {
                ListenableFuture<? extends I> localInputFuture = inputFuture;
                F localFunction = function;
                if (isCancelled() | localInputFuture == null | localFunction == null) {
                    return;
                }
                inputFuture = null;
                function = null;

                I sourceResult;
                try {
                    sourceResult = getUninterruptibly(localInputFuture);
                } catch (CancellationException e) {
                    // Cancel this future and return.
                    // At this point, inputFuture is cancelled and outputFuture doesn't
                    // exist, so the value of mayInterruptIfRunning is irrelevant.
                    cancel(false);
                    return;
                } catch (ExecutionException e) {
                    // Set the cause of the exception as this future's exception
                    setException(e.getCause());
                    return;
                }
                doTransform(localFunction, sourceResult);
            } catch (UndeclaredThrowableException e) {
                // Set the cause of the exception as this future's exception
                setException(e.getCause());
            } catch (Throwable t) {
                // This exception is irrelevant in this thread, but useful for the
                // client
                setException(t);
            }
        }


        abstract void doTransform(F function, I result) throws Exception;

        @Override
        final void done() {
            maybePropagateCancellation(inputFuture);
            this.inputFuture = null;
            this.function = null;
        }
    }


    private static final class ChainingFuture<I, O>
            extends AbstractChainingFuture<I, O, Function<? super I, ? extends O>> {

        ChainingFuture(ListenableFuture<? extends I> inputFuture,
                       Function<? super I, ? extends O> function) {
            super(inputFuture, function);
        }

        @Override
        void doTransform(Function<? super I, ? extends O> function, I input) {
            // TODO(lukes): move the UndeclaredThrowable catch block here?
            set(function.apply(input));
        }
    }


}
