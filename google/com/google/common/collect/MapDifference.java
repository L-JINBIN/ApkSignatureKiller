

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;

import java.util.Map;


@GwtCompatible
public interface MapDifference<K, V> {

    boolean areEqual();


    Map<K, V> entriesOnlyOnLeft();


    Map<K, V> entriesOnlyOnRight();


    Map<K, V> entriesInCommon();


    Map<K, ValueDifference<V>> entriesDiffering();


    @Override
    boolean equals(@Nullable Object object);


    @Override
    int hashCode();


    interface ValueDifference<V> {

        V leftValue();


        V rightValue();


        @Override
        boolean equals(@Nullable Object other);


        @Override
        int hashCode();
    }

}
