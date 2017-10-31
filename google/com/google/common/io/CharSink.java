

package com.google.common.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

import static com.google.common.base.Preconditions.checkNotNull;


public abstract class CharSink {


    protected CharSink() {
    }


    public abstract Writer openStream() throws IOException;


    public Writer openBufferedStream() throws IOException {
        Writer writer = openStream();
        return (writer instanceof BufferedWriter)
                ? (BufferedWriter) writer
                : new BufferedWriter(writer);
    }


    public void write(CharSequence charSequence) throws IOException {
        checkNotNull(charSequence);

        Closer closer = Closer.create();
        try {
            Writer out = closer.register(openStream());
            out.append(charSequence);
            out.flush(); // https://code.google.com/p/guava-libraries/issues/detail?id=1330
        } catch (Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }
    }


    public void writeLines(Iterable<? extends CharSequence> lines) throws IOException {
        writeLines(lines, System.getProperty("line.separator"));
    }


    public void writeLines(Iterable<? extends CharSequence> lines, String lineSeparator)
            throws IOException {
        checkNotNull(lines);
        checkNotNull(lineSeparator);

        Closer closer = Closer.create();
        try {
            Writer out = closer.register(openBufferedStream());
            for (CharSequence line : lines) {
                out.append(line).append(lineSeparator);
            }
            out.flush(); // https://code.google.com/p/guava-libraries/issues/detail?id=1330
        } catch (Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }
    }


    public long writeFrom(Readable readable) throws IOException {
        checkNotNull(readable);

        Closer closer = Closer.create();
        try {
            Writer out = closer.register(openStream());
            long written = CharStreams.copy(readable, out);
            out.flush(); // https://code.google.com/p/guava-libraries/issues/detail?id=1330
            return written;
        } catch (Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }
    }
}
