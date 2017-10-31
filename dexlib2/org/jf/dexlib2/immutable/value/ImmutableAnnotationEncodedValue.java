

package org.jf.dexlib2.immutable.value;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.collect.ImmutableSet;

import org.jf.dexlib2.base.value.BaseAnnotationEncodedValue;
import org.jf.dexlib2.iface.AnnotationElement;
import org.jf.dexlib2.iface.value.AnnotationEncodedValue;
import org.jf.dexlib2.immutable.ImmutableAnnotationElement;
import org.jf.util.ImmutableUtils;

import java.util.Collection;

public class ImmutableAnnotationEncodedValue extends BaseAnnotationEncodedValue implements ImmutableEncodedValue {
    @NonNull
    protected final String type;
    @NonNull
    protected final ImmutableSet<? extends ImmutableAnnotationElement> elements;

    public ImmutableAnnotationEncodedValue(@NonNull String type,
                                           @Nullable Collection<? extends AnnotationElement> elements) {
        this.type = type;
        this.elements = ImmutableAnnotationElement.immutableSetOf(elements);
    }

    public ImmutableAnnotationEncodedValue(@NonNull String type,
                                           @Nullable ImmutableSet<? extends ImmutableAnnotationElement> elements) {
        this.type = type;
        this.elements = ImmutableUtils.nullToEmptySet(elements);
    }

    public static ImmutableAnnotationEncodedValue of(AnnotationEncodedValue annotationEncodedValue) {
        if (annotationEncodedValue instanceof ImmutableAnnotationEncodedValue) {
            return (ImmutableAnnotationEncodedValue) annotationEncodedValue;
        }
        return new ImmutableAnnotationEncodedValue(
                annotationEncodedValue.getType(),
                annotationEncodedValue.getElements());
    }

    @NonNull
    @Override
    public String getType() {
        return type;
    }

    @NonNull
    @Override
    public ImmutableSet<? extends ImmutableAnnotationElement> getElements() {
        return elements;
    }
}
