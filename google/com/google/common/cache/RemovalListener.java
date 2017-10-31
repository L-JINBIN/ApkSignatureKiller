

package com.google.common.cache;

import com.google.common.annotations.GwtCompatible;


@GwtCompatible
public interface RemovalListener<K, V> {

    // Technically should accept RemovalNotification<? extends K, ? extends V>, but because
    // RemovalNotification is guaranteed covariant, let's make users' lives simpler.
    void onRemoval(RemovalNotification<K, V> notification);
}
