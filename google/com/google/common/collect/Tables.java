

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.Table.Cell;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;


@GwtCompatible
public final class Tables {
    private Tables() {
    }


    public static <R, C, V> Cell<R, C, V> immutableCell(
            @Nullable R rowKey, @Nullable C columnKey, @Nullable V value) {
        return new ImmutableCell<>(rowKey, columnKey, value);
    }

    static final class ImmutableCell<R, C, V>
            extends AbstractCell<R, C, V> implements Serializable {
        private final R rowKey;
        private final C columnKey;
        private final V value;

        ImmutableCell(
                @Nullable R rowKey, @Nullable C columnKey, @Nullable V value) {
            this.rowKey = rowKey;
            this.columnKey = columnKey;
            this.value = value;
        }

        @Override
        public R getRowKey() {
            return rowKey;
        }

        @Override
        public C getColumnKey() {
            return columnKey;
        }

        @Override
        public V getValue() {
            return value;
        }

        private static final long serialVersionUID = 0;
    }

    abstract static class AbstractCell<R, C, V> implements Cell<R, C, V> {
        // needed for serialization
        AbstractCell() {
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof Cell) {
                Cell<?, ?, ?> other = (Cell<?, ?, ?>) obj;
                return Objects.equal(getRowKey(), other.getRowKey())
                        && Objects.equal(getColumnKey(), other.getColumnKey())
                        && Objects.equal(getValue(), other.getValue());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(getRowKey(), getColumnKey(), getValue());
        }

        @Override
        public String toString() {
            return "(" + getRowKey() + "," + getColumnKey() + ")=" + getValue();
        }
    }


    public static <R, C, V> Table<C, R, V> transpose(Table<R, C, V> table) {
        return (table instanceof TransposeTable)
                ? ((TransposeTable<R, C, V>) table).original
                : new TransposeTable<>(table);
    }

    private static class TransposeTable<C, R, V> extends AbstractTable<C, R, V> {
        final Table<R, C, V> original;

        TransposeTable(Table<R, C, V> original) {
            this.original = checkNotNull(original);
        }

        @Override
        public void clear() {
            original.clear();
        }

        @Override
        public Map<C, V> column(R columnKey) {
            return original.row(columnKey);
        }

        @Override
        public Set<R> columnKeySet() {
            return original.rowKeySet();
        }

        @Override
        public Map<R, Map<C, V>> columnMap() {
            return original.rowMap();
        }

        @Override
        public boolean contains(
                @Nullable Object rowKey, @Nullable Object columnKey) {
            return original.contains(columnKey, rowKey);
        }

        @Override
        public boolean containsColumn(@Nullable Object columnKey) {
            return original.containsRow(columnKey);
        }

        @Override
        public boolean containsRow(@Nullable Object rowKey) {
            return original.containsColumn(rowKey);
        }

        @Override
        public boolean containsValue(@Nullable Object value) {
            return original.containsValue(value);
        }

        @Override
        public V get(@Nullable Object rowKey, @Nullable Object columnKey) {
            return original.get(columnKey, rowKey);
        }

        @Override
        public V put(C rowKey, R columnKey, V value) {
            return original.put(columnKey, rowKey, value);
        }

        @Override
        public void putAll(Table<? extends C, ? extends R, ? extends V> table) {
            original.putAll(transpose(table));
        }

        @Override
        public V remove(@Nullable Object rowKey, @Nullable Object columnKey) {
            return original.remove(columnKey, rowKey);
        }

        @Override
        public Map<R, V> row(C rowKey) {
            return original.column(rowKey);
        }

        @Override
        public Set<C> rowKeySet() {
            return original.columnKeySet();
        }

        @Override
        public Map<C, Map<R, V>> rowMap() {
            return original.columnMap();
        }

        @Override
        public int size() {
            return original.size();
        }

        @Override
        public Collection<V> values() {
            return original.values();
        }

        // Will cast TRANSPOSE_CELL to a type that always succeeds
        private static final Function<Cell<?, ?, ?>, Cell<?, ?, ?>> TRANSPOSE_CELL =
                new Function<Cell<?, ?, ?>, Cell<?, ?, ?>>() {
                    @Override
                    public Cell<?, ?, ?> apply(Cell<?, ?, ?> cell) {
                        return immutableCell(
                                cell.getColumnKey(), cell.getRowKey(), cell.getValue());
                    }
                };

        @SuppressWarnings("unchecked")
        @Override
        Iterator<Cell<C, R, V>> cellIterator() {
            return Iterators.transform(original.cellSet().iterator(), (Function) TRANSPOSE_CELL);
        }
    }


    private static class UnmodifiableTable<R, C, V>
            extends ForwardingTable<R, C, V> implements Serializable {
        final Table<? extends R, ? extends C, ? extends V> delegate;

        UnmodifiableTable(Table<? extends R, ? extends C, ? extends V> delegate) {
            this.delegate = checkNotNull(delegate);
        }

        @SuppressWarnings("unchecked") // safe, covariant cast
        @Override
        protected Table<R, C, V> delegate() {
            return (Table<R, C, V>) delegate;
        }

        @Override
        public Set<Cell<R, C, V>> cellSet() {
            return Collections.unmodifiableSet(super.cellSet());
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<R, V> column(@Nullable C columnKey) {
            return Collections.unmodifiableMap(super.column(columnKey));
        }

        @Override
        public Set<C> columnKeySet() {
            return Collections.unmodifiableSet(super.columnKeySet());
        }

        @Override
        public Map<C, Map<R, V>> columnMap() {
            Function<Map<R, V>, Map<R, V>> wrapper = unmodifiableWrapper();
            return Collections.unmodifiableMap(Maps.transformValues(super.columnMap(), wrapper));
        }

        @Override
        public V put(@Nullable R rowKey, @Nullable C columnKey, @Nullable V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putAll(Table<? extends R, ? extends C, ? extends V> table) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V remove(@Nullable Object rowKey, @Nullable Object columnKey) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<C, V> row(@Nullable R rowKey) {
            return Collections.unmodifiableMap(super.row(rowKey));
        }

        @Override
        public Set<R> rowKeySet() {
            return Collections.unmodifiableSet(super.rowKeySet());
        }

        @Override
        public Map<R, Map<C, V>> rowMap() {
            Function<Map<C, V>, Map<C, V>> wrapper = unmodifiableWrapper();
            return Collections.unmodifiableMap(Maps.transformValues(super.rowMap(), wrapper));
        }

        @Override
        public Collection<V> values() {
            return Collections.unmodifiableCollection(super.values());
        }

        private static final long serialVersionUID = 0;
    }


    @SuppressWarnings("unchecked")
    private static <K, V> Function<Map<K, V>, Map<K, V>> unmodifiableWrapper() {
        return (Function) UNMODIFIABLE_WRAPPER;
    }

    private static final Function<? extends Map<?, ?>, ? extends Map<?, ?>> UNMODIFIABLE_WRAPPER =
            new Function<Map<Object, Object>, Map<Object, Object>>() {
                @Override
                public Map<Object, Object> apply(Map<Object, Object> input) {
                    return Collections.unmodifiableMap(input);
                }
            };

    static boolean equalsImpl(Table<?, ?, ?> table, @Nullable Object obj) {
        if (obj == table) {
            return true;
        } else if (obj instanceof Table) {
            Table<?, ?, ?> that = (Table<?, ?, ?>) obj;
            return table.cellSet().equals(that.cellSet());
        } else {
            return false;
        }
    }
}
