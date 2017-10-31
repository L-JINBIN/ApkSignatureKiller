

package com.google.common.base;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;

import static com.google.common.base.Preconditions.checkNotNull;


@GwtCompatible

public final class Functions {
    private Functions() {
    }


    public static Function<Object, String> toStringFunction() {
        return ToStringFunction.INSTANCE;
    }

    // enum singleton pattern
    private enum ToStringFunction implements Function<Object, String> {
        INSTANCE;

        @Override
        public String apply(Object o) {
            checkNotNull(o); // eager for GWT.
            return o.toString();
        }

        @Override
        public String toString() {
            return "Functions.toStringFunction()";
        }
    }


    // enum singleton pattern
    private enum IdentityFunction implements Function<Object, Object> {
        ;

        @Override
        @Nullable
        public Object apply(@Nullable Object o) {
            return o;
        }

        @Override
        public String toString() {
            return "Functions.identity()";
        }
    }


}
