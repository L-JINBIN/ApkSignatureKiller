

package com.google.common.io;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkPositionIndexes;


// TODO(cgdecker): make this public? as a type, or a method in CharStreams?
final class CharSequenceReader extends Reader {

    private CharSequence seq;
    private int pos;
    private int mark;


    public CharSequenceReader(CharSequence seq) {
        this.seq = checkNotNull(seq);
    }

    private void checkOpen() throws IOException {
        if (seq == null) {
            throw new IOException("reader closed");
        }
    }

    private boolean hasRemaining() {
        return remaining() > 0;
    }

    private int remaining() {
        return seq.length() - pos;
    }

    @Override
    public synchronized int read(CharBuffer target) throws IOException {
        checkNotNull(target);
        checkOpen();
        if (!hasRemaining()) {
            return -1;
        }
        int charsToRead = Math.min(target.remaining(), remaining());
        for (int i = 0; i < charsToRead; i++) {
            target.put(seq.charAt(pos++));
        }
        return charsToRead;
    }

    @Override
    public synchronized int read() throws IOException {
        checkOpen();
        return hasRemaining() ? seq.charAt(pos++) : -1;
    }

    @Override
    public synchronized int read(char[] cbuf, int off, int len) throws IOException {
        checkPositionIndexes(off, off + len, cbuf.length);
        checkOpen();
        if (!hasRemaining()) {
            return -1;
        }
        int charsToRead = Math.min(len, remaining());
        for (int i = 0; i < charsToRead; i++) {
            cbuf[off + i] = seq.charAt(pos++);
        }
        return charsToRead;
    }

    @Override
    public synchronized long skip(long n) throws IOException {
        checkArgument(n >= 0, "n (%s) may not be negative", n);
        checkOpen();
        int charsToSkip = (int) Math.min(remaining(), n); // safe because remaining is an int
        pos += charsToSkip;
        return charsToSkip;
    }

    @Override
    public synchronized boolean ready() throws IOException {
        checkOpen();
        return true;
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public synchronized void mark(int readAheadLimit) throws IOException {
        checkArgument(readAheadLimit >= 0, "readAheadLimit (%s) may not be negative", readAheadLimit);
        checkOpen();
        mark = pos;
    }

    @Override
    public synchronized void reset() throws IOException {
        checkOpen();
        pos = mark;
    }

    @Override
    public synchronized void close() throws IOException {
        seq = null;
    }
}
