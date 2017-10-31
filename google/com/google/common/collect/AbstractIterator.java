

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;

import java.util.NoSuchElementException;

import static com.google.common.base.Preconditions.checkState;


// When making changes to this class, please also update the copy at
// com.google.common.base.AbstractIterator
@GwtCompatible
public abstract class AbstractIterator<T> extends UnmodifiableIterator<T> {
    private State state = State.NOT_READY;


    protected AbstractIterator() {
    }

    private enum State {

        READY,


        NOT_READY,


        DONE,


        FAILED,
    }

    private T next;


    protected abstract T computeNext();


    protected final T endOfData() {
        state = State.DONE;
        return null;
    }

    @Override
    public final boolean hasNext() {
        checkState(state != State.FAILED);
        switch (state) {
            case DONE:
                return false;
            case READY:
                return true;
            default:
        }
        return tryToComputeNext();
    }

    private boolean tryToComputeNext() {
        state = State.FAILED; // temporary pessimism
        next = computeNext();
        if (state != State.DONE) {
            state = State.READY;
            return true;
        }
        return false;
    }

    @Override
    public final T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        state = State.NOT_READY;
        T result = next;
        next = null;
        return result;
    }


    public final T peek() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return next;
    }
}
