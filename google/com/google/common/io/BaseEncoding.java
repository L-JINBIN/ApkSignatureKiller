

package com.google.common.io;

import android.support.annotation.Nullable;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Ascii;
import com.google.common.base.CharMatcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkPositionIndexes;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.math.IntMath.divide;
import static com.google.common.math.IntMath.log2;
import static java.math.RoundingMode.CEILING;
import static java.math.RoundingMode.FLOOR;
import static java.math.RoundingMode.UNNECESSARY;


@Beta
@GwtCompatible(emulated = true)
public abstract class BaseEncoding {
    // TODO(lowasser): consider making encodeTo(Appendable, byte[], int, int) public.


    public static final class DecodingException extends IOException {
        DecodingException(String message) {
            super(message);
        }

        DecodingException(Throwable cause) {
            super(cause);
        }
    }


    public String encode(byte[] bytes) {
        return encode(bytes, 0, bytes.length);
    }


    public final String encode(byte[] bytes, int off, int len) {
        checkPositionIndexes(off, off + len, bytes.length);
        StringBuilder result = new StringBuilder(maxEncodedSize(len));
        try {
            encodeTo(result, bytes, off, len);
        } catch (IOException impossible) {
            throw new AssertionError(impossible);
        }
        return result.toString();
    }


    @GwtIncompatible("Writer,OutputStream")
    public abstract OutputStream encodingStream(Writer writer);


    @GwtIncompatible("ByteSink,CharSink")
    public final ByteSink encodingSink(final CharSink encodedSink) {
        checkNotNull(encodedSink);
        return new ByteSink() {
            @Override
            public OutputStream openStream() throws IOException {
                return encodingStream(encodedSink.openStream());
            }
        };
    }

    // TODO(lowasser): document the extent of leniency, probably after adding ignore(CharMatcher)

    private static byte[] extract(byte[] result, int length) {
        if (length == result.length) {
            return result;
        } else {
            byte[] trunc = new byte[length];
            System.arraycopy(result, 0, trunc, 0, length);
            return trunc;
        }
    }


    public final byte[] decode(CharSequence chars) {
        try {
            return decodeChecked(chars);
        } catch (DecodingException badInput) {
            throw new IllegalArgumentException(badInput);
        }
    }


    final byte[] decodeChecked(CharSequence chars) throws DecodingException {
        chars = padding().trimTrailingFrom(chars);
        byte[] tmp = new byte[maxDecodedSize(chars.length())];
        int len = decodeTo(tmp, chars);
        return extract(tmp, len);
    }


    @GwtIncompatible("Reader,InputStream")
    public abstract InputStream decodingStream(Reader reader);


    @GwtIncompatible("ByteSource,CharSource")
    public final ByteSource decodingSource(final CharSource encodedSource) {
        checkNotNull(encodedSource);
        return new ByteSource() {
            @Override
            public InputStream openStream() throws IOException {
                return decodingStream(encodedSource.openStream());
            }
        };
    }

    // Implementations for encoding/decoding

    abstract int maxEncodedSize(int bytes);

    abstract void encodeTo(Appendable target, byte[] bytes, int off, int len) throws IOException;

    abstract int maxDecodedSize(int chars);

    abstract int decodeTo(byte[] target, CharSequence chars) throws DecodingException;

    abstract CharMatcher padding();

    // Modified encoding generators


    public abstract BaseEncoding omitPadding();


    public abstract BaseEncoding withPadChar(char padChar);


    public abstract BaseEncoding withSeparator(String separator, int n);


    public abstract BaseEncoding upperCase();


    public abstract BaseEncoding lowerCase();


    private static final BaseEncoding BASE16 = new Base16Encoding("base16()", "0123456789ABCDEF");


    public static BaseEncoding base16() {
        return BASE16;
    }

    private static final class Alphabet extends CharMatcher {
        private final String name;
        // this is meant to be immutable -- don't modify it!
        private final char[] chars;
        final int mask;
        final int bitsPerChar;
        final int charsPerChunk;
        final int bytesPerChunk;
        private final byte[] decodabet;
        private final boolean[] validPadding;

        Alphabet(String name, char[] chars) {
            this.name = checkNotNull(name);
            this.chars = checkNotNull(chars);
            try {
                this.bitsPerChar = log2(chars.length, UNNECESSARY);
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("Illegal alphabet length " + chars.length, e);
            }


            int gcd = Math.min(8, Integer.lowestOneBit(bitsPerChar));
            this.charsPerChunk = 8 / gcd;
            this.bytesPerChunk = bitsPerChar / gcd;

            this.mask = chars.length - 1;

            byte[] decodabet = new byte[Ascii.MAX + 1];
            Arrays.fill(decodabet, (byte) -1);
            for (int i = 0; i < chars.length; i++) {
                char c = chars[i];
                checkArgument(CharMatcher.ASCII.matches(c), "Non-ASCII character: %s", c);
                checkArgument(decodabet[c] == -1, "Duplicate character: %s", c);
                decodabet[c] = (byte) i;
            }
            this.decodabet = decodabet;

            boolean[] validPadding = new boolean[charsPerChunk];
            for (int i = 0; i < bytesPerChunk; i++) {
                validPadding[divide(i * 8, bitsPerChar, CEILING)] = true;
            }
            this.validPadding = validPadding;
        }

        char encode(int bits) {
            return chars[bits];
        }

        boolean isValidPaddingStartPosition(int index) {
            return validPadding[index % charsPerChunk];
        }

        int decode(char ch) throws DecodingException {
            if (ch > Ascii.MAX || decodabet[ch] == -1) {
                throw new DecodingException("Unrecognized character: "
                        + (CharMatcher.INVISIBLE.matches(ch) ? "0x" + Integer.toHexString(ch) : ch));
            }
            return decodabet[ch];
        }

        private boolean hasLowerCase() {
            for (char c : chars) {
                if (Ascii.isLowerCase(c)) {
                    return true;
                }
            }
            return false;
        }

        private boolean hasUpperCase() {
            for (char c : chars) {
                if (Ascii.isUpperCase(c)) {
                    return true;
                }
            }
            return false;
        }

        Alphabet upperCase() {
            if (!hasLowerCase()) {
                return this;
            } else {
                checkState(!hasUpperCase(), "Cannot call upperCase() on a mixed-case alphabet");
                char[] upperCased = new char[chars.length];
                for (int i = 0; i < chars.length; i++) {
                    upperCased[i] = Ascii.toUpperCase(chars[i]);
                }
                return new Alphabet(name + ".upperCase()", upperCased);
            }
        }

        Alphabet lowerCase() {
            if (!hasUpperCase()) {
                return this;
            } else {
                checkState(!hasLowerCase(), "Cannot call lowerCase() on a mixed-case alphabet");
                char[] lowerCased = new char[chars.length];
                for (int i = 0; i < chars.length; i++) {
                    lowerCased[i] = Ascii.toLowerCase(chars[i]);
                }
                return new Alphabet(name + ".lowerCase()", lowerCased);
            }
        }

        @Override
        public boolean matches(char c) {
            return CharMatcher.ASCII.matches(c) && decodabet[c] != -1;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static class StandardBaseEncoding extends BaseEncoding {
        // TODO(lowasser): provide a useful toString
        final Alphabet alphabet;

        @Nullable
        final Character paddingChar;

        StandardBaseEncoding(String name, String alphabetChars, @Nullable Character paddingChar) {
            this(new Alphabet(name, alphabetChars.toCharArray()), paddingChar);
        }

        StandardBaseEncoding(Alphabet alphabet, @Nullable Character paddingChar) {
            this.alphabet = checkNotNull(alphabet);
            checkArgument(paddingChar == null || !alphabet.matches(paddingChar),
                    "Padding character %s was already in alphabet", paddingChar);
            this.paddingChar = paddingChar;
        }

        @Override
        CharMatcher padding() {
            return (paddingChar == null) ? CharMatcher.NONE : CharMatcher.is(paddingChar.charValue());
        }

        @Override
        int maxEncodedSize(int bytes) {
            return alphabet.charsPerChunk * divide(bytes, alphabet.bytesPerChunk, CEILING);
        }

        @GwtIncompatible("Writer,OutputStream")
        @Override
        public OutputStream encodingStream(final Writer out) {
            checkNotNull(out);
            return new OutputStream() {
                int bitBuffer = 0;
                int bitBufferLength = 0;
                int writtenChars = 0;

                @Override
                public void write(int b) throws IOException {
                    bitBuffer <<= 8;
                    bitBuffer |= b & 0xFF;
                    bitBufferLength += 8;
                    while (bitBufferLength >= alphabet.bitsPerChar) {
                        int charIndex = (bitBuffer >> (bitBufferLength - alphabet.bitsPerChar))
                                & alphabet.mask;
                        out.write(alphabet.encode(charIndex));
                        writtenChars++;
                        bitBufferLength -= alphabet.bitsPerChar;
                    }
                }

                @Override
                public void flush() throws IOException {
                    out.flush();
                }

                @Override
                public void close() throws IOException {
                    if (bitBufferLength > 0) {
                        int charIndex = (bitBuffer << (alphabet.bitsPerChar - bitBufferLength))
                                & alphabet.mask;
                        out.write(alphabet.encode(charIndex));
                        writtenChars++;
                        if (paddingChar != null) {
                            while (writtenChars % alphabet.charsPerChunk != 0) {
                                out.write(paddingChar.charValue());
                                writtenChars++;
                            }
                        }
                    }
                    out.close();
                }
            };
        }

        @Override
        void encodeTo(Appendable target, byte[] bytes, int off, int len) throws IOException {
            checkNotNull(target);
            checkPositionIndexes(off, off + len, bytes.length);
            for (int i = 0; i < len; i += alphabet.bytesPerChunk) {
                encodeChunkTo(target, bytes, off + i, Math.min(alphabet.bytesPerChunk, len - i));
            }
        }

        void encodeChunkTo(Appendable target, byte[] bytes, int off, int len)
                throws IOException {
            checkNotNull(target);
            checkPositionIndexes(off, off + len, bytes.length);
            checkArgument(len <= alphabet.bytesPerChunk);
            long bitBuffer = 0;
            for (int i = 0; i < len; ++i) {
                bitBuffer |= bytes[off + i] & 0xFF;
                bitBuffer <<= 8; // Add additional zero byte in the end.
            }
            // Position of first character is length of bitBuffer minus bitsPerChar.
            final int bitOffset = (len + 1) * 8 - alphabet.bitsPerChar;
            int bitsProcessed = 0;
            while (bitsProcessed < len * 8) {
                int charIndex = (int) (bitBuffer >>> (bitOffset - bitsProcessed)) & alphabet.mask;
                target.append(alphabet.encode(charIndex));
                bitsProcessed += alphabet.bitsPerChar;
            }
            if (paddingChar != null) {
                while (bitsProcessed < alphabet.bytesPerChunk * 8) {
                    target.append(paddingChar.charValue());
                    bitsProcessed += alphabet.bitsPerChar;
                }
            }
        }

        @Override
        int maxDecodedSize(int chars) {
            return (int) ((alphabet.bitsPerChar * (long) chars + 7L) / 8L);
        }

        @Override
        int decodeTo(byte[] target, CharSequence chars) throws DecodingException {
            checkNotNull(target);
            chars = padding().trimTrailingFrom(chars);
            if (!alphabet.isValidPaddingStartPosition(chars.length())) {
                throw new DecodingException("Invalid input length " + chars.length());
            }
            int bytesWritten = 0;
            for (int charIdx = 0; charIdx < chars.length(); charIdx += alphabet.charsPerChunk) {
                long chunk = 0;
                int charsProcessed = 0;
                for (int i = 0; i < alphabet.charsPerChunk; i++) {
                    chunk <<= alphabet.bitsPerChar;
                    if (charIdx + i < chars.length()) {
                        chunk |= alphabet.decode(chars.charAt(charIdx + charsProcessed++));
                    }
                }
                final int minOffset = alphabet.bytesPerChunk * 8 - charsProcessed * alphabet.bitsPerChar;
                for (int offset = (alphabet.bytesPerChunk - 1) * 8; offset >= minOffset; offset -= 8) {
                    target[bytesWritten++] = (byte) ((chunk >>> offset) & 0xFF);
                }
            }
            return bytesWritten;
        }

        @GwtIncompatible("Reader,InputStream")
        @Override
        public InputStream decodingStream(final Reader reader) {
            checkNotNull(reader);
            return new InputStream() {
                int bitBuffer = 0;
                int bitBufferLength = 0;
                int readChars = 0;
                boolean hitPadding = false;
                final CharMatcher paddingMatcher = padding();

                @Override
                public int read() throws IOException {
                    while (true) {
                        int readChar = reader.read();
                        if (readChar == -1) {
                            if (!hitPadding && !alphabet.isValidPaddingStartPosition(readChars)) {
                                throw new DecodingException("Invalid input length " + readChars);
                            }
                            return -1;
                        }
                        readChars++;
                        char ch = (char) readChar;
                        if (paddingMatcher.matches(ch)) {
                            if (!hitPadding
                                    && (readChars == 1 || !alphabet.isValidPaddingStartPosition(readChars - 1))) {
                                throw new DecodingException("Padding cannot start at index " + readChars);
                            }
                            hitPadding = true;
                        } else if (hitPadding) {
                            throw new DecodingException(
                                    "Expected padding character but found '" + ch + "' at index " + readChars);
                        } else {
                            bitBuffer <<= alphabet.bitsPerChar;
                            bitBuffer |= alphabet.decode(ch);
                            bitBufferLength += alphabet.bitsPerChar;

                            if (bitBufferLength >= 8) {
                                bitBufferLength -= 8;
                                return (bitBuffer >> bitBufferLength) & 0xFF;
                            }
                        }
                    }
                }

                @Override
                public void close() throws IOException {
                    reader.close();
                }
            };
        }

        @Override
        public BaseEncoding omitPadding() {
            return (paddingChar == null) ? this : newInstance(alphabet, null);
        }

        @Override
        public BaseEncoding withPadChar(char padChar) {
            if (8 % alphabet.bitsPerChar == 0 ||
                    (paddingChar != null && paddingChar.charValue() == padChar)) {
                return this;
            } else {
                return newInstance(alphabet, padChar);
            }
        }

        @Override
        public BaseEncoding withSeparator(String separator, int afterEveryChars) {
            checkArgument(padding().or(alphabet).matchesNoneOf(separator),
                    "Separator (%s) cannot contain alphabet or padding characters", separator);
            return new SeparatedBaseEncoding(this, separator, afterEveryChars);
        }

        private transient BaseEncoding upperCase;
        private transient BaseEncoding lowerCase;

        @Override
        public BaseEncoding upperCase() {
            BaseEncoding result = upperCase;
            if (result == null) {
                Alphabet upper = alphabet.upperCase();
                result = upperCase =
                        (upper == alphabet) ? this : newInstance(upper, paddingChar);
            }
            return result;
        }

        @Override
        public BaseEncoding lowerCase() {
            BaseEncoding result = lowerCase;
            if (result == null) {
                Alphabet lower = alphabet.lowerCase();
                result = lowerCase =
                        (lower == alphabet) ? this : newInstance(lower, paddingChar);
            }
            return result;
        }

        BaseEncoding newInstance(Alphabet alphabet, @Nullable Character paddingChar) {
            return new StandardBaseEncoding(alphabet, paddingChar);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("BaseEncoding.");
            builder.append(alphabet.toString());
            if (8 % alphabet.bitsPerChar != 0) {
                if (paddingChar == null) {
                    builder.append(".omitPadding()");
                } else {
                    builder.append(".withPadChar(").append(paddingChar).append(')');
                }
            }
            return builder.toString();
        }
    }

    static final class Base16Encoding extends StandardBaseEncoding {
        final char[] encoding = new char[512];

        Base16Encoding(String name, String alphabetChars) {
            this(new Alphabet(name, alphabetChars.toCharArray()));
        }

        private Base16Encoding(Alphabet alphabet) {
            super(alphabet, null);
            checkArgument(alphabet.chars.length == 16);
            for (int i = 0; i < 256; ++i) {
                encoding[i] = alphabet.encode(i >>> 4);
                encoding[i | 0x100] = alphabet.encode(i & 0xF);
            }
        }

        @Override
        void encodeTo(Appendable target, byte[] bytes, int off, int len) throws IOException {
            checkNotNull(target);
            checkPositionIndexes(off, off + len, bytes.length);
            for (int i = 0; i < len; ++i) {
                int b = bytes[off + i] & 0xFF;
                target.append(encoding[b]);
                target.append(encoding[b | 0x100]);
            }
        }

        @Override
        int decodeTo(byte[] target, CharSequence chars) throws DecodingException {
            checkNotNull(target);
            if (chars.length() % 2 == 1) {
                throw new DecodingException("Invalid input length " + chars.length());
            }
            int bytesWritten = 0;
            for (int i = 0; i < chars.length(); i += 2) {
                int decoded = alphabet.decode(chars.charAt(i)) << 4 | alphabet.decode(chars.charAt(i + 1));
                target[bytesWritten++] = (byte) decoded;
            }
            return bytesWritten;
        }

        @Override
        BaseEncoding newInstance(Alphabet alphabet, @Nullable Character paddingChar) {
            return new Base16Encoding(alphabet);
        }
    }

    static final class Base64Encoding extends StandardBaseEncoding {
        Base64Encoding(String name, String alphabetChars, @Nullable Character paddingChar) {
            this(new Alphabet(name, alphabetChars.toCharArray()), paddingChar);
        }

        private Base64Encoding(Alphabet alphabet, @Nullable Character paddingChar) {
            super(alphabet, paddingChar);
            checkArgument(alphabet.chars.length == 64);
        }

        @Override
        void encodeTo(Appendable target, byte[] bytes, int off, int len) throws IOException {
            checkNotNull(target);
            checkPositionIndexes(off, off + len, bytes.length);
            int i = off;
            for (int remaining = len; remaining >= 3; remaining -= 3) {
                int chunk = (bytes[i++] & 0xFF) << 16 | (bytes[i++] & 0xFF) << 8 | bytes[i++] & 0xFF;
                target.append(alphabet.encode(chunk >>> 18));
                target.append(alphabet.encode((chunk >>> 12) & 0x3F));
                target.append(alphabet.encode((chunk >>> 6) & 0x3F));
                target.append(alphabet.encode(chunk & 0x3F));
            }
            if (i < off + len) {
                encodeChunkTo(target, bytes, i, off + len - i);
            }
        }

        @Override
        int decodeTo(byte[] target, CharSequence chars) throws DecodingException {
            checkNotNull(target);
            chars = padding().trimTrailingFrom(chars);
            if (!alphabet.isValidPaddingStartPosition(chars.length())) {
                throw new DecodingException("Invalid input length " + chars.length());
            }
            int bytesWritten = 0;
            for (int i = 0; i < chars.length(); ) {
                int chunk = alphabet.decode(chars.charAt(i++)) << 18;
                chunk |= alphabet.decode(chars.charAt(i++)) << 12;
                target[bytesWritten++] = (byte) (chunk >>> 16);
                if (i < chars.length()) {
                    chunk |= alphabet.decode(chars.charAt(i++)) << 6;
                    target[bytesWritten++] = (byte) ((chunk >>> 8) & 0xFF);
                    if (i < chars.length()) {
                        chunk |= alphabet.decode(chars.charAt(i++));
                        target[bytesWritten++] = (byte) (chunk & 0xFF);
                    }
                }
            }
            return bytesWritten;
        }

        @Override
        BaseEncoding newInstance(Alphabet alphabet, @Nullable Character paddingChar) {
            return new Base64Encoding(alphabet, paddingChar);
        }
    }

    @GwtIncompatible("Reader")
    static Reader ignoringReader(final Reader delegate, final CharMatcher toIgnore) {
        checkNotNull(delegate);
        checkNotNull(toIgnore);
        return new Reader() {
            @Override
            public int read() throws IOException {
                int readChar;
                do {
                    readChar = delegate.read();
                } while (readChar != -1 && toIgnore.matches((char) readChar));
                return readChar;
            }

            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void close() throws IOException {
                delegate.close();
            }
        };
    }

    static Appendable separatingAppendable(
            final Appendable delegate, final String separator, final int afterEveryChars) {
        checkNotNull(delegate);
        checkNotNull(separator);
        checkArgument(afterEveryChars > 0);
        return new Appendable() {
            int charsUntilSeparator = afterEveryChars;

            @Override
            public Appendable append(char c) throws IOException {
                if (charsUntilSeparator == 0) {
                    delegate.append(separator);
                    charsUntilSeparator = afterEveryChars;
                }
                delegate.append(c);
                charsUntilSeparator--;
                return this;
            }

            @Override
            public Appendable append(CharSequence chars, int off, int len) throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public Appendable append(CharSequence chars) throws IOException {
                throw new UnsupportedOperationException();
            }
        };
    }

    @GwtIncompatible("Writer")
    static Writer separatingWriter(
            final Writer delegate, final String separator, final int afterEveryChars) {
        final Appendable seperatingAppendable =
                separatingAppendable(delegate, separator, afterEveryChars);
        return new Writer() {
            @Override
            public void write(int c) throws IOException {
                seperatingAppendable.append((char) c);
            }

            @Override
            public void write(char[] chars, int off, int len) throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void flush() throws IOException {
                delegate.flush();
            }

            @Override
            public void close() throws IOException {
                delegate.close();
            }
        };
    }

    static final class SeparatedBaseEncoding extends BaseEncoding {
        private final BaseEncoding delegate;
        private final String separator;
        private final int afterEveryChars;
        private final CharMatcher separatorChars;

        SeparatedBaseEncoding(BaseEncoding delegate, String separator, int afterEveryChars) {
            this.delegate = checkNotNull(delegate);
            this.separator = checkNotNull(separator);
            this.afterEveryChars = afterEveryChars;
            checkArgument(
                    afterEveryChars > 0, "Cannot add a separator after every %s chars", afterEveryChars);
            this.separatorChars = CharMatcher.anyOf(separator).precomputed();
        }

        @Override
        CharMatcher padding() {
            return delegate.padding();
        }

        @Override
        int maxEncodedSize(int bytes) {
            int unseparatedSize = delegate.maxEncodedSize(bytes);
            return unseparatedSize + separator.length()
                    * divide(Math.max(0, unseparatedSize - 1), afterEveryChars, FLOOR);
        }

        @GwtIncompatible("Writer,OutputStream")
        @Override
        public OutputStream encodingStream(final Writer output) {
            return delegate.encodingStream(separatingWriter(output, separator, afterEveryChars));
        }

        @Override
        void encodeTo(Appendable target, byte[] bytes, int off, int len) throws IOException {
            delegate.encodeTo(separatingAppendable(target, separator, afterEveryChars), bytes, off, len);
        }

        @Override
        int maxDecodedSize(int chars) {
            return delegate.maxDecodedSize(chars);
        }

        @Override
        int decodeTo(byte[] target, CharSequence chars) throws DecodingException {
            return delegate.decodeTo(target, separatorChars.removeFrom(chars));
        }

        @GwtIncompatible("Reader,InputStream")
        @Override
        public InputStream decodingStream(final Reader reader) {
            return delegate.decodingStream(ignoringReader(reader, separatorChars));
        }

        @Override
        public BaseEncoding omitPadding() {
            return delegate.omitPadding().withSeparator(separator, afterEveryChars);
        }

        @Override
        public BaseEncoding withPadChar(char padChar) {
            return delegate.withPadChar(padChar).withSeparator(separator, afterEveryChars);
        }

        @Override
        public BaseEncoding withSeparator(String separator, int afterEveryChars) {
            throw new UnsupportedOperationException("Already have a separator");
        }

        @Override
        public BaseEncoding upperCase() {
            return delegate.upperCase().withSeparator(separator, afterEveryChars);
        }

        @Override
        public BaseEncoding lowerCase() {
            return delegate.lowerCase().withSeparator(separator, afterEveryChars);
        }

        @Override
        public String toString() {
            return delegate.toString() +
                    ".withSeparator(\"" + separator + "\", " + afterEveryChars + ")";
        }
    }
}
