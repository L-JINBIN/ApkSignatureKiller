

package com.google.common.cache;

import com.google.common.annotations.GwtCompatible;


@GwtCompatible
public interface Weigher<K, V> {


    int weigh(K key, V value);
}
