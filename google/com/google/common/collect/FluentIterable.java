

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;


@GwtCompatible(emulated = true)
public abstract class FluentIterable<E> implements Iterable<E> {
    // We store 'iterable' and use it instead of 'this' to allow Iterables to perform instanceof
    // checks on the _original_ iterable when FluentIterable.from is used.
    private final Iterable<E> iterable;


    protected FluentIterable() {
        this.iterable = this;
    }

    FluentIterable(Iterable<E> iterable) {
        this.iterable = checkNotNull(iterable);
    }


    public static <E> FluentIterable<E> from(final Iterable<E> iterable) {
        return (iterable instanceof FluentIterable)
                ? (FluentIterable<E>) iterable
                : new FluentIterable<E>(iterable) {
            @Override
            public Iterator<E> iterator() {
                return iterable.iterator();
            }
        };
    }


    @Override

    public String toString() {
        return Iterables.toString(iterable);
    }


    public final FluentIterable<E> filter(Predicate<? super E> predicate) {
        return from(Iterables.filter(iterable, predicate));
    }


    public final <T> FluentIterable<T> transform(Function<? super E, T> function) {
        return from(Iterables.transform(iterable, function));
    }


    public final FluentIterable<E> limit(int size) {
        return from(Iterables.limit(iterable, size));
    }


    private static class FromIterableFunction<E> implements Function<Iterable<E>, FluentIterable<E>> {
        @Override
        public FluentIterable<E> apply(Iterable<E> fromObject) {
            return FluentIterable.from(fromObject);
        }
    }
}
