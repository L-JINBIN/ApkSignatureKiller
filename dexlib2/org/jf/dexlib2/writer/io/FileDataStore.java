package org.jf.dexlib2.writer.io;

import android.support.annotation.NonNull;

import org.jf.util.RandomAccessFileInputStream;
import org.jf.util.RandomAccessFileOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class FileDataStore implements DexDataStore {
    private final RandomAccessFile raf;

    public FileDataStore(@NonNull File file) throws IOException {
        this.raf = new RandomAccessFile(file, "rw");
        this.raf.setLength(0);
    }

    @NonNull
    @Override
    public OutputStream outputAt(int offset) {
        return new RandomAccessFileOutputStream(raf, offset);
    }

    @NonNull
    @Override
    public InputStream readAt(int offset) {
        return new RandomAccessFileInputStream(raf, offset);
    }

    @Override
    public void close() throws IOException {
        raf.close();
    }
}
