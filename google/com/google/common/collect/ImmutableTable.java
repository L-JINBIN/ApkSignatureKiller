

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.MoreObjects;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;


@GwtCompatible
// TODO(gak): make serializable
public abstract class ImmutableTable<R, C, V> extends AbstractTable<R, C, V> {
    private static final ImmutableTable<Object, Object, Object> EMPTY =
            new SparseImmutableTable<>(
                    ImmutableList.<Cell<Object, Object, Object>>of(),
                    ImmutableSet.of(),
                    ImmutableSet.of());


    @SuppressWarnings("unchecked")
    public static <R, C, V> ImmutableTable<R, C, V> of() {
        return (ImmutableTable<R, C, V>) EMPTY;
    }


    static <R, C, V> Cell<R, C, V> cellOf(R rowKey, C columnKey, V value) {
        return Tables.immutableCell(checkNotNull(rowKey), checkNotNull(columnKey), checkNotNull(value));
    }


    public static final class Builder<R, C, V> {
        private final List<Cell<R, C, V>> cells = Lists.newArrayList();
        private Comparator<? super R> rowComparator;
        private Comparator<? super C> columnComparator;


        public Builder() {
        }


        public Builder<R, C, V> orderRowsBy(Comparator<? super R> rowComparator) {
            this.rowComparator = checkNotNull(rowComparator);
            return this;
        }


        public Builder<R, C, V> orderColumnsBy(Comparator<? super C> columnComparator) {
            this.columnComparator = checkNotNull(columnComparator);
            return this;
        }


        public Builder<R, C, V> put(R rowKey, C columnKey, V value) {
            cells.add(cellOf(rowKey, columnKey, value));
            return this;
        }


        public Builder<R, C, V> put(Cell<? extends R, ? extends C, ? extends V> cell) {
            if (cell instanceof Tables.ImmutableCell) {
                checkNotNull(cell.getRowKey());
                checkNotNull(cell.getColumnKey());
                checkNotNull(cell.getValue());
                @SuppressWarnings("unchecked") // all supported methods are covariant
                        Cell<R, C, V> immutableCell = (Cell<R, C, V>) cell;
                cells.add(immutableCell);
            } else {
                put(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
            }
            return this;
        }


        public Builder<R, C, V> putAll(Table<? extends R, ? extends C, ? extends V> table) {
            for (Cell<? extends R, ? extends C, ? extends V> cell : table.cellSet()) {
                put(cell);
            }
            return this;
        }


        public ImmutableTable<R, C, V> build() {
            int size = cells.size();
            switch (size) {
                case 0:
                    return of();
                case 1:
                    return new SingletonImmutableTable<>(Iterables.getOnlyElement(cells));
                default:
                    return RegularImmutableTable.forCells(cells, rowComparator, columnComparator);
            }
        }
    }

    @Override
    public ImmutableSet<Cell<R, C, V>> cellSet() {
        return (ImmutableSet<Cell<R, C, V>>) super.cellSet();
    }

    @Override
    abstract ImmutableSet<Cell<R, C, V>> createCellSet();

    @Override
    final UnmodifiableIterator<Cell<R, C, V>> cellIterator() {
        throw new AssertionError("should never be called");
    }

    @Override
    public ImmutableCollection<V> values() {
        return (ImmutableCollection<V>) super.values();
    }

    @Override
    abstract ImmutableCollection<V> createValues();

    @Override
    final Iterator<V> valuesIterator() {
        throw new AssertionError("should never be called");
    }


    @Override
    public ImmutableMap<R, V> column(C columnKey) {
        checkNotNull(columnKey);
        return MoreObjects.firstNonNull(
                (ImmutableMap<R, V>) columnMap().get(columnKey),
                ImmutableMap.<R, V>of());
    }

    @Override
    public ImmutableSet<C> columnKeySet() {
        return columnMap().keySet();
    }


    @Override
    public abstract ImmutableMap<C, Map<R, V>> columnMap();


    @Override
    public ImmutableMap<C, V> row(R rowKey) {
        checkNotNull(rowKey);
        return MoreObjects.firstNonNull(
                (ImmutableMap<C, V>) rowMap().get(rowKey),
                ImmutableMap.<C, V>of());
    }

    @Override
    public ImmutableSet<R> rowKeySet() {
        return rowMap().keySet();
    }


    @Override
    public abstract ImmutableMap<R, Map<C, V>> rowMap();

    @Override
    public boolean contains(@Nullable Object rowKey, @Nullable Object columnKey) {
        return get(rowKey, columnKey) != null;
    }

    @Override
    public boolean containsValue(@Nullable Object value) {
        return values().contains(value);
    }


    @Deprecated
    @Override
    public final void clear() {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    @Override
    public final V put(R rowKey, C columnKey, V value) {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    @Override
    public final void putAll(Table<? extends R, ? extends C, ? extends V> table) {
        throw new UnsupportedOperationException();
    }


    @Deprecated
    @Override
    public final V remove(Object rowKey, Object columnKey) {
        throw new UnsupportedOperationException();
    }
}
