

package com.google.common.io;

import java.io.IOException;


abstract class LineBuffer {

    private StringBuilder line = new StringBuilder();

    private boolean sawReturn;


    protected void add(char[] cbuf, int off, int len) throws IOException {
        int pos = off;
        if (sawReturn && len > 0) {
            // Last call to add ended with a CR; we can handle the line now.
            if (finishLine(cbuf[pos] == '\n')) {
                pos++;
            }
        }

        int start = pos;
        for (int end = off + len; pos < end; pos++) {
            switch (cbuf[pos]) {
                case '\r':
                    line.append(cbuf, start, pos - start);
                    sawReturn = true;
                    if (pos + 1 < end) {
                        if (finishLine(cbuf[pos + 1] == '\n')) {
                            pos++;
                        }
                    }
                    start = pos + 1;
                    break;

                case '\n':
                    line.append(cbuf, start, pos - start);
                    finishLine(true);
                    start = pos + 1;
                    break;

                default:
                    // do nothing
            }
        }
        line.append(cbuf, start, off + len - start);
    }


    private boolean finishLine(boolean sawNewline) throws IOException {
        handleLine(line.toString(), sawReturn
                ? (sawNewline ? "\r\n" : "\r")
                : (sawNewline ? "\n" : ""));
        line = new StringBuilder();
        sawReturn = false;
        return sawNewline;
    }


    protected void finish() throws IOException {
        if (sawReturn || line.length() > 0) {
            finishLine(false);
        }
    }


    protected abstract void handleLine(String line, String end)
            throws IOException;
}
