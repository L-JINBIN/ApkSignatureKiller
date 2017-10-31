

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ImmutableMapEntry.NonTerminalImmutableMapEntry;

import static com.google.common.base.Preconditions.checkPositionIndex;
import static com.google.common.collect.CollectPreconditions.checkEntryNotNull;
import static com.google.common.collect.ImmutableMapEntry.createEntryArray;


@GwtCompatible(serializable = true, emulated = true)
final class RegularImmutableMap<K, V> extends ImmutableMap<K, V> {

    // entries in insertion order
    private final transient Entry<K, V>[] entries;
    // array of linked lists of entries
    private final transient ImmutableMapEntry<K, V>[] table;
    // 'and' with an int to get a table index
    private final transient int mask;

    static <K, V> RegularImmutableMap<K, V> fromEntries(Entry<K, V>... entries) {
        return fromEntryArray(entries.length, entries);
    }


    static <K, V> RegularImmutableMap<K, V> fromEntryArray(int n, Entry<K, V>[] entryArray) {
        checkPositionIndex(n, entryArray.length);
        Entry<K, V>[] entries;
        if (n == entryArray.length) {
            entries = entryArray;
        } else {
            entries = createEntryArray(n);
        }
        int tableSize = Hashing.closedTableSize(n, MAX_LOAD_FACTOR);
        ImmutableMapEntry<K, V>[] table = createEntryArray(tableSize);
        int mask = tableSize - 1;
        for (int entryIndex = 0; entryIndex < n; entryIndex++) {
            Entry<K, V> entry = entryArray[entryIndex];
            K key = entry.getKey();
            V value = entry.getValue();
            checkEntryNotNull(key, value);
            int tableIndex = Hashing.smear(key.hashCode()) & mask;
            ImmutableMapEntry<K, V> existing = table[tableIndex];
            // prepend, not append, so the entries can be immutable
            ImmutableMapEntry<K, V> newEntry;
            if (existing == null) {
                boolean reusable = entry instanceof ImmutableMapEntry
                        && ((ImmutableMapEntry<K, V>) entry).isReusable();
                newEntry =
                        reusable ? (ImmutableMapEntry<K, V>) entry : new ImmutableMapEntry<>(key, value);
            } else {
                newEntry = new NonTerminalImmutableMapEntry<>(key, value, existing);
            }
            table[tableIndex] = newEntry;
            entries[entryIndex] = newEntry;
            checkNoConflictInKeyBucket(key, newEntry, existing);
        }
        return new RegularImmutableMap<>(entries, table, mask);
    }

    private RegularImmutableMap(Entry<K, V>[] entries, ImmutableMapEntry<K, V>[] table,
                                int mask) {
        this.entries = entries;
        this.table = table;
        this.mask = mask;
    }

    static void checkNoConflictInKeyBucket(
            Object key, Entry<?, ?> entry, @Nullable ImmutableMapEntry<?, ?> keyBucketHead) {
        for (; keyBucketHead != null; keyBucketHead = keyBucketHead.getNextInKeyBucket()) {
            checkNoConflict(!key.equals(keyBucketHead.getKey()), "key", entry, keyBucketHead);
        }
    }


    private static final double MAX_LOAD_FACTOR = 1.2;

    @Override
    public V get(@Nullable Object key) {
        return get(key, table, mask);
    }

    @Nullable
    static <V> V get(@Nullable Object key, ImmutableMapEntry<?, V>[] keyTable, int mask) {
        if (key == null) {
            return null;
        }
        int index = Hashing.smear(key.hashCode()) & mask;
        for (ImmutableMapEntry<?, V> entry = keyTable[index];
             entry != null;
             entry = entry.getNextInKeyBucket()) {
            Object candidateKey = entry.getKey();


            if (key.equals(candidateKey)) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Override
    public int size() {
        return entries.length;
    }

    @Override
    boolean isPartialView() {
        return false;
    }

    @Override
    ImmutableSet<Entry<K, V>> createEntrySet() {
        return new ImmutableMapEntrySet.RegularEntrySet<>(this, entries);
    }

    // This class is never actually serialized directly, but we have to make the
    // warning go away (and suppressing would suppress for all nested classes too)
    private static final long serialVersionUID = 0;
}
