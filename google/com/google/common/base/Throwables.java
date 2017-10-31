

package com.google.common.base;

import android.support.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;


public final class Throwables {
    private Throwables() {
    }


    public static <X extends Throwable> void propagateIfInstanceOf(
            @Nullable Throwable throwable, Class<X> declaredType) throws X {
        // Check for null is needed to avoid frequent JNI calls to isInstance().
        if (throwable != null && declaredType.isInstance(throwable)) {
            throw declaredType.cast(throwable);
        }
    }


    public static void propagateIfPossible(@Nullable Throwable throwable) {
        propagateIfInstanceOf(throwable, Error.class);
        propagateIfInstanceOf(throwable, RuntimeException.class);
    }


    public static <X extends Throwable> void propagateIfPossible(
            @Nullable Throwable throwable, Class<X> declaredType) throws X {
        propagateIfInstanceOf(throwable, declaredType);
        propagateIfPossible(throwable);
    }


    public static <X1 extends Throwable, X2 extends Throwable> void propagateIfPossible(
            @Nullable Throwable throwable, Class<X1> declaredType1, Class<X2> declaredType2)
            throws X1, X2 {
        checkNotNull(declaredType2);
        propagateIfInstanceOf(throwable, declaredType1);
        propagateIfPossible(throwable, declaredType2);
    }


}
