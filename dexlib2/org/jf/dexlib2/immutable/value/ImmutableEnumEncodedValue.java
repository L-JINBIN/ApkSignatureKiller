

package org.jf.dexlib2.immutable.value;

import android.support.annotation.NonNull;

import org.jf.dexlib2.base.value.BaseEnumEncodedValue;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.value.EnumEncodedValue;

public class ImmutableEnumEncodedValue extends BaseEnumEncodedValue implements ImmutableEncodedValue {
    @NonNull
    protected final FieldReference value;

    public ImmutableEnumEncodedValue(@NonNull FieldReference value) {
        this.value = value;
    }

    public static ImmutableEnumEncodedValue of(EnumEncodedValue enumEncodedValue) {
        if (enumEncodedValue instanceof ImmutableEnumEncodedValue) {
            return (ImmutableEnumEncodedValue) enumEncodedValue;
        }
        return new ImmutableEnumEncodedValue(enumEncodedValue.getValue());
    }

    @NonNull
    @Override
    public FieldReference getValue() {
        return value;
    }
}
