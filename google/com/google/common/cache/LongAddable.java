

package com.google.common.cache;

import com.google.common.annotations.GwtCompatible;


@GwtCompatible
interface LongAddable {
    void increment();

    void add(long x);

    long sum();
}
