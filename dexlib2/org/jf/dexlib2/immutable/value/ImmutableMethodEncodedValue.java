

package org.jf.dexlib2.immutable.value;

import android.support.annotation.NonNull;

import org.jf.dexlib2.base.value.BaseMethodEncodedValue;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.value.MethodEncodedValue;

public class ImmutableMethodEncodedValue extends BaseMethodEncodedValue implements ImmutableEncodedValue {
    @NonNull
    protected final MethodReference value;

    public ImmutableMethodEncodedValue(@NonNull MethodReference value) {
        this.value = value;
    }

    public static ImmutableMethodEncodedValue of(@NonNull MethodEncodedValue methodEncodedValue) {
        if (methodEncodedValue instanceof ImmutableMethodEncodedValue) {
            return (ImmutableMethodEncodedValue) methodEncodedValue;
        }
        return new ImmutableMethodEncodedValue(methodEncodedValue.getValue());
    }

    @NonNull
    @Override
    public MethodReference getValue() {
        return value;
    }
}
