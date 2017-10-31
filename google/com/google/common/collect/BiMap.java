

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;

import java.util.Map;
import java.util.Set;


@GwtCompatible
public interface BiMap<K, V> extends Map<K, V> {
    // Modification Operations


    @Override
    @Nullable
    V put(@Nullable K key, @Nullable V value);


    @Nullable
    V forcePut(@Nullable K key, @Nullable V value);

    // Bulk Operations


    @Override
    void putAll(Map<? extends K, ? extends V> map);

    // Views


    @Override
    Set<V> values();


    BiMap<V, K> inverse();
}
