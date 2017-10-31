

package com.google.common.base;

import android.support.annotation.Nullable;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import static com.google.common.base.Preconditions.checkNotNull;


@GwtCompatible
public class Joiner {


    public static Joiner on(String separator) {
        return new Joiner(separator);
    }


    public static Joiner on(char separator) {
        return new Joiner(String.valueOf(separator));
    }

    private final String separator;

    private Joiner(String separator) {
        this.separator = checkNotNull(separator);
    }

    private Joiner(Joiner prototype) {
        this.separator = prototype.separator;
    }


    public <A extends Appendable> A appendTo(A appendable, Iterator<?> parts) throws IOException {
        checkNotNull(appendable);
        if (parts.hasNext()) {
            appendable.append(toString(parts.next()));
            while (parts.hasNext()) {
                appendable.append(separator);
                appendable.append(toString(parts.next()));
            }
        }
        return appendable;
    }


    public final StringBuilder appendTo(StringBuilder builder, Iterable<?> parts) {
        return appendTo(builder, parts.iterator());
    }


    public final StringBuilder appendTo(StringBuilder builder, Iterator<?> parts) {
        try {
            appendTo((Appendable) builder, parts);
        } catch (IOException impossible) {
            throw new AssertionError(impossible);
        }
        return builder;
    }


    public final String join(Iterable<?> parts) {
        return join(parts.iterator());
    }


    public final String join(Iterator<?> parts) {
        return appendTo(new StringBuilder(), parts).toString();
    }


    public final String join(Object[] parts) {
        return join(Arrays.asList(parts));
    }


    public Joiner useForNull(final String nullText) {
        checkNotNull(nullText);
        return new Joiner(this) {
            @Override
            CharSequence toString(@Nullable Object part) {
                return (part == null) ? nullText : Joiner.this.toString(part);
            }

            @Override
            public Joiner useForNull(String nullText) {
                throw new UnsupportedOperationException("already specified useForNull");
            }

        };
    }


    public MapJoiner withKeyValueSeparator(String keyValueSeparator) {
        return new MapJoiner(this, keyValueSeparator);
    }


    public static final class MapJoiner {
        private final Joiner joiner;
        private final String keyValueSeparator;

        private MapJoiner(Joiner joiner, String keyValueSeparator) {
            this.joiner = joiner; // only "this" is ever passed, so don't checkNotNull
            this.keyValueSeparator = checkNotNull(keyValueSeparator);
        }


        public StringBuilder appendTo(StringBuilder builder, Map<?, ?> map) {
            return appendTo(builder, map.entrySet());
        }


        @Beta
        public <A extends Appendable> A appendTo(A appendable, Iterator<? extends Entry<?, ?>> parts)
                throws IOException {
            checkNotNull(appendable);
            if (parts.hasNext()) {
                Entry<?, ?> entry = parts.next();
                appendable.append(joiner.toString(entry.getKey()));
                appendable.append(keyValueSeparator);
                appendable.append(joiner.toString(entry.getValue()));
                while (parts.hasNext()) {
                    appendable.append(joiner.separator);
                    Entry<?, ?> e = parts.next();
                    appendable.append(joiner.toString(e.getKey()));
                    appendable.append(keyValueSeparator);
                    appendable.append(joiner.toString(e.getValue()));
                }
            }
            return appendable;
        }


        @Beta
        public StringBuilder appendTo(StringBuilder builder, Iterable<? extends Entry<?, ?>> entries) {
            return appendTo(builder, entries.iterator());
        }


        @Beta
        public StringBuilder appendTo(StringBuilder builder, Iterator<? extends Entry<?, ?>> entries) {
            try {
                appendTo((Appendable) builder, entries);
            } catch (IOException impossible) {
                throw new AssertionError(impossible);
            }
            return builder;
        }


    }

    CharSequence toString(Object part) {
        checkNotNull(part); // checkNotNull for GWT (do not optimize).
        return (part instanceof CharSequence) ? (CharSequence) part : part.toString();
    }

}
