

package org.jf.dexlib2.immutable.value;

import android.support.annotation.NonNull;

import org.jf.dexlib2.base.value.BaseFieldEncodedValue;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.value.FieldEncodedValue;

public class ImmutableFieldEncodedValue extends BaseFieldEncodedValue implements ImmutableEncodedValue {
    @NonNull
    protected final FieldReference value;

    public ImmutableFieldEncodedValue(@NonNull FieldReference value) {
        this.value = value;
    }

    public static ImmutableFieldEncodedValue of(@NonNull FieldEncodedValue fieldEncodedValue) {
        if (fieldEncodedValue instanceof ImmutableFieldEncodedValue) {
            return (ImmutableFieldEncodedValue) fieldEncodedValue;
        }
        return new ImmutableFieldEncodedValue(fieldEncodedValue.getValue());
    }

    @NonNull
    @Override
    public FieldReference getValue() {
        return value;
    }
}
