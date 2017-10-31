

package com.google.common.io;

import com.google.common.annotations.Beta;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkPositionIndex;


@Beta
public final class ByteStreams {
    private static final int BUF_SIZE = 8192;

    private static final int ZERO_COPY_CHUNK_SIZE = 512 * 1024;

    private ByteStreams() {
    }


    public static long copy(InputStream from, OutputStream to)
            throws IOException {
        checkNotNull(from);
        checkNotNull(to);
        byte[] buf = new byte[BUF_SIZE];
        long total = 0;
        while (true) {
            int r = from.read(buf);
            if (r == -1) {
                break;
            }
            to.write(buf, 0, r);
            total += r;
        }
        return total;
    }


    public static long copy(ReadableByteChannel from,
                            WritableByteChannel to) throws IOException {
        checkNotNull(from);
        checkNotNull(to);
        if (from instanceof FileChannel) {
            FileChannel sourceChannel = (FileChannel) from;
            long oldPosition = sourceChannel.position();
            long position = oldPosition;
            long copied;
            do {
                copied = sourceChannel.transferTo(position, ZERO_COPY_CHUNK_SIZE, to);
                position += copied;
                sourceChannel.position(position);
            } while (copied > 0 || position < sourceChannel.size());
            return position - oldPosition;
        }

        ByteBuffer buf = ByteBuffer.allocate(BUF_SIZE);
        long total = 0;
        while (from.read(buf) != -1) {
            buf.flip();
            while (buf.hasRemaining()) {
                total += to.write(buf);
            }
            buf.clear();
        }
        return total;
    }


    public static byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(in, out);
        return out.toByteArray();
    }


    static byte[] toByteArray(
            InputStream in, int expectedSize) throws IOException {
        byte[] bytes = new byte[expectedSize];
        int remaining = expectedSize;

        while (remaining > 0) {
            int off = expectedSize - remaining;
            int read = in.read(bytes, off, remaining);
            if (read == -1) {
                // end of stream before reading expectedSize bytes
                // just return the bytes read so far
                return Arrays.copyOf(bytes, off);
            }
            remaining -= read;
        }

        // bytes is now full
        int b = in.read();
        if (b == -1) {
            return bytes;
        }

        // the stream was longer, so read the rest normally
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.write(b); // write the byte we read when testing for end of stream
        copy(in, out);

        byte[] result = new byte[bytes.length + out.size()];
        System.arraycopy(bytes, 0, result, 0, bytes.length);
        out.writeTo(result, bytes.length);
        return result;
    }


    private static final class FastByteArrayOutputStream
            extends ByteArrayOutputStream {

        void writeTo(byte[] b, int off) {
            System.arraycopy(buf, 0, b, off, count);
        }
    }


    public static ByteArrayDataInput newDataInput(byte[] bytes) {
        return newDataInput(new ByteArrayInputStream(bytes));
    }


    public static ByteArrayDataInput newDataInput(byte[] bytes, int start) {
        checkPositionIndex(start, bytes.length);
        return newDataInput(
                new ByteArrayInputStream(bytes, start, bytes.length - start));
    }


    public static ByteArrayDataInput newDataInput(
            ByteArrayInputStream byteArrayInputStream) {
        return new ByteArrayDataInputStream(checkNotNull(byteArrayInputStream));
    }

    private static class ByteArrayDataInputStream implements ByteArrayDataInput {
        final DataInput input;

        ByteArrayDataInputStream(ByteArrayInputStream byteArrayInputStream) {
            this.input = new DataInputStream(byteArrayInputStream);
        }

        @Override
        public void readFully(byte b[]) {
            try {
                input.readFully(b);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void readFully(byte b[], int off, int len) {
            try {
                input.readFully(b, off, len);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public int skipBytes(int n) {
            try {
                return input.skipBytes(n);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public boolean readBoolean() {
            try {
                return input.readBoolean();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public byte readByte() {
            try {
                return input.readByte();
            } catch (EOFException e) {
                throw new IllegalStateException(e);
            } catch (IOException impossible) {
                throw new AssertionError(impossible);
            }
        }

        @Override
        public int readUnsignedByte() {
            try {
                return input.readUnsignedByte();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public short readShort() {
            try {
                return input.readShort();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public int readUnsignedShort() {
            try {
                return input.readUnsignedShort();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public char readChar() {
            try {
                return input.readChar();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public int readInt() {
            try {
                return input.readInt();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public long readLong() {
            try {
                return input.readLong();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public float readFloat() {
            try {
                return input.readFloat();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public double readDouble() {
            try {
                return input.readDouble();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public String readLine() {
            try {
                return input.readLine();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public String readUTF() {
            try {
                return input.readUTF();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }


    public static ByteArrayDataOutput newDataOutput() {
        return newDataOutput(new ByteArrayOutputStream());
    }


    public static ByteArrayDataOutput newDataOutput(int size) {
        // When called at high frequency, boxing size generates too much garbage,
        // so avoid doing that if we can.
        if (size < 0) {
            throw new IllegalArgumentException(String.format("Invalid size: %s", size));
        }
        return newDataOutput(new ByteArrayOutputStream(size));
    }


    public static ByteArrayDataOutput newDataOutput(
            ByteArrayOutputStream byteArrayOutputSteam) {
        return new ByteArrayDataOutputStream(checkNotNull(byteArrayOutputSteam));
    }

    @SuppressWarnings("deprecation") // for writeBytes
    private static class ByteArrayDataOutputStream
            implements ByteArrayDataOutput {

        final DataOutput output;
        final ByteArrayOutputStream byteArrayOutputSteam;

        ByteArrayDataOutputStream(ByteArrayOutputStream byteArrayOutputSteam) {
            this.byteArrayOutputSteam = byteArrayOutputSteam;
            output = new DataOutputStream(byteArrayOutputSteam);
        }

        @Override
        public void write(int b) {
            try {
                output.write(b);
            } catch (IOException impossible) {
                throw new AssertionError(impossible);
            }
        }

        @Override
        public void write(byte[] b) {
            try {
                output.write(b);
            } catch (IOException impossible) {
                throw new AssertionError(impossible);
            }
        }

        @Override
        public void write(byte[] b, int off, int len) {
            try {
                output.write(b, off, len);
            } catch (IOException impossible) {
                throw new AssertionError(impossible);
            }
        }

        @Override
        public void writeBoolean(boolean v) {
            try {
                output.writeBoolean(v);
            } catch (IOException impossible) {
                throw new AssertionError(impossible);
            }
        }

        @Override
        public void writeByte(int v) {
            try {
                output.writeByte(v);
            } catch (IOException impossible) {
                throw new AssertionError(impossible);
            }
        }

        @Override
        public void writeBytes(String s) {
            try {
                output.writeBytes(s);
            } catch (IOException impossible) {
                throw new AssertionError(impossible);
            }
        }

        @Override
        public void writeChar(int v) {
            try {
                output.writeChar(v);
            } catch (IOException impossible) {
                throw new AssertionError(impossible);
            }
        }

        @Override
        public void writeChars(String s) {
            try {
                output.writeChars(s);
            } catch (IOException impossible) {
                throw new AssertionError(impossible);
            }
        }

        @Override
        public void writeDouble(double v) {
            try {
                output.writeDouble(v);
            } catch (IOException impossible) {
                throw new AssertionError(impossible);
            }
        }

        @Override
        public void writeFloat(float v) {
            try {
                output.writeFloat(v);
            } catch (IOException impossible) {
                throw new AssertionError(impossible);
            }
        }

        @Override
        public void writeInt(int v) {
            try {
                output.writeInt(v);
            } catch (IOException impossible) {
                throw new AssertionError(impossible);
            }
        }

        @Override
        public void writeLong(long v) {
            try {
                output.writeLong(v);
            } catch (IOException impossible) {
                throw new AssertionError(impossible);
            }
        }

        @Override
        public void writeShort(int v) {
            try {
                output.writeShort(v);
            } catch (IOException impossible) {
                throw new AssertionError(impossible);
            }
        }

        @Override
        public void writeUTF(String s) {
            try {
                output.writeUTF(s);
            } catch (IOException impossible) {
                throw new AssertionError(impossible);
            }
        }

        @Override
        public byte[] toByteArray() {
            return byteArrayOutputSteam.toByteArray();
        }
    }

    private static final OutputStream NULL_OUTPUT_STREAM =
            new OutputStream() {

                @Override
                public void write(int b) {
                }

                @Override
                public void write(byte[] b) {
                    checkNotNull(b);
                }

                @Override
                public void write(byte[] b, int off, int len) {
                    checkNotNull(b);
                }

                @Override
                public String toString() {
                    return "ByteStreams.nullOutputStream()";
                }
            };


    public static OutputStream nullOutputStream() {
        return NULL_OUTPUT_STREAM;
    }


    public static InputStream limit(InputStream in, long limit) {
        return new LimitedInputStream(in, limit);
    }

    private static final class LimitedInputStream extends FilterInputStream {

        private long left;
        private long mark = -1;

        LimitedInputStream(InputStream in, long limit) {
            super(in);
            checkNotNull(in);
            checkArgument(limit >= 0, "limit must be non-negative");
            left = limit;
        }

        @Override
        public int available() throws IOException {
            return (int) Math.min(in.available(), left);
        }

        // it's okay to mark even if mark isn't supported, as reset won't work
        @Override
        public synchronized void mark(int readLimit) {
            in.mark(readLimit);
            mark = left;
        }

        @Override
        public int read() throws IOException {
            if (left == 0) {
                return -1;
            }

            int result = in.read();
            if (result != -1) {
                --left;
            }
            return result;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (left == 0) {
                return -1;
            }

            len = (int) Math.min(len, left);
            int result = in.read(b, off, len);
            if (result != -1) {
                left -= result;
            }
            return result;
        }

        @Override
        public synchronized void reset() throws IOException {
            if (!in.markSupported()) {
                throw new IOException("Mark not supported");
            }
            if (mark == -1) {
                throw new IOException("Mark not set");
            }

            in.reset();
            left = mark;
        }

        @Override
        public long skip(long n) throws IOException {
            n = Math.min(n, left);
            long skipped = in.skip(n);
            left -= skipped;
            return skipped;
        }
    }


    public static void readFully(InputStream in, byte[] b) throws IOException {
        readFully(in, b, 0, b.length);
    }


    public static void readFully(
            InputStream in, byte[] b, int off, int len) throws IOException {
        int read = read(in, b, off, len);
        if (read != len) {
            throw new EOFException("reached end of stream after reading "
                    + read + " bytes; " + len + " bytes expected");
        }
    }


    public static void skipFully(InputStream in, long n) throws IOException {
        long toSkip = n;
        while (n > 0) {
            long amt = in.skip(n);
            if (amt == 0) {
                // Force a blocking read to avoid infinite loop
                if (in.read() == -1) {
                    long skipped = toSkip - n;
                    throw new EOFException("reached end of stream after skipping "
                            + skipped + " bytes; " + toSkip + " bytes expected");
                }
                n--;
            } else {
                n -= amt;
            }
        }
    }


    public static <T> T readBytes(
            InputStream input, ByteProcessor<T> processor) throws IOException {
        checkNotNull(input);
        checkNotNull(processor);

        byte[] buf = new byte[BUF_SIZE];
        int read;
        do {
            read = input.read(buf);
        } while (read != -1 && processor.processBytes(buf, 0, read));
        return processor.getResult();
    }


    public static int read(InputStream in, byte[] b, int off, int len)
            throws IOException {
        checkNotNull(in);
        checkNotNull(b);
        if (len < 0) {
            throw new IndexOutOfBoundsException("len is negative");
        }
        int total = 0;
        while (total < len) {
            int result = in.read(b, off + total, len - total);
            if (result == -1) {
                break;
            }
            total += result;
        }
        return total;
    }
}
