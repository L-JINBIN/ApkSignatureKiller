

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;

import java.util.Comparator;
import java.util.Iterator;


@GwtCompatible
interface SortedIterable<T> extends Iterable<T> {

    Comparator<? super T> comparator();


    @Override
    Iterator<T> iterator();
}
