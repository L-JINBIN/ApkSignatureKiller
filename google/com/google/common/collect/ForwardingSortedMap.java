

package com.google.common.collect;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.SortedMap;

import static com.google.common.base.Preconditions.checkArgument;


@GwtCompatible
public abstract class ForwardingSortedMap<K, V> extends ForwardingMap<K, V>
        implements SortedMap<K, V> {
    // TODO(lowasser): identify places where thread safety is actually lost


    protected ForwardingSortedMap() {
    }

    @Override
    protected abstract SortedMap<K, V> delegate();

    @Override
    public Comparator<? super K> comparator() {
        return delegate().comparator();
    }

    @Override
    public K firstKey() {
        return delegate().firstKey();
    }

    @NonNull
    @Override
    public SortedMap<K, V> headMap(K toKey) {
        return delegate().headMap(toKey);
    }

    @Override
    public K lastKey() {
        return delegate().lastKey();
    }

    @NonNull
    @Override
    public SortedMap<K, V> subMap(K fromKey, K toKey) {
        return delegate().subMap(fromKey, toKey);
    }

    @NonNull
    @Override
    public SortedMap<K, V> tailMap(K fromKey) {
        return delegate().tailMap(fromKey);
    }


    // unsafe, but worst case is a CCE is thrown, which callers will be expecting
    @SuppressWarnings("unchecked")
    private int unsafeCompare(Object k1, Object k2) {
        Comparator<? super K> comparator = comparator();
        if (comparator == null) {
            return ((Comparable<Object>) k1).compareTo(k2);
        } else {
            return ((Comparator<Object>) comparator).compare(k1, k2);
        }
    }


    @Override
    @Beta
    protected boolean standardContainsKey(@Nullable Object key) {
        try {
            // any CCE will be caught
            @SuppressWarnings("unchecked")
            SortedMap<Object, V> self = (SortedMap<Object, V>) this;
            Object ceilingKey = self.tailMap(key).firstKey();
            return unsafeCompare(ceilingKey, key) == 0;
        } catch (ClassCastException e) {
            return false;
        } catch (NoSuchElementException e) {
            return false;
        } catch (NullPointerException e) {
            return false;
        }
    }


    @Beta
    protected SortedMap<K, V> standardSubMap(K fromKey, K toKey) {
        checkArgument(unsafeCompare(fromKey, toKey) <= 0, "fromKey must be <= toKey");
        return tailMap(fromKey).headMap(toKey);
    }
}
