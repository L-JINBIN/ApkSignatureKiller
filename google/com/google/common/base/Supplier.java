

package com.google.common.base;

import com.google.common.annotations.GwtCompatible;


@GwtCompatible
public interface Supplier<T> {

    T get();
}
