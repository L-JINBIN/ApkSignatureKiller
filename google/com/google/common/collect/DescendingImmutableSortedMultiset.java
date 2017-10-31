

package com.google.common.collect;

import android.support.annotation.Nullable;


@SuppressWarnings("serial") // uses writeReplace, not default serialization
final class DescendingImmutableSortedMultiset<E> extends ImmutableSortedMultiset<E> {
    private final transient ImmutableSortedMultiset<E> forward;

    DescendingImmutableSortedMultiset(ImmutableSortedMultiset<E> forward) {
        this.forward = forward;
    }

    @Override
    public int count(@Nullable Object element) {
        return forward.count(element);
    }

    @Override
    public Entry<E> firstEntry() {
        return forward.lastEntry();
    }

    @Override
    public Entry<E> lastEntry() {
        return forward.firstEntry();
    }

    @Override
    public int size() {
        return forward.size();
    }

    @Override
    public ImmutableSortedSet<E> elementSet() {
        return forward.elementSet().descendingSet();
    }

    @Override
    Entry<E> getEntry(int index) {
        return forward
                .entrySet()
                .asList()
                .reverse()
                .get(index);
    }

    @Override
    public ImmutableSortedMultiset<E> descendingMultiset() {
        return forward;
    }

    @Override
    public ImmutableSortedMultiset<E> headMultiset(E upperBound, BoundType boundType) {
        return forward.tailMultiset(upperBound, boundType).descendingMultiset();
    }

    @Override
    public ImmutableSortedMultiset<E> tailMultiset(E lowerBound, BoundType boundType) {
        return forward.headMultiset(lowerBound, boundType).descendingMultiset();
    }

    @Override
    boolean isPartialView() {
        return forward.isPartialView();
    }
}
