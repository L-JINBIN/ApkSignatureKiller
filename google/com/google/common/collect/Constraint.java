

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;


@GwtCompatible
interface Constraint<E> {

    E checkElement(E element);


    @Override
    String toString();
}
