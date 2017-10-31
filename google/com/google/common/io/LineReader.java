

package com.google.common.io;

import com.google.common.annotations.Beta;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.LinkedList;
import java.util.Queue;

import static com.google.common.base.Preconditions.checkNotNull;


@Beta
public final class LineReader {
    private final Readable readable;
    private final Reader reader;
    private final char[] buf = new char[0x1000]; // 4K
    private final CharBuffer cbuf = CharBuffer.wrap(buf);

    private final Queue<String> lines = new LinkedList<>();
    private final LineBuffer lineBuf = new LineBuffer() {
        @Override
        protected void handleLine(String line, String end) {
            lines.add(line);
        }
    };


    public LineReader(Readable readable) {
        this.readable = checkNotNull(readable);
        this.reader = (readable instanceof Reader) ? (Reader) readable : null;
    }


    public String readLine() throws IOException {
        while (lines.peek() == null) {
            cbuf.clear();
            // The default implementation of Reader#read(CharBuffer) allocates a
            // temporary char[], so we call Reader#read(char[], int, int) instead.
            int read = (reader != null)
                    ? reader.read(buf, 0, buf.length)
                    : readable.read(cbuf);
            if (read == -1) {
                lineBuf.finish();
                break;
            }
            lineBuf.add(buf, 0, read);
        }
        return lines.poll();
    }
}
