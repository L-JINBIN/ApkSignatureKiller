

package com.google.common.primitives;

import com.google.common.annotations.GwtCompatible;


@GwtCompatible(emulated = true)
public final class Shorts {
    private Shorts() {
    }


    public static int compare(short a, short b) {
        return a - b; // safe due to restricted range
    }

}
