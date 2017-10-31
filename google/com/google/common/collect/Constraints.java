

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedSet;

import static com.google.common.base.Preconditions.checkNotNull;


@GwtCompatible
final class Constraints {
    private Constraints() {
    }


    public static <E> Collection<E> constrainedCollection(
            Collection<E> collection, Constraint<? super E> constraint) {
        return new ConstrainedCollection<>(collection, constraint);
    }


    static class ConstrainedCollection<E> extends ForwardingCollection<E> {
        private final Collection<E> delegate;
        private final Constraint<? super E> constraint;

        public ConstrainedCollection(Collection<E> delegate, Constraint<? super E> constraint) {
            this.delegate = checkNotNull(delegate);
            this.constraint = checkNotNull(constraint);
        }

        @Override
        protected Collection<E> delegate() {
            return delegate;
        }

        @Override
        public boolean add(E element) {
            constraint.checkElement(element);
            return delegate.add(element);
        }

        @Override
        public boolean addAll(Collection<? extends E> elements) {
            return delegate.addAll(checkElements(elements, constraint));
        }
    }


    public static <E> Set<E> constrainedSet(Set<E> set, Constraint<? super E> constraint) {
        return new ConstrainedSet<>(set, constraint);
    }


    static class ConstrainedSet<E> extends ForwardingSet<E> {
        private final Set<E> delegate;
        private final Constraint<? super E> constraint;

        public ConstrainedSet(Set<E> delegate, Constraint<? super E> constraint) {
            this.delegate = checkNotNull(delegate);
            this.constraint = checkNotNull(constraint);
        }

        @Override
        protected Set<E> delegate() {
            return delegate;
        }

        @Override
        public boolean add(E element) {
            constraint.checkElement(element);
            return delegate.add(element);
        }

        @Override
        public boolean addAll(Collection<? extends E> elements) {
            return delegate.addAll(checkElements(elements, constraint));
        }
    }


    public static <E> SortedSet<E> constrainedSortedSet(
            SortedSet<E> sortedSet, Constraint<? super E> constraint) {
        return new ConstrainedSortedSet<>(sortedSet, constraint);
    }


    private static class ConstrainedSortedSet<E> extends ForwardingSortedSet<E> {
        final SortedSet<E> delegate;
        final Constraint<? super E> constraint;

        ConstrainedSortedSet(SortedSet<E> delegate, Constraint<? super E> constraint) {
            this.delegate = checkNotNull(delegate);
            this.constraint = checkNotNull(constraint);
        }

        @Override
        protected SortedSet<E> delegate() {
            return delegate;
        }

        @Override
        public SortedSet<E> headSet(E toElement) {
            return constrainedSortedSet(delegate.headSet(toElement), constraint);
        }

        @Override
        public SortedSet<E> subSet(E fromElement, E toElement) {
            return constrainedSortedSet(delegate.subSet(fromElement, toElement), constraint);
        }

        @Override
        public SortedSet<E> tailSet(E fromElement) {
            return constrainedSortedSet(delegate.tailSet(fromElement), constraint);
        }

        @Override
        public boolean add(E element) {
            constraint.checkElement(element);
            return delegate.add(element);
        }

        @Override
        public boolean addAll(Collection<? extends E> elements) {
            return delegate.addAll(checkElements(elements, constraint));
        }
    }


    public static <E> List<E> constrainedList(List<E> list, Constraint<? super E> constraint) {
        return (list instanceof RandomAccess)
                ? new ConstrainedRandomAccessList<>(list, constraint)
                : new ConstrainedList<>(list, constraint);
    }


    @GwtCompatible
    private static class ConstrainedList<E> extends ForwardingList<E> {
        final List<E> delegate;
        final Constraint<? super E> constraint;

        ConstrainedList(List<E> delegate, Constraint<? super E> constraint) {
            this.delegate = checkNotNull(delegate);
            this.constraint = checkNotNull(constraint);
        }

        @Override
        protected List<E> delegate() {
            return delegate;
        }

        @Override
        public boolean add(E element) {
            constraint.checkElement(element);
            return delegate.add(element);
        }

        @Override
        public void add(int index, E element) {
            constraint.checkElement(element);
            delegate.add(index, element);
        }

        @Override
        public boolean addAll(Collection<? extends E> elements) {
            return delegate.addAll(checkElements(elements, constraint));
        }

        @Override
        public boolean addAll(int index, Collection<? extends E> elements) {
            return delegate.addAll(index, checkElements(elements, constraint));
        }

        @Override
        public ListIterator<E> listIterator() {
            return constrainedListIterator(delegate.listIterator(), constraint);
        }

        @Override
        public ListIterator<E> listIterator(int index) {
            return constrainedListIterator(delegate.listIterator(index), constraint);
        }

        @Override
        public E set(int index, E element) {
            constraint.checkElement(element);
            return delegate.set(index, element);
        }

        @Override
        public List<E> subList(int fromIndex, int toIndex) {
            return constrainedList(delegate.subList(fromIndex, toIndex), constraint);
        }
    }


    static class ConstrainedRandomAccessList<E> extends ConstrainedList<E> implements RandomAccess {
        ConstrainedRandomAccessList(List<E> delegate, Constraint<? super E> constraint) {
            super(delegate, constraint);
        }
    }


    private static <E> ListIterator<E> constrainedListIterator(
            ListIterator<E> listIterator, Constraint<? super E> constraint) {
        return new ConstrainedListIterator<>(listIterator, constraint);
    }


    static class ConstrainedListIterator<E> extends ForwardingListIterator<E> {
        private final ListIterator<E> delegate;
        private final Constraint<? super E> constraint;

        public ConstrainedListIterator(ListIterator<E> delegate, Constraint<? super E> constraint) {
            this.delegate = delegate;
            this.constraint = constraint;
        }

        @Override
        protected ListIterator<E> delegate() {
            return delegate;
        }

        @Override
        public void add(E element) {
            constraint.checkElement(element);
            delegate.add(element);
        }

        @Override
        public void set(E element) {
            constraint.checkElement(element);
            delegate.set(element);
        }
    }

    static <E> Collection<E> constrainedTypePreservingCollection(
            Collection<E> collection, Constraint<E> constraint) {
        if (collection instanceof SortedSet) {
            return constrainedSortedSet((SortedSet<E>) collection, constraint);
        } else if (collection instanceof Set) {
            return constrainedSet((Set<E>) collection, constraint);
        } else if (collection instanceof List) {
            return constrainedList((List<E>) collection, constraint);
        } else {
            return constrainedCollection(collection, constraint);
        }
    }


    private static <E> Collection<E> checkElements(
            Collection<E> elements, Constraint<? super E> constraint) {
        Collection<E> copy = Lists.newArrayList(elements);
        for (E element : copy) {
            constraint.checkElement(element);
        }
        return copy;
    }
}
