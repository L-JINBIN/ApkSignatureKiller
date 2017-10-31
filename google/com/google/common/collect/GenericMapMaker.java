

package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.MoreObjects;
import com.google.common.collect.MapMaker.RemovalListener;
import com.google.common.collect.MapMaker.RemovalNotification;


@Beta
@Deprecated
@GwtCompatible(emulated = true)
abstract class GenericMapMaker<K0, V0> {
    @GwtIncompatible("To be supported")
    enum NullListener implements RemovalListener<Object, Object> {
        INSTANCE;

        @Override
        public void onRemoval(RemovalNotification<Object, Object> notification) {
        }
    }

    // Set by MapMaker, but sits in this class to preserve the type relationship
    @GwtIncompatible("To be supported")
    RemovalListener<K0, V0> removalListener;

    // No subclasses but our own
    GenericMapMaker() {
    }


    @SuppressWarnings("unchecked") // safe covariant cast
    @GwtIncompatible("To be supported")
    <K extends K0, V extends V0> RemovalListener<K, V> getRemovalListener() {
        return (RemovalListener<K, V>) MoreObjects.firstNonNull(removalListener, NullListener.INSTANCE);
    }


}
