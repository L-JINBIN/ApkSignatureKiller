

package com.google.common.cache;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;


@GwtCompatible
public abstract class AbstractCache<K, V> implements Cache<K, V> {


    protected AbstractCache() {
    }


    @Override
    public V get(K key, Callable<? extends V> valueLoader) throws ExecutionException {
        throw new UnsupportedOperationException();
    }


    @Override
    public ImmutableMap<K, V> getAllPresent(Iterable<?> keys) {
        Map<K, V> result = Maps.newLinkedHashMap();
        for (Object key : keys) {
            if (!result.containsKey(key)) {
                @SuppressWarnings("unchecked")
                K castKey = (K) key;
                V value = getIfPresent(key);
                if (value != null) {
                    result.put(castKey, value);
                }
            }
        }
        return ImmutableMap.copyOf(result);
    }


    @Override
    public void put(K key, V value) {
        throw new UnsupportedOperationException();
    }


    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void cleanUp() {
    }

    @Override
    public long size() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void invalidate(Object key) {
        throw new UnsupportedOperationException();
    }


    @Override
    public void invalidateAll(Iterable<?> keys) {
        for (Object key : keys) {
            invalidate(key);
        }
    }

    @Override
    public void invalidateAll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CacheStats stats() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConcurrentMap<K, V> asMap() {
        throw new UnsupportedOperationException();
    }


    public interface StatsCounter {

        void recordHits(int count);


        void recordMisses(int count);


        void recordLoadSuccess(long loadTime);


        void recordLoadException(long loadTime);


        void recordEviction();


        CacheStats snapshot();
    }


    public static final class SimpleStatsCounter implements StatsCounter {
        private final LongAddable hitCount = LongAddables.create();
        private final LongAddable missCount = LongAddables.create();
        private final LongAddable loadSuccessCount = LongAddables.create();
        private final LongAddable loadExceptionCount = LongAddables.create();
        private final LongAddable totalLoadTime = LongAddables.create();
        private final LongAddable evictionCount = LongAddables.create();


        public SimpleStatsCounter() {
        }


        @Override
        public void recordHits(int count) {
            hitCount.add(count);
        }


        @Override
        public void recordMisses(int count) {
            missCount.add(count);
        }

        @Override
        public void recordLoadSuccess(long loadTime) {
            loadSuccessCount.increment();
            totalLoadTime.add(loadTime);
        }

        @Override
        public void recordLoadException(long loadTime) {
            loadExceptionCount.increment();
            totalLoadTime.add(loadTime);
        }

        @Override
        public void recordEviction() {
            evictionCount.increment();
        }

        @Override
        public CacheStats snapshot() {
            return new CacheStats(
                    hitCount.sum(),
                    missCount.sum(),
                    loadSuccessCount.sum(),
                    loadExceptionCount.sum(),
                    totalLoadTime.sum(),
                    evictionCount.sum());
        }


        public void incrementBy(StatsCounter other) {
            CacheStats otherStats = other.snapshot();
            hitCount.add(otherStats.hitCount());
            missCount.add(otherStats.missCount());
            loadSuccessCount.add(otherStats.loadSuccessCount());
            loadExceptionCount.add(otherStats.loadExceptionCount());
            totalLoadTime.add(otherStats.totalLoadTime());
            evictionCount.add(otherStats.evictionCount());
        }
    }
}
