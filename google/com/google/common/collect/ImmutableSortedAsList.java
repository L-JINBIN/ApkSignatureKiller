

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;

import java.util.Comparator;


@GwtCompatible(emulated = true)
@SuppressWarnings("serial")
final class ImmutableSortedAsList<E> extends RegularImmutableAsList<E>
        implements SortedIterable<E> {
    ImmutableSortedAsList(ImmutableSortedSet<E> backingSet, ImmutableList<E> backingList) {
        super(backingSet, backingList);
    }

    @Override
    ImmutableSortedSet<E> delegateCollection() {
        return (ImmutableSortedSet<E>) super.delegateCollection();
    }

    @Override
    public Comparator<? super E> comparator() {
        return delegateCollection().comparator();
    }

    // Override indexOf() and lastIndexOf() to be O(log N) instead of O(N).

    @GwtIncompatible("ImmutableSortedSet.indexOf")
    // TODO(cpovirk): consider manual binary search under GWT to preserve O(log N) lookup
    @Override
    public int indexOf(@Nullable Object target) {
        int index = delegateCollection().indexOf(target);

        // TODO(kevinb): reconsider if it's really worth making feeble attempts at
        // sanity for inconsistent comparators.

        // The equals() check is needed when the comparator isn't compatible with
        // equals().
        return (index >= 0 && get(index).equals(target)) ? index : -1;
    }

    @GwtIncompatible("ImmutableSortedSet.indexOf")
    @Override
    public int lastIndexOf(@Nullable Object target) {
        return indexOf(target);
    }

    @Override
    public boolean contains(Object target) {
        // Necessary for ISS's with comparators inconsistent with equals.
        return indexOf(target) >= 0;
    }

    @GwtIncompatible("super.subListUnchecked does not exist; inherited subList is valid if slow")

    @Override
    ImmutableList<E> subListUnchecked(int fromIndex, int toIndex) {
        ImmutableList<E> parentSubList = super.subListUnchecked(fromIndex, toIndex);
        return new RegularImmutableSortedSet<>(parentSubList, comparator()).asList();
    }
}
