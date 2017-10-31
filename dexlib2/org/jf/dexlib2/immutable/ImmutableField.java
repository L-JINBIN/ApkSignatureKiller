

package org.jf.dexlib2.immutable;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;

import org.jf.dexlib2.base.reference.BaseFieldReference;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.value.EncodedValue;
import org.jf.dexlib2.immutable.value.ImmutableEncodedValue;
import org.jf.dexlib2.immutable.value.ImmutableEncodedValueFactory;
import org.jf.util.ImmutableConverter;
import org.jf.util.ImmutableUtils;

import java.util.Collection;

public class ImmutableField extends BaseFieldReference implements Field {
    @NonNull
    protected final String definingClass;
    @NonNull
    protected final String name;
    @NonNull
    protected final String type;
    protected final int accessFlags;
    @Nullable
    protected final ImmutableEncodedValue initialValue;
    @NonNull
    protected final ImmutableSet<? extends ImmutableAnnotation> annotations;

    public ImmutableField(@NonNull String definingClass,
                          @NonNull String name,
                          @NonNull String type,
                          int accessFlags,
                          @Nullable EncodedValue initialValue,
                          @Nullable Collection<? extends Annotation> annotations) {
        this.definingClass = definingClass;
        this.name = name;
        this.type = type;
        this.accessFlags = accessFlags;
        this.initialValue = ImmutableEncodedValueFactory.ofNullable(initialValue);
        this.annotations = ImmutableAnnotation.immutableSetOf(annotations);
    }

    public ImmutableField(@NonNull String definingClass,
                          @NonNull String name,
                          @NonNull String type,
                          int accessFlags,
                          @Nullable ImmutableEncodedValue initialValue,
                          @Nullable ImmutableSet<? extends ImmutableAnnotation> annotations) {
        this.definingClass = definingClass;
        this.name = name;
        this.type = type;
        this.accessFlags = accessFlags;
        this.initialValue = initialValue;
        this.annotations = ImmutableUtils.nullToEmptySet(annotations);
    }

    public static ImmutableField of(Field field) {
        if (field instanceof ImmutableField) {
            return (ImmutableField) field;
        }
        return new ImmutableField(
                field.getDefiningClass(),
                field.getName(),
                field.getType(),
                field.getAccessFlags(),
                field.getInitialValue(),
                field.getAnnotations());
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
    public String getType() {
        return type;
    }

    @Override
    public int getAccessFlags() {
        return accessFlags;
    }

    @Override
    public EncodedValue getInitialValue() {
        return initialValue;
    }

    @NonNull
    @Override
    public ImmutableSet<? extends ImmutableAnnotation> getAnnotations() {
        return annotations;
    }

    @NonNull
    public static ImmutableSortedSet<ImmutableField> immutableSetOf(@Nullable Iterable<? extends Field> list) {
        return CONVERTER.toSortedSet(Ordering.natural(), list);
    }

    private static final ImmutableConverter<ImmutableField, Field> CONVERTER =
            new ImmutableConverter<ImmutableField, Field>() {
                @Override
                protected boolean isImmutable(@NonNull Field item) {
                    return item instanceof ImmutableField;
                }

                @NonNull
                @Override
                protected ImmutableField makeImmutable(@NonNull Field item) {
                    return ImmutableField.of(item);
                }
            };
}
