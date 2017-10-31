

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.WeakOuter;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;

import static com.google.common.base.Preconditions.checkNotNull;


@GwtCompatible(emulated = true)
abstract class AbstractSortedMultiset<E> extends AbstractMultiset<E> implements SortedMultiset<E> {
    @GwtTransient
    final Comparator<? super E> comparator;

    // needed for serialization
    @SuppressWarnings("unchecked")
    AbstractSortedMultiset() {
        this((Comparator) Ordering.natural());
    }

    AbstractSortedMultiset(Comparator<? super E> comparator) {
        this.comparator = checkNotNull(comparator);
    }

    @Override
    public NavigableSet<E> elementSet() {
        return (NavigableSet<E>) super.elementSet();
    }

    @Override
    NavigableSet<E> createElementSet() {
        return new SortedMultisets.NavigableElementSet<>(this);
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public Entry<E> firstEntry() {
        Iterator<Entry<E>> entryIterator = entryIterator();
        return entryIterator.hasNext() ? entryIterator.next() : null;
    }

    @Override
    public Entry<E> lastEntry() {
        Iterator<Entry<E>> entryIterator = descendingEntryIterator();
        return entryIterator.hasNext() ? entryIterator.next() : null;
    }

    @Override
    public Entry<E> pollFirstEntry() {
        Iterator<Entry<E>> entryIterator = entryIterator();
        if (entryIterator.hasNext()) {
            Entry<E> result = entryIterator.next();
            result = Multisets.immutableEntry(result.getElement(), result.getCount());
            entryIterator.remove();
            return result;
        }
        return null;
    }

    @Override
    public Entry<E> pollLastEntry() {
        Iterator<Entry<E>> entryIterator = descendingEntryIterator();
        if (entryIterator.hasNext()) {
            Entry<E> result = entryIterator.next();
            result = Multisets.immutableEntry(result.getElement(), result.getCount());
            entryIterator.remove();
            return result;
        }
        return null;
    }

    @Override
    public SortedMultiset<E> subMultiset(
            @Nullable E fromElement,
            BoundType fromBoundType,
            @Nullable E toElement,
            BoundType toBoundType) {
        // These are checked elsewhere, but NullPointerTester wants them checked eagerly.
        checkNotNull(fromBoundType);
        checkNotNull(toBoundType);
        return tailMultiset(fromElement, fromBoundType).headMultiset(toElement, toBoundType);
    }

    abstract Iterator<Entry<E>> descendingEntryIterator();

    Iterator<E> descendingIterator() {
        return Multisets.iteratorImpl(descendingMultiset());
    }

    private transient SortedMultiset<E> descendingMultiset;

    @Override
    public SortedMultiset<E> descendingMultiset() {
        SortedMultiset<E> result = descendingMultiset;
        return (result == null) ? descendingMultiset = createDescendingMultiset() : result;
    }

    SortedMultiset<E> createDescendingMultiset() {
        @WeakOuter
        class DescendingMultisetImpl extends DescendingMultiset<E> {
            @Override
            SortedMultiset<E> forwardMultiset() {
                return AbstractSortedMultiset.this;
            }

            @Override
            Iterator<Entry<E>> entryIterator() {
                return descendingEntryIterator();
            }

            @Override
            public Iterator<E> iterator() {
                return descendingIterator();
            }
        }
        return new DescendingMultisetImpl();
    }
}
