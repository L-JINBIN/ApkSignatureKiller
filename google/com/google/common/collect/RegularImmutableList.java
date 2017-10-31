

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;


@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial")
        // uses writeReplace(), not default serialization
class RegularImmutableList<E> extends ImmutableList<E> {
    static final ImmutableList<Object> EMPTY =
            new RegularImmutableList<>(ObjectArrays.EMPTY_ARRAY);

    private final transient int offset;
    private final transient int size;
    private final transient Object[] array;

    RegularImmutableList(Object[] array, int offset, int size) {
        this.offset = offset;
        this.size = size;
        this.array = array;
    }

    RegularImmutableList(Object[] array) {
        this(array, 0, array.length);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    boolean isPartialView() {
        return size != array.length;
    }

    @Override
    int copyIntoArray(Object[] dst, int dstOff) {
        System.arraycopy(array, offset, dst, dstOff, size);
        return dstOff + size;
    }

    // The fake cast to E is safe because the creation methods only allow E's
    @Override
    @SuppressWarnings("unchecked")
    public E get(int index) {
        Preconditions.checkElementIndex(index, size);
        return (E) array[index + offset];
    }

    @Override
    ImmutableList<E> subListUnchecked(int fromIndex, int toIndex) {
        return new RegularImmutableList<>(
                array, offset + fromIndex, toIndex - fromIndex);
    }

    @SuppressWarnings("unchecked")
    @Override
    public UnmodifiableListIterator<E> listIterator(int index) {
        // for performance
        // The fake cast to E is safe because the creation methods only allow E's
        return (UnmodifiableListIterator<E>)
                Iterators.forArray(array, offset, size, index);
    }

    // TODO(lowasser): benchmark optimizations for equals() and see if they're worthwhile
}
