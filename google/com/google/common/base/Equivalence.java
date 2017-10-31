

package com.google.common.base;

import android.support.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;

import java.io.Serializable;


@GwtCompatible
public abstract class Equivalence<T> {


    public final boolean equivalent(@Nullable T a, @Nullable T b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return doEquivalent(a, b);
    }


    protected abstract boolean doEquivalent(T a, T b);


    public final int hash(@Nullable T t) {
        if (t == null) {
            return 0;
        }
        return doHash(t);
    }


    protected abstract int doHash(T t);


    public static Equivalence<Object> equals() {
        return Equals.INSTANCE;
    }


    public static Equivalence<Object> identity() {
        return Identity.INSTANCE;
    }

    static final class Equals extends Equivalence<Object> implements Serializable {

        static final Equals INSTANCE = new Equals();

        @Override
        protected boolean doEquivalent(Object a, Object b) {
            return a.equals(b);
        }

        @Override
        protected int doHash(Object o) {
            return o.hashCode();
        }

        private Object readResolve() {
            return INSTANCE;
        }

        private static final long serialVersionUID = 1;
    }

    static final class Identity extends Equivalence<Object> implements Serializable {

        static final Identity INSTANCE = new Identity();

        @Override
        protected boolean doEquivalent(Object a, Object b) {
            return false;
        }

        @Override
        protected int doHash(Object o) {
            return System.identityHashCode(o);
        }

        private Object readResolve() {
            return INSTANCE;
        }

        private static final long serialVersionUID = 1;
    }
}
