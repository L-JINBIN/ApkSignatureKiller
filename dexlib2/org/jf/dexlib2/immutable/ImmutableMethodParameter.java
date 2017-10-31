

package org.jf.dexlib2.immutable;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.jf.dexlib2.base.BaseMethodParameter;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.MethodParameter;
import org.jf.util.ImmutableConverter;
import org.jf.util.ImmutableUtils;

import java.util.Set;

public class ImmutableMethodParameter extends BaseMethodParameter {
    @NonNull
    protected final String type;
    @NonNull
    protected final ImmutableSet<? extends ImmutableAnnotation> annotations;
    @Nullable
    protected final String name;

    public ImmutableMethodParameter(@NonNull String type,
                                    @Nullable Set<? extends Annotation> annotations,
                                    @Nullable String name) {
        this.type = type;
        this.annotations = ImmutableAnnotation.immutableSetOf(annotations);
        this.name = name;
    }

    public ImmutableMethodParameter(@NonNull String type,
                                    @Nullable ImmutableSet<? extends ImmutableAnnotation> annotations,
                                    @Nullable String name) {
        this.type = type;
        this.annotations = ImmutableUtils.nullToEmptySet(annotations);
        this.name = name;
    }

    public static ImmutableMethodParameter of(MethodParameter methodParameter) {
        if (methodParameter instanceof ImmutableMethodParameter) {
            return (ImmutableMethodParameter) methodParameter;
        }
        return new ImmutableMethodParameter(
                methodParameter.getType(),
                methodParameter.getAnnotations(),
                methodParameter.getName());
    }

    @NonNull
    @Override
    public String getType() {
        return type;
    }

    @NonNull
    @Override
    public Set<? extends Annotation> getAnnotations() {
        return annotations;
    }

    @Nullable
    @Override
    public String getName() {
        return name;
    }

    //TODO: iterate over the annotations to get the signature
    @Nullable
    @Override
    public String getSignature() {
        return null;
    }

    @NonNull
    public static ImmutableList<ImmutableMethodParameter> immutableListOf(
            @Nullable Iterable<? extends MethodParameter> list) {
        return CONVERTER.toList(list);
    }

    private static final ImmutableConverter<ImmutableMethodParameter, MethodParameter> CONVERTER =
            new ImmutableConverter<ImmutableMethodParameter, MethodParameter>() {
                @Override
                protected boolean isImmutable(@NonNull MethodParameter item) {
                    return item instanceof ImmutableMethodParameter;
                }

                @NonNull
                @Override
                protected ImmutableMethodParameter makeImmutable(@NonNull MethodParameter item) {
                    return ImmutableMethodParameter.of(item);
                }
            };
}
