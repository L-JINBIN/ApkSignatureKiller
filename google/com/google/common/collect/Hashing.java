

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.primitives.Ints;


@GwtCompatible
final class Hashing {
    private Hashing() {
    }

    private static final int C1 = 0xcc9e2d51;
    private static final int C2 = 0x1b873593;


    static int smear(int hashCode) {
        return C2 * Integer.rotateLeft(hashCode * C1, 15);
    }

    static int smearedHash(@Nullable Object o) {
        return smear((o == null) ? 0 : o.hashCode());
    }

    private static int MAX_TABLE_SIZE = Ints.MAX_POWER_OF_TWO;

    static int closedTableSize(int expectedEntries, double loadFactor) {
        // Get the recommended table size.
        // Round down to the nearest power of 2.
        expectedEntries = Math.max(expectedEntries, 2);
        int tableSize = Integer.highestOneBit(expectedEntries);
        // Check to make sure that we will not exceed the maximum load factor.
        if (expectedEntries > (int) (loadFactor * tableSize)) {
            tableSize <<= 1;
            return (tableSize > 0) ? tableSize : MAX_TABLE_SIZE;
        }
        return tableSize;
    }

}
