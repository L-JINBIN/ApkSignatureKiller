

package com.google.common.io;

import android.support.annotation.Nullable;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;


public abstract class CharSource {


    protected CharSource() {
    }


    public abstract Reader openStream() throws IOException;


    public BufferedReader openBufferedStream() throws IOException {
        Reader reader = openStream();
        return (reader instanceof BufferedReader)
                ? (BufferedReader) reader
                : new BufferedReader(reader);
    }


    @Beta
    public Optional<Long> lengthIfKnown() {
        return Optional.absent();
    }


    @Beta
    public long length() throws IOException {
        Optional<Long> lengthIfKnown = lengthIfKnown();
        if (lengthIfKnown.isPresent()) {
            return lengthIfKnown.get();
        }

        Closer closer = Closer.create();
        try {
            Reader reader = closer.register(openStream());
            return countBySkipping(reader);
        } catch (Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }
    }

    private long countBySkipping(Reader reader) throws IOException {
        long count = 0;
        long read;
        while ((read = reader.skip(Long.MAX_VALUE)) != 0) {
            count += read;
        }
        return count;
    }


    public long copyTo(Appendable appendable) throws IOException {
        checkNotNull(appendable);

        Closer closer = Closer.create();
        try {
            Reader reader = closer.register(openStream());
            return CharStreams.copy(reader, appendable);
        } catch (Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }
    }


    public long copyTo(CharSink sink) throws IOException {
        checkNotNull(sink);

        Closer closer = Closer.create();
        try {
            Reader reader = closer.register(openStream());
            Writer writer = closer.register(sink.openStream());
            return CharStreams.copy(reader, writer);
        } catch (Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }
    }


    public String read() throws IOException {
        Closer closer = Closer.create();
        try {
            Reader reader = closer.register(openStream());
            return CharStreams.toString(reader);
        } catch (Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }
    }


    @Nullable
    public String readFirstLine() throws IOException {
        Closer closer = Closer.create();
        try {
            BufferedReader reader = closer.register(openBufferedStream());
            return reader.readLine();
        } catch (Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }
    }


    public ImmutableList<String> readLines() throws IOException {
        Closer closer = Closer.create();
        try {
            BufferedReader reader = closer.register(openBufferedStream());
            List<String> result = Lists.newArrayList();
            String line;
            while ((line = reader.readLine()) != null) {
                result.add(line);
            }
            return ImmutableList.copyOf(result);
        } catch (Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }
    }


    @Beta
    public <T> T readLines(LineProcessor<T> processor) throws IOException {
        checkNotNull(processor);

        Closer closer = Closer.create();
        try {
            Reader reader = closer.register(openStream());
            return CharStreams.readLines(reader, processor);
        } catch (Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }
    }


    public boolean isEmpty() throws IOException {
        Optional<Long> lengthIfKnown = lengthIfKnown();
        if (lengthIfKnown.isPresent() && lengthIfKnown.get() == 0L) {
            return true;
        }
        Closer closer = Closer.create();
        try {
            Reader reader = closer.register(openStream());
            return reader.read() == -1;
        } catch (Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }
    }


    public static CharSource concat(Iterable<? extends CharSource> sources) {
        return new ConcatenatedCharSource(sources);
    }


    public static CharSource concat(Iterator<? extends CharSource> sources) {
        return concat(ImmutableList.copyOf(sources));
    }


    public static CharSource concat(CharSource... sources) {
        return concat(ImmutableList.copyOf(sources));
    }


    public static CharSource wrap(CharSequence charSequence) {
        return new CharSequenceCharSource(charSequence);
    }


    public static CharSource empty() {
        return EmptyCharSource.INSTANCE;
    }

    private static class CharSequenceCharSource extends CharSource {

        private static final Splitter LINE_SPLITTER
                = Splitter.on(Pattern.compile("\r\n|\n|\r"));

        private final CharSequence seq;

        protected CharSequenceCharSource(CharSequence seq) {
            this.seq = checkNotNull(seq);
        }

        @Override
        public Reader openStream() {
            return new CharSequenceReader(seq);
        }

        @Override
        public String read() {
            return seq.toString();
        }

        @Override
        public boolean isEmpty() {
            return seq.length() == 0;
        }

        @Override
        public long length() {
            return seq.length();
        }

        @Override
        public Optional<Long> lengthIfKnown() {
            return Optional.of((long) seq.length());
        }


        private Iterable<String> lines() {
            return new Iterable<String>() {
                @Override
                public Iterator<String> iterator() {
                    return new AbstractIterator<String>() {
                        Iterator<String> lines = LINE_SPLITTER.split(seq).iterator();

                        @Override
                        protected String computeNext() {
                            if (lines.hasNext()) {
                                String next = lines.next();
                                // skip last line if it's empty
                                if (lines.hasNext() || !next.isEmpty()) {
                                    return next;
                                }
                            }
                            return endOfData();
                        }
                    };
                }
            };
        }

        @Override
        public String readFirstLine() {
            Iterator<String> lines = lines().iterator();
            return lines.hasNext() ? lines.next() : null;
        }

        @Override
        public ImmutableList<String> readLines() {
            return ImmutableList.copyOf(lines());
        }

        @Override
        public <T> T readLines(LineProcessor<T> processor) throws IOException {
            for (String line : lines()) {
                if (!processor.processLine(line)) {
                    break;
                }
            }
            return processor.getResult();
        }

        @Override
        public String toString() {
            return "CharSource.wrap(" + truncate(seq, 30, "...") + ")";
        }


        private static String truncate(CharSequence seq, int maxLength, String truncationIndicator) {
            checkNotNull(seq);

            // length to truncate the sequence to, not including the truncation indicator
            int truncationLength = maxLength - truncationIndicator.length();

            // in this worst case, this allows a maxLength equal to the length of the truncationIndicator,
            // meaning that a string will be truncated to just the truncation indicator itself
            checkArgument(truncationLength >= 0,
                    "maxLength (%s) must be >= length of the truncation indicator (%s)",
                    maxLength, truncationIndicator.length());

            if (seq.length() <= maxLength) {
                String string = seq.toString();
                if (string.length() <= maxLength) {
                    return string;
                }
                // if the length of the toString() result was > maxLength for some reason, truncate that
                seq = string;
            }

            return new StringBuilder(maxLength)
                    .append(seq, 0, truncationLength)
                    .append(truncationIndicator)
                    .toString();
        }
    }

    private static final class EmptyCharSource extends CharSequenceCharSource {

        private static final EmptyCharSource INSTANCE = new EmptyCharSource();

        private EmptyCharSource() {
            super("");
        }

        @Override
        public String toString() {
            return "CharSource.empty()";
        }
    }

    private static final class ConcatenatedCharSource extends CharSource {

        private final Iterable<? extends CharSource> sources;

        ConcatenatedCharSource(Iterable<? extends CharSource> sources) {
            this.sources = checkNotNull(sources);
        }

        @Override
        public Reader openStream() throws IOException {
            return new MultiReader(sources.iterator());
        }

        @Override
        public boolean isEmpty() throws IOException {
            for (CharSource source : sources) {
                if (!source.isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public Optional<Long> lengthIfKnown() {
            long result = 0L;
            for (CharSource source : sources) {
                Optional<Long> lengthIfKnown = source.lengthIfKnown();
                if (!lengthIfKnown.isPresent()) {
                    return Optional.absent();
                }
                result += lengthIfKnown.get();
            }
            return Optional.of(result);
        }

        @Override
        public long length() throws IOException {
            long result = 0L;
            for (CharSource source : sources) {
                result += source.length();
            }
            return result;
        }

        @Override
        public String toString() {
            return "CharSource.concat(" + sources + ")";
        }
    }
}
