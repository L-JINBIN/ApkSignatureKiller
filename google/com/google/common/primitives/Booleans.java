

package com.google.common.primitives;

import com.google.common.annotations.GwtCompatible;


@GwtCompatible
public final class Booleans {
    private Booleans() {
    }


    public static int compare(boolean a, boolean b) {
        return (a == b) ? 0 : (a ? 1 : -1);
    }

}
