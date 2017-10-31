

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.annotations.Weak;
import com.google.common.annotations.WeakOuter;
import com.google.common.base.Equivalence;
import com.google.common.base.Ticker;
import com.google.common.collect.GenericMapMaker.NullListener;
import com.google.common.collect.MapMaker.RemovalCause;
import com.google.common.collect.MapMaker.RemovalListener;
import com.google.common.collect.MapMaker.RemovalNotification;
import com.google.common.primitives.Ints;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractQueue;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.CollectPreconditions.checkRemove;


class MapMakerInternalMap<K, V>
        extends AbstractMap<K, V> implements ConcurrentMap<K, V>, Serializable {


    // Constants


    static final int MAXIMUM_CAPACITY = Ints.MAX_POWER_OF_TWO;


    static final int MAX_SEGMENTS = 1 << 16; // slightly conservative


    static final int CONTAINS_VALUE_RETRIES = 3;


    static final int DRAIN_THRESHOLD = 0x3F;


    // TODO(fry): empirically optimize this
    static final int DRAIN_MAX = 16;

    static final long CLEANUP_EXECUTOR_DELAY_SECS = 60;

    // Fields

    private static final Logger logger = Logger.getLogger(MapMakerInternalMap.class.getName());


    final transient int segmentMask;


    final transient int segmentShift;


    final transient Segment<K, V>[] segments;


    final int concurrencyLevel;


    final Equivalence<Object> keyEquivalence;


    final Equivalence<Object> valueEquivalence;


    final Strength keyStrength;


    final Strength valueStrength;


    final int maximumSize;


    final long expireAfterAccessNanos;


    final long expireAfterWriteNanos;


    // TODO(fry): define a new type which creates event objects and automates the clear logic
    final Queue<RemovalNotification<K, V>> removalNotificationQueue;


    final RemovalListener<K, V> removalListener;


    final transient EntryFactory entryFactory;


    final Ticker ticker;


    MapMakerInternalMap(MapMaker builder) {
        concurrencyLevel = Math.min(builder.getConcurrencyLevel(), MAX_SEGMENTS);

        keyStrength = builder.getKeyStrength();
        valueStrength = builder.getValueStrength();

        keyEquivalence = builder.getKeyEquivalence();
        valueEquivalence = valueStrength.defaultEquivalence();

        maximumSize = builder.maximumSize;
        expireAfterAccessNanos = builder.getExpireAfterAccessNanos();
        expireAfterWriteNanos = builder.getExpireAfterWriteNanos();

        entryFactory = EntryFactory.getFactory(keyStrength, expires(), evictsBySize());
        ticker = builder.getTicker();

        removalListener = builder.getRemovalListener();
        removalNotificationQueue = (removalListener == NullListener.INSTANCE)
                ? MapMakerInternalMap.<RemovalNotification<K, V>>discardingQueue()
                : new ConcurrentLinkedQueue<RemovalNotification<K, V>>();

        int initialCapacity = Math.min(builder.getInitialCapacity(), MAXIMUM_CAPACITY);
        if (evictsBySize()) {
            initialCapacity = Math.min(initialCapacity, maximumSize);
        }

        // Find power-of-two sizes best matching arguments. Constraints:
        // (segmentCount <= maximumSize)
        // && (concurrencyLevel > maximumSize || segmentCount > concurrencyLevel)
        int segmentShift = 0;
        int segmentCount = 1;
        while (segmentCount < concurrencyLevel
                && (!evictsBySize() || segmentCount * 2 <= maximumSize)) {
            ++segmentShift;
            segmentCount <<= 1;
        }
        this.segmentShift = 32 - segmentShift;
        segmentMask = segmentCount - 1;

        this.segments = newSegmentArray(segmentCount);

        int segmentCapacity = initialCapacity / segmentCount;
        if (segmentCapacity * segmentCount < initialCapacity) {
            ++segmentCapacity;
        }

        int segmentSize = 1;
        while (segmentSize < segmentCapacity) {
            segmentSize <<= 1;
        }

        if (evictsBySize()) {
            // Ensure sum of segment max sizes = overall max size
            int maximumSegmentSize = maximumSize / segmentCount + 1;
            int remainder = maximumSize % segmentCount;
            for (int i = 0; i < this.segments.length; ++i) {
                if (i == remainder) {
                    maximumSegmentSize--;
                }
                this.segments[i] =
                        createSegment(segmentSize, maximumSegmentSize);
            }
        } else {
            for (int i = 0; i < this.segments.length; ++i) {
                this.segments[i] =
                        createSegment(segmentSize, MapMaker.UNSET_INT);
            }
        }
    }

    boolean evictsBySize() {
        return maximumSize != MapMaker.UNSET_INT;
    }

    boolean expires() {
        return expiresAfterWrite() || expiresAfterAccess();
    }

    boolean expiresAfterWrite() {
        return expireAfterWriteNanos > 0;
    }

    boolean expiresAfterAccess() {
        return expireAfterAccessNanos > 0;
    }

    boolean usesKeyReferences() {
        return keyStrength != Strength.STRONG;
    }

    boolean usesValueReferences() {
        return valueStrength != Strength.STRONG;
    }

    enum Strength {


        STRONG {
            @Override
            <K, V> ValueReference<K, V> referenceValue(
                    Segment<K, V> segment, ReferenceEntry<K, V> entry, V value) {
                return new StrongValueReference<>(value);
            }

            @Override
            Equivalence<Object> defaultEquivalence() {
                return Equivalence.equals();
            }
        },

        SOFT {
            @Override
            <K, V> ValueReference<K, V> referenceValue(
                    Segment<K, V> segment, ReferenceEntry<K, V> entry, V value) {
                return new SoftValueReference<>(segment.valueReferenceQueue, value, entry);
            }

            @Override
            Equivalence<Object> defaultEquivalence() {
                return Equivalence.identity();
            }
        },

        WEAK {
            @Override
            <K, V> ValueReference<K, V> referenceValue(
                    Segment<K, V> segment, ReferenceEntry<K, V> entry, V value) {
                return new WeakValueReference<>(segment.valueReferenceQueue, value, entry);
            }

            @Override
            Equivalence<Object> defaultEquivalence() {
                return Equivalence.identity();
            }
        };


        abstract <K, V> ValueReference<K, V> referenceValue(
                Segment<K, V> segment, ReferenceEntry<K, V> entry, V value);


        abstract Equivalence<Object> defaultEquivalence();
    }


    enum EntryFactory {
        STRONG {
            @Override
            <K, V> ReferenceEntry<K, V> newEntry(
                    Segment<K, V> segment, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
                return new StrongEntry<>(key, hash, next);
            }
        },
        STRONG_EXPIRABLE {
            @Override
            <K, V> ReferenceEntry<K, V> newEntry(
                    Segment<K, V> segment, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
                return new StrongExpirableEntry<>(key, hash, next);
            }

            @Override
            <K, V> ReferenceEntry<K, V> copyEntry(
                    Segment<K, V> segment, ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
                ReferenceEntry<K, V> newEntry = super.copyEntry(segment, original, newNext);
                copyExpirableEntry(original, newEntry);
                return newEntry;
            }
        },
        STRONG_EVICTABLE {
            @Override
            <K, V> ReferenceEntry<K, V> newEntry(
                    Segment<K, V> segment, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
                return new StrongEvictableEntry<>(key, hash, next);
            }

            @Override
            <K, V> ReferenceEntry<K, V> copyEntry(
                    Segment<K, V> segment, ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
                ReferenceEntry<K, V> newEntry = super.copyEntry(segment, original, newNext);
                copyEvictableEntry(original, newEntry);
                return newEntry;
            }
        },
        STRONG_EXPIRABLE_EVICTABLE {
            @Override
            <K, V> ReferenceEntry<K, V> newEntry(
                    Segment<K, V> segment, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
                return new StrongExpirableEvictableEntry<>(key, hash, next);
            }

            @Override
            <K, V> ReferenceEntry<K, V> copyEntry(
                    Segment<K, V> segment, ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
                ReferenceEntry<K, V> newEntry = super.copyEntry(segment, original, newNext);
                copyExpirableEntry(original, newEntry);
                copyEvictableEntry(original, newEntry);
                return newEntry;
            }
        },

        WEAK {
            @Override
            <K, V> ReferenceEntry<K, V> newEntry(
                    Segment<K, V> segment, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
                return new WeakEntry<>(segment.keyReferenceQueue, key, hash, next);
            }
        },
        WEAK_EXPIRABLE {
            @Override
            <K, V> ReferenceEntry<K, V> newEntry(
                    Segment<K, V> segment, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
                return new WeakExpirableEntry<>(segment.keyReferenceQueue, key, hash, next);
            }

            @Override
            <K, V> ReferenceEntry<K, V> copyEntry(
                    Segment<K, V> segment, ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
                ReferenceEntry<K, V> newEntry = super.copyEntry(segment, original, newNext);
                copyExpirableEntry(original, newEntry);
                return newEntry;
            }
        },
        WEAK_EVICTABLE {
            @Override
            <K, V> ReferenceEntry<K, V> newEntry(
                    Segment<K, V> segment, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
                return new WeakEvictableEntry<>(segment.keyReferenceQueue, key, hash, next);
            }

            @Override
            <K, V> ReferenceEntry<K, V> copyEntry(
                    Segment<K, V> segment, ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
                ReferenceEntry<K, V> newEntry = super.copyEntry(segment, original, newNext);
                copyEvictableEntry(original, newEntry);
                return newEntry;
            }
        },
        WEAK_EXPIRABLE_EVICTABLE {
            @Override
            <K, V> ReferenceEntry<K, V> newEntry(
                    Segment<K, V> segment, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
                return new WeakExpirableEvictableEntry<>(segment.keyReferenceQueue, key, hash, next);
            }

            @Override
            <K, V> ReferenceEntry<K, V> copyEntry(
                    Segment<K, V> segment, ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
                ReferenceEntry<K, V> newEntry = super.copyEntry(segment, original, newNext);
                copyExpirableEntry(original, newEntry);
                copyEvictableEntry(original, newEntry);
                return newEntry;
            }
        };


        static final int EXPIRABLE_MASK = 1;
        static final int EVICTABLE_MASK = 2;


        static final EntryFactory[][] factories = {
                {STRONG, STRONG_EXPIRABLE, STRONG_EVICTABLE, STRONG_EXPIRABLE_EVICTABLE},
                {}, // no support for SOFT keys
                {WEAK, WEAK_EXPIRABLE, WEAK_EVICTABLE, WEAK_EXPIRABLE_EVICTABLE}
        };

        static EntryFactory getFactory(Strength keyStrength, boolean expireAfterWrite,
                                       boolean evictsBySize) {
            int flags = (expireAfterWrite ? EXPIRABLE_MASK : 0) | (evictsBySize ? EVICTABLE_MASK : 0);
            return factories[keyStrength.ordinal()][flags];
        }


        abstract <K, V> ReferenceEntry<K, V> newEntry(
                Segment<K, V> segment, K key, int hash, @Nullable ReferenceEntry<K, V> next);


        // Guarded By Segment.this
        <K, V> ReferenceEntry<K, V> copyEntry(
                Segment<K, V> segment, ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
            return newEntry(segment, original.getKey(), original.getHash(), newNext);
        }

        // Guarded By Segment.this
        <K, V> void copyExpirableEntry(ReferenceEntry<K, V> original, ReferenceEntry<K, V> newEntry) {
            // TODO(fry): when we link values instead of entries this method can go
            // away, as can connectExpirables, nullifyExpirable.
            newEntry.setExpirationTime(original.getExpirationTime());

            connectExpirables(original.getPreviousExpirable(), newEntry);
            connectExpirables(newEntry, original.getNextExpirable());

            nullifyExpirable(original);
        }

        // Guarded By Segment.this
        <K, V> void copyEvictableEntry(ReferenceEntry<K, V> original, ReferenceEntry<K, V> newEntry) {
            // TODO(fry): when we link values instead of entries this method can go
            // away, as can connectEvictables, nullifyEvictable.
            connectEvictables(original.getPreviousEvictable(), newEntry);
            connectEvictables(newEntry, original.getNextEvictable());

            nullifyEvictable(original);
        }
    }


    interface ValueReference<K, V> {

        V get();


        V waitForValue() throws ExecutionException;


        ReferenceEntry<K, V> getEntry();


        ValueReference<K, V> copyFor(
                ReferenceQueue<V> queue, @Nullable V value, ReferenceEntry<K, V> entry);


        void clear(@Nullable ValueReference<K, V> newValue);


        boolean isComputingReference();
    }


    static final ValueReference<Object, Object> UNSET = new ValueReference<Object, Object>() {
        @Override
        public Object get() {
            return null;
        }

        @Override
        public ReferenceEntry<Object, Object> getEntry() {
            return null;
        }

        @Override
        public ValueReference<Object, Object> copyFor(ReferenceQueue<Object> queue,
                                                      @Nullable Object value, ReferenceEntry<Object, Object> entry) {
            return this;
        }

        @Override
        public boolean isComputingReference() {
            return false;
        }

        @Override
        public Object waitForValue() {
            return null;
        }

        @Override
        public void clear(ValueReference<Object, Object> newValue) {
        }
    };


    @SuppressWarnings("unchecked") // impl never uses a parameter or returns any non-null value
    static <K, V> ValueReference<K, V> unset() {
        return (ValueReference<K, V>) UNSET;
    }


    interface ReferenceEntry<K, V> {

        ValueReference<K, V> getValueReference();


        void setValueReference(ValueReference<K, V> valueReference);


        ReferenceEntry<K, V> getNext();


        int getHash();


        K getKey();


        long getExpirationTime();


        void setExpirationTime(long time);


        ReferenceEntry<K, V> getNextExpirable();


        void setNextExpirable(ReferenceEntry<K, V> next);


        ReferenceEntry<K, V> getPreviousExpirable();


        void setPreviousExpirable(ReferenceEntry<K, V> previous);


        ReferenceEntry<K, V> getNextEvictable();


        void setNextEvictable(ReferenceEntry<K, V> next);


        ReferenceEntry<K, V> getPreviousEvictable();


        void setPreviousEvictable(ReferenceEntry<K, V> previous);
    }

    private enum NullEntry implements ReferenceEntry<Object, Object> {
        INSTANCE;

        @Override
        public ValueReference<Object, Object> getValueReference() {
            return null;
        }

        @Override
        public void setValueReference(ValueReference<Object, Object> valueReference) {
        }

        @Override
        public ReferenceEntry<Object, Object> getNext() {
            return null;
        }

        @Override
        public int getHash() {
            return 0;
        }

        @Override
        public Object getKey() {
            return null;
        }

        @Override
        public long getExpirationTime() {
            return 0;
        }

        @Override
        public void setExpirationTime(long time) {
        }

        @Override
        public ReferenceEntry<Object, Object> getNextExpirable() {
            return this;
        }

        @Override
        public void setNextExpirable(ReferenceEntry<Object, Object> next) {
        }

        @Override
        public ReferenceEntry<Object, Object> getPreviousExpirable() {
            return this;
        }

        @Override
        public void setPreviousExpirable(ReferenceEntry<Object, Object> previous) {
        }

        @Override
        public ReferenceEntry<Object, Object> getNextEvictable() {
            return this;
        }

        @Override
        public void setNextEvictable(ReferenceEntry<Object, Object> next) {
        }

        @Override
        public ReferenceEntry<Object, Object> getPreviousEvictable() {
            return this;
        }

        @Override
        public void setPreviousEvictable(ReferenceEntry<Object, Object> previous) {
        }
    }

    abstract static class AbstractReferenceEntry<K, V> implements ReferenceEntry<K, V> {
        @Override
        public ValueReference<K, V> getValueReference() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setValueReference(ValueReference<K, V> valueReference) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ReferenceEntry<K, V> getNext() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getHash() {
            throw new UnsupportedOperationException();
        }

        @Override
        public K getKey() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getExpirationTime() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setExpirationTime(long time) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ReferenceEntry<K, V> getNextExpirable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setNextExpirable(ReferenceEntry<K, V> next) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ReferenceEntry<K, V> getPreviousExpirable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setPreviousExpirable(ReferenceEntry<K, V> previous) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ReferenceEntry<K, V> getNextEvictable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setNextEvictable(ReferenceEntry<K, V> next) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ReferenceEntry<K, V> getPreviousEvictable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setPreviousEvictable(ReferenceEntry<K, V> previous) {
            throw new UnsupportedOperationException();
        }
    }

    @SuppressWarnings("unchecked") // impl never uses a parameter or returns any non-null value
    static <K, V> ReferenceEntry<K, V> nullEntry() {
        return (ReferenceEntry<K, V>) NullEntry.INSTANCE;
    }

    static final Queue<? extends Object> DISCARDING_QUEUE = new AbstractQueue<Object>() {
        @Override
        public boolean offer(Object o) {
            return true;
        }

        @Override
        public Object peek() {
            return null;
        }

        @Override
        public Object poll() {
            return null;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public Iterator<Object> iterator() {
            return Iterators.emptyIterator();
        }
    };


    @SuppressWarnings("unchecked") // impl never uses a parameter or returns any non-null value
    static <E> Queue<E> discardingQueue() {
        return (Queue) DISCARDING_QUEUE;
    }


    static class StrongEntry<K, V> implements ReferenceEntry<K, V> {
        final K key;

        StrongEntry(K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            this.key = key;
            this.hash = hash;
            this.next = next;
        }

        @Override
        public K getKey() {
            return this.key;
        }

        // null expiration

        @Override
        public long getExpirationTime() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setExpirationTime(long time) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ReferenceEntry<K, V> getNextExpirable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setNextExpirable(ReferenceEntry<K, V> next) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ReferenceEntry<K, V> getPreviousExpirable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setPreviousExpirable(ReferenceEntry<K, V> previous) {
            throw new UnsupportedOperationException();
        }

        // null eviction

        @Override
        public ReferenceEntry<K, V> getNextEvictable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setNextEvictable(ReferenceEntry<K, V> next) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ReferenceEntry<K, V> getPreviousEvictable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setPreviousEvictable(ReferenceEntry<K, V> previous) {
            throw new UnsupportedOperationException();
        }

        // The code below is exactly the same for each entry type.

        final int hash;
        final ReferenceEntry<K, V> next;
        volatile ValueReference<K, V> valueReference = unset();

        @Override
        public ValueReference<K, V> getValueReference() {
            return valueReference;
        }

        @Override
        public void setValueReference(ValueReference<K, V> valueReference) {
            ValueReference<K, V> previous = this.valueReference;
            this.valueReference = valueReference;
            previous.clear(valueReference);
        }

        @Override
        public int getHash() {
            return hash;
        }

        @Override
        public ReferenceEntry<K, V> getNext() {
            return next;
        }
    }

    static final class StrongExpirableEntry<K, V> extends StrongEntry<K, V>
            implements ReferenceEntry<K, V> {
        StrongExpirableEntry(K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            super(key, hash, next);
        }

        // The code below is exactly the same for each expirable entry type.

        volatile long time = Long.MAX_VALUE;

        @Override
        public long getExpirationTime() {
            return time;
        }

        @Override
        public void setExpirationTime(long time) {
            this.time = time;
        }

        // Guarded By Segment.this
        ReferenceEntry<K, V> nextExpirable = nullEntry();

        @Override
        public ReferenceEntry<K, V> getNextExpirable() {
            return nextExpirable;
        }

        @Override
        public void setNextExpirable(ReferenceEntry<K, V> next) {
            this.nextExpirable = next;
        }

        // Guarded By Segment.this
        ReferenceEntry<K, V> previousExpirable = nullEntry();

        @Override
        public ReferenceEntry<K, V> getPreviousExpirable() {
            return previousExpirable;
        }

        @Override
        public void setPreviousExpirable(ReferenceEntry<K, V> previous) {
            this.previousExpirable = previous;
        }
    }

    static final class StrongEvictableEntry<K, V>
            extends StrongEntry<K, V> implements ReferenceEntry<K, V> {
        StrongEvictableEntry(K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            super(key, hash, next);
        }

        // The code below is exactly the same for each evictable entry type.

        // Guarded By Segment.this
        ReferenceEntry<K, V> nextEvictable = nullEntry();

        @Override
        public ReferenceEntry<K, V> getNextEvictable() {
            return nextEvictable;
        }

        @Override
        public void setNextEvictable(ReferenceEntry<K, V> next) {
            this.nextEvictable = next;
        }

        // Guarded By Segment.this
        ReferenceEntry<K, V> previousEvictable = nullEntry();

        @Override
        public ReferenceEntry<K, V> getPreviousEvictable() {
            return previousEvictable;
        }

        @Override
        public void setPreviousEvictable(ReferenceEntry<K, V> previous) {
            this.previousEvictable = previous;
        }
    }

    static final class StrongExpirableEvictableEntry<K, V>
            extends StrongEntry<K, V> implements ReferenceEntry<K, V> {
        StrongExpirableEvictableEntry(K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            super(key, hash, next);
        }

        // The code below is exactly the same for each expirable entry type.

        volatile long time = Long.MAX_VALUE;

        @Override
        public long getExpirationTime() {
            return time;
        }

        @Override
        public void setExpirationTime(long time) {
            this.time = time;
        }

        // Guarded By Segment.this
        ReferenceEntry<K, V> nextExpirable = nullEntry();

        @Override
        public ReferenceEntry<K, V> getNextExpirable() {
            return nextExpirable;
        }

        @Override
        public void setNextExpirable(ReferenceEntry<K, V> next) {
            this.nextExpirable = next;
        }

        // Guarded By Segment.this
        ReferenceEntry<K, V> previousExpirable = nullEntry();

        @Override
        public ReferenceEntry<K, V> getPreviousExpirable() {
            return previousExpirable;
        }

        @Override
        public void setPreviousExpirable(ReferenceEntry<K, V> previous) {
            this.previousExpirable = previous;
        }

        // The code below is exactly the same for each evictable entry type.

        // Guarded By Segment.this
        ReferenceEntry<K, V> nextEvictable = nullEntry();

        @Override
        public ReferenceEntry<K, V> getNextEvictable() {
            return nextEvictable;
        }

        @Override
        public void setNextEvictable(ReferenceEntry<K, V> next) {
            this.nextEvictable = next;
        }

        // Guarded By Segment.this
        ReferenceEntry<K, V> previousEvictable = nullEntry();

        @Override
        public ReferenceEntry<K, V> getPreviousEvictable() {
            return previousEvictable;
        }

        @Override
        public void setPreviousEvictable(ReferenceEntry<K, V> previous) {
            this.previousEvictable = previous;
        }
    }


    static class SoftEntry<K, V> extends SoftReference<K> implements ReferenceEntry<K, V> {
        SoftEntry(ReferenceQueue<K> queue, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            super(key, queue);
            this.hash = hash;
            this.next = next;
        }

        @Override
        public K getKey() {
            return get();
        }

        // null expiration
        @Override
        public long getExpirationTime() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setExpirationTime(long time) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ReferenceEntry<K, V> getNextExpirable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setNextExpirable(ReferenceEntry<K, V> next) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ReferenceEntry<K, V> getPreviousExpirable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setPreviousExpirable(ReferenceEntry<K, V> previous) {
            throw new UnsupportedOperationException();
        }

        // null eviction

        @Override
        public ReferenceEntry<K, V> getNextEvictable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setNextEvictable(ReferenceEntry<K, V> next) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ReferenceEntry<K, V> getPreviousEvictable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setPreviousEvictable(ReferenceEntry<K, V> previous) {
            throw new UnsupportedOperationException();
        }

        // The code below is exactly the same for each entry type.

        final int hash;
        final ReferenceEntry<K, V> next;
        volatile ValueReference<K, V> valueReference = unset();

        @Override
        public ValueReference<K, V> getValueReference() {
            return valueReference;
        }

        @Override
        public void setValueReference(ValueReference<K, V> valueReference) {
            ValueReference<K, V> previous = this.valueReference;
            this.valueReference = valueReference;
            previous.clear(valueReference);
        }

        @Override
        public int getHash() {
            return hash;
        }

        @Override
        public ReferenceEntry<K, V> getNext() {
            return next;
        }
    }

    static final class SoftExpirableEntry<K, V>
            extends SoftEntry<K, V> implements ReferenceEntry<K, V> {
        SoftExpirableEntry(
                ReferenceQueue<K> queue, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            super(queue, key, hash, next);
        }

        // The code below is exactly the same for each expirable entry type.

        volatile long time = Long.MAX_VALUE;

        @Override
        public long getExpirationTime() {
            return time;
        }

        @Override
        public void setExpirationTime(long time) {
            this.time = time;
        }

        // Guarded By Segment.this
        ReferenceEntry<K, V> nextExpirable = nullEntry();

        @Override
        public ReferenceEntry<K, V> getNextExpirable() {
            return nextExpirable;
        }

        @Override
        public void setNextExpirable(ReferenceEntry<K, V> next) {
            this.nextExpirable = next;
        }

        // Guarded By Segment.this
        ReferenceEntry<K, V> previousExpirable = nullEntry();

        @Override
        public ReferenceEntry<K, V> getPreviousExpirable() {
            return previousExpirable;
        }

        @Override
        public void setPreviousExpirable(ReferenceEntry<K, V> previous) {
            this.previousExpirable = previous;
        }
    }

    static final class SoftEvictableEntry<K, V>
            extends SoftEntry<K, V> implements ReferenceEntry<K, V> {
        SoftEvictableEntry(
                ReferenceQueue<K> queue, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            super(queue, key, hash, next);
        }

        // The code below is exactly the same for each evictable entry type.

        // Guarded By Segment.this
        ReferenceEntry<K, V> nextEvictable = nullEntry();

        @Override
        public ReferenceEntry<K, V> getNextEvictable() {
            return nextEvictable;
        }

        @Override
        public void setNextEvictable(ReferenceEntry<K, V> next) {
            this.nextEvictable = next;
        }

        // Guarded By Segment.this
        ReferenceEntry<K, V> previousEvictable = nullEntry();

        @Override
        public ReferenceEntry<K, V> getPreviousEvictable() {
            return previousEvictable;
        }

        @Override
        public void setPreviousEvictable(ReferenceEntry<K, V> previous) {
            this.previousEvictable = previous;
        }
    }

    static final class SoftExpirableEvictableEntry<K, V>
            extends SoftEntry<K, V> implements ReferenceEntry<K, V> {
        SoftExpirableEvictableEntry(
                ReferenceQueue<K> queue, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            super(queue, key, hash, next);
        }

        // The code below is exactly the same for each expirable entry type.

        volatile long time = Long.MAX_VALUE;

        @Override
        public long getExpirationTime() {
            return time;
        }

        @Override
        public void setExpirationTime(long time) {
            this.time = time;
        }

        // Guarded By Segment.this
        ReferenceEntry<K, V> nextExpirable = nullEntry();

        @Override
        public ReferenceEntry<K, V> getNextExpirable() {
            return nextExpirable;
        }

        @Override
        public void setNextExpirable(ReferenceEntry<K, V> next) {
            this.nextExpirable = next;
        }

        // Guarded By Segment.this
        ReferenceEntry<K, V> previousExpirable = nullEntry();

        @Override
        public ReferenceEntry<K, V> getPreviousExpirable() {
            return previousExpirable;
        }

        @Override
        public void setPreviousExpirable(ReferenceEntry<K, V> previous) {
            this.previousExpirable = previous;
        }

        // The code below is exactly the same for each evictable entry type.

        // Guarded By Segment.this
        ReferenceEntry<K, V> nextEvictable = nullEntry();

        @Override
        public ReferenceEntry<K, V> getNextEvictable() {
            return nextEvictable;
        }

        @Override
        public void setNextEvictable(ReferenceEntry<K, V> next) {
            this.nextEvictable = next;
        }

        // Guarded By Segment.this
        ReferenceEntry<K, V> previousEvictable = nullEntry();

        @Override
        public ReferenceEntry<K, V> getPreviousEvictable() {
            return previousEvictable;
        }

        @Override
        public void setPreviousEvictable(ReferenceEntry<K, V> previous) {
            this.previousEvictable = previous;
        }
    }


    static class WeakEntry<K, V> extends WeakReference<K> implements ReferenceEntry<K, V> {
        WeakEntry(ReferenceQueue<K> queue, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            super(key, queue);
            this.hash = hash;
            this.next = next;
        }

        @Override
        public K getKey() {
            return get();
        }

        // null expiration

        @Override
        public long getExpirationTime() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setExpirationTime(long time) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ReferenceEntry<K, V> getNextExpirable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setNextExpirable(ReferenceEntry<K, V> next) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ReferenceEntry<K, V> getPreviousExpirable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setPreviousExpirable(ReferenceEntry<K, V> previous) {
            throw new UnsupportedOperationException();
        }

        // null eviction

        @Override
        public ReferenceEntry<K, V> getNextEvictable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setNextEvictable(ReferenceEntry<K, V> next) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ReferenceEntry<K, V> getPreviousEvictable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setPreviousEvictable(ReferenceEntry<K, V> previous) {
            throw new UnsupportedOperationException();
        }

        // The code below is exactly the same for each entry type.

        final int hash;
        final ReferenceEntry<K, V> next;
        volatile ValueReference<K, V> valueReference = unset();

        @Override
        public ValueReference<K, V> getValueReference() {
            return valueReference;
        }

        @Override
        public void setValueReference(ValueReference<K, V> valueReference) {
            ValueReference<K, V> previous = this.valueReference;
            this.valueReference = valueReference;
            previous.clear(valueReference);
        }

        @Override
        public int getHash() {
            return hash;
        }

        @Override
        public ReferenceEntry<K, V> getNext() {
            return next;
        }
    }

    static final class WeakExpirableEntry<K, V>
            extends WeakEntry<K, V> implements ReferenceEntry<K, V> {
        WeakExpirableEntry(
                ReferenceQueue<K> queue, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            super(queue, key, hash, next);
        }

        // The code below is exactly the same for each expirable entry type.

        volatile long time = Long.MAX_VALUE;

        @Override
        public long getExpirationTime() {
            return time;
        }

        @Override
        public void setExpirationTime(long time) {
            this.time = time;
        }

        // Guarded By Segment.this
        ReferenceEntry<K, V> nextExpirable = nullEntry();

        @Override
        public ReferenceEntry<K, V> getNextExpirable() {
            return nextExpirable;
        }

        @Override
        public void setNextExpirable(ReferenceEntry<K, V> next) {
            this.nextExpirable = next;
        }

        // Guarded By Segment.this
        ReferenceEntry<K, V> previousExpirable = nullEntry();

        @Override
        public ReferenceEntry<K, V> getPreviousExpirable() {
            return previousExpirable;
        }

        @Override
        public void setPreviousExpirable(ReferenceEntry<K, V> previous) {
            this.previousExpirable = previous;
        }
    }

    static final class WeakEvictableEntry<K, V>
            extends WeakEntry<K, V> implements ReferenceEntry<K, V> {
        WeakEvictableEntry(
                ReferenceQueue<K> queue, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            super(queue, key, hash, next);
        }

        // The code below is exactly the same for each evictable entry type.

        // Guarded By Segment.this
        ReferenceEntry<K, V> nextEvictable = nullEntry();

        @Override
        public ReferenceEntry<K, V> getNextEvictable() {
            return nextEvictable;
        }

        @Override
        public void setNextEvictable(ReferenceEntry<K, V> next) {
            this.nextEvictable = next;
        }

        // Guarded By Segment.this
        ReferenceEntry<K, V> previousEvictable = nullEntry();

        @Override
        public ReferenceEntry<K, V> getPreviousEvictable() {
            return previousEvictable;
        }

        @Override
        public void setPreviousEvictable(ReferenceEntry<K, V> previous) {
            this.previousEvictable = previous;
        }
    }

    static final class WeakExpirableEvictableEntry<K, V>
            extends WeakEntry<K, V> implements ReferenceEntry<K, V> {
        WeakExpirableEvictableEntry(
                ReferenceQueue<K> queue, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            super(queue, key, hash, next);
        }

        // The code below is exactly the same for each expirable entry type.

        volatile long time = Long.MAX_VALUE;

        @Override
        public long getExpirationTime() {
            return time;
        }

        @Override
        public void setExpirationTime(long time) {
            this.time = time;
        }

        // Guarded By Segment.this
        ReferenceEntry<K, V> nextExpirable = nullEntry();

        @Override
        public ReferenceEntry<K, V> getNextExpirable() {
            return nextExpirable;
        }

        @Override
        public void setNextExpirable(ReferenceEntry<K, V> next) {
            this.nextExpirable = next;
        }

        // Guarded By Segment.this
        ReferenceEntry<K, V> previousExpirable = nullEntry();

        @Override
        public ReferenceEntry<K, V> getPreviousExpirable() {
            return previousExpirable;
        }

        @Override
        public void setPreviousExpirable(ReferenceEntry<K, V> previous) {
            this.previousExpirable = previous;
        }

        // The code below is exactly the same for each evictable entry type.

        // Guarded By Segment.this
        ReferenceEntry<K, V> nextEvictable = nullEntry();

        @Override
        public ReferenceEntry<K, V> getNextEvictable() {
            return nextEvictable;
        }

        @Override
        public void setNextEvictable(ReferenceEntry<K, V> next) {
            this.nextEvictable = next;
        }

        // Guarded By Segment.this
        ReferenceEntry<K, V> previousEvictable = nullEntry();

        @Override
        public ReferenceEntry<K, V> getPreviousEvictable() {
            return previousEvictable;
        }

        @Override
        public void setPreviousEvictable(ReferenceEntry<K, V> previous) {
            this.previousEvictable = previous;
        }
    }


    static final class WeakValueReference<K, V>
            extends WeakReference<V> implements ValueReference<K, V> {
        final ReferenceEntry<K, V> entry;

        WeakValueReference(ReferenceQueue<V> queue, V referent, ReferenceEntry<K, V> entry) {
            super(referent, queue);
            this.entry = entry;
        }

        @Override
        public ReferenceEntry<K, V> getEntry() {
            return entry;
        }

        @Override
        public void clear(ValueReference<K, V> newValue) {
            clear();
        }

        @Override
        public ValueReference<K, V> copyFor(
                ReferenceQueue<V> queue, V value, ReferenceEntry<K, V> entry) {
            return new WeakValueReference<>(queue, value, entry);
        }

        @Override
        public boolean isComputingReference() {
            return false;
        }

        @Override
        public V waitForValue() {
            return get();
        }
    }


    static final class SoftValueReference<K, V>
            extends SoftReference<V> implements ValueReference<K, V> {
        final ReferenceEntry<K, V> entry;

        SoftValueReference(ReferenceQueue<V> queue, V referent, ReferenceEntry<K, V> entry) {
            super(referent, queue);
            this.entry = entry;
        }

        @Override
        public ReferenceEntry<K, V> getEntry() {
            return entry;
        }

        @Override
        public void clear(ValueReference<K, V> newValue) {
            clear();
        }

        @Override
        public ValueReference<K, V> copyFor(
                ReferenceQueue<V> queue, V value, ReferenceEntry<K, V> entry) {
            return new SoftValueReference<>(queue, value, entry);
        }

        @Override
        public boolean isComputingReference() {
            return false;
        }

        @Override
        public V waitForValue() {
            return get();
        }
    }


    static final class StrongValueReference<K, V> implements ValueReference<K, V> {
        final V referent;

        StrongValueReference(V referent) {
            this.referent = referent;
        }

        @Override
        public V get() {
            return referent;
        }

        @Override
        public ReferenceEntry<K, V> getEntry() {
            return null;
        }

        @Override
        public ValueReference<K, V> copyFor(
                ReferenceQueue<V> queue, V value, ReferenceEntry<K, V> entry) {
            return this;
        }

        @Override
        public boolean isComputingReference() {
            return false;
        }

        @Override
        public V waitForValue() {
            return get();
        }

        @Override
        public void clear(ValueReference<K, V> newValue) {
        }
    }


    static int rehash(int h) {
        // Spread bits to regularize both segment and index locations,
        // using variant of single-word Wang/Jenkins hash.
        // TODO(kevinb): use Hashing/move this to Hashing?
        h += (h << 15) ^ 0xffffcd7d;
        h ^= (h >>> 10);
        h += (h << 3);
        h ^= (h >>> 6);
        h += (h << 2) + (h << 14);
        return h ^ (h >>> 16);
    }


    // Guarded By Segment.this
    @VisibleForTesting
    ReferenceEntry<K, V> newEntry(K key, int hash, @Nullable ReferenceEntry<K, V> next) {
        return segmentFor(hash).newEntry(key, hash, next);
    }


    // Guarded By Segment.this
    @VisibleForTesting
    ReferenceEntry<K, V> copyEntry(ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
        int hash = original.getHash();
        return segmentFor(hash).copyEntry(original, newNext);
    }


    // Guarded By Segment.this
    @VisibleForTesting
    ValueReference<K, V> newValueReference(ReferenceEntry<K, V> entry, V value) {
        int hash = entry.getHash();
        return valueStrength.referenceValue(segmentFor(hash), entry, value);
    }

    int hash(Object key) {
        int h = keyEquivalence.hash(key);
        return rehash(h);
    }

    void reclaimValue(ValueReference<K, V> valueReference) {
        ReferenceEntry<K, V> entry = valueReference.getEntry();
        int hash = entry.getHash();
        segmentFor(hash).reclaimValue(entry.getKey(), hash, valueReference);
    }

    void reclaimKey(ReferenceEntry<K, V> entry) {
        int hash = entry.getHash();
        segmentFor(hash).reclaimKey(entry, hash);
    }


    @VisibleForTesting
    boolean isLive(ReferenceEntry<K, V> entry) {
        return segmentFor(entry.getHash()).getLiveValue(entry) != null;
    }


    Segment<K, V> segmentFor(int hash) {
        // TODO(fry): Lazily create segments?
        return segments[(hash >>> segmentShift) & segmentMask];
    }

    Segment<K, V> createSegment(int initialCapacity, int maxSegmentSize) {
        return new Segment<>(this, initialCapacity, maxSegmentSize);
    }


    V getLiveValue(ReferenceEntry<K, V> entry) {
        if (entry.getKey() == null) {
            return null;
        }
        V value = entry.getValueReference().get();
        if (value == null) {
            return null;
        }

        if (expires() && isExpired(entry)) {
            return null;
        }
        return value;
    }

    // expiration


    boolean isExpired(ReferenceEntry<K, V> entry) {
        return isExpired(entry, ticker.read());
    }


    boolean isExpired(ReferenceEntry<K, V> entry, long now) {
        // if the expiration time had overflowed, this "undoes" the overflow
        return now - entry.getExpirationTime() > 0;
    }

    // Guarded By Segment.this
    static <K, V> void connectExpirables(ReferenceEntry<K, V> previous, ReferenceEntry<K, V> next) {
        previous.setNextExpirable(next);
        next.setPreviousExpirable(previous);
    }

    // Guarded By Segment.this
    static <K, V> void nullifyExpirable(ReferenceEntry<K, V> nulled) {
        ReferenceEntry<K, V> nullEntry = nullEntry();
        nulled.setNextExpirable(nullEntry);
        nulled.setPreviousExpirable(nullEntry);
    }

    // eviction


    void processPendingNotifications() {
        RemovalNotification<K, V> notification;
        while ((notification = removalNotificationQueue.poll()) != null) {
            try {
                removalListener.onRemoval(notification);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception thrown by removal listener", e);
            }
        }
    }


    // Guarded By Segment.this
    static <K, V> void connectEvictables(ReferenceEntry<K, V> previous, ReferenceEntry<K, V> next) {
        previous.setNextEvictable(next);
        next.setPreviousEvictable(previous);
    }

    // Guarded By Segment.this
    static <K, V> void nullifyEvictable(ReferenceEntry<K, V> nulled) {
        ReferenceEntry<K, V> nullEntry = nullEntry();
        nulled.setNextEvictable(nullEntry);
        nulled.setPreviousEvictable(nullEntry);
    }

    @SuppressWarnings("unchecked")
    final Segment<K, V>[] newSegmentArray(int ssize) {
        return new Segment[ssize];
    }

    // Inner Classes


    @SuppressWarnings("serial") // This class is never serialized.
    static class Segment<K, V> extends ReentrantLock {


        @Weak
        final MapMakerInternalMap<K, V> map;


        volatile int count;


        int modCount;


        int threshold;


        volatile AtomicReferenceArray<ReferenceEntry<K, V>> table;


        final int maxSegmentSize;


        final ReferenceQueue<K> keyReferenceQueue;


        final ReferenceQueue<V> valueReferenceQueue;


        final Queue<ReferenceEntry<K, V>> recencyQueue;


        final AtomicInteger readCount = new AtomicInteger();


        final Queue<ReferenceEntry<K, V>> evictionQueue;


        final Queue<ReferenceEntry<K, V>> expirationQueue;

        Segment(MapMakerInternalMap<K, V> map, int initialCapacity, int maxSegmentSize) {
            this.map = map;
            this.maxSegmentSize = maxSegmentSize;
            initTable(newEntryArray(initialCapacity));

            keyReferenceQueue = map.usesKeyReferences()
                    ? new ReferenceQueue<K>() : null;

            valueReferenceQueue = map.usesValueReferences()
                    ? new ReferenceQueue<V>() : null;

            recencyQueue = (map.evictsBySize() || map.expiresAfterAccess())
                    ? new ConcurrentLinkedQueue<ReferenceEntry<K, V>>()
                    : MapMakerInternalMap.<ReferenceEntry<K, V>>discardingQueue();

            evictionQueue = map.evictsBySize()
                    ? new EvictionQueue<K, V>()
                    : MapMakerInternalMap.<ReferenceEntry<K, V>>discardingQueue();

            expirationQueue = map.expires()
                    ? new ExpirationQueue<K, V>()
                    : MapMakerInternalMap.<ReferenceEntry<K, V>>discardingQueue();
        }

        AtomicReferenceArray<ReferenceEntry<K, V>> newEntryArray(int size) {
            return new AtomicReferenceArray<>(size);
        }

        void initTable(AtomicReferenceArray<ReferenceEntry<K, V>> newTable) {
            this.threshold = newTable.length() * 3 / 4; // 0.75
            if (this.threshold == maxSegmentSize) {
                // prevent spurious expansion before eviction
                this.threshold++;
            }
            this.table = newTable;
        }


        ReferenceEntry<K, V> newEntry(K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            return map.entryFactory.newEntry(this, key, hash, next);
        }


        ReferenceEntry<K, V> copyEntry(ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
            if (original.getKey() == null) {
                // key collected
                return null;
            }

            ValueReference<K, V> valueReference = original.getValueReference();
            V value = valueReference.get();
            if ((value == null) && !valueReference.isComputingReference()) {
                // value collected
                return null;
            }

            ReferenceEntry<K, V> newEntry = map.entryFactory.copyEntry(this, original, newNext);
            newEntry.setValueReference(valueReference.copyFor(this.valueReferenceQueue, value, newEntry));
            return newEntry;
        }


        void setValue(ReferenceEntry<K, V> entry, V value) {
            ValueReference<K, V> valueReference = map.valueStrength.referenceValue(this, entry, value);
            entry.setValueReference(valueReference);
            recordWrite(entry);
        }

        // reference queues, for garbage collection cleanup


        void tryDrainReferenceQueues() {
            if (tryLock()) {
                try {
                    drainReferenceQueues();
                } finally {
                    unlock();
                }
            }
        }


        void drainReferenceQueues() {
            if (map.usesKeyReferences()) {
                drainKeyReferenceQueue();
            }
            if (map.usesValueReferences()) {
                drainValueReferenceQueue();
            }
        }


        void drainKeyReferenceQueue() {
            Reference<? extends K> ref;
            int i = 0;
            while ((ref = keyReferenceQueue.poll()) != null) {
                @SuppressWarnings("unchecked")
                ReferenceEntry<K, V> entry = (ReferenceEntry<K, V>) ref;
                map.reclaimKey(entry);
                if (++i == DRAIN_MAX) {
                    break;
                }
            }
        }


        void drainValueReferenceQueue() {
            Reference<? extends V> ref;
            int i = 0;
            while ((ref = valueReferenceQueue.poll()) != null) {
                @SuppressWarnings("unchecked")
                ValueReference<K, V> valueReference = (ValueReference<K, V>) ref;
                map.reclaimValue(valueReference);
                if (++i == DRAIN_MAX) {
                    break;
                }
            }
        }


        void clearReferenceQueues() {
            if (map.usesKeyReferences()) {
                clearKeyReferenceQueue();
            }
            if (map.usesValueReferences()) {
                clearValueReferenceQueue();
            }
        }

        void clearKeyReferenceQueue() {
            while (keyReferenceQueue.poll() != null) {
            }
        }

        void clearValueReferenceQueue() {
            while (valueReferenceQueue.poll() != null) {
            }
        }

        // recency queue, shared by expiration and eviction


        void recordRead(ReferenceEntry<K, V> entry) {
            if (map.expiresAfterAccess()) {
                recordExpirationTime(entry, map.expireAfterAccessNanos);
            }
            recencyQueue.add(entry);
        }


        void recordLockedRead(ReferenceEntry<K, V> entry) {
            evictionQueue.add(entry);
            if (map.expiresAfterAccess()) {
                recordExpirationTime(entry, map.expireAfterAccessNanos);
                expirationQueue.add(entry);
            }
        }


        void recordWrite(ReferenceEntry<K, V> entry) {
            // we are already under lock, so drain the recency queue immediately
            drainRecencyQueue();
            evictionQueue.add(entry);
            if (map.expires()) {
                // currently MapMaker ensures that expireAfterWrite and
                // expireAfterAccess are mutually exclusive
                long expiration = map.expiresAfterAccess()
                        ? map.expireAfterAccessNanos
                        : map.expireAfterWriteNanos;
                recordExpirationTime(entry, expiration);
                expirationQueue.add(entry);
            }
        }


        void drainRecencyQueue() {
            ReferenceEntry<K, V> e;
            while ((e = recencyQueue.poll()) != null) {
                // An entry may be in the recency queue despite it being removed from
                // the map . This can occur when the entry was concurrently read while a
                // writer is removing it from the segment or after a clear has removed
                // all of the segment's entries.
                if (evictionQueue.contains(e)) {
                    evictionQueue.add(e);
                }
                if (map.expiresAfterAccess() && expirationQueue.contains(e)) {
                    expirationQueue.add(e);
                }
            }
        }

        // expiration

        void recordExpirationTime(ReferenceEntry<K, V> entry, long expirationNanos) {
            // might overflow, but that's okay (see isExpired())
            entry.setExpirationTime(map.ticker.read() + expirationNanos);
        }


        void tryExpireEntries() {
            if (tryLock()) {
                try {
                    expireEntries();
                } finally {
                    unlock();
                    // don't call postWriteCleanup as we're in a read
                }
            }
        }


        void expireEntries() {
            drainRecencyQueue();

            if (expirationQueue.isEmpty()) {
                // There's no point in calling nanoTime() if we have no entries to
                // expire.
                return;
            }
            long now = map.ticker.read();
            ReferenceEntry<K, V> e;
            while ((e = expirationQueue.peek()) != null && map.isExpired(e, now)) {
                if (!removeEntry(e, e.getHash(), RemovalCause.EXPIRED)) {
                    throw new AssertionError();
                }
            }
        }

        // eviction

        void enqueueNotification(ReferenceEntry<K, V> entry, RemovalCause cause) {
            enqueueNotification(entry.getKey(), entry.getHash(), entry.getValueReference().get(), cause);
        }

        void enqueueNotification(@Nullable K key, int hash, @Nullable V value, RemovalCause cause) {
            if (map.removalNotificationQueue != DISCARDING_QUEUE) {
                RemovalNotification<K, V> notification = new RemovalNotification<>(key, value, cause);
                map.removalNotificationQueue.offer(notification);
            }
        }


        boolean evictEntries() {
            if (map.evictsBySize() && count >= maxSegmentSize) {
                drainRecencyQueue();

                ReferenceEntry<K, V> e = evictionQueue.remove();
                if (!removeEntry(e, e.getHash(), RemovalCause.SIZE)) {
                    throw new AssertionError();
                }
                return true;
            }
            return false;
        }


        ReferenceEntry<K, V> getFirst(int hash) {
            // read this volatile field only once
            AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
            return table.get(hash & (table.length() - 1));
        }

        // Specialized implementations of map methods

        ReferenceEntry<K, V> getEntry(Object key, int hash) {
            if (count != 0) { // read-volatile
                for (ReferenceEntry<K, V> e = getFirst(hash); e != null; e = e.getNext()) {
                    if (e.getHash() != hash) {
                        continue;
                    }

                    K entryKey = e.getKey();
                    if (entryKey == null) {
                        tryDrainReferenceQueues();
                        continue;
                    }

                    if (map.keyEquivalence.equivalent(key, entryKey)) {
                        return e;
                    }
                }
            }

            return null;
        }

        ReferenceEntry<K, V> getLiveEntry(Object key, int hash) {
            ReferenceEntry<K, V> e = getEntry(key, hash);
            if (e == null) {
                return null;
            } else if (map.expires() && map.isExpired(e)) {
                tryExpireEntries();
                return null;
            }
            return e;
        }

        V get(Object key, int hash) {
            try {
                ReferenceEntry<K, V> e = getLiveEntry(key, hash);
                if (e == null) {
                    return null;
                }

                V value = e.getValueReference().get();
                if (value != null) {
                    recordRead(e);
                } else {
                    tryDrainReferenceQueues();
                }
                return value;
            } finally {
                postReadCleanup();
            }
        }

        boolean containsKey(Object key, int hash) {
            try {
                if (count != 0) { // read-volatile
                    ReferenceEntry<K, V> e = getLiveEntry(key, hash);
                    if (e == null) {
                        return false;
                    }
                    return e.getValueReference().get() != null;
                }

                return false;
            } finally {
                postReadCleanup();
            }
        }


        @VisibleForTesting
        boolean containsValue(Object value) {
            try {
                if (count != 0) { // read-volatile
                    AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
                    int length = table.length();
                    for (int i = 0; i < length; ++i) {
                        for (ReferenceEntry<K, V> e = table.get(i); e != null; e = e.getNext()) {
                            V entryValue = getLiveValue(e);
                            if (entryValue == null) {
                                continue;
                            }
                            if (map.valueEquivalence.equivalent(value, entryValue)) {
                                return true;
                            }
                        }
                    }
                }

                return false;
            } finally {
                postReadCleanup();
            }
        }

        V put(K key, int hash, V value, boolean onlyIfAbsent) {
            lock();
            try {
                preWriteCleanup();

                int newCount = this.count + 1;
                if (newCount > this.threshold) { // ensure capacity
                    expand();
                    newCount = this.count + 1;
                }

                AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
                int index = hash & (table.length() - 1);
                ReferenceEntry<K, V> first = table.get(index);

                // Look for an existing entry.
                for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
                    K entryKey = e.getKey();
                    if (e.getHash() == hash && entryKey != null
                            && map.keyEquivalence.equivalent(key, entryKey)) {
                        // We found an existing entry.

                        ValueReference<K, V> valueReference = e.getValueReference();
                        V entryValue = valueReference.get();

                        if (entryValue == null) {
                            ++modCount;
                            setValue(e, value);
                            if (!valueReference.isComputingReference()) {
                                enqueueNotification(key, hash, entryValue, RemovalCause.COLLECTED);
                                newCount = this.count; // count remains unchanged
                            } else if (evictEntries()) { // evictEntries after setting new value
                                newCount = this.count + 1;
                            }
                            this.count = newCount; // write-volatile
                            return null;
                        } else if (onlyIfAbsent) {
                            // Mimic
                            // "if (!map.containsKey(key)) ...
                            // else return map.get(key);
                            recordLockedRead(e);
                            return entryValue;
                        } else {
                            // clobber existing entry, count remains unchanged
                            ++modCount;
                            enqueueNotification(key, hash, entryValue, RemovalCause.REPLACED);
                            setValue(e, value);
                            return entryValue;
                        }
                    }
                }

                // Create a new entry.
                ++modCount;
                ReferenceEntry<K, V> newEntry = newEntry(key, hash, first);
                setValue(newEntry, value);
                table.set(index, newEntry);
                if (evictEntries()) { // evictEntries after setting new value
                    newCount = this.count + 1;
                }
                this.count = newCount; // write-volatile
                return null;
            } finally {
                unlock();
                postWriteCleanup();
            }
        }


        void expand() {
            AtomicReferenceArray<ReferenceEntry<K, V>> oldTable = table;
            int oldCapacity = oldTable.length();
            if (oldCapacity >= MAXIMUM_CAPACITY) {
                return;
            }


            int newCount = count;
            AtomicReferenceArray<ReferenceEntry<K, V>> newTable = newEntryArray(oldCapacity << 1);
            threshold = newTable.length() * 3 / 4;
            int newMask = newTable.length() - 1;
            for (int oldIndex = 0; oldIndex < oldCapacity; ++oldIndex) {
                // We need to guarantee that any existing reads of old Map can
                // proceed. So we cannot yet null out each bin.
                ReferenceEntry<K, V> head = oldTable.get(oldIndex);

                if (head != null) {
                    ReferenceEntry<K, V> next = head.getNext();
                    int headIndex = head.getHash() & newMask;

                    // Single node on list
                    if (next == null) {
                        newTable.set(headIndex, head);
                    } else {
                        // Reuse the consecutive sequence of nodes with the same target
                        // index from the end of the list. tail points to the first
                        // entry in the reusable list.
                        ReferenceEntry<K, V> tail = head;
                        int tailIndex = headIndex;
                        for (ReferenceEntry<K, V> e = next; e != null; e = e.getNext()) {
                            int newIndex = e.getHash() & newMask;
                            if (newIndex != tailIndex) {
                                // The index changed. We'll need to copy the previous entry.
                                tailIndex = newIndex;
                                tail = e;
                            }
                        }
                        newTable.set(tailIndex, tail);

                        // Clone nodes leading up to the tail.
                        for (ReferenceEntry<K, V> e = head; e != tail; e = e.getNext()) {
                            int newIndex = e.getHash() & newMask;
                            ReferenceEntry<K, V> newNext = newTable.get(newIndex);
                            ReferenceEntry<K, V> newFirst = copyEntry(e, newNext);
                            if (newFirst != null) {
                                newTable.set(newIndex, newFirst);
                            } else {
                                removeCollectedEntry(e);
                                newCount--;
                            }
                        }
                    }
                }
            }
            table = newTable;
            this.count = newCount;
        }

        boolean replace(K key, int hash, V oldValue, V newValue) {
            lock();
            try {
                preWriteCleanup();

                AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
                int index = hash & (table.length() - 1);
                ReferenceEntry<K, V> first = table.get(index);

                for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
                    K entryKey = e.getKey();
                    if (e.getHash() == hash && entryKey != null
                            && map.keyEquivalence.equivalent(key, entryKey)) {
                        // If the value disappeared, this entry is partially collected,
                        // and we should pretend like it doesn't exist.
                        ValueReference<K, V> valueReference = e.getValueReference();
                        V entryValue = valueReference.get();
                        if (entryValue == null) {
                            if (isCollected(valueReference)) {
                                int newCount = this.count - 1;
                                ++modCount;
                                enqueueNotification(entryKey, hash, entryValue, RemovalCause.COLLECTED);
                                ReferenceEntry<K, V> newFirst = removeFromChain(first, e);
                                newCount = this.count - 1;
                                table.set(index, newFirst);
                                this.count = newCount; // write-volatile
                            }
                            return false;
                        }

                        if (map.valueEquivalence.equivalent(oldValue, entryValue)) {
                            ++modCount;
                            enqueueNotification(key, hash, entryValue, RemovalCause.REPLACED);
                            setValue(e, newValue);
                            return true;
                        } else {
                            // Mimic
                            // "if (map.containsKey(key) && map.get(key).equals(oldValue))..."
                            recordLockedRead(e);
                            return false;
                        }
                    }
                }

                return false;
            } finally {
                unlock();
                postWriteCleanup();
            }
        }

        V replace(K key, int hash, V newValue) {
            lock();
            try {
                preWriteCleanup();

                AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
                int index = hash & (table.length() - 1);
                ReferenceEntry<K, V> first = table.get(index);

                for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
                    K entryKey = e.getKey();
                    if (e.getHash() == hash && entryKey != null
                            && map.keyEquivalence.equivalent(key, entryKey)) {
                        // If the value disappeared, this entry is partially collected,
                        // and we should pretend like it doesn't exist.
                        ValueReference<K, V> valueReference = e.getValueReference();
                        V entryValue = valueReference.get();
                        if (entryValue == null) {
                            if (isCollected(valueReference)) {
                                int newCount = this.count - 1;
                                ++modCount;
                                enqueueNotification(entryKey, hash, entryValue, RemovalCause.COLLECTED);
                                ReferenceEntry<K, V> newFirst = removeFromChain(first, e);
                                newCount = this.count - 1;
                                table.set(index, newFirst);
                                this.count = newCount; // write-volatile
                            }
                            return null;
                        }

                        ++modCount;
                        enqueueNotification(key, hash, entryValue, RemovalCause.REPLACED);
                        setValue(e, newValue);
                        return entryValue;
                    }
                }

                return null;
            } finally {
                unlock();
                postWriteCleanup();
            }
        }

        V remove(Object key, int hash) {
            lock();
            try {
                preWriteCleanup();

                int newCount = this.count - 1;
                AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
                int index = hash & (table.length() - 1);
                ReferenceEntry<K, V> first = table.get(index);

                for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
                    K entryKey = e.getKey();
                    if (e.getHash() == hash && entryKey != null
                            && map.keyEquivalence.equivalent(key, entryKey)) {
                        ValueReference<K, V> valueReference = e.getValueReference();
                        V entryValue = valueReference.get();

                        RemovalCause cause;
                        if (entryValue != null) {
                            cause = RemovalCause.EXPLICIT;
                        } else if (isCollected(valueReference)) {
                            cause = RemovalCause.COLLECTED;
                        } else {
                            return null;
                        }

                        ++modCount;
                        enqueueNotification(entryKey, hash, entryValue, cause);
                        ReferenceEntry<K, V> newFirst = removeFromChain(first, e);
                        newCount = this.count - 1;
                        table.set(index, newFirst);
                        this.count = newCount; // write-volatile
                        return entryValue;
                    }
                }

                return null;
            } finally {
                unlock();
                postWriteCleanup();
            }
        }

        boolean remove(Object key, int hash, Object value) {
            lock();
            try {
                preWriteCleanup();

                int newCount = this.count - 1;
                AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
                int index = hash & (table.length() - 1);
                ReferenceEntry<K, V> first = table.get(index);

                for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
                    K entryKey = e.getKey();
                    if (e.getHash() == hash && entryKey != null
                            && map.keyEquivalence.equivalent(key, entryKey)) {
                        ValueReference<K, V> valueReference = e.getValueReference();
                        V entryValue = valueReference.get();

                        RemovalCause cause;
                        if (map.valueEquivalence.equivalent(value, entryValue)) {
                            cause = RemovalCause.EXPLICIT;
                        } else if (isCollected(valueReference)) {
                            cause = RemovalCause.COLLECTED;
                        } else {
                            return false;
                        }

                        ++modCount;
                        enqueueNotification(entryKey, hash, entryValue, cause);
                        ReferenceEntry<K, V> newFirst = removeFromChain(first, e);
                        newCount = this.count - 1;
                        table.set(index, newFirst);
                        this.count = newCount; // write-volatile
                        return (cause == RemovalCause.EXPLICIT);
                    }
                }

                return false;
            } finally {
                unlock();
                postWriteCleanup();
            }
        }

        void clear() {
            if (count != 0) {
                lock();
                try {
                    AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
                    if (map.removalNotificationQueue != DISCARDING_QUEUE) {
                        for (int i = 0; i < table.length(); ++i) {
                            for (ReferenceEntry<K, V> e = table.get(i); e != null; e = e.getNext()) {
                                // Computing references aren't actually in the map yet.
                                if (!e.getValueReference().isComputingReference()) {
                                    enqueueNotification(e, RemovalCause.EXPLICIT);
                                }
                            }
                        }
                    }
                    for (int i = 0; i < table.length(); ++i) {
                        table.set(i, null);
                    }
                    clearReferenceQueues();
                    evictionQueue.clear();
                    expirationQueue.clear();
                    readCount.set(0);

                    ++modCount;
                    count = 0; // write-volatile
                } finally {
                    unlock();
                    postWriteCleanup();
                }
            }
        }


        ReferenceEntry<K, V> removeFromChain(ReferenceEntry<K, V> first, ReferenceEntry<K, V> entry) {
            evictionQueue.remove(entry);
            expirationQueue.remove(entry);

            int newCount = count;
            ReferenceEntry<K, V> newFirst = entry.getNext();
            for (ReferenceEntry<K, V> e = first; e != entry; e = e.getNext()) {
                ReferenceEntry<K, V> next = copyEntry(e, newFirst);
                if (next != null) {
                    newFirst = next;
                } else {
                    removeCollectedEntry(e);
                    newCount--;
                }
            }
            this.count = newCount;
            return newFirst;
        }

        void removeCollectedEntry(ReferenceEntry<K, V> entry) {
            enqueueNotification(entry, RemovalCause.COLLECTED);
            evictionQueue.remove(entry);
            expirationQueue.remove(entry);
        }


        boolean reclaimKey(ReferenceEntry<K, V> entry, int hash) {
            lock();
            try {
                int newCount = count - 1;
                AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
                int index = hash & (table.length() - 1);
                ReferenceEntry<K, V> first = table.get(index);

                for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
                    if (e == entry) {
                        ++modCount;
                        enqueueNotification(
                                e.getKey(), hash, e.getValueReference().get(), RemovalCause.COLLECTED);
                        ReferenceEntry<K, V> newFirst = removeFromChain(first, e);
                        newCount = this.count - 1;
                        table.set(index, newFirst);
                        this.count = newCount; // write-volatile
                        return true;
                    }
                }

                return false;
            } finally {
                unlock();
                postWriteCleanup();
            }
        }


        boolean reclaimValue(K key, int hash, ValueReference<K, V> valueReference) {
            lock();
            try {
                int newCount = this.count - 1;
                AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
                int index = hash & (table.length() - 1);
                ReferenceEntry<K, V> first = table.get(index);

                for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
                    K entryKey = e.getKey();
                    if (e.getHash() == hash && entryKey != null
                            && map.keyEquivalence.equivalent(key, entryKey)) {
                        ValueReference<K, V> v = e.getValueReference();
                        if (v == valueReference) {
                            ++modCount;
                            enqueueNotification(key, hash, valueReference.get(), RemovalCause.COLLECTED);
                            ReferenceEntry<K, V> newFirst = removeFromChain(first, e);
                            newCount = this.count - 1;
                            table.set(index, newFirst);
                            this.count = newCount; // write-volatile
                            return true;
                        }
                        return false;
                    }
                }

                return false;
            } finally {
                unlock();
                if (!isHeldByCurrentThread()) { // don't cleanup inside of put
                    postWriteCleanup();
                }
            }
        }


        boolean clearValue(K key, int hash, ValueReference<K, V> valueReference) {
            lock();
            try {
                AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
                int index = hash & (table.length() - 1);
                ReferenceEntry<K, V> first = table.get(index);

                for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
                    K entryKey = e.getKey();
                    if (e.getHash() == hash && entryKey != null
                            && map.keyEquivalence.equivalent(key, entryKey)) {
                        ValueReference<K, V> v = e.getValueReference();
                        if (v == valueReference) {
                            ReferenceEntry<K, V> newFirst = removeFromChain(first, e);
                            table.set(index, newFirst);
                            return true;
                        }
                        return false;
                    }
                }

                return false;
            } finally {
                unlock();
                postWriteCleanup();
            }
        }


        boolean removeEntry(ReferenceEntry<K, V> entry, int hash, RemovalCause cause) {
            int newCount = this.count - 1;
            AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
            int index = hash & (table.length() - 1);
            ReferenceEntry<K, V> first = table.get(index);

            for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
                if (e == entry) {
                    ++modCount;
                    enqueueNotification(e.getKey(), hash, e.getValueReference().get(), cause);
                    ReferenceEntry<K, V> newFirst = removeFromChain(first, e);
                    newCount = this.count - 1;
                    table.set(index, newFirst);
                    this.count = newCount; // write-volatile
                    return true;
                }
            }

            return false;
        }


        boolean isCollected(ValueReference<K, V> valueReference) {
            if (valueReference.isComputingReference()) {
                return false;
            }
            return (valueReference.get() == null);
        }


        V getLiveValue(ReferenceEntry<K, V> entry) {
            if (entry.getKey() == null) {
                tryDrainReferenceQueues();
                return null;
            }
            V value = entry.getValueReference().get();
            if (value == null) {
                tryDrainReferenceQueues();
                return null;
            }

            if (map.expires() && map.isExpired(entry)) {
                tryExpireEntries();
                return null;
            }
            return value;
        }


        void postReadCleanup() {
            if ((readCount.incrementAndGet() & DRAIN_THRESHOLD) == 0) {
                runCleanup();
            }
        }


        void preWriteCleanup() {
            runLockedCleanup();
        }


        void postWriteCleanup() {
            runUnlockedCleanup();
        }

        void runCleanup() {
            runLockedCleanup();
            runUnlockedCleanup();
        }

        void runLockedCleanup() {
            if (tryLock()) {
                try {
                    drainReferenceQueues();
                    expireEntries(); // calls drainRecencyQueue
                    readCount.set(0);
                } finally {
                    unlock();
                }
            }
        }

        void runUnlockedCleanup() {
            // locked cleanup may generate notifications we can send unlocked
            if (!isHeldByCurrentThread()) {
                map.processPendingNotifications();
            }
        }

    }

    // Queues


    static final class EvictionQueue<K, V> extends AbstractQueue<ReferenceEntry<K, V>> {
        final ReferenceEntry<K, V> head = new AbstractReferenceEntry<K, V>() {

            ReferenceEntry<K, V> nextEvictable = this;

            @Override
            public ReferenceEntry<K, V> getNextEvictable() {
                return nextEvictable;
            }

            @Override
            public void setNextEvictable(ReferenceEntry<K, V> next) {
                this.nextEvictable = next;
            }

            ReferenceEntry<K, V> previousEvictable = this;

            @Override
            public ReferenceEntry<K, V> getPreviousEvictable() {
                return previousEvictable;
            }

            @Override
            public void setPreviousEvictable(ReferenceEntry<K, V> previous) {
                this.previousEvictable = previous;
            }
        };

        // implements Queue

        @Override
        public boolean offer(ReferenceEntry<K, V> entry) {
            // unlink
            connectEvictables(entry.getPreviousEvictable(), entry.getNextEvictable());

            // add to tail
            connectEvictables(head.getPreviousEvictable(), entry);
            connectEvictables(entry, head);

            return true;
        }

        @Override
        public ReferenceEntry<K, V> peek() {
            ReferenceEntry<K, V> next = head.getNextEvictable();
            return (next == head) ? null : next;
        }

        @Override
        public ReferenceEntry<K, V> poll() {
            ReferenceEntry<K, V> next = head.getNextEvictable();
            if (next == head) {
                return null;
            }

            remove(next);
            return next;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean remove(Object o) {
            ReferenceEntry<K, V> e = (ReferenceEntry) o;
            ReferenceEntry<K, V> previous = e.getPreviousEvictable();
            ReferenceEntry<K, V> next = e.getNextEvictable();
            connectEvictables(previous, next);
            nullifyEvictable(e);

            return next != NullEntry.INSTANCE;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean contains(Object o) {
            ReferenceEntry<K, V> e = (ReferenceEntry) o;
            return e.getNextEvictable() != NullEntry.INSTANCE;
        }

        @Override
        public boolean isEmpty() {
            return head.getNextEvictable() == head;
        }

        @Override
        public int size() {
            int size = 0;
            for (ReferenceEntry<K, V> e = head.getNextEvictable(); e != head; e = e.getNextEvictable()) {
                size++;
            }
            return size;
        }

        @Override
        public void clear() {
            ReferenceEntry<K, V> e = head.getNextEvictable();
            while (e != head) {
                ReferenceEntry<K, V> next = e.getNextEvictable();
                nullifyEvictable(e);
                e = next;
            }

            head.setNextEvictable(head);
            head.setPreviousEvictable(head);
        }

        @Override
        public Iterator<ReferenceEntry<K, V>> iterator() {
            return new AbstractSequentialIterator<ReferenceEntry<K, V>>(peek()) {
                @Override
                protected ReferenceEntry<K, V> computeNext(ReferenceEntry<K, V> previous) {
                    ReferenceEntry<K, V> next = previous.getNextEvictable();
                    return (next == head) ? null : next;
                }
            };
        }
    }


    static final class ExpirationQueue<K, V> extends AbstractQueue<ReferenceEntry<K, V>> {
        final ReferenceEntry<K, V> head = new AbstractReferenceEntry<K, V>() {

            @Override
            public long getExpirationTime() {
                return Long.MAX_VALUE;
            }

            @Override
            public void setExpirationTime(long time) {
            }

            ReferenceEntry<K, V> nextExpirable = this;

            @Override
            public ReferenceEntry<K, V> getNextExpirable() {
                return nextExpirable;
            }

            @Override
            public void setNextExpirable(ReferenceEntry<K, V> next) {
                this.nextExpirable = next;
            }

            ReferenceEntry<K, V> previousExpirable = this;

            @Override
            public ReferenceEntry<K, V> getPreviousExpirable() {
                return previousExpirable;
            }

            @Override
            public void setPreviousExpirable(ReferenceEntry<K, V> previous) {
                this.previousExpirable = previous;
            }
        };

        // implements Queue

        @Override
        public boolean offer(ReferenceEntry<K, V> entry) {
            // unlink
            connectExpirables(entry.getPreviousExpirable(), entry.getNextExpirable());

            // add to tail
            connectExpirables(head.getPreviousExpirable(), entry);
            connectExpirables(entry, head);

            return true;
        }

        @Override
        public ReferenceEntry<K, V> peek() {
            ReferenceEntry<K, V> next = head.getNextExpirable();
            return (next == head) ? null : next;
        }

        @Override
        public ReferenceEntry<K, V> poll() {
            ReferenceEntry<K, V> next = head.getNextExpirable();
            if (next == head) {
                return null;
            }

            remove(next);
            return next;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean remove(Object o) {
            ReferenceEntry<K, V> e = (ReferenceEntry) o;
            ReferenceEntry<K, V> previous = e.getPreviousExpirable();
            ReferenceEntry<K, V> next = e.getNextExpirable();
            connectExpirables(previous, next);
            nullifyExpirable(e);

            return next != NullEntry.INSTANCE;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean contains(Object o) {
            ReferenceEntry<K, V> e = (ReferenceEntry) o;
            return e.getNextExpirable() != NullEntry.INSTANCE;
        }

        @Override
        public boolean isEmpty() {
            return head.getNextExpirable() == head;
        }

        @Override
        public int size() {
            int size = 0;
            for (ReferenceEntry<K, V> e = head.getNextExpirable(); e != head; e = e.getNextExpirable()) {
                size++;
            }
            return size;
        }

        @Override
        public void clear() {
            ReferenceEntry<K, V> e = head.getNextExpirable();
            while (e != head) {
                ReferenceEntry<K, V> next = e.getNextExpirable();
                nullifyExpirable(e);
                e = next;
            }

            head.setNextExpirable(head);
            head.setPreviousExpirable(head);
        }

        @Override
        public Iterator<ReferenceEntry<K, V>> iterator() {
            return new AbstractSequentialIterator<ReferenceEntry<K, V>>(peek()) {
                @Override
                protected ReferenceEntry<K, V> computeNext(ReferenceEntry<K, V> previous) {
                    ReferenceEntry<K, V> next = previous.getNextExpirable();
                    return (next == head) ? null : next;
                }
            };
        }
    }

    static final class CleanupMapTask implements Runnable {
        final WeakReference<MapMakerInternalMap<?, ?>> mapReference;

        public CleanupMapTask(MapMakerInternalMap<?, ?> map) {
            this.mapReference = new WeakReference<MapMakerInternalMap<?, ?>>(map);
        }

        @Override
        public void run() {
            MapMakerInternalMap<?, ?> map = mapReference.get();
            if (map == null) {
                throw new CancellationException();
            }

            for (Segment<?, ?> segment : map.segments) {
                segment.runCleanup();
            }
        }
    }

    // ConcurrentMap methods

    @Override
    public boolean isEmpty() {

        long sum = 0L;
        Segment<K, V>[] segments = this.segments;
        for (int i = 0; i < segments.length; ++i) {
            if (segments[i].count != 0) {
                return false;
            }
            sum += segments[i].modCount;
        }

        if (sum != 0L) { // recheck unless no modifications
            for (int i = 0; i < segments.length; ++i) {
                if (segments[i].count != 0) {
                    return false;
                }
                sum -= segments[i].modCount;
            }
            if (sum != 0L) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int size() {
        Segment<K, V>[] segments = this.segments;
        long sum = 0;
        for (int i = 0; i < segments.length; ++i) {
            sum += segments[i].count;
        }
        return Ints.saturatedCast(sum);
    }

    @Override
    public V get(@Nullable Object key) {
        if (key == null) {
            return null;
        }
        int hash = hash(key);
        return segmentFor(hash).get(key, hash);
    }


    ReferenceEntry<K, V> getEntry(@Nullable Object key) {
        if (key == null) {
            return null;
        }
        int hash = hash(key);
        return segmentFor(hash).getEntry(key, hash);
    }

    @Override
    public boolean containsKey(@Nullable Object key) {
        if (key == null) {
            return false;
        }
        int hash = hash(key);
        return segmentFor(hash).containsKey(key, hash);
    }

    @Override
    public boolean containsValue(@Nullable Object value) {
        if (value == null) {
            return false;
        }

        // This implementation is patterned after ConcurrentHashMap, but without the locking. The only
        // way for it to return a false negative would be for the target value to jump around in the map
        // such that none of the subsequent iterations observed it, despite the fact that at every point
        // in time it was present somewhere int the map. This becomes increasingly unlikely as
        // CONTAINS_VALUE_RETRIES increases, though without locking it is theoretically possible.
        final Segment<K, V>[] segments = this.segments;
        long last = -1L;
        for (int i = 0; i < CONTAINS_VALUE_RETRIES; i++) {
            long sum = 0L;
            for (Segment<K, V> segment : segments) {
                // ensure visibility of most recent completed write
                @SuppressWarnings({"UnusedDeclaration", "unused"})
                int c = segment.count; // read-volatile

                AtomicReferenceArray<ReferenceEntry<K, V>> table = segment.table;
                for (int j = 0; j < table.length(); j++) {
                    for (ReferenceEntry<K, V> e = table.get(j); e != null; e = e.getNext()) {
                        V v = segment.getLiveValue(e);
                        if (v != null && valueEquivalence.equivalent(value, v)) {
                            return true;
                        }
                    }
                }
                sum += segment.modCount;
            }
            if (sum == last) {
                break;
            }
            last = sum;
        }
        return false;
    }

    @Override
    public V put(K key, V value) {
        checkNotNull(key);
        checkNotNull(value);
        int hash = hash(key);
        return segmentFor(hash).put(key, hash, value, false);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        checkNotNull(key);
        checkNotNull(value);
        int hash = hash(key);
        return segmentFor(hash).put(key, hash, value, true);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    @Override
    public V remove(@Nullable Object key) {
        if (key == null) {
            return null;
        }
        int hash = hash(key);
        return segmentFor(hash).remove(key, hash);
    }

    @Override
    public boolean remove(@Nullable Object key, @Nullable Object value) {
        if (key == null || value == null) {
            return false;
        }
        int hash = hash(key);
        return segmentFor(hash).remove(key, hash, value);
    }

    @Override
    public boolean replace(K key, @Nullable V oldValue, V newValue) {
        checkNotNull(key);
        checkNotNull(newValue);
        if (oldValue == null) {
            return false;
        }
        int hash = hash(key);
        return segmentFor(hash).replace(key, hash, oldValue, newValue);
    }

    @Override
    public V replace(K key, V value) {
        checkNotNull(key);
        checkNotNull(value);
        int hash = hash(key);
        return segmentFor(hash).replace(key, hash, value);
    }

    @Override
    public void clear() {
        for (Segment<K, V> segment : segments) {
            segment.clear();
        }
    }

    transient Set<K> keySet;

    @Override
    public Set<K> keySet() {
        Set<K> ks = keySet;
        return (ks != null) ? ks : (keySet = new KeySet());
    }

    transient Collection<V> values;

    @Override
    public Collection<V> values() {
        Collection<V> vs = values;
        return (vs != null) ? vs : (values = new Values());
    }

    transient Set<Entry<K, V>> entrySet;

    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> es = entrySet;
        return (es != null) ? es : (entrySet = new EntrySet());
    }

    // Iterator Support

    abstract class HashIterator<E> implements Iterator<E> {

        int nextSegmentIndex;
        int nextTableIndex;
        Segment<K, V> currentSegment;
        AtomicReferenceArray<ReferenceEntry<K, V>> currentTable;
        ReferenceEntry<K, V> nextEntry;
        WriteThroughEntry nextExternal;
        WriteThroughEntry lastReturned;

        HashIterator() {
            nextSegmentIndex = segments.length - 1;
            nextTableIndex = -1;
            advance();
        }

        @Override
        public abstract E next();

        final void advance() {
            nextExternal = null;

            if (nextInChain()) {
                return;
            }

            if (nextInTable()) {
                return;
            }

            while (nextSegmentIndex >= 0) {
                currentSegment = segments[nextSegmentIndex--];
                if (currentSegment.count != 0) {
                    currentTable = currentSegment.table;
                    nextTableIndex = currentTable.length() - 1;
                    if (nextInTable()) {
                        return;
                    }
                }
            }
        }


        boolean nextInChain() {
            if (nextEntry != null) {
                for (nextEntry = nextEntry.getNext(); nextEntry != null; nextEntry = nextEntry.getNext()) {
                    if (advanceTo(nextEntry)) {
                        return true;
                    }
                }
            }
            return false;
        }


        boolean nextInTable() {
            while (nextTableIndex >= 0) {
                if ((nextEntry = currentTable.get(nextTableIndex--)) != null) {
                    if (advanceTo(nextEntry) || nextInChain()) {
                        return true;
                    }
                }
            }
            return false;
        }


        boolean advanceTo(ReferenceEntry<K, V> entry) {
            try {
                K key = entry.getKey();
                V value = getLiveValue(entry);
                if (value != null) {
                    nextExternal = new WriteThroughEntry(key, value);
                    return true;
                } else {
                    // Skip stale entry.
                    return false;
                }
            } finally {
                currentSegment.postReadCleanup();
            }
        }

        @Override
        public boolean hasNext() {
            return nextExternal != null;
        }

        WriteThroughEntry nextEntry() {
            if (nextExternal == null) {
                throw new NoSuchElementException();
            }
            lastReturned = nextExternal;
            advance();
            return lastReturned;
        }

        @Override
        public void remove() {
            checkRemove(lastReturned != null);
            MapMakerInternalMap.this.remove(lastReturned.getKey());
            lastReturned = null;
        }
    }

    final class KeyIterator extends HashIterator<K> {

        @Override
        public K next() {
            return nextEntry().getKey();
        }
    }

    final class ValueIterator extends HashIterator<V> {

        @Override
        public V next() {
            return nextEntry().getValue();
        }
    }


    final class WriteThroughEntry extends AbstractMapEntry<K, V> {
        final K key; // non-null
        V value; // non-null

        WriteThroughEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public boolean equals(@Nullable Object object) {
            // Cannot use key and value equivalence
            if (object instanceof Entry) {
                Entry<?, ?> that = (Entry<?, ?>) object;
                return key.equals(that.getKey()) && value.equals(that.getValue());
            }
            return false;
        }

        @Override
        public int hashCode() {
            // Cannot use key and value equivalence
            return key.hashCode() ^ value.hashCode();
        }

        @Override
        public V setValue(V newValue) {
            V oldValue = put(key, newValue);
            value = newValue; // only if put succeeds
            return oldValue;
        }
    }

    final class EntryIterator extends HashIterator<Entry<K, V>> {

        @Override
        public Entry<K, V> next() {
            return nextEntry();
        }
    }

    @WeakOuter
    final class KeySet extends AbstractSet<K> {

        @Override
        public Iterator<K> iterator() {
            return new KeyIterator();
        }

        @Override
        public int size() {
            return MapMakerInternalMap.this.size();
        }

        @Override
        public boolean isEmpty() {
            return MapMakerInternalMap.this.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return MapMakerInternalMap.this.containsKey(o);
        }

        @Override
        public boolean remove(Object o) {
            return MapMakerInternalMap.this.remove(o) != null;
        }

        @Override
        public void clear() {
            MapMakerInternalMap.this.clear();
        }
    }

    @WeakOuter
    final class Values extends AbstractCollection<V> {

        @Override
        public Iterator<V> iterator() {
            return new ValueIterator();
        }

        @Override
        public int size() {
            return MapMakerInternalMap.this.size();
        }

        @Override
        public boolean isEmpty() {
            return MapMakerInternalMap.this.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return MapMakerInternalMap.this.containsValue(o);
        }

        @Override
        public void clear() {
            MapMakerInternalMap.this.clear();
        }
    }

    @WeakOuter
    final class EntrySet extends AbstractSet<Entry<K, V>> {

        @Override
        public Iterator<Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Entry)) {
                return false;
            }
            Entry<?, ?> e = (Entry<?, ?>) o;
            Object key = e.getKey();
            if (key == null) {
                return false;
            }
            V v = MapMakerInternalMap.this.get(key);

            return v != null && valueEquivalence.equivalent(e.getValue(), v);
        }

        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Entry)) {
                return false;
            }
            Entry<?, ?> e = (Entry<?, ?>) o;
            Object key = e.getKey();
            return key != null && MapMakerInternalMap.this.remove(key, e.getValue());
        }

        @Override
        public int size() {
            return MapMakerInternalMap.this.size();
        }

        @Override
        public boolean isEmpty() {
            return MapMakerInternalMap.this.isEmpty();
        }

        @Override
        public void clear() {
            MapMakerInternalMap.this.clear();
        }
    }

    // Serialization Support

    private static final long serialVersionUID = 5;

    Object writeReplace() {
        return new SerializationProxy<>(keyStrength, valueStrength, keyEquivalence,
                valueEquivalence, expireAfterWriteNanos, expireAfterAccessNanos, maximumSize,
                concurrencyLevel, removalListener, this);
    }


    abstract static class AbstractSerializationProxy<K, V>
            extends ForwardingConcurrentMap<K, V> implements Serializable {
        private static final long serialVersionUID = 3;

        final Strength keyStrength;
        final Strength valueStrength;
        final Equivalence<Object> keyEquivalence;
        final Equivalence<Object> valueEquivalence;
        final long expireAfterWriteNanos;
        final long expireAfterAccessNanos;
        final int maximumSize;
        final int concurrencyLevel;
        final RemovalListener<? super K, ? super V> removalListener;

        transient ConcurrentMap<K, V> delegate;

        AbstractSerializationProxy(Strength keyStrength, Strength valueStrength,
                                   Equivalence<Object> keyEquivalence, Equivalence<Object> valueEquivalence,
                                   long expireAfterWriteNanos, long expireAfterAccessNanos, int maximumSize,
                                   int concurrencyLevel, RemovalListener<? super K, ? super V> removalListener,
                                   ConcurrentMap<K, V> delegate) {
            this.keyStrength = keyStrength;
            this.valueStrength = valueStrength;
            this.keyEquivalence = keyEquivalence;
            this.valueEquivalence = valueEquivalence;
            this.expireAfterWriteNanos = expireAfterWriteNanos;
            this.expireAfterAccessNanos = expireAfterAccessNanos;
            this.maximumSize = maximumSize;
            this.concurrencyLevel = concurrencyLevel;
            this.removalListener = removalListener;
            this.delegate = delegate;
        }

        @Override
        protected ConcurrentMap<K, V> delegate() {
            return delegate;
        }

        void writeMapTo(ObjectOutputStream out) throws IOException {
            out.writeInt(delegate.size());
            for (Entry<K, V> entry : delegate.entrySet()) {
                out.writeObject(entry.getKey());
                out.writeObject(entry.getValue());
            }
            out.writeObject(null); // terminate entries
        }

        @SuppressWarnings("deprecation")
            // serialization of deprecated feature
        MapMaker readMapMaker(ObjectInputStream in) throws IOException {
            int size = in.readInt();
            MapMaker mapMaker = new MapMaker()
                    .initialCapacity(size)
                    .setKeyStrength(keyStrength)
                    .setValueStrength(valueStrength)
                    .keyEquivalence(keyEquivalence)
                    .concurrencyLevel(concurrencyLevel);
            mapMaker.removalListener(removalListener);
            if (expireAfterWriteNanos > 0) {
                mapMaker.expireAfterWrite(expireAfterWriteNanos, TimeUnit.NANOSECONDS);
            }
            if (expireAfterAccessNanos > 0) {
                mapMaker.expireAfterAccess(expireAfterAccessNanos, TimeUnit.NANOSECONDS);
            }
            if (maximumSize != MapMaker.UNSET_INT) {
                mapMaker.maximumSize(maximumSize);
            }
            return mapMaker;
        }

        @SuppressWarnings("unchecked")
        void readEntries(ObjectInputStream in) throws IOException, ClassNotFoundException {
            while (true) {
                K key = (K) in.readObject();
                if (key == null) {
                    break; // terminator
                }
                V value = (V) in.readObject();
                delegate.put(key, value);
            }
        }
    }


    private static final class SerializationProxy<K, V> extends AbstractSerializationProxy<K, V> {
        private static final long serialVersionUID = 3;

        SerializationProxy(Strength keyStrength, Strength valueStrength,
                           Equivalence<Object> keyEquivalence, Equivalence<Object> valueEquivalence,
                           long expireAfterWriteNanos, long expireAfterAccessNanos, int maximumSize,
                           int concurrencyLevel, RemovalListener<? super K, ? super V> removalListener,
                           ConcurrentMap<K, V> delegate) {
            super(keyStrength, valueStrength, keyEquivalence, valueEquivalence, expireAfterWriteNanos,
                    expireAfterAccessNanos, maximumSize, concurrencyLevel, removalListener, delegate);
        }

        private void writeObject(ObjectOutputStream out) throws IOException {
            out.defaultWriteObject();
            writeMapTo(out);
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            MapMaker mapMaker = readMapMaker(in);
            delegate = mapMaker.makeMap();
            readEntries(in);
        }

        private Object readResolve() {
            return delegate;
        }
    }
}
