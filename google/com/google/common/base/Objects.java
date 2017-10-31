

package com.google.common.base;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;

import java.util.Arrays;


@GwtCompatible
public final class Objects {
    private Objects() {
    }


    public static boolean equal(@Nullable Object a, @Nullable Object b) {
        return a == b || (a != null && a.equals(b));
    }


    public static int hashCode(@Nullable Object... objects) {
        return Arrays.hashCode(objects);
    }


}
