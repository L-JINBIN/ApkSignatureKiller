

package org.jf.dexlib2.immutable.value;

import android.support.annotation.NonNull;

import org.jf.dexlib2.base.value.BaseTypeEncodedValue;
import org.jf.dexlib2.iface.value.TypeEncodedValue;

public class ImmutableTypeEncodedValue extends BaseTypeEncodedValue implements ImmutableEncodedValue {
    @NonNull
    protected final String value;

    public ImmutableTypeEncodedValue(@NonNull String value) {
        this.value = value;
    }

    public static ImmutableTypeEncodedValue of(@NonNull TypeEncodedValue typeEncodedValue) {
        if (typeEncodedValue instanceof ImmutableTypeEncodedValue) {
            return (ImmutableTypeEncodedValue) typeEncodedValue;
        }
        return new ImmutableTypeEncodedValue(typeEncodedValue.getValue());
    }

    @NonNull
    @Override
    public String getValue() {
        return value;
    }
}
