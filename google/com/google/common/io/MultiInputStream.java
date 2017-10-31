

package com.google.common.io;

import android.support.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;


final class MultiInputStream extends InputStream {

    private Iterator<? extends ByteSource> it;
    private InputStream in;


    public MultiInputStream(
            Iterator<? extends ByteSource> it) throws IOException {
        this.it = checkNotNull(it);
        advance();
    }

    @Override
    public void close() throws IOException {
        if (in != null) {
            try {
                in.close();
            } finally {
                in = null;
            }
        }
    }


    private void advance() throws IOException {
        close();
        if (it.hasNext()) {
            in = it.next().openStream();
        }
    }

    @Override
    public int available() throws IOException {
        if (in == null) {
            return 0;
        }
        return in.available();
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public int read() throws IOException {
        if (in == null) {
            return -1;
        }
        int result = in.read();
        if (result == -1) {
            advance();
            return read();
        }
        return result;
    }

    @Override
    public int read(@Nullable byte[] b, int off, int len) throws IOException {
        if (in == null) {
            return -1;
        }
        int result = in.read(b, off, len);
        if (result == -1) {
            advance();
            return read(b, off, len);
        }
        return result;
    }

    @Override
    public long skip(long n) throws IOException {
        if (in == null || n <= 0) {
            return 0;
        }
        long result = in.skip(n);
        if (result != 0) {
            return result;
        }
        if (read() == -1) {
            return 0;
        }
        return 1 + in.skip(n - 1);
    }
}
