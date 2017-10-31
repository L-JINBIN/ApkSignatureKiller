

package com.google.common.cache;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;

import java.util.Map.Entry;

import static com.google.common.base.Preconditions.checkNotNull;


@GwtCompatible
public final class RemovalNotification<K, V> implements Entry<K, V> {
    @Nullable
    private final K key;
    @Nullable
    private final V value;
    private final RemovalCause cause;


    public static <K, V> RemovalNotification<K, V> create(
            @Nullable K key, @Nullable V value, RemovalCause cause) {
        return new RemovalNotification(key, value, cause);
    }

    private RemovalNotification(@Nullable K key, @Nullable V value, RemovalCause cause) {
        this.key = key;
        this.value = value;
        this.cause = checkNotNull(cause);
    }


    public RemovalCause getCause() {
        return cause;
    }


    public boolean wasEvicted() {
        return cause.wasEvicted();
    }

    @Nullable
    @Override
    public K getKey() {
        return key;
    }

    @Nullable
    @Override
    public V getValue() {
        return value;
    }

    @Override
    public final V setValue(V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(@Nullable Object object) {
        if (object instanceof Entry) {
            Entry<?, ?> that = (Entry<?, ?>) object;
            return Objects.equal(this.getKey(), that.getKey())
                    && Objects.equal(this.getValue(), that.getValue());
        }
        return false;
    }

    @Override
    public int hashCode() {
        K k = getKey();
        V v = getValue();
        return ((k == null) ? 0 : k.hashCode()) ^ ((v == null) ? 0 : v.hashCode());
    }


    @Override
    public String toString() {
        return getKey() + "=" + getValue();
    }

    private static final long serialVersionUID = 0;
}
