

package org.jf.dexlib2.immutable.reference;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import org.jf.dexlib2.base.reference.BaseMethodReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.immutable.util.CharSequenceConverter;
import org.jf.util.ImmutableUtils;

public class ImmutableMethodReference extends BaseMethodReference implements ImmutableReference {
    @NonNull
    protected final String definingClass;
    @NonNull
    protected final String name;
    @NonNull
    protected final ImmutableList<String> parameters;
    @NonNull
    protected final String returnType;

    public ImmutableMethodReference(@NonNull String definingClass,
                                    @NonNull String name,
                                    @Nullable Iterable<? extends CharSequence> parameters,
                                    @NonNull String returnType) {
        this.definingClass = definingClass;
        this.name = name;
        this.parameters = CharSequenceConverter.immutableStringList(parameters);
        this.returnType = returnType;
    }

    public ImmutableMethodReference(@NonNull String definingClass,
                                    @NonNull String name,
                                    @Nullable ImmutableList<String> parameters,
                                    @NonNull String returnType) {
        this.definingClass = definingClass;
        this.name = name;
        this.parameters = ImmutableUtils.nullToEmptyList(parameters);
        this.returnType = returnType;
    }

    @NonNull
    public static ImmutableMethodReference of(@NonNull MethodReference methodReference) {
        if (methodReference instanceof ImmutableMethodReference) {
            return (ImmutableMethodReference) methodReference;
        }
        return new ImmutableMethodReference(
                methodReference.getDefiningClass(),
                methodReference.getName(),
                methodReference.getParameterTypes(),
                methodReference.getReturnType());
    }

    @NonNull
    @Override
    public String getDefiningClass() {
        return definingClass;
    }

    @NonNull
    @Override
    public String getName() {
        return name;
    }

    @NonNull
    @Override
    public ImmutableList<String> getParameterTypes() {
        return parameters;
    }

    @NonNull
    @Override
    public String getReturnType() {
        return returnType;
    }


}
