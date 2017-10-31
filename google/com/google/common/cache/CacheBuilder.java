

package com.google.common.cache;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Ascii;
import com.google.common.base.Equivalence;
import com.google.common.base.MoreObjects;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.base.Ticker;
import com.google.common.cache.AbstractCache.SimpleStatsCounter;
import com.google.common.cache.AbstractCache.StatsCounter;
import com.google.common.cache.LocalCache.Strength;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;


@GwtCompatible(emulated = true)
public final class CacheBuilder<K, V> {
    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final int DEFAULT_CONCURRENCY_LEVEL = 4;
    private static final int DEFAULT_EXPIRATION_NANOS = 0;
    private static final int DEFAULT_REFRESH_NANOS = 0;

    static final Supplier<? extends StatsCounter> NULL_STATS_COUNTER = Suppliers.ofInstance(
            new StatsCounter() {
                @Override
                public void recordHits(int count) {
                }

                @Override
                public void recordMisses(int count) {
                }

                @Override
                public void recordLoadSuccess(long loadTime) {
                }

                @Override
                public void recordLoadException(long loadTime) {
                }

                @Override
                public void recordEviction() {
                }

                @Override
                public CacheStats snapshot() {
                    return EMPTY_STATS;
                }
            });
    static final CacheStats EMPTY_STATS = new CacheStats(0, 0, 0, 0, 0, 0);

    static final Supplier<StatsCounter> CACHE_STATS_COUNTER =
            new Supplier<StatsCounter>() {
                @Override
                public StatsCounter get() {
                    return new SimpleStatsCounter();
                }
            };

    enum NullListener implements RemovalListener<Object, Object> {
        INSTANCE;

        @Override
        public void onRemoval(RemovalNotification<Object, Object> notification) {
        }
    }

    enum OneWeigher implements Weigher<Object, Object> {
        INSTANCE;

        @Override
        public int weigh(Object key, Object value) {
            return 1;
        }
    }

    static final Ticker NULL_TICKER = new Ticker() {
        @Override
        public long read() {
            return 0;
        }
    };

    private static final Logger logger = Logger.getLogger(CacheBuilder.class.getName());

    static final int UNSET_INT = -1;

    boolean strictParsing = true;

    int initialCapacity = UNSET_INT;
    int concurrencyLevel = UNSET_INT;
    long maximumSize = UNSET_INT;
    long maximumWeight = UNSET_INT;
    Weigher<? super K, ? super V> weigher;

    Strength keyStrength;
    Strength valueStrength;

    long expireAfterWriteNanos = UNSET_INT;
    long expireAfterAccessNanos = UNSET_INT;
    long refreshNanos = UNSET_INT;

    Equivalence<Object> keyEquivalence;
    Equivalence<Object> valueEquivalence;

    RemovalListener<? super K, ? super V> removalListener;
    Ticker ticker;

    Supplier<? extends StatsCounter> statsCounterSupplier = NULL_STATS_COUNTER;

    // TODO(fry): make constructor private and update tests to use newBuilder
    CacheBuilder() {
    }


    public static CacheBuilder<Object, Object> newBuilder() {
        return new CacheBuilder<>();
    }


    @GwtIncompatible("To be supported")
    public static CacheBuilder<Object, Object> from(CacheBuilderSpec spec) {
        return spec.toCacheBuilder()
                .lenientParsing();
    }


    @GwtIncompatible("To be supported")
    public static CacheBuilder<Object, Object> from(String spec) {
        return from(CacheBuilderSpec.parse(spec));
    }


    @GwtIncompatible("To be supported")
    CacheBuilder<K, V> lenientParsing() {
        strictParsing = false;
        return this;
    }


    @GwtIncompatible("To be supported")
    CacheBuilder<K, V> keyEquivalence(Equivalence<Object> equivalence) {
        checkState(keyEquivalence == null, "key equivalence was already set to %s", keyEquivalence);
        keyEquivalence = checkNotNull(equivalence);
        return this;
    }

    Equivalence<Object> getKeyEquivalence() {
        return MoreObjects.firstNonNull(keyEquivalence, getKeyStrength().defaultEquivalence());
    }


    @GwtIncompatible("To be supported")
    CacheBuilder<K, V> valueEquivalence(Equivalence<Object> equivalence) {
        checkState(valueEquivalence == null,
                "value equivalence was already set to %s", valueEquivalence);
        this.valueEquivalence = checkNotNull(equivalence);
        return this;
    }

    Equivalence<Object> getValueEquivalence() {
        return MoreObjects.firstNonNull(valueEquivalence, getValueStrength().defaultEquivalence());
    }


    public CacheBuilder<K, V> initialCapacity(int initialCapacity) {
        checkState(this.initialCapacity == UNSET_INT, "initial capacity was already set to %s",
                this.initialCapacity);
        checkArgument(initialCapacity >= 0);
        this.initialCapacity = initialCapacity;
        return this;
    }

    int getInitialCapacity() {
        return (initialCapacity == UNSET_INT) ? DEFAULT_INITIAL_CAPACITY : initialCapacity;
    }


    public CacheBuilder<K, V> concurrencyLevel(int concurrencyLevel) {
        checkState(this.concurrencyLevel == UNSET_INT, "concurrency level was already set to %s",
                this.concurrencyLevel);
        checkArgument(concurrencyLevel > 0);
        this.concurrencyLevel = concurrencyLevel;
        return this;
    }

    int getConcurrencyLevel() {
        return (concurrencyLevel == UNSET_INT) ? DEFAULT_CONCURRENCY_LEVEL : concurrencyLevel;
    }


    public CacheBuilder<K, V> maximumSize(long size) {
        checkState(this.maximumSize == UNSET_INT, "maximum size was already set to %s",
                this.maximumSize);
        checkState(this.maximumWeight == UNSET_INT, "maximum weight was already set to %s",
                this.maximumWeight);
        checkState(this.weigher == null, "maximum size can not be combined with weigher");
        checkArgument(size >= 0, "maximum size must not be negative");
        this.maximumSize = size;
        return this;
    }


    @GwtIncompatible("To be supported")
    public CacheBuilder<K, V> maximumWeight(long weight) {
        checkState(this.maximumWeight == UNSET_INT, "maximum weight was already set to %s",
                this.maximumWeight);
        checkState(this.maximumSize == UNSET_INT, "maximum size was already set to %s",
                this.maximumSize);
        this.maximumWeight = weight;
        checkArgument(weight >= 0, "maximum weight must not be negative");
        return this;
    }


    @GwtIncompatible("To be supported")
    public <K1 extends K, V1 extends V> CacheBuilder<K1, V1> weigher(
            Weigher<? super K1, ? super V1> weigher) {
        checkState(this.weigher == null);
        if (strictParsing) {
            checkState(this.maximumSize == UNSET_INT, "weigher can not be combined with maximum size",
                    this.maximumSize);
        }

        // safely limiting the kinds of caches this can produce
        @SuppressWarnings("unchecked")
        CacheBuilder<K1, V1> me = (CacheBuilder<K1, V1>) this;
        me.weigher = checkNotNull(weigher);
        return me;
    }

    long getMaximumWeight() {
        if (expireAfterWriteNanos == 0 || expireAfterAccessNanos == 0) {
            return 0;
        }
        return (weigher == null) ? maximumSize : maximumWeight;
    }

    // Make a safe contravariant cast now so we don't have to do it over and over.
    @SuppressWarnings("unchecked")
    <K1 extends K, V1 extends V> Weigher<K1, V1> getWeigher() {
        return (Weigher<K1, V1>) MoreObjects.firstNonNull(weigher, OneWeigher.INSTANCE);
    }


    @GwtIncompatible("java.lang.ref.WeakReference")
    public CacheBuilder<K, V> weakKeys() {
        return setKeyStrength(Strength.WEAK);
    }

    CacheBuilder<K, V> setKeyStrength(Strength strength) {
        checkState(keyStrength == null, "Key strength was already set to %s", keyStrength);
        keyStrength = checkNotNull(strength);
        return this;
    }

    Strength getKeyStrength() {
        return MoreObjects.firstNonNull(keyStrength, Strength.STRONG);
    }


    @GwtIncompatible("java.lang.ref.WeakReference")
    public CacheBuilder<K, V> weakValues() {
        return setValueStrength(Strength.WEAK);
    }


    @GwtIncompatible("java.lang.ref.SoftReference")
    public CacheBuilder<K, V> softValues() {
        return setValueStrength(Strength.SOFT);
    }

    CacheBuilder<K, V> setValueStrength(Strength strength) {
        checkState(valueStrength == null, "Value strength was already set to %s", valueStrength);
        valueStrength = checkNotNull(strength);
        return this;
    }

    Strength getValueStrength() {
        return MoreObjects.firstNonNull(valueStrength, Strength.STRONG);
    }


    public CacheBuilder<K, V> expireAfterWrite(long duration, TimeUnit unit) {
        checkState(expireAfterWriteNanos == UNSET_INT, "expireAfterWrite was already set to %s ns",
                expireAfterWriteNanos);
        checkArgument(duration >= 0, "duration cannot be negative: %s %s", duration, unit);
        this.expireAfterWriteNanos = unit.toNanos(duration);
        return this;
    }

    long getExpireAfterWriteNanos() {
        return (expireAfterWriteNanos == UNSET_INT) ? DEFAULT_EXPIRATION_NANOS : expireAfterWriteNanos;
    }


    public CacheBuilder<K, V> expireAfterAccess(long duration, TimeUnit unit) {
        checkState(expireAfterAccessNanos == UNSET_INT, "expireAfterAccess was already set to %s ns",
                expireAfterAccessNanos);
        checkArgument(duration >= 0, "duration cannot be negative: %s %s", duration, unit);
        this.expireAfterAccessNanos = unit.toNanos(duration);
        return this;
    }

    long getExpireAfterAccessNanos() {
        return (expireAfterAccessNanos == UNSET_INT)
                ? DEFAULT_EXPIRATION_NANOS : expireAfterAccessNanos;
    }


    @GwtIncompatible("To be supported (synchronously).")
    public CacheBuilder<K, V> refreshAfterWrite(long duration, TimeUnit unit) {
        checkNotNull(unit);
        checkState(refreshNanos == UNSET_INT, "refresh was already set to %s ns", refreshNanos);
        checkArgument(duration > 0, "duration must be positive: %s %s", duration, unit);
        this.refreshNanos = unit.toNanos(duration);
        return this;
    }

    long getRefreshNanos() {
        return (refreshNanos == UNSET_INT) ? DEFAULT_REFRESH_NANOS : refreshNanos;
    }


    public CacheBuilder<K, V> ticker(Ticker ticker) {
        checkState(this.ticker == null);
        this.ticker = checkNotNull(ticker);
        return this;
    }

    Ticker getTicker(boolean recordsTime) {
        if (ticker != null) {
            return ticker;
        }
        return recordsTime ? Ticker.systemTicker() : NULL_TICKER;
    }


    public <K1 extends K, V1 extends V> CacheBuilder<K1, V1> removalListener(
            RemovalListener<? super K1, ? super V1> listener) {
        checkState(this.removalListener == null);

        // safely limiting the kinds of caches this can produce
        @SuppressWarnings("unchecked")
        CacheBuilder<K1, V1> me = (CacheBuilder<K1, V1>) this;
        me.removalListener = checkNotNull(listener);
        return me;
    }

    // Make a safe contravariant cast now so we don't have to do it over and over.
    @SuppressWarnings("unchecked")
    <K1 extends K, V1 extends V> RemovalListener<K1, V1> getRemovalListener() {
        return (RemovalListener<K1, V1>)
                MoreObjects.firstNonNull(removalListener, NullListener.INSTANCE);
    }


    public CacheBuilder<K, V> recordStats() {
        statsCounterSupplier = CACHE_STATS_COUNTER;
        return this;
    }

    boolean isRecordingStats() {
        return statsCounterSupplier == CACHE_STATS_COUNTER;
    }

    Supplier<? extends StatsCounter> getStatsCounterSupplier() {
        return statsCounterSupplier;
    }


    public <K1 extends K, V1 extends V> LoadingCache<K1, V1> build(
            CacheLoader<? super K1, V1> loader) {
        checkWeightWithWeigher();
        return new LocalCache.LocalLoadingCache<>(this, loader);
    }


    public <K1 extends K, V1 extends V> Cache<K1, V1> build() {
        checkWeightWithWeigher();
        checkNonLoadingCache();
        return new LocalCache.LocalManualCache<>(this);
    }

    private void checkNonLoadingCache() {
        checkState(refreshNanos == UNSET_INT, "refreshAfterWrite requires a LoadingCache");
    }

    private void checkWeightWithWeigher() {
        if (weigher == null) {
            checkState(maximumWeight == UNSET_INT, "maximumWeight requires weigher");
        } else {
            if (strictParsing) {
                checkState(maximumWeight != UNSET_INT, "weigher requires maximumWeight");
            } else {
                if (maximumWeight == UNSET_INT) {
                    logger.log(Level.WARNING, "ignoring weigher specified without maximumWeight");
                }
            }
        }
    }


    @Override
    public String toString() {
        MoreObjects.ToStringHelper s = MoreObjects.toStringHelper(this);
        if (initialCapacity != UNSET_INT) {
            s.add("initialCapacity", initialCapacity);
        }
        if (concurrencyLevel != UNSET_INT) {
            s.add("concurrencyLevel", concurrencyLevel);
        }
        if (maximumSize != UNSET_INT) {
            s.add("maximumSize", maximumSize);
        }
        if (maximumWeight != UNSET_INT) {
            s.add("maximumWeight", maximumWeight);
        }
        if (expireAfterWriteNanos != UNSET_INT) {
            s.add("expireAfterWrite", expireAfterWriteNanos + "ns");
        }
        if (expireAfterAccessNanos != UNSET_INT) {
            s.add("expireAfterAccess", expireAfterAccessNanos + "ns");
        }
        if (keyStrength != null) {
            s.add("keyStrength", Ascii.toLowerCase(keyStrength.toString()));
        }
        if (valueStrength != null) {
            s.add("valueStrength", Ascii.toLowerCase(valueStrength.toString()));
        }
        if (keyEquivalence != null) {
            s.addValue("keyEquivalence");
        }
        if (valueEquivalence != null) {
            s.addValue("valueEquivalence");
        }
        if (removalListener != null) {
            s.addValue("removalListener");
        }
        return s.toString();
    }
}
