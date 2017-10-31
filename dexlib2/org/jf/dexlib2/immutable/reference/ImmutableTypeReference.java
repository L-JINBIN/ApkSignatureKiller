

package org.jf.dexlib2.immutable.reference;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import org.jf.dexlib2.base.reference.BaseTypeReference;
import org.jf.dexlib2.iface.reference.TypeReference;
import org.jf.util.ImmutableConverter;

import java.util.List;

public class ImmutableTypeReference extends BaseTypeReference implements ImmutableReference {
    @NonNull
    protected final String type;

    public ImmutableTypeReference(String type) {
        this.type = type;
    }

    @NonNull
    public static ImmutableTypeReference of(@NonNull TypeReference typeReference) {
        if (typeReference instanceof ImmutableTypeReference) {
            return (ImmutableTypeReference) typeReference;
        }
        return new ImmutableTypeReference(typeReference.getType());
    }

    @NonNull
    @Override
    public String getType() {
        return type;
    }

    @NonNull
    public static ImmutableList<ImmutableTypeReference> immutableListOf(@Nullable List<? extends TypeReference> list) {
        return CONVERTER.toList(list);
    }

    private static final ImmutableConverter<ImmutableTypeReference, TypeReference> CONVERTER =
            new ImmutableConverter<ImmutableTypeReference, TypeReference>() {
                @Override
                protected boolean isImmutable(@NonNull TypeReference item) {
                    return item instanceof ImmutableTypeReference;
                }

                @NonNull
                @Override
                protected ImmutableTypeReference makeImmutable(@NonNull TypeReference item) {
                    return ImmutableTypeReference.of(item);
                }
            };
}
