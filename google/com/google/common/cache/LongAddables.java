

package com.google.common.cache;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Supplier;

import java.util.concurrent.atomic.AtomicLong;


@GwtCompatible(emulated = true)
final class LongAddables {
    private static final Supplier<LongAddable> SUPPLIER = new Supplier<LongAddable>() {
        @Override
        public LongAddable get() {
            return new PureJavaLongAddable();
        }
    };

    static {
    }

    public static LongAddable create() {
        return SUPPLIER.get();
    }

    private static final class PureJavaLongAddable extends AtomicLong implements LongAddable {
        @Override
        public void increment() {
            getAndIncrement();
        }

        @Override
        public void add(long x) {
            getAndAdd(x);
        }

        @Override
        public long sum() {
            return get();
        }
    }
}
