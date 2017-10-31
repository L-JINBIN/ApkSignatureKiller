

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;

import java.util.Collection;
import java.util.Map;
import java.util.Set;


@GwtCompatible
public interface Table<R, C, V> {
    // TODO(jlevy): Consider adding methods similar to ConcurrentMap methods.

    // Accessors


    boolean contains(@Nullable Object rowKey, @Nullable Object columnKey);


    boolean containsRow(@Nullable Object rowKey);


    boolean containsColumn(@Nullable Object columnKey);


    boolean containsValue(@Nullable Object value);


    V get(@Nullable Object rowKey, @Nullable Object columnKey);


    boolean isEmpty();


    int size();


    @Override
    boolean equals(@Nullable Object obj);


    @Override
    int hashCode();

    // Mutators


    void clear();


    @Nullable
    V put(R rowKey, C columnKey, V value);


    void putAll(Table<? extends R, ? extends C, ? extends V> table);


    @Nullable
    V remove(@Nullable Object rowKey, @Nullable Object columnKey);

    // Views


    Map<C, V> row(R rowKey);


    Map<R, V> column(C columnKey);


    Set<Cell<R, C, V>> cellSet();


    Set<R> rowKeySet();


    Set<C> columnKeySet();


    Collection<V> values();


    Map<R, Map<C, V>> rowMap();


    Map<C, Map<R, V>> columnMap();


    interface Cell<R, C, V> {

        @Nullable
        R getRowKey();


        @Nullable
        C getColumnKey();


        @Nullable
        V getValue();


        @Override
        boolean equals(@Nullable Object obj);


        @Override
        int hashCode();
    }
}
