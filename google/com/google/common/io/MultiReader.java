

package com.google.common.io;

import android.support.annotation.Nullable;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;


class MultiReader extends Reader {
    private final Iterator<? extends CharSource> it;
    private Reader current;

    MultiReader(Iterator<? extends CharSource> readers) throws IOException {
        this.it = readers;
        advance();
    }


    private void advance() throws IOException {
        close();
        if (it.hasNext()) {
            current = it.next().openStream();
        }
    }

    @Override
    public int read(@Nullable char cbuf[], int off, int len) throws IOException {
        if (current == null) {
            return -1;
        }
        int result = current.read(cbuf, off, len);
        if (result == -1) {
            advance();
            return read(cbuf, off, len);
        }
        return result;
    }

    @Override
    public long skip(long n) throws IOException {
        Preconditions.checkArgument(n >= 0, "n is negative");
        if (n > 0) {
            while (current != null) {
                long result = current.skip(n);
                if (result > 0) {
                    return result;
                }
                advance();
            }
        }
        return 0;
    }

    @Override
    public boolean ready() throws IOException {
        return (current != null) && current.ready();
    }

    @Override
    public void close() throws IOException {
        if (current != null) {
            try {
                current.close();
            } finally {
                current = null;
            }
        }
    }
}
