

package com.google.common.primitives;

import com.google.common.annotations.GwtCompatible;


@GwtCompatible(emulated = true)
public final class Chars {
    private Chars() {
    }


    public static int compare(char a, char b) {
        return a - b; // safe due to restricted range
    }

}
