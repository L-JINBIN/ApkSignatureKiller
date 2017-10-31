

package com.google.common.base;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.VisibleForTesting;

import java.io.Serializable;


@GwtCompatible
public final class Suppliers {
    private Suppliers() {
    }


    public static <T> Supplier<T> memoize(Supplier<T> delegate) {
        return (delegate instanceof MemoizingSupplier)
                ? delegate
                : new MemoizingSupplier<>(Preconditions.checkNotNull(delegate));
    }

    @VisibleForTesting
    static class MemoizingSupplier<T> implements Supplier<T>, Serializable {
        final Supplier<T> delegate;
        transient volatile boolean initialized;
        // "value" does not need to be volatile; visibility piggy-backs
        // on volatile read of "initialized".
        transient T value;

        MemoizingSupplier(Supplier<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public T get() {
            // A 2-field variant of Double Checked Locking.
            if (!initialized) {
                synchronized (this) {
                    if (!initialized) {
                        T t = delegate.get();
                        value = t;
                        initialized = true;
                        return t;
                    }
                }
            }
            return value;
        }

        @Override
        public String toString() {
            return "Suppliers.memoize(" + delegate + ")";
        }

        private static final long serialVersionUID = 0;
    }


    public static <T> Supplier<T> ofInstance(@Nullable T instance) {
        return new SupplierOfInstance<>(instance);
    }

    private static class SupplierOfInstance<T> implements Supplier<T>, Serializable {
        final T instance;

        SupplierOfInstance(@Nullable T instance) {
            this.instance = instance;
        }

        @Override
        public T get() {
            return instance;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof SupplierOfInstance) {
                SupplierOfInstance<?> that = (SupplierOfInstance<?>) obj;
                return Objects.equal(instance, that.instance);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(instance);
        }

        @Override
        public String toString() {
            return "Suppliers.ofInstance(" + instance + ")";
        }

        private static final long serialVersionUID = 0;
    }


    private interface SupplierFunction<T> extends Function<Supplier<T>, T> {
    }

    private enum SupplierFunctionImpl implements SupplierFunction<Object> {
        ;

        // Note: This makes T a "pass-through type"
        @Override
        public Object apply(Supplier<Object> input) {
            return input.get();
        }

        @Override
        public String toString() {
            return "Suppliers.supplierFunction()";
        }
    }
}
