

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtIncompatible;

import static com.google.common.collect.CollectPreconditions.checkEntryNotNull;


@GwtIncompatible("unnecessary")
class ImmutableMapEntry<K, V> extends ImmutableEntry<K, V> {

    @SuppressWarnings("unchecked") // Safe as long as the javadocs are followed
    static <K, V> ImmutableMapEntry<K, V>[] createEntryArray(int size) {
        return new ImmutableMapEntry[size];
    }

    ImmutableMapEntry(K key, V value) {
        super(key, value);
        checkEntryNotNull(key, value);
    }

    @Nullable
    ImmutableMapEntry<K, V> getNextInKeyBucket() {
        return null;
    }

    @Nullable
    ImmutableMapEntry<K, V> getNextInValueBucket() {
        return null;
    }


    boolean isReusable() {
        return true;
    }

    static class NonTerminalImmutableMapEntry<K, V> extends ImmutableMapEntry<K, V> {
        private final transient ImmutableMapEntry<K, V> nextInKeyBucket;

        NonTerminalImmutableMapEntry(K key, V value, ImmutableMapEntry<K, V> nextInKeyBucket) {
            super(key, value);
            this.nextInKeyBucket = nextInKeyBucket;
        }

        @Override
        @Nullable
        final ImmutableMapEntry<K, V> getNextInKeyBucket() {
            return nextInKeyBucket;
        }

        @Override
        final boolean isReusable() {
            return false;
        }
    }

    static final class NonTerminalImmutableBiMapEntry<K, V>
            extends NonTerminalImmutableMapEntry<K, V> {
        private final transient ImmutableMapEntry<K, V> nextInValueBucket;

        NonTerminalImmutableBiMapEntry(
                K key,
                V value,
                ImmutableMapEntry<K, V> nextInKeyBucket,
                ImmutableMapEntry<K, V> nextInValueBucket) {
            super(key, value, nextInKeyBucket);
            this.nextInValueBucket = nextInValueBucket;
        }

        @Override
        @Nullable
        ImmutableMapEntry<K, V> getNextInValueBucket() {
            return nextInValueBucket;
        }
    }
}
