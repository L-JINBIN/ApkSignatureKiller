

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.primitives.Ints;

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.CollectPreconditions.checkNonnegative;
import static com.google.common.collect.CollectPreconditions.checkRemove;


@GwtCompatible(emulated = true)
abstract class AbstractMapBasedMultiset<E> extends AbstractMultiset<E> implements Serializable {

    private transient Map<E, Count> backingMap;


    private transient long size;


    protected AbstractMapBasedMultiset(Map<E, Count> backingMap) {
        this.backingMap = checkNotNull(backingMap);
        this.size = super.size();
    }


    void setBackingMap(Map<E, Count> backingMap) {
        this.backingMap = backingMap;
    }

    // Required Implementations


    @Override
    public Set<Multiset.Entry<E>> entrySet() {
        return super.entrySet();
    }

    @Override
    Iterator<Entry<E>> entryIterator() {
        final Iterator<Map.Entry<E, Count>> backingEntries = backingMap.entrySet().iterator();
        return new Iterator<Multiset.Entry<E>>() {
            Map.Entry<E, Count> toRemove;

            @Override
            public boolean hasNext() {
                return backingEntries.hasNext();
            }

            @Override
            public Multiset.Entry<E> next() {
                final Map.Entry<E, Count> mapEntry = backingEntries.next();
                toRemove = mapEntry;
                return new Multisets.AbstractEntry<E>() {
                    @Override
                    public E getElement() {
                        return mapEntry.getKey();
                    }

                    @Override
                    public int getCount() {
                        Count count = mapEntry.getValue();
                        if (count == null || count.get() == 0) {
                            Count frequency = backingMap.get(getElement());
                            if (frequency != null) {
                                return frequency.get();
                            }
                        }
                        return (count == null) ? 0 : count.get();
                    }
                };
            }

            @Override
            public void remove() {
                checkRemove(toRemove != null);
                size -= toRemove.getValue().getAndSet(0);
                backingEntries.remove();
                toRemove = null;
            }
        };
    }

    @Override
    public void clear() {
        for (Count frequency : backingMap.values()) {
            frequency.set(0);
        }
        backingMap.clear();
        size = 0L;
    }

    @Override
    int distinctElements() {
        return backingMap.size();
    }

    // Optimizations - Query Operations

    @Override
    public int size() {
        return Ints.saturatedCast(size);
    }

    @Override
    public Iterator<E> iterator() {
        return new MapBasedMultisetIterator();
    }


    private class MapBasedMultisetIterator implements Iterator<E> {
        final Iterator<Map.Entry<E, Count>> entryIterator;
        Map.Entry<E, Count> currentEntry;
        int occurrencesLeft;
        boolean canRemove;

        MapBasedMultisetIterator() {
            this.entryIterator = backingMap.entrySet().iterator();
        }

        @Override
        public boolean hasNext() {
            return occurrencesLeft > 0 || entryIterator.hasNext();
        }

        @Override
        public E next() {
            if (occurrencesLeft == 0) {
                currentEntry = entryIterator.next();
                occurrencesLeft = currentEntry.getValue().get();
            }
            occurrencesLeft--;
            canRemove = true;
            return currentEntry.getKey();
        }

        @Override
        public void remove() {
            checkRemove(canRemove);
            int frequency = currentEntry.getValue().get();
            if (frequency <= 0) {
                throw new ConcurrentModificationException();
            }
            if (currentEntry.getValue().addAndGet(-1) == 0) {
                entryIterator.remove();
            }
            size--;
            canRemove = false;
        }
    }

    @Override
    public int count(@Nullable Object element) {
        Count frequency = Maps.safeGet(backingMap, element);
        return (frequency == null) ? 0 : frequency.get();
    }

    // Optional Operations - Modification Operations


    @Override
    public int add(@Nullable E element, int occurrences) {
        if (occurrences == 0) {
            return count(element);
        }
        checkArgument(occurrences > 0, "occurrences cannot be negative: %s", occurrences);
        Count frequency = backingMap.get(element);
        int oldCount;
        if (frequency == null) {
            oldCount = 0;
            backingMap.put(element, new Count(occurrences));
        } else {
            oldCount = frequency.get();
            long newCount = (long) oldCount + (long) occurrences;
            checkArgument(newCount <= Integer.MAX_VALUE, "too many occurrences: %s", newCount);
            frequency.getAndAdd(occurrences);
        }
        size += occurrences;
        return oldCount;
    }

    @Override
    public int remove(@Nullable Object element, int occurrences) {
        if (occurrences == 0) {
            return count(element);
        }
        checkArgument(occurrences > 0, "occurrences cannot be negative: %s", occurrences);
        Count frequency = backingMap.get(element);
        if (frequency == null) {
            return 0;
        }

        int oldCount = frequency.get();

        int numberRemoved;
        if (oldCount > occurrences) {
            numberRemoved = occurrences;
        } else {
            numberRemoved = oldCount;
            backingMap.remove(element);
        }

        frequency.addAndGet(-numberRemoved);
        size -= numberRemoved;
        return oldCount;
    }

    // Roughly a 33% performance improvement over AbstractMultiset.setCount().
    @Override
    public int setCount(@Nullable E element, int count) {
        checkNonnegative(count, "count");

        Count existingCounter;
        int oldCount;
        if (count == 0) {
            existingCounter = backingMap.remove(element);
            oldCount = getAndSet(existingCounter, count);
        } else {
            existingCounter = backingMap.get(element);
            oldCount = getAndSet(existingCounter, count);

            if (existingCounter == null) {
                backingMap.put(element, new Count(count));
            }
        }

        size += (count - oldCount);
        return oldCount;
    }

    private static int getAndSet(Count i, int count) {
        if (i == null) {
            return 0;
        }

        return i.getAndSet(count);
    }

    // Don't allow default serialization.
    @GwtIncompatible("java.io.ObjectStreamException")
    @SuppressWarnings("unused") // actually used during deserialization
    private void readObjectNoData() throws ObjectStreamException {
        throw new InvalidObjectException("Stream data required");
    }

    @GwtIncompatible("not needed in emulated source.")
    private static final long serialVersionUID = -2250766705698539974L;
}
