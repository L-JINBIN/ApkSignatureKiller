

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;


@GwtCompatible
@Beta
@Deprecated
public interface MapConstraint<K, V> {

    void checkKeyValue(@Nullable K key, @Nullable V value);


    @Override
    String toString();
}
