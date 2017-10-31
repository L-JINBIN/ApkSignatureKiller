

package org.jf.dexlib2.immutable.reference;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import org.jf.dexlib2.base.reference.BaseMethodProtoReference;
import org.jf.dexlib2.iface.reference.MethodProtoReference;
import org.jf.dexlib2.immutable.util.CharSequenceConverter;

import java.util.List;

public class ImmutableMethodProtoReference extends BaseMethodProtoReference implements ImmutableReference {
    @NonNull
    protected final ImmutableList<String> parameters;
    @NonNull
    protected final String returnType;

    public ImmutableMethodProtoReference(@Nullable Iterable<? extends CharSequence> parameters,
                                         @NonNull String returnType) {
        this.parameters = CharSequenceConverter.immutableStringList(parameters);
        this.returnType = returnType;
    }

    @NonNull
    public static ImmutableMethodProtoReference of(@NonNull MethodProtoReference methodProtoReference) {
        if (methodProtoReference instanceof ImmutableMethodProtoReference) {
            return (ImmutableMethodProtoReference) methodProtoReference;
        }
        return new ImmutableMethodProtoReference(
                methodProtoReference.getParameterTypes(),
                methodProtoReference.getReturnType());
    }

    @Override
    public List<? extends CharSequence> getParameterTypes() {
        return parameters;
    }

    @Override
    public String getReturnType() {
        return returnType;
    }
}
