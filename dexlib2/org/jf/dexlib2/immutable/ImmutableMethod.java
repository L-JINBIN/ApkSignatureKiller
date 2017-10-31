

package org.jf.dexlib2.immutable;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;

import org.jf.dexlib2.base.reference.BaseMethodReference;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.MethodParameter;
import org.jf.util.ImmutableConverter;
import org.jf.util.ImmutableUtils;

import java.util.Set;

public class ImmutableMethod extends BaseMethodReference implements Method {
    @NonNull
    protected final String definingClass;
    @NonNull
    protected final String name;
    @NonNull
    protected final ImmutableList<? extends ImmutableMethodParameter> parameters;
    @NonNull
    protected final String returnType;
    protected final int accessFlags;
    @NonNull
    protected final ImmutableSet<? extends ImmutableAnnotation> annotations;
    @Nullable
    protected final ImmutableMethodImplementation methodImplementation;

    public ImmutableMethod(@NonNull String definingClass,
                           @NonNull String name,
                           @Nullable Iterable<? extends MethodParameter> parameters,
                           @NonNull String returnType,
                           int accessFlags,
                           @Nullable Set<? extends Annotation> annotations,
                           @Nullable MethodImplementation methodImplementation) {
        this.definingClass = definingClass;
        this.name = name;
        this.parameters = ImmutableMethodParameter.immutableListOf(parameters);
        this.returnType = returnType;
        this.accessFlags = accessFlags;
        this.annotations = ImmutableAnnotation.immutableSetOf(annotations);
        this.methodImplementation = ImmutableMethodImplementation.of(methodImplementation);
    }

    public ImmutableMethod(@NonNull String definingClass,
                           @NonNull String name,
                           @Nullable ImmutableList<? extends ImmutableMethodParameter> parameters,
                           @NonNull String returnType,
                           int accessFlags,
                           @Nullable ImmutableSet<? extends ImmutableAnnotation> annotations,
                           @Nullable ImmutableMethodImplementation methodImplementation) {
        this.definingClass = definingClass;
        this.name = name;
        this.parameters = ImmutableUtils.nullToEmptyList(parameters);
        this.returnType = returnType;
        this.accessFlags = accessFlags;
        this.annotations = ImmutableUtils.nullToEmptySet(annotations);
        this.methodImplementation = methodImplementation;
    }

    public static ImmutableMethod of(Method method) {
        if (method instanceof ImmutableMethod) {
            return (ImmutableMethod) method;
        }
        return new ImmutableMethod(
                method.getDefiningClass(),
                method.getName(),
                method.getParameters(),
                method.getReturnType(),
                method.getAccessFlags(),
                method.getAnnotations(),
                method.getImplementation());
    }

    @Override
    @NonNull
    public String getDefiningClass() {
        return definingClass;
    }

    @Override
    @NonNull
    public String getName() {
        return name;
    }

    @Override
    @NonNull
    public ImmutableList<? extends CharSequence> getParameterTypes() {
        return parameters;
    }

    @Override
    @NonNull
    public ImmutableList<? extends ImmutableMethodParameter> getParameters() {
        return parameters;
    }

    @Override
    @NonNull
    public String getReturnType() {
        return returnType;
    }

    @Override
    public int getAccessFlags() {
        return accessFlags;
    }

    @Override
    @NonNull
    public ImmutableSet<? extends ImmutableAnnotation> getAnnotations() {
        return annotations;
    }

    @Override
    @Nullable
    public ImmutableMethodImplementation getImplementation() {
        return methodImplementation;
    }

    @NonNull
    public static ImmutableSortedSet<ImmutableMethod> immutableSetOf(@Nullable Iterable<? extends Method> list) {
        return CONVERTER.toSortedSet(Ordering.natural(), list);
    }

    private static final ImmutableConverter<ImmutableMethod, Method> CONVERTER =
            new ImmutableConverter<ImmutableMethod, Method>() {
                @Override
                protected boolean isImmutable(@NonNull Method item) {
                    return item instanceof ImmutableMethod;
                }

                @NonNull
                @Override
                protected ImmutableMethod makeImmutable(@NonNull Method item) {
                    return ImmutableMethod.of(item);
                }
            };
}
