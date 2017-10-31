

package com.google.common.cache;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import static com.google.common.base.Preconditions.checkNotNull;


@GwtCompatible(emulated = true)
public abstract class CacheLoader<K, V> {

    protected CacheLoader() {
    }


    public abstract V load(K key) throws Exception;


    @GwtIncompatible("Futures")
    public ListenableFuture<V> reload(K key, V oldValue) throws Exception {
        checkNotNull(key);
        checkNotNull(oldValue);
        return Futures.immediateFuture(load(key));
    }


    public Map<K, V> loadAll(Iterable<? extends K> keys) throws Exception {
        // This will be caught by getAll(), causing it to fall back to multiple calls to
        // LoadingCache.get
        throw new UnsupportedLoadingOperationException();
    }


    public static <K, V> CacheLoader<K, V> from(Function<K, V> function) {
        return new FunctionToCacheLoader<>(function);
    }

    private static final class FunctionToCacheLoader<K, V>
            extends CacheLoader<K, V> implements Serializable {
        private final Function<K, V> computingFunction;

        public FunctionToCacheLoader(Function<K, V> computingFunction) {
            this.computingFunction = checkNotNull(computingFunction);
        }

        @Override
        public V load(K key) {
            return computingFunction.apply(checkNotNull(key));
        }

        private static final long serialVersionUID = 0;
    }


    public static <V> CacheLoader<Object, V> from(Supplier<V> supplier) {
        return new SupplierToCacheLoader<>(supplier);
    }


    @GwtIncompatible("Executor + Futures")
    public static <K, V> CacheLoader<K, V> asyncReloading(final CacheLoader<K, V> loader,
                                                          final Executor executor) {
        checkNotNull(loader);
        checkNotNull(executor);
        return new CacheLoader<K, V>() {
            @Override
            public V load(K key) throws Exception {
                return loader.load(key);
            }

            @Override
            public ListenableFuture<V> reload(final K key, final V oldValue) throws Exception {
                ListenableFutureTask<V> task = ListenableFutureTask.create(new Callable<V>() {
                    @Override
                    public V call() throws Exception {
                        return loader.reload(key, oldValue).get();
                    }
                });
                executor.execute(task);
                return task;
            }

            @Override
            public Map<K, V> loadAll(Iterable<? extends K> keys) throws Exception {
                return loader.loadAll(keys);
            }
        };
    }

    private static final class SupplierToCacheLoader<V>
            extends CacheLoader<Object, V> implements Serializable {
        private final Supplier<V> computingSupplier;

        public SupplierToCacheLoader(Supplier<V> computingSupplier) {
            this.computingSupplier = checkNotNull(computingSupplier);
        }

        @Override
        public V load(Object key) {
            checkNotNull(key);
            return computingSupplier.get();
        }

        private static final long serialVersionUID = 0;
    }


    public static final class UnsupportedLoadingOperationException
            extends UnsupportedOperationException {
        // Package-private because this should only be thrown by loadAll() when it is not overridden.
        // Cache implementors may want to catch it but should not need to be able to throw it.
        UnsupportedLoadingOperationException() {
        }
    }


    public static final class InvalidCacheLoadException extends RuntimeException {
        public InvalidCacheLoadException(String message) {
            super(message);
        }
    }
}
