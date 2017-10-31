

package org.jf.dexlib2.immutable.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import org.jf.util.ImmutableConverter;

public final class CharSequenceConverter {
    private CharSequenceConverter() {
    }

    @NonNull
    public static ImmutableList<String> immutableStringList(@Nullable Iterable<? extends CharSequence> iterable) {
        return CONVERTER.toList(iterable);
    }

    private static final ImmutableConverter<String, CharSequence> CONVERTER =
            new ImmutableConverter<String, CharSequence>() {
                @Override
                protected boolean isImmutable(@NonNull CharSequence item) {
                    return item instanceof String;
                }

                @NonNull
                @Override
                protected String makeImmutable(@NonNull CharSequence item) {
                    return item.toString();
                }
            };
}
