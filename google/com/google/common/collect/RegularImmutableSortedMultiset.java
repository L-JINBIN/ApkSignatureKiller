

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.primitives.Ints;

import java.util.Comparator;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkPositionIndexes;
import static com.google.common.collect.BoundType.CLOSED;


@SuppressWarnings("serial") // uses writeReplace, not default serialization
final class RegularImmutableSortedMultiset<E> extends ImmutableSortedMultiset<E> {
    private static final long[] ZERO_CUMULATIVE_COUNTS = {0};

    private final transient RegularImmutableSortedSet<E> elementSet;
    private final transient long[] cumulativeCounts;
    private final transient int offset;
    private final transient int length;

    RegularImmutableSortedMultiset(Comparator<? super E> comparator) {
        this.elementSet = ImmutableSortedSet.emptySet(comparator);
        this.cumulativeCounts = ZERO_CUMULATIVE_COUNTS;
        this.offset = 0;
        this.length = 0;
    }

    RegularImmutableSortedMultiset(
            RegularImmutableSortedSet<E> elementSet,
            long[] cumulativeCounts,
            int offset,
            int length) {
        this.elementSet = elementSet;
        this.cumulativeCounts = cumulativeCounts;
        this.offset = offset;
        this.length = length;
    }

    private int getCount(int index) {
        return (int) (cumulativeCounts[offset + index + 1] - cumulativeCounts[offset + index]);
    }

    @Override
    Entry<E> getEntry(int index) {
        return Multisets.immutableEntry(elementSet.asList().get(index), getCount(index));
    }

    @Override
    public Entry<E> firstEntry() {
        return isEmpty() ? null : getEntry(0);
    }

    @Override
    public Entry<E> lastEntry() {
        return isEmpty() ? null : getEntry(length - 1);
    }

    @Override
    public int count(@Nullable Object element) {
        int index = elementSet.indexOf(element);
        return (index >= 0) ? getCount(index) : 0;
    }

    @Override
    public int size() {
        long size = cumulativeCounts[offset + length] - cumulativeCounts[offset];
        return Ints.saturatedCast(size);
    }

    @Override
    public ImmutableSortedSet<E> elementSet() {
        return elementSet;
    }

    @Override
    public ImmutableSortedMultiset<E> headMultiset(E upperBound, BoundType boundType) {
        return getSubMultiset(0, elementSet.headIndex(upperBound, checkNotNull(boundType) == CLOSED));
    }

    @Override
    public ImmutableSortedMultiset<E> tailMultiset(E lowerBound, BoundType boundType) {
        return getSubMultiset(elementSet.tailIndex(lowerBound, checkNotNull(boundType) == CLOSED),
                length);
    }

    ImmutableSortedMultiset<E> getSubMultiset(int from, int to) {
        checkPositionIndexes(from, to, length);
        if (from == to) {
            return emptyMultiset(comparator());
        } else if (from == 0 && to == length) {
            return this;
        } else {
            RegularImmutableSortedSet<E> subElementSet =
                    (RegularImmutableSortedSet<E>) elementSet.getSubSet(from, to);
            return new RegularImmutableSortedMultiset<>(
                    subElementSet, cumulativeCounts, offset + from, to - from);
        }
    }

    @Override
    boolean isPartialView() {
        return offset > 0 || length < cumulativeCounts.length - 1;
    }
}
