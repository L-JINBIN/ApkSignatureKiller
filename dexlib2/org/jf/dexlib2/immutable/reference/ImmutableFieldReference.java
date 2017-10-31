

package org.jf.dexlib2.immutable.reference;

import android.support.annotation.NonNull;

import org.jf.dexlib2.base.reference.BaseFieldReference;
import org.jf.dexlib2.iface.reference.FieldReference;

public class ImmutableFieldReference extends BaseFieldReference implements ImmutableReference {
    @NonNull
    protected final String definingClass;
    @NonNull
    protected final String name;
    @NonNull
    protected final String type;

    public ImmutableFieldReference(@NonNull String definingClass,
                                   @NonNull String name,
                                   @NonNull String type) {
        this.definingClass = definingClass;
        this.name = name;
        this.type = type;
    }

    @NonNull
    public static ImmutableFieldReference of(@NonNull FieldReference fieldReference) {
        if (fieldReference instanceof ImmutableFieldReference) {
            return (ImmutableFieldReference) fieldReference;
        }
        return new ImmutableFieldReference(
                fieldReference.getDefiningClass(),
                fieldReference.getName(),
                fieldReference.getType());
    }

    @NonNull
    public String getDefiningClass() {
        return definingClass;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getType() {
        return type;
    }
}
