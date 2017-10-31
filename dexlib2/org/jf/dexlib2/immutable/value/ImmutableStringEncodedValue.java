

package org.jf.dexlib2.immutable.value;

import android.support.annotation.NonNull;

import org.jf.dexlib2.base.value.BaseStringEncodedValue;
import org.jf.dexlib2.iface.value.StringEncodedValue;

public class ImmutableStringEncodedValue extends BaseStringEncodedValue implements ImmutableEncodedValue {
    @NonNull
    protected final String value;

    public ImmutableStringEncodedValue(@NonNull String value) {
        this.value = value;
    }

    public static ImmutableStringEncodedValue of(@NonNull StringEncodedValue stringEncodedValue) {
        if (stringEncodedValue instanceof ImmutableStringEncodedValue) {
            return (ImmutableStringEncodedValue) stringEncodedValue;
        }
        return new ImmutableStringEncodedValue(stringEncodedValue.getValue());
    }

    @NonNull
    @Override
    public String getValue() {
        return value;
    }
}
