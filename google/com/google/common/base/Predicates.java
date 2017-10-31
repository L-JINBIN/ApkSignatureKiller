

package com.google.common.base;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;


@GwtCompatible(emulated = true)
public final class Predicates {
    private Predicates() {
    }

    // TODO(kevinb): considering having these implement a VisitablePredicate
    // interface which specifies an accept(PredicateVisitor) method.


    @GwtCompatible(serializable = true)
    public static <T> Predicate<T> isNull() {
        return ObjectPredicate.IS_NULL.withNarrowedType();
    }


    @GwtCompatible(serializable = true)
    public static <T> Predicate<T> notNull() {
        return ObjectPredicate.NOT_NULL.withNarrowedType();
    }


    public static <T> Predicate<T> not(Predicate<T> predicate) {
        return new NotPredicate<>(predicate);
    }


    public static <T> Predicate<T> and(Iterable<? extends Predicate<? super T>> components) {
        return new AndPredicate<>(defensiveCopy(components));
    }


    public static <T> Predicate<T> and(Predicate<? super T>... components) {
        return new AndPredicate<>(defensiveCopy(components));
    }


    public static <T> Predicate<T> and(Predicate<? super T> first, Predicate<? super T> second) {
        return new AndPredicate<>(Predicates.asList(checkNotNull(first), checkNotNull(second)));
    }


    public static <T> Predicate<T> equalTo(@Nullable T target) {
        return (target == null) ? Predicates.<T>isNull() : new IsEqualToPredicate<>(target);
    }


    @GwtIncompatible("Class.isInstance")
    public static Predicate<Object> instanceOf(Class<?> clazz) {
        return new InstanceOfPredicate(clazz);
    }


    public static <T> Predicate<T> in(Collection<? extends T> target) {
        return new InPredicate<>(target);
    }


    public static <A, B> Predicate<A> compose(
            Predicate<B> predicate, Function<A, ? extends B> function) {
        return new CompositionPredicate<>(predicate, function);
    }


    // End public API, begin private implementation classes.

    // Package private for GWT serialization.
    enum ObjectPredicate implements Predicate<Object> {

        IS_NULL {
            @Override
            public boolean apply(@Nullable Object o) {
                return o == null;
            }

            @Override
            public String toString() {
                return "Predicates.isNull()";
            }
        },

        NOT_NULL {
            @Override
            public boolean apply(@Nullable Object o) {
                return o != null;
            }

            @Override
            public String toString() {
                return "Predicates.notNull()";
            }
        };

        @SuppressWarnings("unchecked")
            // safe contravariant cast
        <T> Predicate<T> withNarrowedType() {
            return (Predicate<T>) this;
        }
    }


    private static class NotPredicate<T> implements Predicate<T>, Serializable {
        final Predicate<T> predicate;

        NotPredicate(Predicate<T> predicate) {
            this.predicate = checkNotNull(predicate);
        }

        @Override
        public boolean apply(@Nullable T t) {
            return !predicate.apply(t);
        }

        @Override
        public int hashCode() {
            return ~predicate.hashCode();
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof NotPredicate) {
                NotPredicate<?> that = (NotPredicate<?>) obj;
                return predicate.equals(that.predicate);
            }
            return false;
        }

        @Override
        public String toString() {
            return "Predicates.not(" + predicate + ")";
        }

        private static final long serialVersionUID = 0;
    }

    private static final Joiner COMMA_JOINER = Joiner.on(',');


    private static class AndPredicate<T> implements Predicate<T>, Serializable {
        private final List<? extends Predicate<? super T>> components;

        private AndPredicate(List<? extends Predicate<? super T>> components) {
            this.components = components;
        }

        @Override
        public boolean apply(@Nullable T t) {
            // Avoid using the Iterator to avoid generating garbage (issue 820).
            for (int i = 0; i < components.size(); i++) {
                if (!components.get(i).apply(t)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            // add a random number to avoid collisions with OrPredicate
            return components.hashCode() + 0x12472c2c;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof AndPredicate) {
                AndPredicate<?> that = (AndPredicate<?>) obj;
                return components.equals(that.components);
            }
            return false;
        }

        @Override
        public String toString() {
            return "Predicates.and(" + COMMA_JOINER.join(components) + ")";
        }

        private static final long serialVersionUID = 0;
    }


    private static class IsEqualToPredicate<T> implements Predicate<T>, Serializable {
        private final T target;

        private IsEqualToPredicate(T target) {
            this.target = target;
        }

        @Override
        public boolean apply(T t) {
            return target.equals(t);
        }

        @Override
        public int hashCode() {
            return target.hashCode();
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof IsEqualToPredicate) {
                IsEqualToPredicate<?> that = (IsEqualToPredicate<?>) obj;
                return target.equals(that.target);
            }
            return false;
        }

        @Override
        public String toString() {
            return "Predicates.equalTo(" + target + ")";
        }

        private static final long serialVersionUID = 0;
    }


    @GwtIncompatible("Class.isInstance")
    private static class InstanceOfPredicate implements Predicate<Object>, Serializable {
        private final Class<?> clazz;

        private InstanceOfPredicate(Class<?> clazz) {
            this.clazz = checkNotNull(clazz);
        }

        @Override
        public boolean apply(@Nullable Object o) {
            return clazz.isInstance(o);
        }

        @Override
        public int hashCode() {
            return clazz.hashCode();
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof InstanceOfPredicate) {
                InstanceOfPredicate that = (InstanceOfPredicate) obj;
                return clazz == that.clazz;
            }
            return false;
        }

        @Override
        public String toString() {
            return "Predicates.instanceOf(" + clazz.getName() + ")";
        }

        private static final long serialVersionUID = 0;
    }


    private static class InPredicate<T> implements Predicate<T>, Serializable {
        private final Collection<?> target;

        private InPredicate(Collection<?> target) {
            this.target = checkNotNull(target);
        }

        @Override
        public boolean apply(@Nullable T t) {
            try {
                return target.contains(t);
            } catch (NullPointerException e) {
                return false;
            } catch (ClassCastException e) {
                return false;
            }
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof InPredicate) {
                InPredicate<?> that = (InPredicate<?>) obj;
                return target.equals(that.target);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return target.hashCode();
        }

        @Override
        public String toString() {
            return "Predicates.in(" + target + ")";
        }

        private static final long serialVersionUID = 0;
    }


    private static class CompositionPredicate<A, B> implements Predicate<A>, Serializable {
        final Predicate<B> p;
        final Function<A, ? extends B> f;

        private CompositionPredicate(Predicate<B> p, Function<A, ? extends B> f) {
            this.p = checkNotNull(p);
            this.f = checkNotNull(f);
        }

        @Override
        public boolean apply(@Nullable A a) {
            return p.apply(f.apply(a));
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof CompositionPredicate) {
                CompositionPredicate<?, ?> that = (CompositionPredicate<?, ?>) obj;
                return f.equals(that.f) && p.equals(that.p);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return f.hashCode() ^ p.hashCode();
        }

        @Override
        public String toString() {
            // TODO(cpovirk): maybe make this look like the method call does ("Predicates.compose(...)")
            return p + "(" + f + ")";
        }

        private static final long serialVersionUID = 0;
    }


    private static <T> List<Predicate<? super T>> asList(
            Predicate<? super T> first, Predicate<? super T> second) {
        // TODO(kevinb): understand why we still get a warning despite @SafeVarargs!
        return Arrays.<Predicate<? super T>>asList(first, second);
    }

    private static <T> List<T> defensiveCopy(T... array) {
        return defensiveCopy(Arrays.asList(array));
    }

    static <T> List<T> defensiveCopy(Iterable<T> iterable) {
        ArrayList<T> list = new ArrayList<>();
        for (T element : iterable) {
            list.add(checkNotNull(element));
        }
        return list;
    }
}
