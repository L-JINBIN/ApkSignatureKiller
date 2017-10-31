

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;


@GwtCompatible
class SingletonImmutableTable<R, C, V> extends ImmutableTable<R, C, V> {
    final R singleRowKey;
    final C singleColumnKey;
    final V singleValue;

    SingletonImmutableTable(R rowKey, C columnKey, V value) {
        this.singleRowKey = checkNotNull(rowKey);
        this.singleColumnKey = checkNotNull(columnKey);
        this.singleValue = checkNotNull(value);
    }

    SingletonImmutableTable(Cell<R, C, V> cell) {
        this(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
    }

    @Override
    public ImmutableMap<R, V> column(C columnKey) {
        checkNotNull(columnKey);
        return containsColumn(columnKey)
                ? ImmutableMap.of(singleRowKey, singleValue)
                : ImmutableMap.<R, V>of();
    }

    @Override
    public ImmutableMap<C, Map<R, V>> columnMap() {
        return ImmutableMap.of(singleColumnKey,
                (Map<R, V>) ImmutableMap.of(singleRowKey, singleValue));
    }

    @Override
    public ImmutableMap<R, Map<C, V>> rowMap() {
        return ImmutableMap.of(singleRowKey,
                (Map<C, V>) ImmutableMap.of(singleColumnKey, singleValue));
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    ImmutableSet<Cell<R, C, V>> createCellSet() {
        return ImmutableSet.of(
                cellOf(singleRowKey, singleColumnKey, singleValue));
    }

    @Override
    ImmutableCollection<V> createValues() {
        return ImmutableSet.of(singleValue);
    }
}
