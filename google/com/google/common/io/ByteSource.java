

package com.google.common.io;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.Funnels;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;


public abstract class ByteSource {

    private static final int BUF_SIZE = 0x1000; // 4K


    protected ByteSource() {
    }


    public CharSource asCharSource(Charset charset) {
        return new AsCharSource(charset);
    }


    public abstract InputStream openStream() throws IOException;


    public InputStream openBufferedStream() throws IOException {
        InputStream in = openStream();
        return (in instanceof BufferedInputStream)
                ? (BufferedInputStream) in
                : new BufferedInputStream(in);
    }


    public ByteSource slice(long offset, long length) {
        return new SlicedByteSource(offset, length);
    }


    public boolean isEmpty() throws IOException {
        Optional<Long> sizeIfKnown = sizeIfKnown();
        if (sizeIfKnown.isPresent() && sizeIfKnown.get() == 0L) {
            return true;
        }
        Closer closer = Closer.create();
        try {
            InputStream in = closer.register(openStream());
            return in.read() == -1;
        } catch (Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }
    }


    @Beta
    public Optional<Long> sizeIfKnown() {
        return Optional.absent();
    }


    public long size() throws IOException {
        Optional<Long> sizeIfKnown = sizeIfKnown();
        if (sizeIfKnown.isPresent()) {
            return sizeIfKnown.get();
        }

        Closer closer = Closer.create();
        try {
            InputStream in = closer.register(openStream());
            return countBySkipping(in);
        } catch (IOException e) {
            // skip may not be supported... at any rate, try reading
        } finally {
            closer.close();
        }

        closer = Closer.create();
        try {
            InputStream in = closer.register(openStream());
            return countByReading(in);
        } catch (Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }
    }


    private long countBySkipping(InputStream in) throws IOException {
        long count = 0;
        while (true) {
            // don't try to skip more than available()
            // things may work really wrong with FileInputStream otherwise
            long skipped = in.skip(Math.min(in.available(), Integer.MAX_VALUE));
            if (skipped <= 0) {
                if (in.read() == -1) {
                    return count;
                } else if (count == 0 && in.available() == 0) {
                    // if available is still zero after reading a single byte, it
                    // will probably always be zero, so we should countByReading
                    throw new IOException();
                }
                count++;
            } else {
                count += skipped;
            }
        }
    }

    private static final byte[] countBuffer = new byte[BUF_SIZE];

    private long countByReading(InputStream in) throws IOException {
        long count = 0;
        long read;
        while ((read = in.read(countBuffer)) != -1) {
            count += read;
        }
        return count;
    }


    public long copyTo(OutputStream output) throws IOException {
        checkNotNull(output);

        Closer closer = Closer.create();
        try {
            InputStream in = closer.register(openStream());
            return ByteStreams.copy(in, output);
        } catch (Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }
    }


    public long copyTo(ByteSink sink) throws IOException {
        checkNotNull(sink);

        Closer closer = Closer.create();
        try {
            InputStream in = closer.register(openStream());
            OutputStream out = closer.register(sink.openStream());
            return ByteStreams.copy(in, out);
        } catch (Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }
    }


    public byte[] read() throws IOException {
        Closer closer = Closer.create();
        try {
            InputStream in = closer.register(openStream());
            return ByteStreams.toByteArray(in);
        } catch (Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }
    }


    @Beta
    public <T> T read(ByteProcessor<T> processor) throws IOException {
        checkNotNull(processor);

        Closer closer = Closer.create();
        try {
            InputStream in = closer.register(openStream());
            return ByteStreams.readBytes(in, processor);
        } catch (Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }
    }


    public HashCode hash(HashFunction hashFunction) throws IOException {
        Hasher hasher = hashFunction.newHasher();
        copyTo(Funnels.asOutputStream(hasher));
        return hasher.hash();
    }


    public boolean contentEquals(ByteSource other) throws IOException {
        checkNotNull(other);

        byte[] buf1 = new byte[BUF_SIZE];
        byte[] buf2 = new byte[BUF_SIZE];

        Closer closer = Closer.create();
        try {
            InputStream in1 = closer.register(openStream());
            InputStream in2 = closer.register(other.openStream());
            while (true) {
                int read1 = ByteStreams.read(in1, buf1, 0, BUF_SIZE);
                int read2 = ByteStreams.read(in2, buf2, 0, BUF_SIZE);
                if (read1 != read2 || !Arrays.equals(buf1, buf2)) {
                    return false;
                } else if (read1 != BUF_SIZE) {
                    return true;
                }
            }
        } catch (Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }
    }


    public static ByteSource concat(Iterable<? extends ByteSource> sources) {
        return new ConcatenatedByteSource(sources);
    }


    public static ByteSource concat(Iterator<? extends ByteSource> sources) {
        return concat(ImmutableList.copyOf(sources));
    }


    public static ByteSource concat(ByteSource... sources) {
        return concat(ImmutableList.copyOf(sources));
    }


    public static ByteSource wrap(byte[] b) {
        return new ByteArrayByteSource(b);
    }


    public static ByteSource empty() {
        return EmptyByteSource.INSTANCE;
    }


    private final class AsCharSource extends CharSource {

        private final Charset charset;

        private AsCharSource(Charset charset) {
            this.charset = checkNotNull(charset);
        }

        @Override
        public Reader openStream() throws IOException {
            return new InputStreamReader(ByteSource.this.openStream(), charset);
        }

        @Override
        public String toString() {
            return ByteSource.this.toString() + ".asCharSource(" + charset + ")";
        }
    }


    private final class SlicedByteSource extends ByteSource {

        final long offset;
        final long length;

        SlicedByteSource(long offset, long length) {
            checkArgument(offset >= 0, "offset (%s) may not be negative", offset);
            checkArgument(length >= 0, "length (%s) may not be negative", length);
            this.offset = offset;
            this.length = length;
        }

        @Override
        public InputStream openStream() throws IOException {
            return sliceStream(ByteSource.this.openStream());
        }

        @Override
        public InputStream openBufferedStream() throws IOException {
            return sliceStream(ByteSource.this.openBufferedStream());
        }

        private InputStream sliceStream(InputStream in) throws IOException {
            if (offset > 0) {
                try {
                    ByteStreams.skipFully(in, offset);
                } catch (Throwable e) {
                    Closer closer = Closer.create();
                    closer.register(in);
                    try {
                        throw closer.rethrow(e);
                    } finally {
                        closer.close();
                    }
                }
            }
            return ByteStreams.limit(in, length);
        }

        @Override
        public ByteSource slice(long offset, long length) {
            checkArgument(offset >= 0, "offset (%s) may not be negative", offset);
            checkArgument(length >= 0, "length (%s) may not be negative", length);
            long maxLength = this.length - offset;
            return ByteSource.this.slice(this.offset + offset, Math.min(length, maxLength));
        }

        @Override
        public boolean isEmpty() throws IOException {
            return length == 0 || super.isEmpty();
        }

        @Override
        public Optional<Long> sizeIfKnown() {
            Optional<Long> unslicedSize = ByteSource.this.sizeIfKnown();
            if (unslicedSize.isPresent()) {
                return Optional.of(Math.min(offset + length, unslicedSize.get()) - offset);
            }
            return Optional.absent();
        }

        @Override
        public String toString() {
            return ByteSource.this.toString() + ".slice(" + offset + ", " + length + ")";
        }
    }

    private static class ByteArrayByteSource extends ByteSource {

        final byte[] bytes;
        final int offset;
        final int length;

        ByteArrayByteSource(byte[] bytes) {
            this(bytes, 0, bytes.length);
        }

        // NOTE: Preconditions are enforced by slice, the only non-trivial caller.
        ByteArrayByteSource(byte[] bytes, int offset, int length) {
            this.bytes = bytes;
            this.offset = offset;
            this.length = length;
        }

        @Override
        public InputStream openStream() {
            return new ByteArrayInputStream(bytes, offset, length);
        }

        @Override
        public InputStream openBufferedStream() throws IOException {
            return openStream();
        }

        @Override
        public boolean isEmpty() {
            return length == 0;
        }

        @Override
        public long size() {
            return length;
        }

        @Override
        public Optional<Long> sizeIfKnown() {
            return Optional.of((long) length);
        }

        @Override
        public byte[] read() {
            return Arrays.copyOfRange(bytes, offset, offset + length);
        }

        @Override
        public long copyTo(OutputStream output) throws IOException {
            output.write(bytes, offset, length);
            return length;
        }

        @Override
        public <T> T read(ByteProcessor<T> processor) throws IOException {
            processor.processBytes(bytes, offset, length);
            return processor.getResult();
        }

        @Override
        public HashCode hash(HashFunction hashFunction) throws IOException {
            return hashFunction.hashBytes(bytes, offset, length);
        }

        @Override
        public ByteSource slice(long offset, long length) {
            checkArgument(offset >= 0, "offset (%s) may not be negative", offset);
            checkArgument(length >= 0, "length (%s) may not be negative", length);

            int newOffset = this.offset + (int) Math.min(this.length, offset);
            int endOffset = this.offset + (int) Math.min(this.length, offset + length);
            return new ByteArrayByteSource(bytes, newOffset, endOffset - newOffset);
        }

        @Override
        public String toString() {
            return "ByteSource.wrap("
                    + truncate(BaseEncoding.base16().encode(bytes, offset, length), 30, "...") + ")";
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

    private static final class EmptyByteSource extends ByteArrayByteSource {

        static final EmptyByteSource INSTANCE = new EmptyByteSource();

        EmptyByteSource() {
            super(new byte[0]);
        }

        @Override
        public CharSource asCharSource(Charset charset) {
            checkNotNull(charset);
            return CharSource.empty();
        }

        @Override
        public byte[] read() {
            return bytes; // length is 0, no need to clone
        }

        @Override
        public String toString() {
            return "ByteSource.empty()";
        }
    }

    private static final class ConcatenatedByteSource extends ByteSource {

        final Iterable<? extends ByteSource> sources;

        ConcatenatedByteSource(Iterable<? extends ByteSource> sources) {
            this.sources = checkNotNull(sources);
        }

        @Override
        public InputStream openStream() throws IOException {
            return new MultiInputStream(sources.iterator());
        }

        @Override
        public boolean isEmpty() throws IOException {
            for (ByteSource source : sources) {
                if (!source.isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public Optional<Long> sizeIfKnown() {
            long result = 0L;
            for (ByteSource source : sources) {
                Optional<Long> sizeIfKnown = source.sizeIfKnown();
                if (!sizeIfKnown.isPresent()) {
                    return Optional.absent();
                }
                result += sizeIfKnown.get();
            }
            return Optional.of(result);
        }

        @Override
        public long size() throws IOException {
            long result = 0L;
            for (ByteSource source : sources) {
                result += source.size();
            }
            return result;
        }

        @Override
        public String toString() {
            return "ByteSource.concat(" + sources + ")";
        }
    }
}
