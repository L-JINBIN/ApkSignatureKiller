

package com.google.common.primitives;

import com.google.common.annotations.GwtCompatible;


@GwtCompatible
public final class Longs {
    private Longs() {
    }


    public static int compare(long a, long b) {
        return (a < b) ? -1 : ((a > b) ? 1 : 0);
    }

}
