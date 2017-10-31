

package com.google.common.base;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;

import java.util.Arrays;

import static com.google.common.base.Preconditions.checkNotNull;


@GwtCompatible
public final class MoreObjects {


    public static <T> T firstNonNull(@Nullable T first, @Nullable T second) {
        return first != null ? first : checkNotNull(second);
    }


    public static ToStringHelper toStringHelper(Object self) {
        return new ToStringHelper(self.getClass().getSimpleName());
    }


    public static final class ToStringHelper {
        private final String className;
        private ValueHolder holderHead = new ValueHolder();
        private ValueHolder holderTail = holderHead;
        private boolean omitNullValues = false;


        private ToStringHelper(String className) {
            this.className = checkNotNull(className);
        }


        public ToStringHelper add(String name, @Nullable Object value) {
            return addHolder(name, value);
        }


        public ToStringHelper add(String name, int value) {
            return addHolder(name, String.valueOf(value));
        }


        public ToStringHelper add(String name, long value) {
            return addHolder(name, String.valueOf(value));
        }


        public ToStringHelper addValue(@Nullable Object value) {
            return addHolder(value);
        }


        @Override
        public String toString() {
            // create a copy to keep it consistent in case value changes
            boolean omitNullValuesSnapshot = omitNullValues;
            String nextSeparator = "";
            StringBuilder builder = new StringBuilder(32).append(className).append('{');
            for (ValueHolder valueHolder = holderHead.next;
                 valueHolder != null;
                 valueHolder = valueHolder.next) {
                Object value = valueHolder.value;
                if (!omitNullValuesSnapshot || value != null) {
                    builder.append(nextSeparator);
                    nextSeparator = ", ";

                    if (valueHolder.name != null) {
                        builder.append(valueHolder.name).append('=');
                    }
                    if (value != null && value.getClass().isArray()) {
                        Object[] objectArray = {value};
                        String arrayString = Arrays.deepToString(objectArray);
                        builder.append(arrayString.substring(1, arrayString.length() - 1));
                    } else {
                        builder.append(value);
                    }
                }
            }
            return builder.append('}').toString();
        }

        private ValueHolder addHolder() {
            ValueHolder valueHolder = new ValueHolder();
            holderTail = holderTail.next = valueHolder;
            return valueHolder;
        }

        private ToStringHelper addHolder(@Nullable Object value) {
            ValueHolder valueHolder = addHolder();
            valueHolder.value = value;
            return this;
        }

        private ToStringHelper addHolder(String name, @Nullable Object value) {
            ValueHolder valueHolder = addHolder();
            valueHolder.value = value;
            valueHolder.name = checkNotNull(name);
            return this;
        }

        private static final class ValueHolder {
            String name;
            Object value;
            ValueHolder next;
        }
    }

    private MoreObjects() {
    }
}
