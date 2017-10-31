

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Predicate;

import java.util.Map.Entry;


@GwtCompatible
interface FilteredMultimap<K, V> extends Multimap<K, V> {
    Multimap<K, V> unfiltered();

    Predicate<? super Entry<K, V>> entryPredicate();
}
