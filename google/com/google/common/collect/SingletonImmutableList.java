

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;

import static com.google.common.base.Preconditions.checkNotNull;


@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial") // uses writeReplace(), not default serialization
final class SingletonImmutableList<E> extends ImmutableList<E> {

    final transient E element;

    SingletonImmutableList(E element) {
        this.element = checkNotNull(element);
    }

    @Override
    public E get(int index) {
        Preconditions.checkElementIndex(index, 1);
        return element;
    }

    @Override
    public UnmodifiableIterator<E> iterator() {
        return Iterators.singletonIterator(element);
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public ImmutableList<E> subList(int fromIndex, int toIndex) {
        Preconditions.checkPositionIndexes(fromIndex, toIndex, 1);
        return (fromIndex == toIndex) ? ImmutableList.<E>of() : this;
    }

    @Override
    public String toString() {
        String elementToString = element.toString();
        return new StringBuilder(elementToString.length() + 2)
                .append('[')
                .append(elementToString)
                .append(']')
                .toString();
    }

    @Override
    boolean isPartialView() {
        return false;
    }
}
