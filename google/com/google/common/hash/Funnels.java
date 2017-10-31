

package com.google.common.hash;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;

import java.io.OutputStream;


@Beta

public final class Funnels {
    private Funnels() {
    }


    public static OutputStream asOutputStream(PrimitiveSink sink) {
        return new SinkAsStream(sink);
    }

    private static class SinkAsStream extends OutputStream {
        final PrimitiveSink sink;

        SinkAsStream(PrimitiveSink sink) {
            this.sink = Preconditions.checkNotNull(sink);
        }

        @Override
        public void write(int b) {
            sink.putByte((byte) b);
        }

        @Override
        public void write(byte[] bytes) {
            sink.putBytes(bytes);
        }

        @Override
        public void write(byte[] bytes, int off, int len) {
            sink.putBytes(bytes, off, len);
        }

        @Override
        public String toString() {
            return "Funnels.asOutputStream(" + sink + ")";
        }
    }
}
