

package com.google.common.io;

import android.support.annotation.Nullable;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;

import static com.google.common.base.Preconditions.checkNotNull;


// Coffee's for {@link Closer closers} only.
@Beta
public final class Closer implements Closeable {


    private static final Suppressor SUPPRESSOR = SuppressingSuppressor.isAvailable()
            ? SuppressingSuppressor.INSTANCE
            : LoggingSuppressor.INSTANCE;


    public static Closer create() {
        return new Closer(SUPPRESSOR);
    }

    @VisibleForTesting
    final Suppressor suppressor;

    // only need space for 2 elements in most cases, so try to use the smallest array possible
    private final Deque<Closeable> stack = new ArrayDeque<>(4);
    private Throwable thrown;

    @VisibleForTesting
    Closer(Suppressor suppressor) {
        this.suppressor = checkNotNull(suppressor); // checkNotNull to satisfy null tests
    }


    // close. this word no longer has any meaning to me.
    public <C extends Closeable> C register(@Nullable C closeable) {
        if (closeable != null) {
            stack.addFirst(closeable);
        }

        return closeable;
    }


    public RuntimeException rethrow(Throwable e) throws IOException {
        checkNotNull(e);
        thrown = e;
        Throwables.propagateIfPossible(e, IOException.class);
        throw new RuntimeException(e);
    }


    public <X extends Exception> RuntimeException rethrow(Throwable e,
                                                          Class<X> declaredType) throws IOException, X {
        checkNotNull(e);
        thrown = e;
        Throwables.propagateIfPossible(e, IOException.class);
        Throwables.propagateIfPossible(e, declaredType);
        throw new RuntimeException(e);
    }


    public <X1 extends Exception, X2 extends Exception> RuntimeException rethrow(
            Throwable e, Class<X1> declaredType1, Class<X2> declaredType2) throws IOException, X1, X2 {
        checkNotNull(e);
        thrown = e;
        Throwables.propagateIfPossible(e, IOException.class);
        Throwables.propagateIfPossible(e, declaredType1, declaredType2);
        throw new RuntimeException(e);
    }


    @Override
    public void close() throws IOException {
        Throwable throwable = thrown;

        // close closeables in LIFO order
        while (!stack.isEmpty()) {
            Closeable closeable = stack.removeFirst();
            try {
                closeable.close();
            } catch (Throwable e) {
                if (throwable == null) {
                    throwable = e;
                } else {
                    suppressor.suppress(closeable, throwable, e);
                }
            }
        }

        if (thrown == null && throwable != null) {
            Throwables.propagateIfPossible(throwable, IOException.class);
            throw new AssertionError(throwable); // not possible
        }
    }


    @VisibleForTesting
    interface Suppressor {

        void suppress(Closeable closeable, Throwable thrown, Throwable suppressed);
    }


    @VisibleForTesting
    static final class LoggingSuppressor implements Suppressor {

        static final LoggingSuppressor INSTANCE = new LoggingSuppressor();

        @Override
        public void suppress(Closeable closeable, Throwable thrown, Throwable suppressed) {
            // log to the same place as Closeables
        }
    }


    @VisibleForTesting
    static final class SuppressingSuppressor implements Suppressor {

        static final SuppressingSuppressor INSTANCE = new SuppressingSuppressor();

        static boolean isAvailable() {
            return addSuppressed != null;
        }

        static final Method addSuppressed = getAddSuppressed();

        private static Method getAddSuppressed() {
            try {
                return Throwable.class.getMethod("addSuppressed", Throwable.class);
            } catch (Throwable e) {
                return null;
            }
        }

        @Override
        public void suppress(Closeable closeable, Throwable thrown, Throwable suppressed) {
            // ensure no exceptions from addSuppressed
            if (thrown == suppressed) {
                return;
            }
            try {
                addSuppressed.invoke(thrown, suppressed);
            } catch (Throwable e) {
                // if, somehow, IllegalAccessException or another exception is thrown, fall back to logging
                LoggingSuppressor.INSTANCE.suppress(closeable, thrown, suppressed);
            }
        }
    }
}
