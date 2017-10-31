

package org.jf.dexlib2.immutable.value;

import android.support.annotation.NonNull;

import com.google.common.collect.ImmutableList;

import org.jf.dexlib2.base.value.BaseArrayEncodedValue;
import org.jf.dexlib2.iface.value.ArrayEncodedValue;
import org.jf.dexlib2.iface.value.EncodedValue;

import java.util.Collection;

public class ImmutableArrayEncodedValue extends BaseArrayEncodedValue implements ImmutableEncodedValue {
    @NonNull
    protected final ImmutableList<? extends ImmutableEncodedValue> value;

    public ImmutableArrayEncodedValue(@NonNull Collection<? extends EncodedValue> value) {
        this.value = ImmutableEncodedValueFactory.immutableListOf(value);
    }

    public ImmutableArrayEncodedValue(@NonNull ImmutableList<ImmutableEncodedValue> value) {
        this.value = value;
    }

    public static ImmutableArrayEncodedValue of(@NonNull ArrayEncodedValue arrayEncodedValue) {
        if (arrayEncodedValue instanceof ImmutableArrayEncodedValue) {
            return (ImmutableArrayEncodedValue) arrayEncodedValue;
        }
        return new ImmutableArrayEncodedValue(arrayEncodedValue.getValue());
    }

    @NonNull
    public ImmutableList<? extends ImmutableEncodedValue> getValue() {
        return value;
    }
}
