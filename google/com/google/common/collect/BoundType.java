

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;


@GwtCompatible
public enum BoundType {

    OPEN {
        @Override
        BoundType flip() {
            return CLOSED;
        }
    },

    CLOSED {
        @Override
        BoundType flip() {
            return OPEN;
        }
    };


    static BoundType forBoolean(boolean inclusive) {
        return inclusive ? CLOSED : OPEN;
    }

    abstract BoundType flip();
}
