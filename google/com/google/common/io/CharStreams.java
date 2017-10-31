

package com.google.common.io;

import com.google.common.annotations.Beta;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkPositionIndexes;


@Beta
public final class CharStreams {
    private static final int BUF_SIZE = 0x800; // 2K chars (4K bytes)

    private CharStreams() {
    }


    public static long copy(Readable from, Appendable to) throws IOException {
        checkNotNull(from);
        checkNotNull(to);
        CharBuffer buf = CharBuffer.allocate(BUF_SIZE);
        long total = 0;
        while (from.read(buf) != -1) {
            buf.flip();
            to.append(buf);
            total += buf.remaining();
            buf.clear();
        }
        return total;
    }


    public static String toString(Readable r) throws IOException {
        return toStringBuilder(r).toString();
    }


    private static StringBuilder toStringBuilder(Readable r) throws IOException {
        StringBuilder sb = new StringBuilder();
        copy(r, sb);
        return sb;
    }


    public static List<String> readLines(Readable r) throws IOException {
        List<String> result = new ArrayList<>();
        LineReader lineReader = new LineReader(r);
        String line;
        while ((line = lineReader.readLine()) != null) {
            result.add(line);
        }
        return result;
    }


    public static <T> T readLines(
            Readable readable, LineProcessor<T> processor) throws IOException {
        checkNotNull(readable);
        checkNotNull(processor);

        LineReader lineReader = new LineReader(readable);
        String line;
        while ((line = lineReader.readLine()) != null) {
            if (!processor.processLine(line)) {
                break;
            }
        }
        return processor.getResult();
    }


    public static void skipFully(Reader reader, long n) throws IOException {
        checkNotNull(reader);
        while (n > 0) {
            long amt = reader.skip(n);
            if (amt == 0) {
                throw new EOFException();
            }
            n -= amt;
        }
    }


    public static Writer nullWriter() {
        return NullWriter.INSTANCE;
    }

    private static final class NullWriter extends Writer {

        private static final NullWriter INSTANCE = new NullWriter();

        @Override
        public void write(int c) {
        }

        @Override
        public void write(char[] cbuf) {
            checkNotNull(cbuf);
        }

        @Override
        public void write(char[] cbuf, int off, int len) {
            checkPositionIndexes(off, off + len, cbuf.length);
        }

        @Override
        public void write(String str) {
            checkNotNull(str);
        }

        @Override
        public void write(String str, int off, int len) {
            checkPositionIndexes(off, off + len, str.length());
        }

        @Override
        public Writer append(CharSequence csq) {
            checkNotNull(csq);
            return this;
        }

        @Override
        public Writer append(CharSequence csq, int start, int end) {
            checkPositionIndexes(start, end, csq.length());
            return this;
        }

        @Override
        public Writer append(char c) {
            return this;
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        @Override
        public String toString() {
            return "CharStreams.nullWriter()";
        }
    }


    // TODO(cgdecker): Remove these once Input/OutputSupplier methods are removed

    static Reader asReader(final Readable readable) {
        checkNotNull(readable);
        if (readable instanceof Reader) {
            return (Reader) readable;
        }
        return new Reader() {
            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                return read(CharBuffer.wrap(cbuf, off, len));
            }

            @Override
            public int read(CharBuffer target) throws IOException {
                return readable.read(target);
            }

            @Override
            public void close() throws IOException {
                if (readable instanceof Closeable) {
                    ((Closeable) readable).close();
                }
            }
        };
    }
}
