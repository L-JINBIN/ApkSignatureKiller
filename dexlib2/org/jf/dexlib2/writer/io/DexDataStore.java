package org.jf.dexlib2.writer.io;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface DexDataStore {
    @NonNull
    OutputStream outputAt(int offset);

    @NonNull
    InputStream readAt(int offset);

    void close() throws IOException;
}
