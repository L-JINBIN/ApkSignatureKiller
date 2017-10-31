

package org.jf.dexlib2.immutable;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.collect.ImmutableSet;

import org.jf.dexlib2.base.BaseAnnotationElement;
import org.jf.dexlib2.iface.AnnotationElement;
import org.jf.dexlib2.iface.value.EncodedValue;
import org.jf.dexlib2.immutable.value.ImmutableEncodedValue;
import org.jf.dexlib2.immutable.value.ImmutableEncodedValueFactory;
import org.jf.util.ImmutableConverter;

public class ImmutableAnnotationElement extends BaseAnnotationElement {
    @NonNull
    protected final String name;
    @NonNull
    protected final ImmutableEncodedValue value;

    public ImmutableAnnotationElement(@NonNull String name,
                                      @NonNull EncodedValue value) {
        this.name = name;
        this.value = ImmutableEncodedValueFactory.of(value);
    }

    public ImmutableAnnotationElement(@NonNull String name,
                                      @NonNull ImmutableEncodedValue value) {
        this.name = name;
        this.value = value;
    }

    public static ImmutableAnnotationElement of(AnnotationElement annotationElement) {
        if (annotationElement instanceof ImmutableAnnotationElement) {
            return (ImmutableAnnotationElement) annotationElement;
        }
        return new ImmutableAnnotationElement(
                annotationElement.getName(),
                annotationElement.getValue());
    }

    @NonNull
    @Override
    public String getName() {
        return name;
    }

    @NonNull
    @Override
    public EncodedValue getValue() {
        return value;
    }

    @NonNull
    public static ImmutableSet<ImmutableAnnotationElement> immutableSetOf(
            @Nullable Iterable<? extends AnnotationElement> list) {
        return CONVERTER.toSet(list);
    }

    private static final ImmutableConverter<ImmutableAnnotationElement, AnnotationElement> CONVERTER =
            new ImmutableConverter<ImmutableAnnotationElement, AnnotationElement>() {
                @Override
                protected boolean isImmutable(@NonNull AnnotationElement item) {
                    return item instanceof ImmutableAnnotationElement;
                }

                @NonNull
                @Override
                protected ImmutableAnnotationElement makeImmutable(@NonNull AnnotationElement item) {
                    return ImmutableAnnotationElement.of(item);
                }
            };
}
