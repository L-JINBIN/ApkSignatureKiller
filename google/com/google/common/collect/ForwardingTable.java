

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;

import java.util.Collection;
import java.util.Map;
import java.util.Set;


@GwtCompatible
public abstract class ForwardingTable<R, C, V> extends ForwardingObject implements Table<R, C, V> {

    protected ForwardingTable() {
    }

    @Override
    protected abstract Table<R, C, V> delegate();

    @Override
    public Set<Cell<R, C, V>> cellSet() {
        return delegate().cellSet();
    }

    @Override
    public void clear() {
        delegate().clear();
    }

    @Override
    public Map<R, V> column(C columnKey) {
        return delegate().column(columnKey);
    }

    @Override
    public Set<C> columnKeySet() {
        return delegate().columnKeySet();
    }

    @Override
    public Map<C, Map<R, V>> columnMap() {
        return delegate().columnMap();
    }

    @Override
    public boolean contains(Object rowKey, Object columnKey) {
        return delegate().contains(rowKey, columnKey);
    }

    @Override
    public boolean containsColumn(Object columnKey) {
        return delegate().containsColumn(columnKey);
    }

    @Override
    public boolean containsRow(Object rowKey) {
        return delegate().containsRow(rowKey);
    }

    @Override
    public boolean containsValue(Object value) {
        return delegate().containsValue(value);
    }

    @Override
    public V get(Object rowKey, Object columnKey) {
        return delegate().get(rowKey, columnKey);
    }

    @Override
    public boolean isEmpty() {
        return delegate().isEmpty();
    }

    @Override
    public V put(R rowKey, C columnKey, V value) {
        return delegate().put(rowKey, columnKey, value);
    }

    @Override
    public void putAll(Table<? extends R, ? extends C, ? extends V> table) {
        delegate().putAll(table);
    }

    @Override
    public V remove(Object rowKey, Object columnKey) {
        return delegate().remove(rowKey, columnKey);
    }

    @Override
    public Map<C, V> row(R rowKey) {
        return delegate().row(rowKey);
    }

    @Override
    public Set<R> rowKeySet() {
        return delegate().rowKeySet();
    }

    @Override
    public Map<R, Map<C, V>> rowMap() {
        return delegate().rowMap();
    }

    @Override
    public int size() {
        return delegate().size();
    }

    @Override
    public Collection<V> values() {
        return delegate().values();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj == this) || delegate().equals(obj);
    }

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }
}
