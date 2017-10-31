

package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;

import java.util.NoSuchElementException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;


@Beta
@GwtCompatible(emulated = true)
@SuppressWarnings("rawtypes") // allow ungenerified Comparable types
public abstract class ContiguousSet<C extends Comparable> extends ImmutableSortedSet<C> {

    public static <C extends Comparable> ContiguousSet<C> create(
            Range<C> range, DiscreteDomain<C> domain) {
        checkNotNull(range);
        checkNotNull(domain);
        Range<C> effectiveRange = range;
        try {
            if (!range.hasLowerBound()) {
                effectiveRange = effectiveRange.intersection(Range.atLeast(domain.minValue()));
            }
            if (!range.hasUpperBound()) {
                effectiveRange = effectiveRange.intersection(Range.atMost(domain.maxValue()));
            }
        } catch (NoSuchElementException e) {
            throw new IllegalArgumentException(e);
        }

        // Per class spec, we are allowed to throw CCE if necessary
        boolean empty = effectiveRange.isEmpty()
                || Range.compareOrThrow(
                range.lowerBound.leastValueAbove(domain),
                range.upperBound.greatestValueBelow(domain)) > 0;

        return empty
                ? new EmptyContiguousSet<>(domain)
                : new RegularContiguousSet<>(effectiveRange, domain);
    }

    final DiscreteDomain<C> domain;

    ContiguousSet(DiscreteDomain<C> domain) {
        super(Ordering.natural());
        this.domain = domain;
    }

    @Override
    public ContiguousSet<C> headSet(C toElement) {
        return headSetImpl(checkNotNull(toElement), false);
    }


    @GwtIncompatible("NavigableSet")
    @Override
    public ContiguousSet<C> headSet(C toElement, boolean inclusive) {
        return headSetImpl(checkNotNull(toElement), inclusive);
    }

    @Override
    public ContiguousSet<C> subSet(C fromElement, C toElement) {
        checkNotNull(fromElement);
        checkNotNull(toElement);
        checkArgument(comparator().compare(fromElement, toElement) <= 0);
        return subSetImpl(fromElement, true, toElement, false);
    }


    @GwtIncompatible("NavigableSet")
    @Override
    public ContiguousSet<C> subSet(
            C fromElement, boolean fromInclusive, C toElement, boolean toInclusive) {
        checkNotNull(fromElement);
        checkNotNull(toElement);
        checkArgument(comparator().compare(fromElement, toElement) <= 0);
        return subSetImpl(fromElement, fromInclusive, toElement, toInclusive);
    }

    @Override
    public ContiguousSet<C> tailSet(C fromElement) {
        return tailSetImpl(checkNotNull(fromElement), true);
    }


    @GwtIncompatible("NavigableSet")
    @Override
    public ContiguousSet<C> tailSet(C fromElement, boolean inclusive) {
        return tailSetImpl(checkNotNull(fromElement), inclusive);
    }


    // TODO(kevinb): we can probably make these real @Overrides now

    abstract ContiguousSet<C> headSetImpl(C toElement, boolean inclusive);


    abstract ContiguousSet<C> subSetImpl(
            C fromElement, boolean fromInclusive, C toElement, boolean toInclusive);


    abstract ContiguousSet<C> tailSetImpl(C fromElement, boolean inclusive);


    public abstract Range<C> range();


    @Override
    public String toString() {
        return range().toString();
    }


}
