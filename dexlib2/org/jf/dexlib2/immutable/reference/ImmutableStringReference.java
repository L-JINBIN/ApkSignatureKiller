

package org.jf.dexlib2.immutable.reference;

import android.support.annotation.NonNull;

import org.jf.dexlib2.base.reference.BaseStringReference;
import org.jf.dexlib2.iface.reference.StringReference;

public class ImmutableStringReference extends BaseStringReference implements ImmutableReference {
    @NonNull
    protected final String str;

    public ImmutableStringReference(String str) {
        this.str = str;
    }

    @NonNull
    public static ImmutableStringReference of(@NonNull StringReference stringReference) {
        if (stringReference instanceof ImmutableStringReference) {
            return (ImmutableStringReference) stringReference;
        }
        return new ImmutableStringReference(stringReference.getString());
    }

    @NonNull
    @Override
    public String getString() {
        return str;
    }
}
