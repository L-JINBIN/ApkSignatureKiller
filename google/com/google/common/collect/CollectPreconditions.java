

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;

import static com.google.common.base.Preconditions.checkState;


@GwtCompatible
final class CollectPreconditions {

    static void checkEntryNotNull(Object key, Object value) {
        if (key == null) {
            throw new NullPointerException("null key in entry: null=" + value);
        } else if (value == null) {
            throw new NullPointerException("null value in entry: " + key + "=null");
        }
    }

    static int checkNonnegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " cannot be negative but was: " + value);
        }
        return value;
    }


    static void checkRemove(boolean canRemove) {
        checkState(canRemove, "no calls to next() since the last call to remove()");
    }
}
