

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Ascii;
import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.base.Ticker;
import com.google.common.collect.MapMakerInternalMap.Strength;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.MapMakerInternalMap.Strength.SOFT;


@GwtCompatible(emulated = true)
public final class MapMaker extends GenericMapMaker<Object, Object> {
    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final int DEFAULT_CONCURRENCY_LEVEL = 4;
    private static final int DEFAULT_EXPIRATION_NANOS = 0;

    static final int UNSET_INT = -1;

    // TODO(kevinb): dispense with this after benchmarking
    boolean useCustomMap;

    int initialCapacity = UNSET_INT;
    int concurrencyLevel = UNSET_INT;
    int maximumSize = UNSET_INT;

    Strength keyStrength;
    Strength valueStrength;

    long expireAfterWriteNanos = UNSET_INT;
    long expireAfterAccessNanos = UNSET_INT;

    RemovalCause nullRemovalCause;

    Equivalence<Object> keyEquivalence;

    Ticker ticker;


    public MapMaker() {
    }


    @GwtIncompatible("To be supported")
    MapMaker keyEquivalence(Equivalence<Object> equivalence) {
        checkState(keyEquivalence == null, "key equivalence was already set to %s", keyEquivalence);
        keyEquivalence = checkNotNull(equivalence);
        this.useCustomMap = true;
        return this;
    }

    Equivalence<Object> getKeyEquivalence() {
        return MoreObjects.firstNonNull(keyEquivalence, getKeyStrength().defaultEquivalence());
    }


    public MapMaker initialCapacity(int initialCapacity) {
        checkState(this.initialCapacity == UNSET_INT, "initial capacity was already set to %s",
                this.initialCapacity);
        checkArgument(initialCapacity >= 0);
        this.initialCapacity = initialCapacity;
        return this;
    }

    int getInitialCapacity() {
        return (initialCapacity == UNSET_INT) ? DEFAULT_INITIAL_CAPACITY : initialCapacity;
    }


    @Deprecated
    MapMaker maximumSize(int size) {
        checkState(this.maximumSize == UNSET_INT, "maximum size was already set to %s",
                this.maximumSize);
        checkArgument(size >= 0, "maximum size must not be negative");
        this.maximumSize = size;
        this.useCustomMap = true;
        if (maximumSize == 0) {
            // SIZE trumps EXPIRED
            this.nullRemovalCause = RemovalCause.SIZE;
        }
        return this;
    }


    public MapMaker concurrencyLevel(int concurrencyLevel) {
        checkState(this.concurrencyLevel == UNSET_INT, "concurrency level was already set to %s",
                this.concurrencyLevel);
        checkArgument(concurrencyLevel > 0);
        this.concurrencyLevel = concurrencyLevel;
        return this;
    }

    int getConcurrencyLevel() {
        return (concurrencyLevel == UNSET_INT) ? DEFAULT_CONCURRENCY_LEVEL : concurrencyLevel;
    }


    @GwtIncompatible("java.lang.ref.WeakReference")
    public MapMaker weakKeys() {
        return setKeyStrength(Strength.WEAK);
    }

    MapMaker setKeyStrength(Strength strength) {
        checkState(keyStrength == null, "Key strength was already set to %s", keyStrength);
        keyStrength = checkNotNull(strength);
        checkArgument(keyStrength != SOFT, "Soft keys are not supported");
        if (strength != Strength.STRONG) {
            // STRONG could be used during deserialization.
            useCustomMap = true;
        }
        return this;
    }

    Strength getKeyStrength() {
        return MoreObjects.firstNonNull(keyStrength, Strength.STRONG);
    }


    MapMaker setValueStrength(Strength strength) {
        checkState(valueStrength == null, "Value strength was already set to %s", valueStrength);
        valueStrength = checkNotNull(strength);
        if (strength != Strength.STRONG) {
            // STRONG could be used during deserialization.
            useCustomMap = true;
        }
        return this;
    }

    Strength getValueStrength() {
        return MoreObjects.firstNonNull(valueStrength, Strength.STRONG);
    }


    @Deprecated
    MapMaker expireAfterWrite(long duration, TimeUnit unit) {
        checkExpiration(duration, unit);
        this.expireAfterWriteNanos = unit.toNanos(duration);
        if (duration == 0 && this.nullRemovalCause == null) {
            // SIZE trumps EXPIRED
            this.nullRemovalCause = RemovalCause.EXPIRED;
        }
        useCustomMap = true;
        return this;
    }

    private void checkExpiration(long duration, TimeUnit unit) {
        checkState(expireAfterWriteNanos == UNSET_INT, "expireAfterWrite was already set to %s ns",
                expireAfterWriteNanos);
        checkState(expireAfterAccessNanos == UNSET_INT, "expireAfterAccess was already set to %s ns",
                expireAfterAccessNanos);
        checkArgument(duration >= 0, "duration cannot be negative: %s %s", duration, unit);
    }

    long getExpireAfterWriteNanos() {
        return (expireAfterWriteNanos == UNSET_INT) ? DEFAULT_EXPIRATION_NANOS : expireAfterWriteNanos;
    }


    @Deprecated
    @GwtIncompatible("To be supported")
    MapMaker expireAfterAccess(long duration, TimeUnit unit) {
        checkExpiration(duration, unit);
        this.expireAfterAccessNanos = unit.toNanos(duration);
        if (duration == 0 && this.nullRemovalCause == null) {
            // SIZE trumps EXPIRED
            this.nullRemovalCause = RemovalCause.EXPIRED;
        }
        useCustomMap = true;
        return this;
    }

    long getExpireAfterAccessNanos() {
        return (expireAfterAccessNanos == UNSET_INT)
                ? DEFAULT_EXPIRATION_NANOS : expireAfterAccessNanos;
    }

    Ticker getTicker() {
        return MoreObjects.firstNonNull(ticker, Ticker.systemTicker());
    }


    @Deprecated
    @GwtIncompatible("To be supported")
    <K, V> GenericMapMaker<K, V> removalListener(RemovalListener<K, V> listener) {
        checkState(this.removalListener == null);

        // safely limiting the kinds of maps this can produce
        @SuppressWarnings("unchecked")
        GenericMapMaker<K, V> me = (GenericMapMaker<K, V>) this;
        me.removalListener = checkNotNull(listener);
        useCustomMap = true;
        return me;
    }


    public <K, V> ConcurrentMap<K, V> makeMap() {
        if (!useCustomMap) {
            return new ConcurrentHashMap<>(getInitialCapacity(), 0.75f, getConcurrencyLevel());
        }
        return (nullRemovalCause == null)
                ? new MapMakerInternalMap<K, V>(this)
                : new NullConcurrentMap<K, V>(this);
    }


    @Deprecated
    <K, V> ConcurrentMap<K, V> makeComputingMap(
            Function<? super K, ? extends V> computingFunction) {
        return (nullRemovalCause == null)
                ? new MapMaker.ComputingMapAdapter<>(this, computingFunction)
                : new NullComputingConcurrentMap<>(this, computingFunction);
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
        if (removalListener != null) {
            s.addValue("removalListener");
        }
        return s.toString();
    }


    interface RemovalListener<K, V> {

        void onRemoval(RemovalNotification<K, V> notification);
    }


    static final class RemovalNotification<K, V> extends ImmutableEntry<K, V> {
        private static final long serialVersionUID = 0;

        private final RemovalCause cause;

        RemovalNotification(@Nullable K key, @Nullable V value, RemovalCause cause) {
            super(key, value);
            this.cause = cause;
        }


        public RemovalCause getCause() {
            return cause;
        }


        public boolean wasEvicted() {
            return cause.wasEvicted();
        }
    }


    enum RemovalCause {

        EXPLICIT {
            @Override
            boolean wasEvicted() {
                return false;
            }
        },


        REPLACED {
            @Override
            boolean wasEvicted() {
                return false;
            }
        },


        COLLECTED {
            @Override
            boolean wasEvicted() {
                return true;
            }
        },


        EXPIRED {
            @Override
            boolean wasEvicted() {
                return true;
            }
        },


        SIZE {
            @Override
            boolean wasEvicted() {
                return true;
            }
        };


        abstract boolean wasEvicted();
    }


    static class NullConcurrentMap<K, V> extends AbstractMap<K, V>
            implements ConcurrentMap<K, V>, Serializable {
        private static final long serialVersionUID = 0;

        private final RemovalListener<K, V> removalListener;
        private final RemovalCause removalCause;

        NullConcurrentMap(MapMaker mapMaker) {
            removalListener = mapMaker.getRemovalListener();
            removalCause = mapMaker.nullRemovalCause;
        }

        // implements ConcurrentMap

        @Override
        public boolean containsKey(@Nullable Object key) {
            return false;
        }

        @Override
        public boolean containsValue(@Nullable Object value) {
            return false;
        }

        @Override
        public V get(@Nullable Object key) {
            return null;
        }

        void notifyRemoval(K key, V value) {
            RemovalNotification<K, V> notification =
                    new RemovalNotification<>(key, value, removalCause);
            removalListener.onRemoval(notification);
        }

        @Override
        public V put(K key, V value) {
            checkNotNull(key);
            checkNotNull(value);
            notifyRemoval(key, value);
            return null;
        }

        @Override
        public V putIfAbsent(K key, V value) {
            return put(key, value);
        }

        @Override
        public V remove(@Nullable Object key) {
            return null;
        }

        @Override
        public boolean remove(@Nullable Object key, @Nullable Object value) {
            return false;
        }

        @Override
        public V replace(K key, V value) {
            checkNotNull(key);
            checkNotNull(value);
            return null;
        }

        @Override
        public boolean replace(K key, @Nullable V oldValue, V newValue) {
            checkNotNull(key);
            checkNotNull(newValue);
            return false;
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return Collections.emptySet();
        }
    }


    static final class NullComputingConcurrentMap<K, V> extends NullConcurrentMap<K, V> {
        private static final long serialVersionUID = 0;

        final Function<? super K, ? extends V> computingFunction;

        NullComputingConcurrentMap(
                MapMaker mapMaker, Function<? super K, ? extends V> computingFunction) {
            super(mapMaker);
            this.computingFunction = checkNotNull(computingFunction);
        }

        @SuppressWarnings("unchecked") // unsafe, which is why Cache is preferred
        @Override
        public V get(Object k) {
            K key = (K) k;
            V value = compute(key);
            checkNotNull(value, "%s returned null for key %s.", computingFunction, key);
            notifyRemoval(key, value);
            return value;
        }

        private V compute(K key) {
            checkNotNull(key);
            try {
                return computingFunction.apply(key);
            } catch (ComputationException e) {
                throw e;
            } catch (Throwable t) {
                throw new ComputationException(t);
            }
        }
    }


    static final class ComputingMapAdapter<K, V>
            extends ComputingConcurrentHashMap<K, V> implements Serializable {
        private static final long serialVersionUID = 0;

        ComputingMapAdapter(MapMaker mapMaker,
                            Function<? super K, ? extends V> computingFunction) {
            super(mapMaker, computingFunction);
        }

        @SuppressWarnings("unchecked") // unsafe, which is one advantage of Cache over Map
        @Override
        public V get(Object key) {
            V value;
            try {
                value = getOrCompute((K) key);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                Throwables.propagateIfInstanceOf(cause, ComputationException.class);
                throw new ComputationException(cause);
            }

            if (value == null) {
                throw new NullPointerException(computingFunction + " returned null for key " + key + ".");
            }
            return value;
        }
    }
}
